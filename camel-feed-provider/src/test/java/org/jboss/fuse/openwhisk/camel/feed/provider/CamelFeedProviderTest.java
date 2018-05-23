package org.jboss.fuse.openwhisk.camel.feed.provider;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.camel.CamelContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;
import org.jboss.fuse.openwhisk.camel.feed.provider.service.CamelProviderConfiguration;
import org.jboss.fuse.openwhisk.camel.feed.provider.service.ProviderController;
import org.jboss.fuse.openwhisk.camel.feed.provider.db.CouchDBClient;
import org.jboss.fuse.openwhisk.camel.feed.provider.db.DBAlreadyExistsException;
import org.jboss.fuse.openwhisk.camel.feed.provider.db.DBDoesNotExistsException;
import org.jboss.fuse.openwhisk.camel.feed.provider.db.DocumentNotFoundException;
import org.jboss.fuse.openwhisk.camel.feed.provider.service.OpenWhiskAPIService;
import org.jboss.fuse.openwhisk.camel.feed.provider.service.TriggerDataService;

@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@Import(CamelFeedProviderTest.TestConfig.class)
@ConfigurationProperties(prefix = "openwhisk")
public class CamelFeedProviderTest {

    @Autowired
    ProviderController controller;

    @Autowired
    CouchDBClient couchDBClient;

    @Test
    public void contextLoads() throws InterruptedException {
        Map<String, Object> data = new HashMap<>();
        data.put("triggerName", "/_/test_trigger");
        data.put("routeUrl", "timer://foo?fixedRate=true&period=1000");
        data.put("authKey", "xxxx:yyyy");
        controller.addFeedToTrigger(data);

        Thread.sleep(2000);
    }

    @Configuration
    static class TestConfig extends CamelProviderConfiguration {

        @Bean
        public static PropertySourcesPlaceholderConfigurer propertyConfigInDev() {
            PropertySourcesPlaceholderConfigurer p = new PropertySourcesPlaceholderConfigurer();
            YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
            yaml.setResources(new ClassPathResource("application-test.yaml"));
            p.setProperties(yaml.getObject());
            return p;
        }

        @Bean
        public ProviderController controller(OpenWhiskAPIService openWhiskAPIService,
                                             TriggerDataService triggerDataService,
                                             CamelContext camelContext) {
            return new ProviderController(openWhiskAPIService, triggerDataService, camelContext);
        }

        @Bean
        public OpenWhiskAPIService openWhiskAPIService(TriggerDataService triggerDataService,
                                                       RestTemplate restTemplate) {
            return new OpenWhiskAPIService(triggerDataService, restTemplate);
        }

        @Bean
        public TriggerDataService triggerDataService(CouchDBClient client, Gson gson) {
            return new TriggerDataService(client, gson);
        }

        @Override
        public CouchDBClient couchDBClient() {
            return new CouchDBClient() {
                Map<String, Map<String, JsonObject>> dbs = new HashMap<>();

                @Override
                public JsonObject createDB(String db) throws DBAlreadyExistsException {
                    if (dbs.putIfAbsent(db, new HashMap<>()) != null) {
                        throw new DBAlreadyExistsException(null, null);
                    }
                    JsonObject r = new JsonObject();
                    r.addProperty("ok", true);
                    return r;
                }

                @Override
                public JsonObject deleteDB(String db) throws DBDoesNotExistsException {
                    if (dbs.remove(db) == null) {
                        throw new DBDoesNotExistsException(null, null);
                    }
                    dbs.put(db, new HashMap<>());
                    JsonObject r = new JsonObject();
                    r.addProperty("ok", true);
                    return r;
                }

                @Override
                public JsonObject allDocs(String db, JsonObject request) {
                    JsonObject r = new JsonObject();
                    JsonArray a = new JsonArray();
                    db(db).values().forEach(a::add);
                    r.add("docs", a);
                    return r;
                }

                @Override
                public JsonObject saveDoc(String db, String docid, JsonObject doc) {
                    return db(db).put(docid, doc);
                }

                @Override
                public JsonObject deleteDoc(String db, String docid, String rev) {
                    return db(db).remove(docid);
                }

                @Override
                public JsonObject getDocumentById(String db, String docid) throws DocumentNotFoundException {
                    return db(db).get(docid);
                }

                private Map<String, JsonObject> db(String db) {
                    return Optional.ofNullable(dbs.get(db)).orElseThrow(() -> new IllegalArgumentException(db + " does not exist"));
                }

            };
        }
    }

}

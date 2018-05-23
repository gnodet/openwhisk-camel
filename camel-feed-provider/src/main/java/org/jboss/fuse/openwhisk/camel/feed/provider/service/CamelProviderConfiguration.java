package org.jboss.fuse.openwhisk.camel.feed.provider.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import feign.Feign;
import feign.Logger;
import feign.auth.BasicAuthRequestInterceptor;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;
import feign.slf4j.Slf4jLogger;
import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.fuse.openwhisk.camel.feed.provider.db.CouchDBClient;
import org.jboss.fuse.openwhisk.camel.feed.provider.db.CouchDBDecoder;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class CamelProviderConfiguration  {

    private final org.slf4j.Logger log = LoggerFactory.getLogger(CamelProviderConfiguration.class);

    @Value("${COUCHDB_USER}")
    private String couchdbUser;
    @Value("${COUCHDB_PASSWORD}")
    private String couchdbPassword;
    @Value("${COUCHDB_SERVICE_HOST}")
    private String couchdbHost = "couchdb";
    @Value("${COUCHDB_SERVICE_PORT}")
    private String couchdbPort = "5984";

    @Bean
    public TriggerDataService triggerDataService(CouchDBClient client, Gson gson) {
        return new TriggerDataService(client, gson);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public CouchDBClient couchDBClient() {
        String couchdbRestURI = String.format("http://%s:%s", couchdbHost, couchdbPort);
        log.info("Using Trigger Store {}", couchdbRestURI);
        return Feign.builder()
                .decoder(new GsonDecoder())
                .encoder(new GsonEncoder())
                .logLevel(Logger.Level.BASIC)
                .logger(new Slf4jLogger())
                .errorDecoder(new CouchDBDecoder())
                .requestInterceptor(new BasicAuthRequestInterceptor(couchdbUser, couchdbPassword))
                .target(CouchDBClient.class, couchdbRestURI);
    }

    @Bean
    public Gson gson() {
        return new GsonBuilder()
                .create();
    }

    @Bean
    public CamelContext camelContext() throws Exception {
        CamelContext context = new DefaultCamelContext();
        context.start();
        return context;
    }

}

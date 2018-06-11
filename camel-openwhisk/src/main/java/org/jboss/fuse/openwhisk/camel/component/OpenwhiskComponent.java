package org.jboss.fuse.openwhisk.camel.component;

import java.nio.charset.Charset;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.camel.Consumer;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.impl.DefaultComponent;
import org.apache.camel.impl.DefaultEndpoint;
import org.apache.camel.impl.DefaultProducer;
import org.apache.camel.language.simple.SimpleLanguage;
import org.apache.camel.spi.Metadata;
import org.apache.camel.util.MessageHelper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class OpenwhiskComponent extends DefaultComponent {

    private String host = "localhost";
    private String namespace = "_";
    private String username;
    private String password;
    private String token;

    @Metadata(description = "Host and port")
    public void setHost(String host) {
        this.host = host;
    }

    @Metadata(description = "Namespace")
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    /**
     * Username to use.
     */
    @Metadata(label = "security", secret = true, description = "Username to use.")
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Password to use.
     */
    @Metadata(label = "security", secret = true, description = "Password to use.")
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Bearer token
     */
    @Metadata(label = "security", secret = true, description = "Bearer token to use.")
    public void setToken(String token) {
        this.token = token;
    }

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        BaseOpenwhiskEndpoint endpoint;
        if (remaining.startsWith("actions/")) {
            endpoint = new OpenwhiskActionEndpoint(remaining, this);
        } else if (remaining.startsWith("trigger/")) {
            endpoint = new OpenwhiskTriggerEndpoint(remaining, this);
        } else {
            throw new RuntimeCamelException("Unsupported openwhisk uri: " + uri);
        }
        endpoint.setHost(host);
        endpoint.setNamespace(namespace);
        endpoint.setUsername(username);
        endpoint.setPassword(password);
        endpoint.setToken(token);
        setProperties(endpoint, parameters);
        return endpoint;
    }

    @Override
    protected void afterConfiguration(String uri, String remaining, Endpoint endpoint, Map<String, Object> parameters) throws Exception {
        ((BaseOpenwhiskEndpoint) endpoint).validate();
    }

    public static abstract class BaseOpenwhiskEndpoint extends DefaultEndpoint {

        protected RestTemplate restTemplate = new RestTemplate();
        protected Map<String, Expression> mapping;
        protected String host;
        protected String namespace;
        protected String username;
        protected String password;
        protected String token;

        public BaseOpenwhiskEndpoint(String endpointUri, OpenwhiskComponent component) {
            super(endpointUri, component);
        }

        public RestTemplate getRestTemplate() {
            return restTemplate;
        }

        public void setRestTemplate(RestTemplate restTemplate) {
            this.restTemplate = restTemplate;
        }

        public void setMapping(Map<String, Expression> mapping) {
            this.mapping = mapping;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public void setNamespace(String namespace) {
            this.namespace = namespace;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public void setToken(String token) {
            this.token = token;
        }

        @Override
        public OpenwhiskComponent getComponent() {
            return (OpenwhiskComponent) super.getComponent();
        }

        @Override
        public Producer createProducer() throws Exception {
            return new DefaultProducer(this) {
                @Override
                public void process(Exchange exchange) throws Exception {
                    onExchange(exchange);
                }
            };
        }

        protected void onExchange(Exchange exchange) throws Exception {
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            httpHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            if (token != null) {
                httpHeaders.set("Authorization", "Bearer " + base64Encoded(token));
            } else if (username != null && password != null) {
                httpHeaders.set("Authorization", "Basic " + base64Encoded(username + ":" + password));
            }

            Map<String, String> payload = extractPayload(exchange);

            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(payload, httpHeaders);

            String uri = "https://" + host + "/api/v1/namespaces/" + namespace + "/" + getEndpointUri();
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.POST, requestEntity, String.class);

            exchange.getOut().setHeaders((Map) response.getHeaders());
            exchange.getOut().setHeader(Exchange.HTTP_RESPONSE_CODE, response.getStatusCodeValue());
            exchange.getOut().setHeader(Exchange.CONTENT_TYPE, response.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE));
            MessageHelper.copyHeaders(exchange.getIn(), exchange.getOut(), false);
            exchange.getOut().setBody(response.getBody());
        }

        private Map<String, String> extractPayload(Exchange exchange) {
            if (mapping == null || mapping.isEmpty()) {
                mapping = new LinkedHashMap<>();
                mapping.put("body", SimpleLanguage.simple("body"));
            }
            Map<String, String> payload = new LinkedHashMap<>();
            for (Map.Entry<String, Expression> e : mapping.entrySet()) {
                payload.put(e.getKey(), e.getValue().evaluate(exchange, String.class));
            }
            return payload;
        }

        @Override
        public Consumer createConsumer(Processor processor) throws Exception {
            throw new RuntimeCamelException("Cannot consume from a " + getClass().getSimpleName() + ": " + getEndpointUri());
        }

        @Override
        public boolean isSingleton() {
            return true;
        }

        public void validate() {
            if (host == null) {
                throw new IllegalArgumentException("Missing host");
            }
        }
    }

    public static class OpenwhiskActionEndpoint extends BaseOpenwhiskEndpoint {

        public OpenwhiskActionEndpoint(String endpointUri, OpenwhiskComponent component) {
            super(endpointUri, component);
        }

    }

    public static class OpenwhiskTriggerEndpoint extends BaseOpenwhiskEndpoint {

        public OpenwhiskTriggerEndpoint(String endpointUri, OpenwhiskComponent component) {
            super(endpointUri, component);
        }

    }

    public static String base64Encoded(String text) {
        byte[] encodedText = Base64.getEncoder().encode(
                text.getBytes(Charset.forName("US-ASCII")));
        return new String(encodedText);
    }

}

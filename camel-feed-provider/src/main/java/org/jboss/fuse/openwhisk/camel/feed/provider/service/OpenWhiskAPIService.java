package org.jboss.fuse.openwhisk.camel.feed.provider.service;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class OpenWhiskAPIService {

    @Value("${NGINX_SERVICE_HOST}")
    String nginxHost;
    @Value("${NGINX_SERVICE_PORT}")
    String nginxPort;


    TriggerDataService triggerDataService;
    RestTemplate restTemplate;

    private final Logger log = LoggerFactory.getLogger(OpenWhiskAPIService.class);

    @Autowired
    public OpenWhiskAPIService(TriggerDataService triggerDataService, RestTemplate restTemplate) {
        this.triggerDataService = triggerDataService;
        this.restTemplate = restTemplate;
    }

    /**
     * @param triggerName
     * @param payload
     * @return
     */
    public void invokeTriggers(String triggerName, Map<String, String> payload) {
        log.info("Invoking Triggers with Payload {} ", payload);
        Stream<TriggerData> triggers = triggerDataService.findAllByDestination(triggerName);
        triggers.forEach(t -> post(t, payload));
    }

    protected void post(TriggerData trigger, Map<String, String> payload) {
        String triggerName = Utils.shortTriggerID(trigger.getTriggerName());
        try {
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            httpHeaders.set("Authorization", "Basic " + Utils.base64Encoded(trigger.getAuthKey()));

            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(payload, httpHeaders);

            String uri = "http://" + nginxHost + ":" + nginxPort + "/api/v1/namespaces/_/triggers/{trigger}";
            ResponseEntity<String> response = restTemplate.exchange(uri,
                    HttpMethod.POST, requestEntity, String.class, triggerName);

            log.info("Status: {}  Response body:{}", response.getStatusCode().value(), response.getBody());
        } catch (Exception e) {
            log.error("Error with trigger " + triggerName, e);
        }
    }

}

package org.jboss.fuse.openwhisk.camel.feed.provider.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.language.simple.SimpleLanguage;
import org.apache.camel.model.RouteDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/feed")
public class ProviderController {

    private final Logger log = LoggerFactory.getLogger(ProviderController.class);

    private final OpenWhiskAPIService openWhiskAPIService;
    private final TriggerDataService triggerDataService;
    private final CamelContext camelContext;

    @Autowired
    public ProviderController(OpenWhiskAPIService openWhiskAPIService,
                              TriggerDataService triggerDataService,
                              CamelContext camelContext) {
        this.openWhiskAPIService = openWhiskAPIService;
        this.triggerDataService = triggerDataService;
        this.camelContext = camelContext;
    }

    @PostConstruct
    private void reconstructRoutesFromDB() {
        log.info("Reconstructing all routes");
        triggerDataService.findAll()
                .forEach(triggerData -> {
                    log.info("Reconstructing route for trigger {}", triggerData);
                    addRoute(triggerData);
                });
    }

    @RequestMapping(value = "/listener", method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<String> addFeedToTrigger(@RequestBody Map<String, Object> data) {
        log.info("Input Data: {}", data);
        //TODO validations
        if (!data.isEmpty()) {
            TriggerData triggerData = buildTriggerData(data);
            addRoute(triggerData);
            triggerDataService.saveOrUpdate(triggerData);
            final JsonObject response = new JsonObject();
            response.addProperty("status", String.valueOf(HttpStatus.OK));
            response.addProperty("message", String.format("Successfully enabled route for %s", triggerData.getTriggerName()));
            return ResponseEntity.ok(response.toString());
        } else {
            return ResponseEntity.badRequest().body("Request db is not valid or empty");
        }
    }

    @RequestMapping(value = "/listener/{triggerName}", method = RequestMethod.DELETE)
    public ResponseEntity deleteListenerFromTrigger(@PathVariable("triggerName") String triggerName) {
        log.info("Disassociating Trigger {}", Utils.base64Decode(triggerName));
        Optional<TriggerData> triggerData = triggerDataService.getDocumentById(triggerName);
        if (triggerData.isPresent()) {
            removeRoute(triggerData.get().getTriggerName());
            triggerDataService.deleleteDoc(triggerName);
        }
        return ResponseEntity.noContent().build();
    }


    private TriggerData buildTriggerData(Map<String, Object> data) {
        TriggerData td = new TriggerData();
        td.setAuthKey((String) data.get("authKey"));
        td.setTriggerName(Objects.requireNonNull((String) data.get("triggerName"), "triggerName must be specified"));
        td.setTriggerShortName(Utils.shortTriggerID((String) data.get("triggerName")));
        td.setBinary(Optional.ofNullable((Boolean) data.get("binary")).orElse(false));
        td.setRouteUrl(Objects.requireNonNull((String) data.get("routeUrl")));
        Map<?, ?> mapping = null;
        Object mappingObj = data.get("mapping");
        if (mappingObj instanceof String) {
            mapping = new Gson().fromJson(mappingObj.toString(), Map.class);
        } else if (mappingObj instanceof Map) {
            mapping = (Map) mappingObj;
        } else if (mappingObj != null) {
            throw new IllegalArgumentException("unsupported mapping: " + mappingObj);
        }
        td.setMapping(mapping != null
                ? mapping.entrySet().stream().collect(Collectors.toMap(e -> e.getKey().toString(), e -> e.getValue().toString()))
                : null);
        return td;
    }

    private void addRoute(TriggerData route) {
        try {
            Map<String, Expression> mapping = new LinkedHashMap<>();
            if (route.getMapping() == null || route.getMapping().isEmpty()) {
                mapping.put("body", SimpleLanguage.simple("body"));
            } else {
                for (Map.Entry<String, String> e : route.getMapping().entrySet()) {
                    mapping.put(e.getKey(), SimpleLanguage.simple(e.getValue()));
                }
            }
            camelContext.addRoutes(new RouteBuilder() {
                @Override
                public void configure() {
                    RouteDefinition rd = from(route.getRouteUrl()).id(route.getTriggerName());
                    if (route.isBinary()) {
                        rd.marshal("base64");
                    }
                    rd.process(this::process);
                    log.info("Updating route {} with {}", route.getTriggerName(), rd.toString());
                }
                private void process(Exchange exchange) {
                    log.info("Received exchange on route {}: {} ", route.getTriggerName(), exchange);
                    Map<String, String> payload = new LinkedHashMap<>();
                    for (Map.Entry<String, Expression> e : mapping.entrySet()) {
                        payload.put(e.getKey(), e.getValue().evaluate(exchange, String.class));
                    }
                    openWhiskAPIService.invokeTriggers(route.getTriggerName(), payload);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * @param routeId
     */
    private void removeRoute(String routeId) {
        try {
            camelContext.stopRoute(routeId);
            camelContext.removeRoute(routeId);
            log.info("Successfully stopped route {}", routeId);
        } catch (Exception e) {
            log.error("Error stopping route {}", routeId);
        }
    }
}

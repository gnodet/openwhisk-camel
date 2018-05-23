package org.jboss.fuse.openwhisk.camel;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import com.google.gson.JsonObject;
import feign.Feign;
import feign.Headers;
import feign.Logger;
import feign.Param;
import feign.RequestLine;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;
import feign.slf4j.Slf4jLogger;


public class CamelFeedAction {

    public static JsonObject main(JsonObject request) {
        JsonObject response = new JsonObject();
        //TODO validation
        try {
            CamelProvider provider = Feign.builder()
                    .decoder(new GsonDecoder())
                    .encoder(new GsonEncoder())
                    .logLevel(Logger.Level.BASIC)
                    .logger(new Slf4jLogger(CamelProvider.class))
                    .target(CamelProvider.class, "http://camel-feed-provider.openwhisk:8080");
            response.addProperty("done", true);
            String lifecycleEvent = request.get("lifecycleEvent").getAsString();
            if (lifecycleEvent.equalsIgnoreCase("CREATE")) {
                System.out.println("CREATE TRIGGER");
                response = provider.addListenerToTrigger(request);
                System.out.println("RESPONSE:" + response);
            } else if (lifecycleEvent.equalsIgnoreCase("DELETE")) {
                System.out.println("DELETE TRIGGER:");
                String triggerName = new String(Base64.getEncoder().encode(request.get("triggerName")
                        .getAsString().getBytes(StandardCharsets.US_ASCII)));
                response = provider.deleteListenerFromTrigger(triggerName);
            }
        } catch (Exception e) {
            response.addProperty("done", false);
            response.addProperty("error", e.getMessage());
        }
        return response;
    }


    interface CamelProvider {
        @RequestLine("POST /api/feed/listener")
        @Headers("Content-Type: application/json")
        JsonObject addListenerToTrigger(JsonObject request);

        @RequestLine("DELETE /api/feed/listener/{triggerName}")
        JsonObject deleteListenerFromTrigger(@Param("triggerName") String triggerName);
    }
}

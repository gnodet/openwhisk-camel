package org.jboss.fuse.openwhisk.camel.feed.provider;

import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.Test;

public class GsonTest {

    @Test
    public void deserialize() {
        Map<String, String> mapping;
        Object mappingObj = "{ date: \"CamelTimerFiredTime\", counter: \"CamelTimerCounter\" }";
        JsonObject j = new Gson().fromJson(mappingObj.toString(), JsonObject.class);
        Map<String, String> m = (Map) new Gson().fromJson(mappingObj.toString(), Map.class);
        mapping = new LinkedHashMap<>();
        for (String s : j.keySet()) {
            mapping.put(s, j.get(s).getAsString());
        }
    }
}

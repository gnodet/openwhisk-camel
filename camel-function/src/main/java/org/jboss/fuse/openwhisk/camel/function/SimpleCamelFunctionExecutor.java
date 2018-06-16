package org.jboss.fuse.openwhisk.camel.function;

import com.google.gson.JsonObject;

public class SimpleCamelFunctionExecutor {

    public static JsonObject main(JsonObject request) throws Exception {
        CamelFunction function = new SimpleCamelFunction();
        function.start();
        try {
            return function.execute(request);
        } finally {
            function.stop();
        }
    }

}

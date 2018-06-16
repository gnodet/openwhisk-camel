package org.jboss.fuse.openwhisk.camel.function;

import com.google.gson.JsonObject;
import org.apache.camel.Exchange;
import static org.apache.camel.builder.Builder.body;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleCamelFunctionExecutor {

    public static JsonObject main(JsonObject request) {
        return new SimpleCamelFunction().execute(request);
    }

}

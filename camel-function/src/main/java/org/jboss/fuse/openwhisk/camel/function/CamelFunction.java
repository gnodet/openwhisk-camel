package org.jboss.fuse.openwhisk.camel.function;

import java.lang.reflect.Type;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.NoTypeConversionAvailableException;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.TypeConversionException;
import org.apache.camel.TypeConverter;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.direct.DirectComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.impl.DefaultMessage;
import org.apache.camel.impl.SimpleRegistry;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.support.TypeConverterSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class CamelFunction {

    protected static final String INPUT_ENDPOINT_URI = "function:input";

    protected final Logger log = LoggerFactory.getLogger(getClass());


    protected SimpleRegistry registry;
    protected DirectComponent functionComponent;
    protected volatile CamelContext context;

    public CamelFunction() {
    }

    public SimpleRegistry getRegistry() {
        return registry;
    }

    public CamelContext getContext() {
        if (context == null) {
            synchronized (this) {
                if (context == null) {
                    doInitialize();
                }
            }
        }
        return context;
    }

    protected abstract void configure(RouteDefinition from);

    protected void doInitialize() {
        try {
            functionComponent = new DirectComponent();
            registry = new SimpleRegistry();
            registry.put("function", functionComponent);
            context = new DefaultCamelContext(registry);
            context.addRoutes(new RouteBuilder() {
                @Override
                public void configure() {
                    CamelFunction.this.configure(from(INPUT_ENDPOINT_URI));
                }
            });
            context.getTypeConverterRegistry().addFallbackTypeConverter(new GsonTypeConverter(), true);
            context.start();
        } catch (Exception e) {
            throw new RuntimeException("Unable to initialize camel context", e);
        }
    }

    public JsonObject execute(JsonObject request) {
        try {
            CamelContext context = getContext();
            ProducerTemplate template = context.createProducerTemplate();
            try {
                return template.requestBody(INPUT_ENDPOINT_URI, request, JsonObject.class);
            } finally {
                template.stop();
            }
        } catch (Exception e) {
            JsonObject result = new JsonObject();
            result.addProperty("success", false);
            result.addProperty("exception", e.getClass().getName());
            result.addProperty("error", e.getMessage());
            log.error("Error during execution", e);
            return result;
        }
    }

    private class GsonTypeConverter extends TypeConverterSupport {
        final Gson gson = new Gson();
        @Override
        @SuppressWarnings("unchecked")
        public <T> T convertTo(Class<T> type, Exchange exchange, Object value) throws TypeConversionException {
            if (value instanceof JsonElement) {
                if (type == Map.class) {
                    Type t = new TypeToken<Map<String, Object>>(){}.getType();
                    return (T) gson.fromJson((JsonElement) value, t);
                } else if (type.isAssignableFrom(String.class)) {
                    return (T) gson.toJson((JsonElement) value);
                }
            } else if (type == JsonObject.class) {
                if (value instanceof Map) {
                    return (T) gson.toJsonTree(value, Map.class).getAsJsonObject();
                } else {
                    return (T) gson.toJsonTree(value).getAsJsonObject();
                }
            }
            return null;
        }
    }
}

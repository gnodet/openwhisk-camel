package org.jboss.fuse.openwhisk.camel.function;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.TypeConversionException;
import org.apache.camel.component.direct.DirectComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.SimpleRegistry;
import org.apache.camel.main.MainSupport;
import org.apache.camel.spi.Registry;
import org.apache.camel.support.TypeConverterSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CamelFunction extends MainSupport {

    public static final String INPUT_ENDPOINT_URI = "function:input";

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected final SimpleRegistry registry = new SimpleRegistry();

    public CamelFunction() {
        bind("function", new DirectComponent());
    }

    /**
     * Process a request
     */
    public JsonObject execute(JsonObject request) {
        try {
            return getCamelTemplate().requestBody(INPUT_ENDPOINT_URI, request, JsonObject.class);
        } catch (Exception e) {
            JsonObject result = new JsonObject();
            result.addProperty("success", false);
            result.addProperty("exception", e.getClass().getName());
            result.addProperty("error", e.getMessage());
            log.error("Error during execution", e);
            return result;
        }
    }

    /**
     * Binds the given <code>name</code> to the <code>bean</code> object, so
     * that it can be looked up inside the CamelContext this command line tool
     * runs with.
     *
     * @param name the used name through which we do bind
     * @param bean the object to bind
     */
    public void bind(String name, Object bean) {
        registry.put(name, bean);
    }

    /**
     * Using the given <code>name</code> does lookup for the bean being already
     * bound using the {@link #bind(String, Object)} method.
     *
     * @see Registry#lookupByName(String)
     */
    public Object lookup(String name) {
        return registry.get(name);
    }

    /**
     * Using the given <code>name</code> and <code>type</code> does lookup for
     * the bean being already bound using the {@link #bind(String, Object)}
     * method.
     *
     * @see Registry#lookupByNameAndType(String, Class)
     */
    public <T> T lookup(String name, Class<T> type) {
        return registry.lookupByNameAndType(name, type);
    }

    /**
     * Using the given <code>type</code> does lookup for the bean being already
     * bound using the {@link #bind(String, Object)} method.
     *
     * @see Registry#findByTypeWithName(Class)
     */
    public <T> Map<String, T> lookupByType(Class<T> type) {
        return registry.findByTypeWithName(type);
    }

    /**
     *
     * Gets or creates the {@link org.apache.camel.CamelContext} this main class is using.
     *
     * It just create a new CamelContextMap per call, please don't use it to access the camel context that will be ran by main.
     * If you want to setup the CamelContext please use MainListener to get the new created camel context.
     */
    public CamelContext getCamelContext() {
        if (getCamelContexts().size() > 0) {
            return getCamelContexts().get(0);
        } else {
            return null;
        }
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        postProcessContext();
        if (getCamelContexts().size() > 0) {
            try {
                getCamelContexts().get(0).start();
                // if we were veto started then mark as completed
            } finally {
                if (getCamelContexts().get(0).isVetoStarted()) {
                    completed();
                }
            }
        }
    }

    protected void doStop() throws Exception {
        super.doStop();
        if (getCamelContexts().size() > 0) {
            getCamelContexts().get(0).stop();
        }
    }

    protected ProducerTemplate findOrCreateCamelTemplate() {
        CamelContext context = getCamelContext();
        if (context != null) {
            return context.createProducerTemplate();
        } else {
            return null;
        }
    }

    protected Map<String, CamelContext> getCamelContextMap() {
        CamelContext camelContext = createContext();
        return Collections.singletonMap("camel-1", camelContext);
    }

    protected CamelContext createContext() {
        DefaultCamelContext context = new DefaultCamelContext(registry);
        context.getTypeConverterRegistry().addFallbackTypeConverter(new GsonTypeConverter(), true);
        return context;
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

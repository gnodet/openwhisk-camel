package openwhisk.camel.action.function;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.SimpleRegistry;

public abstract class CamelFunctionRouteBuilder extends RouteBuilder {

    public static final String INPUT_ENDPOINT_URI = "function:input";

    protected SimpleRegistry registry;

    public void setRegistry(SimpleRegistry registry) {
        this.registry = registry;
    }

    public void bind(String name, Object bean) {
        registry.put(name, bean);
    }

}

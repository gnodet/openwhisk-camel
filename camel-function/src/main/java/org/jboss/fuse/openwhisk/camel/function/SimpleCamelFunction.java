package org.jboss.fuse.openwhisk.camel.function;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonObject;
import org.apache.camel.Exchange;
import static org.apache.camel.builder.Builder.body;
import org.apache.camel.component.bean.BeanProcessor;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.model.RouteDefinition;

public class SimpleCamelFunction extends CamelFunction {

    @Override
    protected void configure(RouteDefinition from) {
        from
                .transform(this::extractMessage)
                .setHeader(MyOrderService.class.getName(), MyOrderService::new)
                .split(body().tokenize("@"), this::aggregate)
                // each splitted message is then send to this bean where we can process it
                .process(e -> stateless(e, "handleOrder"))
                // this is important to end the splitter route as we do not want to do more routing
                // on each splitted message
                .end()
                // after we have splitted and handled each message we want to send a single combined
                // response back to the original caller, so we let this bean build it for us
                // this bean will receive the result of the aggregate strategy: MyOrderStrategy
                .process(e -> stateless(e, "buildCombinedResponse"));
    }

    public <T> T extractMessage(Exchange exchange, Class<T> type) {
        return type.cast(exchange.getIn().getBody(JsonObject.class).get("message").getAsString());
    }

    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
        // put order together in old exchange by adding the order from new exchange

        List<String> orders;
        if (oldExchange != null) {
            orders = (List) oldExchange.getIn().getBody();
        } else {
            orders = new ArrayList<>();
            oldExchange = new DefaultExchange(getContext());
            oldExchange.getIn().copyFromWithNewBody(newExchange.getIn(), orders);
        }
        String newLine = newExchange.getIn().getBody(String.class);

        log.debug("Aggregate old orders: " + orders);
        log.debug("Aggregate new order: " + newLine);

        // add orders to the list
        orders.add(newLine);

        // return old as this is the one that has all the orders gathered until now
        return oldExchange;
    }

    private void stateless(Exchange exchange, String method) throws Exception {
        BeanProcessor bp = new BeanProcessor(exchange.getIn().getHeader(MyOrderService.class.getName()), getContext());
        bp.setMethod(method);
        bp.process(exchange);
    }

    public class MyOrderService {

        private int counter;

        /**
         * We just handle the order by returning a id line for the order
         */
        public String handleOrder(String line) {
            log.debug("HandleOrder: " + line);
            return "(id=" + ++counter + ",item=" + line + ")";
        }

        /**
         * We use the same bean for building the combined response to send
         * back to the original caller
         */
        public Map<String, Object> buildCombinedResponse(List<String> lines) {
            log.debug("BuildCombinedResponse: " + lines);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("lines", lines);
            return result;
        }
    }

}

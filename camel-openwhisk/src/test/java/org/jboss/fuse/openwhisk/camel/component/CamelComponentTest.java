package org.jboss.fuse.openwhisk.camel.component;

import java.io.IOException;
import java.net.ServerSocket;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.DefaultFluentProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;

public class CamelComponentTest extends CamelTestSupport {

    static int port;

    @BeforeClass
    public static void setup() throws IOException {
        try (ServerSocket ss = new ServerSocket(0)) {
            port = ss.getLocalPort();
        }
    }

    @Override
    protected RoutesBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                        .to("openwhisk:actions/myaction?host=localhost:" + port)
                        .to("mock:result");
            }
        };
    }

    @Test
    @DirtiesContext
    @Ignore
    public void testMocksAreValid() throws Exception {
        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("jetty:http://localhost:" + port + "/api/v1/namespaces/_/actions/myaction")
                        .process(exchange -> {
                            assertEquals("application/json", exchange.getIn().getHeader("Content-Type"));
                            assertEquals("{\"body\":\"Hello\"}", exchange.getIn().getBody(String.class));
                            exchange.getOut().setHeader("Content-Type", "application/json");
                            exchange.getOut().setBody("{\"ok\":true}");
                        });
            }
        });

        Exchange exchange = DefaultFluentProducerTemplate.on(context)
                .withHeader("key-1", "value-1")
                .withHeader("key-2", "value-2")
                .withBody("Hello")
                .to("direct:start")
                .request(Exchange.class);
        assertNotNull("exchange is null", exchange);
        assertNull("exchange is in error", exchange.getException());
        assertNotNull("exchange is null", exchange.getOut());
        assertEquals("{\"ok\":true}", exchange.getOut().getBody(String.class));
    }

}

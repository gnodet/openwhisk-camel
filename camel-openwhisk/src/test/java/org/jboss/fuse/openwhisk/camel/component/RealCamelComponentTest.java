package org.jboss.fuse.openwhisk.camel.component;

import java.io.IOException;
import java.net.ServerSocket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.google.gson.Gson;
import feign.gson.GsonDecoder;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.NoTypeConversionAvailableException;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.TypeConversionException;
import org.apache.camel.TypeConverter;
import org.apache.camel.builder.DefaultFluentProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.language.simple.SimpleLanguage;
import org.apache.camel.support.TypeConverterSupport;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;

public class RealCamelComponentTest extends CamelTestSupport {

    static String nginx = "openwhisk-openwhisk.192.168.64.6.nip.io";

    private static final TrustManager[] UNQUESTIONING_TRUST_MANAGER = new TrustManager[]{
            new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers(){
                    return null;
                }
                public void checkClientTrusted( X509Certificate[] certs, String authType ){}
                public void checkServerTrusted(X509Certificate[] certs, String authType ){}
            }
    };

    public  static void turnOffSslChecking() throws NoSuchAlgorithmException, KeyManagementException {
        // Install the all-trusting trust manager
        final SSLContext sc = SSLContext.getInstance("SSL");
        sc.init( null, UNQUESTIONING_TRUST_MANAGER, null );
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
    }

    @Override
    protected CamelContext createCamelContext() throws Exception {
        CamelContext context = super.createCamelContext();
        context.getTypeConverterRegistry()
                .addTypeConverter(Map.class, String.class, new TypeConverterSupport() {
                    @Override
                    public <T> T convertTo(Class<T> type, Exchange exchange, Object value) throws TypeConversionException {
                        return new Gson().fromJson(value.toString(), type);
                    }
                });
        OpenwhiskComponent ow = context.getComponent("openwhisk", OpenwhiskComponent.class);
        ow.setHost(nginx);
        ow.setUsername("789c46b1-71f6-4ed5-8c54-816aa4f8c502");
        ow.setPassword("aX2dw5DyBVhEHOJ4s8GSCXVR5lB2yPiuVcd45cSDQgwPdL8NHWC1v1OBnRnF3rmj");
        return context;
    }

    @Override
    protected RoutesBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                        .to("openwhisk:actions/alarmsWeb/alarmWebAction?namespace=whisk.system")
                        .to("log:out");
            }
        };
    }

    @Test
    @DirtiesContext
    public void testMocksAreValid() throws Exception {
        turnOffSslChecking();
        Map<String, String> body = new LinkedHashMap<>();
        body.put("authKey", "xxxx:yyyy");
        Exchange exchange = DefaultFluentProducerTemplate.on(context)
                .to("direct:start")
                .withBody(body)
                .request(Exchange.class);
        assertNotNull("exchange is null", exchange);
        assertNull("exchange is in error", exchange.getException());
        assertNotNull("out is null", exchange.getOut());
        assertNotNull("out body is not map", exchange.getOut().getBody(Map.class));
        assertNotNull("no activationId", exchange.getOut().getBody(Map.class).get("activationId"));
    }

}

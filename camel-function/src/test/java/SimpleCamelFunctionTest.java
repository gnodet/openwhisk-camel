import com.google.gson.JsonObject;
import org.jboss.fuse.openwhisk.camel.function.SimpleCamelFunction;
import org.jboss.fuse.openwhisk.camel.function.SimpleCamelFunctionExecutor;
import org.junit.Test;

public class SimpleCamelFunctionTest {

    @Test
    public void testSimpleCamelFunction() {
        JsonObject in = new JsonObject();
        in.addProperty("message", "foo@bar@baz");
        JsonObject out = SimpleCamelFunctionExecutor.main(in);
        System.out.println(out.toString());
    }
}

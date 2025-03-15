package labs;

import org.noear.snack.JsonParser;
import org.noear.snack.ONode;

import java.io.Reader;
import java.io.StringReader;

/**
 * @author noear 2025/3/15 created
 */
public class DemoTest {
    public void case1() {
        String json = "{\n" +
                "  \"name\": \"Alice\",\n" +
                "  \"age\": 28,\n" +
                "  \"scores\": [95.5, 89.0, 92.3],\n" +
                "  \"metadata\": {\n" +
                "    \"uid\": \"\\u0041\\u0042\\u0043\",\n" +
                "    \"active\": true\n" +
                "  }\n" +
                "}";

        try (Reader reader = new StringReader(json)) {
            ONode root = new JsonParser(reader).parse();

            String name = root.getObject().get("name").getString();
            double avgScore = root.getObject().get("scores").getArray().stream()
                    .mapToDouble(ONode::getDouble)
                    .average()
                    .orElse(0);
            String uid = root.getObject().get("metadata").getObject().get("uid").getString();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

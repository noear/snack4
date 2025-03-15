package features;

import org.junit.jupiter.api.Test;
import org.noear.snack.*;
import org.noear.snack.exception.SchemaException;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

/**
 * @author noear 2025/3/15 created
 */
public class BaseTest {
    @Test
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

    @Test
    public void case2() throws IOException {
        // Schema定义示例
        String schemaJson = "{"
                + "\"type\": \"object\","
                + "\"required\": [\"name\", \"age\"],"
                + "\"properties\": {"
                + "  \"name\": {\"type\": \"string\"},"
                + "  \"age\": {\"type\": \"integer\", \"minimum\": 0}"
                + "}"
                + "}";

        // 数据校验
        JsonParser parser = new JsonParser(new StringReader(schemaJson));
        ONode schemaNode = parser.parse();
        JsonSchemaValidator validator = new JsonSchemaValidator(schemaNode);

        ONode data = new JsonParser(new StringReader("{\"name\":\"Alice\",\"age\":-5}")).parse();
        try {
            validator.validate(data);
        } catch (SchemaException e) {
            System.out.println(e.getMessage());
            // 输出: Value -5.0 < minimum(0.0) at $.age
        }
    }
}

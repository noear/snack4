package features;

import org.junit.jupiter.api.Test;
import org.noear.snack.*;
import org.noear.snack.codec.JsonReader;
import org.noear.snack.exception.SchemaException;
import org.noear.snack.schema.validator.SchemaValidator;

import java.io.IOException;
import java.io.StringReader;

/**
 * @author noear 2025/3/15 created
 */
public class BaseTest {
    @Test
    public void case1() throws Exception {
        String json = "{"
                + "\"name\": \"Alice\","
                + "\"age\": 28,"
                + "\"scores\": [95.5, 89.0, 92.3],"
                + "\"metadata\": {"
                + "  \"uid\": \"\\u0041\\u0042\\u0043\","
                + "  \"active\": true"
                + "}"
                + "}";

        ONode node =  JsonReader.parse(json);

        // 验证解析结果
        System.out.println(node.get("name").getString()); // Alice
        System.out.println(node.get("age").getInt()); // 28
        System.out.println(node.get("scores").get(0).getDouble()); // 95.5
        System.out.println(node.get("metadata").get("uid").getString()); // ABC
        System.out.println(node.get("metadata").get("active").getBoolean()); // true
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


        System.out.println(schemaJson);

        // 数据校验
        JsonReader parser = new JsonReader(new StringReader(schemaJson));
        ONode schemaNode = parser.parse();
        SchemaValidator validator = new SchemaValidator(schemaNode);

        ONode data = new JsonReader(new StringReader("{\"name\":\"Alice\",\"age\":-5}")).parse();
        try {
            validator.validate(data);
        } catch (SchemaException e) {
            System.out.println(e.getMessage());
            // 输出: Value -5.0 < minimum(0.0) at $.age
        }
    }
}

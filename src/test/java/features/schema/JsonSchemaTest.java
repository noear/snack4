package features.schema;

import org.junit.jupiter.api.Test;
import org.noear.snack.ONode;
import org.noear.snack.schema.JsonSchemaValidator;

/**
 * @author noear 2025/5/10 created
 */
public class JsonSchemaTest {
    @Test
    public void case1() {
        JsonSchemaValidator schema = new JsonSchemaValidator(ONode.load("{type:'object',properties:{userId:{type:'string'}}}")); //加载架构定义

        ONode.load("{userId:'1'}").validate(schema); //校验格式
    }

    @Test
    public void case2() {
        JsonSchemaValidator schema = new JsonSchemaValidator(ONode.load("{type:'object',properties:{userId:{type:'string'}}}")); //加载架构定义

        Throwable err = null;
        try {
            ONode.load("{userId:1}").validate(schema); //校验格式
        } catch (Throwable e) {
            e.printStackTrace();
            err = e;
        }

        assert err != null;
    }
}

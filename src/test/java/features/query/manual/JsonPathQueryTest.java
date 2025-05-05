package features.query.manual;

import org.junit.jupiter.api.Test;
import org.noear.snack.ONode;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author noear 2025/5/5 created
 */
public class JsonPathQueryTest {
    @Test
    public void allTest() throws Exception {
        ONode n = ONode.loadJson("{code:1,msg:'Hello world',data:{list:[1,2,3,4,5], ary2:[{a:2},{a:3,b:{c:'ddd'}}]}}");

        ONode tmp = n.select("$..*");
        System.out.println(tmp);//打印
        assertEquals(16, tmp.size());
    }
}

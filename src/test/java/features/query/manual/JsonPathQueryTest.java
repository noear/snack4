package features.query.manual;

import org.junit.jupiter.api.Test;
import org.noear.snack.ONode;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author noear 2025/5/5 created
 */
public class JsonPathQueryTest {
    @Test
    public void getAllTest() throws Exception {
        ONode n = ONode.loadJson("{code:1,msg:'Hello world',data:{list:[1,2,3,4,5], ary2:[{a:2},{a:3,b:{c:'ddd'}}]}}");

        ONode tmp = n.select("$..*");
        System.out.println(tmp);//打印
        assertEquals(16, tmp.size());
    }

    @Test
    public void getAllPropTest() throws Exception {
        ONode n = ONode.loadJson("{code:1,msg:'Hello world',data:{list:[1,2,3,4,5], ary2:[{a:2},{a:3,b:{c:'ddd'}}]}}");

        ONode tmp = n.select("$..a");
        System.out.println(tmp);//打印
        assertEquals(2, tmp.size());
    }

    @Test
    public void getArrayDotTest() throws Exception {
        ONode n = ONode.loadJson("{code:1,msg:'Hello world',data:{list:[1,2,3,4,5], ary2:[{a:2},{a:3,b:{c:'ddd'}}]}}");

        ONode tmp = n.select("$.data.list[1,4]");
        System.out.println(tmp);//打印
        assertEquals(2, tmp.size());
    }

    @Test
    public void getArrayRangeTest(){
        ONode n = ONode.loadJson("{code:1,msg:'Hello world',data:{list:[1,2,3,4,5], ary2:[{a:2},{a:3,b:{c:'ddd'}}]}}");

        ONode tmp = n.select("$.data.list[1:4]");
        System.out.println(tmp);//打印
        assertEquals(3, tmp.size());
    }

    @Test
    public void getFilterAndRightExpressionTest() throws Exception {
        ONode n = ONode.loadJson("{code:1,msg:'Hello world',data:{list:[1,2,3,4,5], ary2:[{a:2},{a:3,b:{c:'ddd'}}]}}");

        ONode tmp = n.select("$.data.list[?(@ in $..ary2[0].a)]");
        assert tmp.size() == 1;
        assertEquals(1, tmp.size());
    }
}
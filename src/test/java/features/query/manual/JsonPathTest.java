package features.query.manual;

import org.junit.jupiter.api.Test;
import org.noear.snack.ONode;

import java.util.List;


public class JsonPathTest {
    @Test
    public void demo2() {
        //1.加载json
        ONode n = ONode.loadJson("{code:1,msg:'Hello world',data:{list:[1,2,3,4,5], ary2:[{a:2},{a:3,b:{c:'ddd'}}]}}");


        //4.获取一个数组
        //List<Integer> list = n.get("data").get("list").toBean(List.class);
        List<Integer> list = n.select("$.data.list").toBean(List.class);



        //int mi = n.get("data").get("list").get(0).getInt();
        int mi = n.select("$.data.list[-1]").getInt();

        List<Integer> list2 = n.select("$.data.list[2,4]").toBean(List.class);
        List<Integer> list3 = n.select("$.data.list[1:4]").toBean(List.class);
        List<Integer> list4 = n.select("$.data.list[:4]").toBean(List.class);

        ONode ary2_a = n.select("$.data.ary2[*].b.c");

        ONode ary2_b = n.select("$..b");

        ONode ary2_c = n.select("$.data..b.c");


        assert list.size() == 5;
    }

    @Test
    public void demo3() {
        //1.加载json
        ONode n = ONode.loadJson("{code:1,msg:'Hello world',data:{list:[1,2,3,4,5], ary2:[{a:2},{a:3,b:{c:'ddd'}}]}}");


        ONode ary2_a = n.select("$['code','msg']");
        assert ary2_a.size() == 2;



        ONode ary2_b = n.select("$.data[*]");
        assert ary2_b.size() == 2;



        ONode ary2_c = n.select("$['data']['list'][2]");
        assert ary2_c.getInt() == 3;
    }

    @Test
    public void demo4() {
        //1.加载json
        ONode n = ONode.loadJson("{code:1,msg:'Hello world',data:{list:[1,2,3,4,5], ary2:[{a:2},{a:3,b:{c:'ddd'}}]}}");


        ONode ary2_a = n.select("$.*");
        assert ary2_a.size() == 3;

        ONode ary2_b = n.select("$..*");
        assert ary2_b.size() == 16;

        ONode ary2_c = n.select("$..*[1]");
        assert ary2_c.size()==2;

        ONode ary2_d = n.select("$.*.list[0][0]");
        assert ary2_d.isValue();

        ONode ary2_e = n.select("$..list[0][0]");
        assert ary2_e.isValue();
    }

    @Test
    public void testx1() {
        //1.加载json
        ONode n = ONode.loadJson("{code:1,msg:'Hello world',data:{list:[1,2,3,4,5],b:2,ary2:[{a:2,b:8},{a:3,b:{c:'ddd',b:23}}]}}");

        ONode t1 = n.select("$.data.ary2[*].a");
        assert  t1.size()==2;

        ONode t2 = n.select("$..a");
        assert  t2.size()==2;

        ONode t3 = n.select("$.data.*");
        assert  t3.size()==3;

        ONode t4 = n.select("$.data..a");
        assert  t4.size()==2;

//        ONode t5 = n.select("$..a[0]");//a不是数组，不会支持了
//        assert  t5.getInt()==2;

        ONode t60 = n.select("$..b");
        System.out.println(t60.toJson());

        ONode t6 = n.select("$..list[-1]");
        assert  t6.get(0).getInt()==5;

        ONode t7 = n.select("$..list[0,1]");
        assert  t7.size()==2;

        ONode t8 = n.select("$..list[:2]");
        assert  t8.size()==2;

        ONode t9 = n.select("$..list[1:2]");
        assert  t9.size()==1;

        ONode ta = n.select("$..list[-2:]");
        assert  ta.size()==2;

        ONode tb = n.select("$..list[2:]");
        assert  tb.size()==3;
    }

    @Test
    public void testx2() {
        //1.加载json
        ONode n = ONode.loadJson("[{b:{c:1}}, {b:{d:1}}, {b:{c:2}}, {b:{c:23}}]");

        ONode t0 = n.select("$..b");
        assert  t0.size()==4;

        ONode t1 = n.select("$..b[?(@.c)]");
        assert  t1.size()==3;

        ONode t2 = n.select("$..b[?(@.c == 1)]");
        assert  t2.size()==1;

        ONode t3 = n.select("$..b[?(@.c == 12)]");
        assert  t3.size()==0;

        ONode t4 = n.select("$..b[?(@.c > 1)]");
        assert  t4.size()==2;

        ONode t4_min = n.select("$..b[?(@.c > 1)].c.min()");
        assert  t4_min.getInt()==2;

        ONode t5 = n.select("$..b[?(@.c in [1,2])]");
        assert  t5.size()==2;

        ONode t6 = n.select("$..b[?(@.c nin [1,2])]");
        assert  t6.size()==1;

        ONode t7 = n.select("$..b[?(@.c =~ /\\d+/)]");
        assert  t7.size()==3;
    }

    @Test
    public void testx2_2() {
        //1.加载json
        ONode n = ONode.loadJson("[1,2,3,4]");

        ONode t2 = n.select("$..*[?(@ == 1)]");
        assert t2.size() == 1;
    }

    @Test
    public void testx2_3() {
        //1.加载json
        ONode n = ONode.loadJson("{ \"store\": { \"book\": [ { \"category\": \"reference\", \"author\": \"Nigel Rees\", \"title\": \"Sayings of the Century\", \"price\": 8.95 }, { \"category\": \"fiction\", \"author\": \"Evelyn Waugh\", \"title\": \"Sword of Honour\", \"price\": 12.99 }, { \"category\": \"fiction\", \"author\": \"Herman Melville\", \"title\": \"Moby Dick\", \"isbn\": \"0-553-21311-3\", \"price\": 8.99 }, { \"category\": \"fiction\", \"author\": \"J. R. R. Tolkien\", \"title\": \"The Lord of the Rings\", \"isbn\": \"0-395-19395-8\", \"price\": 22.99 } ], \"bicycle\": { \"color\": \"red\", \"price\": 19.95 } }, \"expensive\": 10 }");

        ONode t2 = n.select("$..*[?(@ == 19.95)]");
        assert t2.size() == 1;
    }

    @Test
    public void testx2_4() {
        //1.加载json
        ONode n = ONode.loadJson("{ \"store\": { \"book\": [ { \"category\": \"reference\", \"author\": \"Nigel Rees\", \"title\": \"Sayings of the Century\", \"price\": 8.95 }, { \"category\": \"fiction\", \"author\": \"Evelyn Waugh\", \"title\": \"Sword of Honour\", \"price\": 12.99 }, { \"category\": \"fiction\", \"author\": \"Herman Melville\", \"title\": \"Moby Dick\", \"isbn\": \"0-553-21311-3\", \"price\": 8.99 }, { \"category\": \"fiction\", \"author\": \"J. R. R. Tolkien\", \"title\": \"The Lord of the Rings\", \"isbn\": \"0-395-19395-8\", \"price\": 22.99 } ], \"bicycle\": { \"color\": \"red\", \"price\": 19.95 } }, \"expensive\": 10 }");

        ONode t2 = n.select("$..*[?(@.price == 19.95)]");
        assert t2.size() == 1;
    }

    @Test
    public void testx2_5() {
        //1.加载json
        ONode n = ONode.loadJson("{ \"store\": { \"book\": [ { \"category\": \"reference\", \"author\": \"Nigel Rees\", \"title\": \"Sayings of the Century\", \"price\": 8.95 }, { \"category\": \"fiction\", \"author\": \"Evelyn Waugh\", \"title\": \"Sword of Honour\", \"price\": 12.99 }, { \"category\": \"fiction\", \"author\": \"Herman Melville\", \"title\": \"Moby Dick\", \"isbn\": \"0-553-21311-3\", \"price\": 8.99 }, { \"category\": \"fiction\", \"author\": \"J. R. R. Tolkien\", \"title\": \"The Lord of the Rings\", \"isbn\": \"0-395-19395-8\", \"price\": 22.99 } ], \"bicycle\": { \"color\": \"red\", \"price\": 19.95 } }, \"expensive\": 10 }");

        ONode t2 = n.select("$..[?(@.price == 19.95)]");
        assert t2.size() == 1;
    }

    @Test
    public void testx3() {
        //1.加载json
        ONode n = ONode.loadJson("[{c:'aaaa'}, {b:'cccc'}, {c:'cccaa'}]");

        ONode t1 = n.select("$[?(@.c =~ /a+/)]");//
        assert  t1.size()==2;

        ONode t2 = n.select("$[?(@.c =~ /a{4}/)]");//
        assert  t2.size()==1;

        ONode t3 = n.select("$..*");//
        assert  t3.size()==6;

        ONode t4 = n.select("$..*[?(@ =~ /c+/)]");//
        assert  t4.size()==2;
    }

    @Test
    public void testx4() {
        //1.加载json
        ONode n = ONode.loadJson("{code:1,msg:'Hello world',data:{list:[1,2,3,4,5],b:2,ary2:[{a:2,b:8},{a:3,b:{c:'ddd',b:23}}]}}");

        ONode t1 = n.select("$..b");
        assert t1.size()==4;

        ONode t2 = n.select("$..b.min()");
        assert t2.getInt()==2;

        ONode t3 = n.select("$..b.max()");
        assert t3.getInt()==23;

        ONode t4 = n.select("$..b.avg()");
        assert t4.getInt()==11;
    }
}

package labs;

import org.noear.snack.ONode;
import org.noear.snack.core.Options;

/**
 * @author noear 2025/3/17 created
 */
public class DemoLab {
    public void case1() {
        ONode oNode = new ONode();
        oNode.set("id", 1);
        oNode.getOrNew("layout").build(o -> {
            o.addNew().set("title", "开始").set("type", "start");
            o.addNew().set("title", "结束").set("type", "end");
        });
    }

    public void case2() {
        String store = "{}";
        ONode.from(store).select("$..book[?(@.tags contains 'war')]").toBean(Book.class);
        ONode.load(store).select("$.store.book.count()");

        ONode.from(store).create("$.store.book[0].category").toJson();

        ONode.from(store).delete("$..book[-1]");
    }

    public void case3() {
        ONode schemaNode = ONode.load("{user:{name:''}}"); //定义架构
        Options options = Options.builder().schema(schemaNode).build();
        ONode.load("{}", options);
    }

    static class Book {
    }
}

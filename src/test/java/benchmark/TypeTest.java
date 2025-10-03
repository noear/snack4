package benchmark;

import org.junit.jupiter.api.Test;
import org.noear.snack4.ONode;
import org.noear.snack4.query.QueryMode;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author noear 2025/10/3 created
 *
 */
public class TypeTest {
    @Test
    public void case11() {
        Object mode = QueryMode.SELECT;

        long start = System.currentTimeMillis();
        for (int i = 0, len = 1000000; i < len; i++) {
            if (mode instanceof QueryMode) {

            }
        }

        long times = System.currentTimeMillis() - start;

        System.out.println(times);
    }

    @Test
    public void case12() {
        QueryMode mode = QueryMode.SELECT;

        long start = System.currentTimeMillis();
        for (int i = 0, len = 1000000; i < len; i++) {
            if (QueryMode.CREATE == mode) {

            }
        }

        long times = System.currentTimeMillis() - start;

        System.out.println(times);
    }

    @Test
    public void case21() {
        Map<String, ONode> map = new HashMap<>();

        long start = System.currentTimeMillis();
        for (int i = 0, len = 1000000; i < len; i++) {
            map.get("xxx");
        }

        long times = System.currentTimeMillis() - start;

        System.out.println(times);
    }

    @Test
    public void case22() {
        Object map = new HashMap<>();

        long start = System.currentTimeMillis();
        for (int i = 0, len = 1000000; i < len; i++) {
            ((Map<String, ONode>) map).get("xxx");
        }

        long times = System.currentTimeMillis() - start;

        System.out.println(times);
    }
}

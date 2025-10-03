package org.noear.snack.query.segment;

import org.noear.snack.ONode;
import org.noear.snack.query.Context;
import org.noear.snack.query.QueryMode;
import org.noear.snack.query.SegmentFunction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 处理递归搜索 ..
 *
 * @author noear 2025/10/3 created
 */
public class RecursiveSegment implements SegmentFunction {
    @Override
    public List<ONode> resolve(List<ONode> currentNodes, Context context, QueryMode mode) {
        List<ONode> result = new ArrayList<>();

        for (ONode node : currentNodes) {
            collectRecursive(node, result);
        }

        return result;
    }

    private void collectRecursive(ONode node, List<ONode> results) {
        if (node.isArray()) {
            for (ONode n1 : node.getArray()) {
                results.add(n1);
                collectRecursive(n1, results);
            }
        } else if (node.isObject()) {
            for (Map.Entry<String, ONode> entry : node.getObject().entrySet()) {
                results.add(entry.getValue());
                collectRecursive(entry.getValue(), results);
            }
        }
    }
}

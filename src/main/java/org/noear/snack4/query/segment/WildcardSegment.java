package org.noear.snack4.query.segment;

import org.noear.snack4.ONode;
import org.noear.snack4.core.JsonSource;
import org.noear.snack4.query.Context;
import org.noear.snack4.query.QueryMode;
import org.noear.snack4.query.SegmentFunction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 处理通配符 * （子级偏平化）
 *
 * @author noear 2025/10/3 created
 */
public class WildcardSegment implements SegmentFunction {
    private boolean flattened;

    public WildcardSegment(boolean flattened) {
        this.flattened = flattened;
    }

    @Override
    public List<ONode> resolve(List<ONode> currentNodes, Context context, QueryMode mode) {
        if (flattened) {
            return currentNodes;
        }

        List<ONode> result = new ArrayList<>();

        for (ONode n : currentNodes) {
            Collection<ONode> childs = null;

            if (n.isArray()) {
                childs = n.getArray();
            } else if (n.isObject()) {
                childs = n.getObject().values();
            }

            if (childs != null) {
                if (mode == QueryMode.DELETE) {
                    JsonSource source = new JsonSource(n, "*", 0);
                    for (ONode child : childs) {
                        child.source = source;
                    }
                }

                result.addAll(childs);
            }
        }

        return result;
    }
}
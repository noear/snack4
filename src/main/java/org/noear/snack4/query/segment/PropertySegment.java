package org.noear.snack4.query.segment;


import org.noear.snack4.ONode;
import org.noear.snack4.core.JsonSource;
import org.noear.snack4.exception.PathResolutionException;
import org.noear.snack4.query.Context;
import org.noear.snack4.query.QueryMode;
import org.noear.snack4.query.SegmentFunction;

import java.util.ArrayList;
import java.util.List;

/**
 * 处理属性获取
 *
 * @author noear
 */
public class PropertySegment implements SegmentFunction {
    private String key;

    public PropertySegment(String key) {
        this.key = key;
    }

    @Override
    public List<ONode> resolve(List<ONode> currentNodes, Context context, QueryMode mode) {
        List<ONode> result = new ArrayList<>();

        for (ONode n : currentNodes) {
            getChild(n, key, mode, result);
        }

        return result;
    }

    private void getChild(ONode node, String key, QueryMode mode, List<ONode> result) {
        if (mode == QueryMode.CREATE) {
            node.newObject();
        }

        if (node.isObject()) {
            ONode child = node.get(key);

            if (child == null) {
                if (mode == QueryMode.CREATE) {
                    child = new ONode();
                    node.set(key, child);
                } else if (false) {
                    throw new PathResolutionException("Missing key '" + key + "'");
                }
            } else {
                child.source = new JsonSource(node, key, 0);
            }

            if (child != null) {
                result.add(child);
            }
        }
    }
}

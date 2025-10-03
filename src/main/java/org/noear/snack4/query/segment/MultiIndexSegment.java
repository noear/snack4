package org.noear.snack4.query.segment;

import org.noear.snack4.ONode;
import org.noear.snack4.core.JsonSource;
import org.noear.snack4.exception.PathResolutionException;
import org.noear.snack4.query.Context;
import org.noear.snack4.query.QueryMode;
import org.noear.snack4.query.SegmentFunction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 处理多索引选择（如 [1,4], ['a','b']）
 *
 * @author noear 2025/10/3 created
 */
public class MultiIndexSegment implements SegmentFunction {
    private boolean isAll;
    private List<String> keys;
    private List<Integer> indices;

    public MultiIndexSegment(String segmentStr) {
        if (segmentStr.indexOf('*') >= 0) {
            //通配符
            isAll = true;
        } else if (segmentStr.indexOf('\'') >= 0) {
            //key
            this.keys = Arrays.stream(segmentStr.split(","))
                    .map(String::trim)
                    .map(k -> k.substring(1, k.length() - 1))
                    .collect(Collectors.toList());
        } else {
            //index
            this.indices = Arrays.stream(segmentStr.split(","))
                    .map(String::trim)
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());
        }
    }

    @Override
    public List<ONode> resolve(List<ONode> currentNodes, Context context, QueryMode mode) {
        List<ONode> result = new ArrayList<>();

        for (ONode n : currentNodes) {
            if (isAll) {
                if (n.isArray()) {
                    result.addAll(n.getArray());
                } else if (n.isObject()) {
                    for (Map.Entry<String, ONode> entry : n.getObject().entrySet()) {
                        result.add(entry.getValue());
                    }
                }
            } else if (keys != null) {
                for (String k : keys) {
                    if (n.isObject()) {
                        ONode p = n.get(k);
                        if (p != null) {
                            result.add(p);
                        }
                    }
                }
            } else {
                for (Integer idx : indices) {
                    if (n.isArray()) {
                        if (idx < 0) idx += n.size();
                        if (idx < 0 || idx >= n.size()) {
                            throw new PathResolutionException("Index out of bounds: " + idx);
                        }
                        ONode node = n.get(idx);
                        node.source = new JsonSource(n, null, idx);
                        result.add(node);
                    }
                }
            }

        }

        return result;
    }
}
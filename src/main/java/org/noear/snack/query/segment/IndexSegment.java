package org.noear.snack.query.segment;

import org.noear.snack.ONode;
import org.noear.snack.core.JsonSource;
import org.noear.snack.exception.PathResolutionException;
import org.noear.snack.query.Context;
import org.noear.snack.query.QueryMode;
import org.noear.snack.query.SegmentFunction;

import java.util.ArrayList;
import java.util.List;

/**
 * 处理精确索引（支持负数）
 *
 * @author noear 2025/10/3 created
 */
public class IndexSegment implements SegmentFunction {
    private String key;
    private int index;


    public IndexSegment(String segmentStr) {
        if (segmentStr.indexOf('\'') < 0) {
            index = Integer.parseInt(segmentStr);
        } else {
            key = segmentStr.substring(1, segmentStr.length() - 1);
        }
    }

    @Override
    public List<ONode> resolve(List<ONode> currentNodes, Context context, QueryMode mode) {
        List<ONode> result = new ArrayList<>();

        if (key != null) {
            forKey(currentNodes, mode, result);
        } else {
            forIndex(currentNodes, mode, result);
        }

        return result;
    }

    private void forKey(List<ONode> currentNodes, QueryMode mode, List<ONode> result) {
        currentNodes.stream()
                .filter(o -> {
                    if (mode == QueryMode.CREATE) {
                        o.newObject();
                        return true;
                    } else {
                        return o.isObject();
                    }
                })
                .map(obj -> {
                    if (mode == QueryMode.CREATE) {
                        obj.getOrNew(key);
                    }

                    ONode rst = obj.get(key);
                    rst.source = new JsonSource(obj, key, 0);

                    return rst;
                })
                .forEach(result::add);
    }

    private void forIndex(List<ONode> currentNodes, QueryMode mode, List<ONode> result) {
        currentNodes.stream()
                .filter(o -> {
                    if (mode == QueryMode.CREATE) {
                        o.newArray();
                        return true;
                    } else {
                        return o.isArray();
                    }
                })
                .map(arr -> {
                    int idx = index;
                    if (idx < 0) {
                        idx = arr.size() + idx;
                    }

                    if (mode == QueryMode.CREATE) {
                        int count = idx + 1 - arr.size();
                        for (int i = 0; i < count; i++) {
                            arr.add(new ONode());
                        }
                    }

                    if (idx < 0 || idx >= arr.size()) {
                        throw new PathResolutionException("Index out of bounds: " + idx);
                    }

                    ONode rst = arr.get(idx);
                    rst.source = new JsonSource(arr, null, idx);

                    return rst;
                })
                .forEach(result::add);
    }
}
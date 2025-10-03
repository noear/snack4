package org.noear.snack.query.segment;

import org.noear.snack.ONode;
import org.noear.snack.query.Context;
import org.noear.snack.query.Expression;
import org.noear.snack.query.QueryMode;
import org.noear.snack.query.SegmentFunction;

import java.util.ArrayList;
import java.util.List;

/**
 * 处理过滤器（如 [?(@.price > 10)]）
 *
 * @author noear 2025/10/3 created
 */
public class FilterSegment implements SegmentFunction {
    private final Expression expression;
    private final boolean flattened;

    /**
     * @param segmentStr `?...`
     */
    public FilterSegment(String segmentStr, boolean flattened) {
        this.expression = Expression.get(segmentStr.substring(1));
        this.flattened = flattened;
    }

    @Override
    public List<ONode> resolve(List<ONode> currentNodes, Context context, QueryMode mode) {
        if (this.flattened) {
            //已经偏平化
            List<ONode> result = new ArrayList<>();
            for (ONode n : currentNodes) {
                if (expression.test(n, context.root)) {
                    result.add(n);
                }
            }
            return result;
        } else {
            //还未偏平化
            List<ONode> result = new ArrayList<>();

            for (ONode n : currentNodes) {
                flattenResolve(n, context, result);
            }

            return result;
        }
    }

    // 新增递归展开方法
    private void flattenResolve(ONode node, Context context, List<ONode> result) {
        if (node.isArray()) {
            for (ONode n1 : node.getArray()) {
                flattenResolve(n1, context, result);
            }
        } else if (node.isNull() == false) {
            if (expression.test(node, context.root)) {
                result.add(node);
            }
        }
    }
}
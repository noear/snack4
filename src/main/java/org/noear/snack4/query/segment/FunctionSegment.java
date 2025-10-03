package org.noear.snack4.query.segment;

import org.noear.snack4.ONode;
import org.noear.snack4.query.Context;
import org.noear.snack4.query.FunctionLib;
import org.noear.snack4.query.QueryMode;
import org.noear.snack4.query.SegmentFunction;

import java.util.Collections;
import java.util.List;

/**
 *
 * @author noear
 * @since 4.0
 */
public class FunctionSegment implements SegmentFunction {
    private final String funcName;

    public FunctionSegment(String segmentStr) {
        this.funcName = segmentStr.substring(0, segmentStr.length() - 2);
    }

    @Override
    public List<ONode> resolve(List<ONode> currentNodes, Context context, QueryMode mode) {
        return Collections.singletonList(
                FunctionLib.get(funcName).apply(currentNodes) // 传入节点列表
        );
    }
}

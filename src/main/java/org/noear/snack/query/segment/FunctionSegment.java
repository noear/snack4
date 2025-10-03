package org.noear.snack.query.segment;

import org.noear.snack.ONode;
import org.noear.snack.query.Context;
import org.noear.snack.query.FunctionLib;
import org.noear.snack.query.QueryMode;
import org.noear.snack.query.SegmentFunction;

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

package org.noear.snack4.query;

import org.noear.snack4.ONode;

import java.util.List;

public interface SegmentFunction {
    List<ONode> resolve(List<ONode> currentNodes, Context context, QueryMode mode);
}
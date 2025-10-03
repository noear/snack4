package org.noear.snack.query;

import org.noear.snack.ONode;

import java.util.List;

public interface SegmentFunction {
    List<ONode> resolve(List<ONode> currentNodes, Context context, QueryMode mode);
}
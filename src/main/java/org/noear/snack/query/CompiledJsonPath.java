package org.noear.snack.query;

import org.noear.snack.ONode;

import java.util.Collections;
import java.util.List;

/**
 * @author noear
 */
public class CompiledJsonPath {
    private final String query;
    private final List<SegmentFunction> segments;
    private final boolean multiple;

    public CompiledJsonPath(String query,List<SegmentFunction> segments) {
        this.query = query;
        this.segments = segments;
        this.multiple = ((query.indexOf('?') < 0 && query.indexOf('*') < 0 && query.indexOf("..") < 0 && query.indexOf(",") < 0 && query.indexOf(":") < 0) || query.indexOf("()") > 0);
    }

    public ONode select(ONode root) {
        List<ONode> currentNodes = Collections.singletonList(root);
        Context context = new Context(root);
        for (SegmentFunction seg : segments) {
            currentNodes = seg.resolve(currentNodes, context, QueryMode.SELECT);
        }

        if (currentNodes.size() > 1) {
            return new ONode(currentNodes);
        } else {
            if (multiple) {
                if (currentNodes.size() > 0) {
                    return currentNodes.get(0);
                } else {
                    return new ONode();
                }
            } else {
                return new ONode(currentNodes);
            }
        }
    }

    public ONode create(ONode root) {
        List<ONode> currentNodes = Collections.singletonList(root);
        Context context = new Context(root);
        for (SegmentFunction seg : segments) {
            currentNodes = seg.resolve(currentNodes, context, QueryMode.CREATE);
        }

        if (currentNodes.size() > 1) {
            return new ONode(currentNodes);
        } else {
            if (multiple) {
                if (currentNodes.size() > 0) {
                    return currentNodes.get(0);
                } else {
                    return new ONode();
                }
            } else {
                return new ONode(currentNodes);
            }
        }
    }

    public void delete(ONode root) {
        List<ONode> currentNodes = Collections.singletonList(root);
        Context context = new Context(root);
        for (SegmentFunction seg : segments) {
            currentNodes = seg.resolve(currentNodes, context, QueryMode.DELETE);
        }

        if (currentNodes.size() == 1) {
            for (ONode n : currentNodes) {
                if (n.source != null) {
                    if (n.source.key != null) {
                        if ("*".equals(n.source.key)) {
                            n.source.parent.clear();
                        } else {
                            n.source.parent.remove(n.source.key);
                        }
                    } else {
                        n.source.parent.remove(n.source.index);
                    }
                }
            }
        }
    }
}
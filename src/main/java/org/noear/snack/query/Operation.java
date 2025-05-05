package org.noear.snack.query;

import org.noear.snack.ONode;

/**
 * @author noear 2025/5/5 created
 */
public interface Operation {
    boolean apply(ONode node, Condition condition, ONode root);
}

package org.noear.snack4.codec.encode;

import org.noear.snack4.ONode;
import org.noear.snack4.Options;
import org.noear.snack4.codec.NodeEncoder;

/**
 *
 * @author noear 2025/10/3 created
 *
 */
public class BooleanEncoder implements NodeEncoder<Boolean> {
    private static final BooleanEncoder instance = new BooleanEncoder();

    public static BooleanEncoder getInstance() {
        return instance;
    }

    @Override
    public ONode encode(Options opts, Boolean value) {
        return new ONode(value);
    }
}
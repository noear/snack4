package org.noear.snack4.codec.encode;

import org.noear.snack4.ONode;
import org.noear.snack4.Options;
import org.noear.snack4.codec.NodeEncoder;

/**
 *
 * @author noear 2025/10/3 created
 *
 */
public class IntegerEncoder implements NodeEncoder<Integer> {
    private static final IntegerEncoder instance = new IntegerEncoder();

    public static IntegerEncoder getInstance() {
        return instance;
    }

    @Override
    public ONode encode(Options opts, Integer value) {
        return new ONode(value);
    }
}
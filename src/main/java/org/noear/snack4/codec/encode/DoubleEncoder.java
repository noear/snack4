package org.noear.snack4.codec.encode;

import org.noear.snack4.ONode;
import org.noear.snack4.Options;
import org.noear.snack4.codec.NodeEncoder;

/**
 *
 * @author noear 2025/10/3 created
 *
 */
public class DoubleEncoder implements NodeEncoder<Double> {
    private static final DoubleEncoder instance = new DoubleEncoder();

    public static DoubleEncoder getInstance() {
        return instance;
    }

    @Override
    public ONode encode(Options opts, Double value) {
        return new ONode(value);
    }
}
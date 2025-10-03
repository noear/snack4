package org.noear.snack4.codec.decode;

import org.noear.snack4.ONode;
import org.noear.snack4.Options;
import org.noear.snack4.codec.NodeDecoder;

import java.lang.reflect.Type;

/**
 *
 * @author noear 2025/10/3 created
 *
 */
public class DoubleDecoder implements NodeDecoder<Double> {
    private static final DoubleDecoder instance = new DoubleDecoder();

    public static DoubleDecoder getInstance() {
        return instance;
    }

    @Override
    public Double decode(Options opts, ONode node, Class<?> clazz) {
        return node.getNumber().doubleValue();
    }
}

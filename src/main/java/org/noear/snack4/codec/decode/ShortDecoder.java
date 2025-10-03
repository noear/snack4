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
public class ShortDecoder implements NodeDecoder<Short> {
    private static final ShortDecoder instance = new ShortDecoder();

    public static ShortDecoder getInstance() {
        return instance;
    }

    @Override
    public Short decode(Options opts, ONode node, Class<?> clazz) {
        return node.getNumber().shortValue();
    }
}

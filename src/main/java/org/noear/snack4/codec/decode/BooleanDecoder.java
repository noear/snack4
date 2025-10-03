package org.noear.snack4.codec.decode;

import org.noear.snack4.ONode;
import org.noear.snack4.Options;
import org.noear.snack4.codec.NodeDecoder;

/**
 *
 * @author noear 2025/10/3 created
 *
 */
public class BooleanDecoder implements NodeDecoder<Boolean> {
    private static final BooleanDecoder instance = new BooleanDecoder();

    public static BooleanDecoder getInstance() {
        return instance;
    }

    @Override
    public Boolean decode(Options opts, ONode node, Class<?> clazz) {
        return node.getBoolean();
    }
}

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
public class LongDecoder implements NodeDecoder<Long> {
    private static final LongDecoder instance = new LongDecoder();

    public static LongDecoder getInstance() {
        return instance;
    }

    @Override
    public Long decode(Options opts, ONode node, Class<?> clazz) {
        return node.getNumber().longValue();
    }
}

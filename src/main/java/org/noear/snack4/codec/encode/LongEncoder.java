package org.noear.snack4.codec.encode;

import org.noear.snack4.ONode;
import org.noear.snack4.Options;
import org.noear.snack4.codec.NodeEncoder;

/**
 *
 * @author noear 2025/10/3 created
 *
 */
public class LongEncoder implements NodeEncoder<Long> {
    private static final LongEncoder instance = new LongEncoder();

    public static LongEncoder getInstance() {
        return instance;
    }

    @Override
    public ONode encode(Options opts, Long value) {
        return new ONode(value);
    }
}
package org.noear.snack4.codec.encode;

import org.noear.snack4.ONode;
import org.noear.snack4.Options;
import org.noear.snack4.codec.NodeEncoder;

/**
 *
 * @author noear 2025/10/3 created
 *
 */
public class EnumEncoder implements NodeEncoder<Enum> {
    private static final EnumEncoder instance = new EnumEncoder();

    public static EnumEncoder getInstance() {
        return instance;
    }

    @Override
    public ONode encode(Options opts, Enum value) {
        return new ONode(value.name());
    }
}
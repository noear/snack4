package org.noear.snack4.codec.encode;

import org.noear.snack4.ONode;
import org.noear.snack4.Options;
import org.noear.snack4.annotation.ONodeAttr;
import org.noear.snack4.codec.NodeEncoder;

/**
 *
 * @author noear 2025/10/3 created
 *
 */
public class ShortEncoder implements NodeEncoder<Short> {
    private static final ShortEncoder instance = new ShortEncoder();

    public static ShortEncoder getInstance() {
        return instance;
    }

    @Override
    public ONode encode(Options opts, ONodeAttr attr, Short value) {
        return new ONode(value);
    }
}
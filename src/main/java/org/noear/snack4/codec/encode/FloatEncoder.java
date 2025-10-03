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
public class FloatEncoder implements NodeEncoder<Float> {
    private static final FloatEncoder instance = new FloatEncoder();

    public static FloatEncoder getInstance() {
        return instance;
    }

    @Override
    public ONode encode(Options opts, ONodeAttr attr, Float value) {
        return new ONode(value);
    }
}
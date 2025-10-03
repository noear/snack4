package org.noear.snack4.codec.encode;

import org.noear.snack4.ONode;
import org.noear.snack4.Options;
import org.noear.snack4.codec.NodeEncoder;

/**
 *
 * @author noear 2025/10/3 created
 *
 */
public class ONodeEncoder implements NodeEncoder<ONode> {
    private static final ONodeEncoder instance = new ONodeEncoder();

    public static ONodeEncoder getInstance() {
        return instance;
    }

    @Override
    public ONode encode(Options opts, ONode value) {
        return value;
    }
}
package org.noear.snack4.codec.encode;

import org.noear.snack4.ONode;
import org.noear.snack4.Options;
import org.noear.snack4.annotation.ONodeAttr;
import org.noear.snack4.codec.NodeEncoder;
import org.noear.snack4.codec.util.BeanUtil;

import java.sql.Clob;

/**
 *
 * @author noear 2025/10/3 created
 *
 */
public class ClobEncoder implements NodeEncoder<Clob> {
    @Override
    public ONode encode(Options opts, ONodeAttr attr, Clob value) {
        return new ONode(BeanUtil.clobToString(value));
    }
}

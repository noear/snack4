package org.noear.snack4.codec;


import org.noear.snack4.ONode;
import org.noear.snack4.Options;

import java.lang.reflect.Type;

/**
 * ONode 解码（用于控制自定义解码）
 *
 * @author noear
 * @since 3.2
 */
public interface NodeDecoder<T> {
    T decode(Options opts, ONode node, Class<?> clazz);
}

package org.noear.snack4.codec;


import org.noear.snack4.ONode;
import org.noear.snack4.Options;

/**
 * ONode 编码（用于自定义编码）
 *
 * @author noear
 * @since 3.2
 * */
public interface NodeEncoder<T> {
    ONode encode(Options opts, T value);
}

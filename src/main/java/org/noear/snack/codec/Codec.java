package org.noear.snack.codec;

import org.noear.snack.ONode;

// 自定义编解码器注册
public interface Codec<T> {
    void encode(ONode node, T value);

    T decode(ONode node);
}
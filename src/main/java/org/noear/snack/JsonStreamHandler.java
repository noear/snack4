package org.noear.snack;

import java.io.IOException;

// 流式解析接口
public interface JsonStreamHandler {
    void startObject() throws IOException;
    void endObject() throws IOException;
    void startArray() throws IOException;
    void endArray() throws IOException;
    void key(String key) throws IOException;
    void value(ONode value) throws IOException;
}
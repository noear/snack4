package org.noear.snack.stream;

import org.noear.snack.ONode;
import org.noear.snack.core.JsonReader;

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;

/**
 * 流式JSON解析器
 */
public class JsonStreamParser {
    private final JsonReader parser;

    public JsonStreamParser(String json) {
        this(new JsonReader(new StringReader(json)));
    }

    public JsonStreamParser(JsonReader parser) {
        this.parser = parser;
    }

    public void parse(JsonStreamHandler handler) throws IOException {
        ONode root = parser.read();
        traverse(root, handler);
    }

    private void traverse(ONode node, JsonStreamHandler handler) throws IOException {
        if (node.isObject()) {
            handler.startObject();
            for (Map.Entry<String, ONode> entry : node.getObject().entrySet()) {
                handler.key(entry.getKey());
                traverse(entry.getValue(), handler);
            }
            handler.endObject();
        } else if (node.isArray()) {
            handler.startArray();
            for (ONode item : node.getArray()) {
                traverse(item, handler);
            }
            handler.endArray();
        } else {
            handler.value(node);
        }
    }
}
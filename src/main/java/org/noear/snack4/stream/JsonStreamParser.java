/*
 * Copyright 2005-2025 noear.org and authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.noear.snack4.stream;

import org.noear.snack4.ONode;
import org.noear.snack4.json.JsonReader;

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
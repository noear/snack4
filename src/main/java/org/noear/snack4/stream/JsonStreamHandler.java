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

import java.io.IOException;
/**
 * 流式处理回调接口
 */
public interface JsonStreamHandler {
    void startObject() throws IOException;
    void endObject() throws IOException;
    void startArray() throws IOException;
    void endArray() throws IOException;
    void key(String key) throws IOException;
    void value(ONode value) throws IOException;
}
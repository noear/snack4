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
package org.noear.snack4.jsonschema.rule;

import org.noear.snack4.ONode;
import org.noear.snack4.exception.SchemaException;
import org.noear.snack4.json.JsonType;

import java.util.HashSet;
import java.util.Set;

/**
 * 类型验证规则实现
 */
public class TypeRule implements ValidationRule {
    private final Set<String> allowedTypes;

    public TypeRule(ONode typeNode) {
        this.allowedTypes = new HashSet<>();
        if (typeNode.isString()) {
            allowedTypes.add(typeNode.getString());
        } else if (typeNode.isArray()) {
            for (ONode t : typeNode.getArray()) {
                allowedTypes.add(t.getString());
            }
        }
    }

    @Override
    public void validate(ONode data) throws SchemaException {
        String actualType = JsonType.getTypeName(data.getType());
        if (!allowedTypes.contains(actualType)) {
            throw new SchemaException("Type mismatch. Expected: " + allowedTypes + ", Actual: " + actualType);
        }
    }
}
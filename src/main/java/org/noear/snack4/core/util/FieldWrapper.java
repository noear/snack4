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
package org.noear.snack4.core.util;

import org.noear.snack4.annotation.ONodeAttr;
import org.noear.snack4.core.NodeCodec;
import org.noear.snack4.exception.AnnotationProcessException;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * @author noear 2025/3/16 created
 */
public class FieldWrapper {
    private final Field field;
    private final ONodeAttr attr;

    private String alias;
    private boolean ignore;
    private NodeCodec codec;

    public FieldWrapper(Field field) {
        this.field = field;
        this.attr = field.getAnnotation(ONodeAttr.class);

        field.setAccessible(true);

        if (attr != null) {
            alias = attr.alias();
            ignore = attr.ignore();

            codec = ReflectionUtil.newInstance(attr.codec(), e -> new AnnotationProcessException("Failed to create codec for field: " + field.getName(), e));
        }

        if (Modifier.isTransient(field.getModifiers())) {
            ignore = true;
        }
    }

    public Field getField() {
        return field;
    }

    public String getAliasName() {
        if (alias == null) {
            return field.getName();
        } else {
            return alias;
        }
    }

    public NodeCodec getCodec() {
        return codec;
    }

    public boolean isIgnore() {
        return ignore;
    }
}
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
package org.noear.snack4.core;

import org.noear.snack4.ONode;
import org.noear.snack4.core.util.FieldWrapper;
import org.noear.snack4.core.util.ReflectionUtil;

import java.lang.reflect.Array;
import java.util.*;

/**
 * Bean 编码器
 *
 * @author noear
 * @since 4.0
 */
public class BeanEncoder {

    // 序列化：对象转ONode
    public static ONode serialize(Object bean) {
        return serialize(bean, Options.def());
    }

    public static ONode serialize(Object bean, Options opts) {
        if (bean == null) {
            return new ONode(null);
        }
        try {
            return convertBeanToNode(bean, new IdentityHashMap<>(), opts);
        } catch (Throwable e) {
            if (e instanceof StackOverflowError) {
                throw (StackOverflowError) e;
            } else if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new RuntimeException("Failed to convert bean to ONode", e);
            }
        }
    }


    // 对象转ONode核心逻辑
    private static ONode convertBeanToNode(Object bean, Map<Object, Object> visited, Options opts) throws Exception {
        if (bean == null) {
            return new ONode(null);
        }

        if (bean instanceof ONode) {
            return (ONode) bean;
        }

        // 优先使用自定义编解码器
        NodeCodec<Object> codec = (NodeCodec<Object>) opts.getCodecRegistry().get(bean.getClass());
        if (codec != null) {
            return codec.encode(bean, new ONode());
        }

        // 处理Properties类型
        if (bean instanceof Properties) {
            return convertPropertiesToNode((Properties) bean, visited, opts);
        }

        // 循环引用检测
        if (visited.containsKey(bean)) {
            throw new StackOverflowError("Circular reference detected: " + bean.getClass().getName());
        }
        visited.put(bean, null);

        Class<?> clazz = bean.getClass();

        if (clazz.isArray()) {
            //数组
            return convertArrayToNode(bean, visited, opts);
        } else if (bean instanceof Collection) {
            //集合
            return convertCollectionToNode((Collection<?>) bean, visited, opts);
        } else if (bean instanceof Map) {
            //字典
            return convertMapToNode((Map) bean, visited, opts);
        } else {
            ONode node = new ONode(new LinkedHashMap<>());

            for (FieldWrapper field : ReflectionUtil.getDeclaredFields(clazz)) {
                Object value = field.getField().get(bean);
                ONode fieldNode = convertValueToNode(value, visited, opts);
                node.set(field.getAliasName(), fieldNode);
            }

            return node;
        }
    }

    // 处理Properties类型
    private static ONode convertPropertiesToNode(Properties properties, Map<Object, Object> visited, Options opts) throws Exception {
        ONode rootNode = new ONode(new LinkedHashMap<>());
        for (String key : properties.stringPropertyNames()) {
            String value = properties.getProperty(key);
            setNestedValue(rootNode, key, value);
        }
        return rootNode;
    }

    // 值转ONode处理
    private static ONode convertValueToNode(Object value, Map<Object, Object> visited, Options opts) throws Exception {
        if (value == null) {
            return new ONode(null);
        }

        // 优先使用自定义编解码器
        NodeCodec<Object> codec = (NodeCodec<Object>) opts.getCodecRegistry().get(value.getClass());
        if (codec != null) {
           return codec.encode(value, new ONode());
        }

        if (value instanceof ONode) {
            return (ONode) value;
        } else if (value instanceof Number) {
            return new ONode((Number) value);
        } else if (value instanceof Boolean) {
            return new ONode((Boolean) value);
        } else if (value instanceof String) {
            return new ONode((String) value);
        } else if (value instanceof Enum) {
            return new ONode(((Enum<?>) value).name());
        } else if (value instanceof Collection) {
            return convertCollectionToNode((Collection<?>) value, visited, opts);
        } else if (value instanceof Map) {
            return convertMapToNode((Map<?, ?>) value, visited, opts);
        } else {
            if (value.getClass().isArray()) {
                return convertArrayToNode(value, visited, opts);
            } else {
                return convertBeanToNode(value, visited, opts);
            }
        }
    }

    // 处理数组类型
    private static ONode convertArrayToNode(Object array, Map<Object, Object> visited, Options opts) throws Exception {
        ONode arrayNode = new ONode(new ArrayList<>());
        int length = Array.getLength(array);
        for (int i = 0; i < length; i++) {
            Object element = Array.get(array, i);
            arrayNode.add(convertValueToNode(element, visited, opts));
        }
        return arrayNode;
    }

    // 处理集合类型
    private static ONode convertCollectionToNode(Collection<?> collection, Map<Object, Object> visited, Options opts) throws Exception {
        ONode arrayNode = new ONode(new ArrayList<>());
        for (Object item : collection) {
            arrayNode.add(convertValueToNode(item, visited, opts));
        }
        return arrayNode;
    }

    // 处理Map类型
    private static ONode convertMapToNode(Map<?, ?> map, Map<Object, Object> visited, Options opts) throws Exception {
        ONode objNode = new ONode(new LinkedHashMap<>());
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = entry.getKey().toString();
            ONode valueNode = convertValueToNode(entry.getValue(), visited, opts);
            objNode.set(key, valueNode);
        }
        return objNode;
    }


    // 设置嵌套值
    private static void setNestedValue(ONode node, String key, String value) {
        String[] parts = key.split("\\.");
        ONode current = node;
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (i == parts.length - 1) {
                current.set(part, new ONode(value));
            } else {
                if (part.endsWith("]")) {
                    // 处理数组
                    String arrayName = part.substring(0, part.indexOf('['));
                    int index = Integer.parseInt(part.substring(part.indexOf('[') + 1, part.indexOf(']')));
                    ONode arrayNode = current.get(arrayName);
                    if (arrayNode == null || arrayNode.isNull()) {
                        arrayNode = new ONode(new ArrayList<>());
                        current.set(arrayName, arrayNode);
                    }
                    while (arrayNode.getArray().size() <= index) {
                        arrayNode.add(new ONode(new LinkedHashMap<>()));
                    }
                    current = arrayNode.get(index);
                } else {
                    ONode nextNode = current.get(part);
                    if (nextNode == null || nextNode.isNull()) {
                        nextNode = new ONode(new LinkedHashMap<>());
                        current.set(part, nextNode);
                    }
                    current = nextNode;
                }
            }
        }
    }
}
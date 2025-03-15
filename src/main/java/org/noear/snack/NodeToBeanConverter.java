package org.noear.snack;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 将 Java Bean 转为 ONode 的转换器
 */
public class NodeToBeanConverter {

    /**
     * 将 Java Bean 转为 ONode
     */
    public static ONode toNode(Object bean) {
        if (bean == null) {
            return new ONode(null);
        }
        try {
            return convertBeanToNode(bean);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert Bean to ONode", e);
        }
    }

    private static ONode convertBeanToNode(Object bean) throws Exception {
        ONode node = new ONode(new LinkedHashMap<>());
        for (Field field : bean.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            String fieldName = field.getName();
            Object value = field.get(bean);
            node.set(fieldName, convertValueToNode(value));
        }
        return node;
    }

    private static ONode convertValueToNode(Object value) {
        if (value == null) {
            return new ONode(null);
        } else if (value instanceof String) {
            return new ONode((String) value);
        } else if (value instanceof Number) {
            return new ONode((Number) value);
        } else if (value instanceof Boolean) {
            return new ONode((Boolean) value);
        } else if (value instanceof List) {
            return convertListToNode((List<?>) value);
        } else if (value instanceof Map) {
            return convertMapToNode((Map<?, ?>) value);
        } else {
            return toNode(value);
        }
    }

    private static ONode convertListToNode(List<?> list) {
        ONode node = new ONode(new ArrayList<>());
        for (Object item : list) {
            node.add(convertValueToNode(item));
        }
        return node;
    }

    private static ONode convertMapToNode(Map<?, ?> map) {
        ONode node = new ONode(new LinkedHashMap<>());
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            node.set(entry.getKey().toString(), convertValueToNode(entry.getValue()));
        }
        return node;
    }
}
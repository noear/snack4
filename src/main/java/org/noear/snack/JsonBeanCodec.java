package org.noear.snack;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 高性能 Java Bean 和 JSON 双向转换器
 */
public class JsonBeanCodec {
    private static final Map<Class<?>, Map<String, Field>> FIELD_CACHE = new ConcurrentHashMap<>();

    public static <T> T toBean(ONode node, Class<T> clazz) {
        if (node == null || clazz == null) return null;
        try {
            return convertNodeToBean(node, clazz);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to convert ONode to Bean", e);
        }
    }

    public static ONode toNode(Object bean) {
        if (bean == null) return new ONode(null);
        try {
            return convertBeanToNode(bean);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to convert Bean to ONode", e);
        }
    }

    private static <T> T convertNodeToBean(ONode node, Class<T> clazz) throws Exception {
        T bean = clazz.getDeclaredConstructor().newInstance();
        Map<String, Field> fields = getFields(clazz);
        for (Map.Entry<String, Field> entry : fields.entrySet()) {
            String fieldName = entry.getKey();
            Field field = entry.getValue();
            field.setAccessible(true);

            if (node.hasKey(fieldName)) {
                ONode fieldNode = node.get(fieldName);
                Object value = convertValue(fieldNode, field.getGenericType());
                field.set(bean, value);
            } else {
                // 处理基本类型的默认值
                setDefaultValueIfPrimitive(bean, field);
            }
        }
        return bean;
    }

    private static ONode convertBeanToNode(Object bean) throws Exception {
        ONode node = new ONode(new HashMap<>());
        Map<String, Field> fields = getFields(bean.getClass());
        for (Map.Entry<String, Field> entry : fields.entrySet()) {
            String fieldName = entry.getKey();
            Field field = entry.getValue();
            field.setAccessible(true);
            Object value = field.get(bean);
            node.set(fieldName, convertValueToNode(value));
        }
        return node;
    }

    private static Object convertValue(ONode node, Type targetType) {
        if (node.isNull()) return null;

        try {
            if (targetType instanceof ParameterizedType) {
                ParameterizedType pType = (ParameterizedType) targetType;
                Class<?> rawType = (Class<?>) pType.getRawType();

                if (List.class.isAssignableFrom(rawType)) {
                    Type elementType = pType.getActualTypeArguments()[0];
                    return node.getArray().stream()
                            .map(itemNode -> convertValue(itemNode, elementType))
                            .collect(Collectors.toList());
                } else if (Map.class.isAssignableFrom(rawType)) {
                    Type keyType = pType.getActualTypeArguments()[0];
                    Type valueType = pType.getActualTypeArguments()[1];
                    Map<Object, Object> map = new HashMap<>();
                    node.getObject().forEach((key, valueNode) -> {
                        Object k = convertValue(new ONode(key), keyType);
                        Object v = convertValue(valueNode, valueType);
                        map.put(k, v);
                    });
                    return map;
                }
            }

            Class<?> clazz = (Class<?>) targetType;
            if (clazz == String.class) return node.getString();
            if (clazz == Integer.class || clazz == int.class) return node.getInt();
            if (clazz == Long.class || clazz == long.class) return node.getLong();
            if (clazz == Double.class || clazz == double.class) return node.getDouble();
            if (clazz == Boolean.class || clazz == boolean.class) return node.getBoolean();
            if (clazz.isEnum()) return Enum.valueOf((Class<Enum>) clazz, node.getString());

            // 处理嵌套对象
            return toBean(node, clazz);
        } catch (Exception e) {
            throw new IllegalArgumentException("Convert error for type: " + targetType, e);
        }
    }

    private static ONode convertValueToNode(Object value) {
        if (value == null) return new ONode(null);
        if (value instanceof String) return new ONode((String) value);
        if (value instanceof Number) return new ONode((Number) value);
        if (value instanceof Boolean) return new ONode((Boolean) value);
        if (value instanceof List) return convertListToNode((List<?>) value);
        if (value instanceof Map) return convertMapToNode((Map<?, ?>) value);
        if (value.getClass().isEnum()) return new ONode(((Enum<?>) value).name());
        return toNode(value);
    }

    private static void setDefaultValueIfPrimitive(Object bean, Field field) throws IllegalAccessException {
        Class<?> type = field.getType();
        if (type.isPrimitive()) {
            if (type == int.class) field.setInt(bean, 0);
            else if (type == long.class) field.setLong(bean, 0L);
            else if (type == boolean.class) field.setBoolean(bean, false);
            else if (type == double.class) field.setDouble(bean, 0.0);
            // 其他基本类型类似处理
        }
    }

    private static ONode convertListToNode(List<?> list) {
        ONode node = new ONode(new ArrayList<>());
        list.forEach(item -> node.add(convertValueToNode(item)));
        return node;
    }

    private static ONode convertMapToNode(Map<?, ?> map) {
        ONode node = new ONode(new HashMap<>());
        map.forEach((k, v) -> node.set(k.toString(), convertValueToNode(v)));
        return node;
    }

    private static Map<String, Field> getFields(Class<?> clazz) {
        return FIELD_CACHE.computeIfAbsent(clazz, k -> {
            Map<String, Field> fields = new HashMap<>();
            while (k != null) {
                for (Field field : k.getDeclaredFields()) {
                    field.setAccessible(true);
                    fields.put(field.getName(), field);
                }
                k = k.getSuperclass(); // 处理继承字段
            }
            return fields;
        });
    }
}
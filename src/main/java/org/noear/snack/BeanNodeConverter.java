package org.noear.snack;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Java Bean 和 ONode 双向转换器
 */
public class BeanNodeConverter {
    private static final Map<Class<?>, List<Field>> FIELD_CACHE = new ConcurrentHashMap<>();

    /**
     * 将 ONode 转为指定类型的 Java Bean
     */
    public static <T> T toBean(ONode node, Class<T> clazz) {
        if (node == null || clazz == null) {
            return null;
        }
        try {
            return convertNodeToBean(node, clazz);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert ONode to Bean", e);
        }
    }

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

    private static <T> T convertNodeToBean(ONode node, Class<T> clazz) throws Exception {
        T bean = clazz.getDeclaredConstructor().newInstance();
        for (Field field : getFields(clazz)) {
            String fieldName = field.getName();
            if (node.hasKey(fieldName)) {
                ONode fieldNode = node.get(fieldName);
                Object value = convertValue(fieldNode, field.getGenericType());
                field.set(bean, value);
            }
        }
        return bean;
    }

    private static ONode convertBeanToNode(Object bean) throws Exception {
        ONode node = new ONode(new HashMap<>());
        for (Field field : getFields(bean.getClass())) {
            String fieldName = field.getName();
            Object value = field.get(bean);
            node.set(fieldName, convertValueToNode(value));
        }
        return node;
    }

    private static Object convertValue(ONode node, Type type) {
        if (node.isNull()) {
            return null;
        }
        if (type instanceof Class) {
            Class<?> clazz = (Class<?>) type;
            if (clazz == String.class) {
                return node.getString();
            } else if (clazz == Integer.class || clazz == int.class) {
                return node.getInt();
            } else if (clazz == Long.class || clazz == long.class) {
                return node.getLong();
            } else if (clazz == Double.class || clazz == double.class) {
                return node.getDouble();
            } else if (clazz == Boolean.class || clazz == boolean.class) {
                return node.getBoolean();
            } else if (clazz == List.class) {
                return convertToList(node, String.class); // 默认处理为 String 列表
            } else if (clazz == Map.class) {
                return convertToMap(node, String.class, String.class); // 默认处理为 String -> String 的 Map
            } else {
                return toBean(node, clazz);
            }
        } else if (type instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) type;
            Type rawType = pType.getRawType();
            if (rawType == List.class) {
                Type elementType = pType.getActualTypeArguments()[0];
                return convertToList(node, (Class<?>) elementType);
            } else if (rawType == Map.class) {
                Type keyType = pType.getActualTypeArguments()[0];
                Type valueType = pType.getActualTypeArguments()[1];
                return convertToMap(node, (Class<?>) keyType, (Class<?>) valueType);
            }
        }
        throw new IllegalArgumentException("Unsupported type: " + type);
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

    private static <T> List<T> convertToList(ONode node, Class<T> elementType) {
        return node.getArray().stream()
                .map(item -> (T) convertValue(item, elementType))
                .collect(Collectors.toList());
    }

    private static <K, V> Map<K, V> convertToMap(ONode node, Class<K> keyType, Class<V> valueType) {
        Map<K, V> map = new HashMap<>();
        for (Map.Entry<String, ONode> entry : node.getObject().entrySet()) {
            K key = (K) convertValue(new ONode(entry.getKey()), keyType);
            V value = (V) convertValue(entry.getValue(), valueType);
            map.put(key, value);
        }
        return map;
    }

    private static ONode convertListToNode(List<?> list) {
        ONode node = new ONode(new ArrayList<>());
        list.forEach(item -> node.add(convertValueToNode(item)));
        return node;
    }

    private static ONode convertMapToNode(Map<?, ?> map) {
        ONode node = new ONode(new HashMap<>());
        map.forEach((key, value) -> node.set(key.toString(), convertValueToNode(value)));
        return node;
    }

    private static List<Field> getFields(Class<?> clazz) {
        return FIELD_CACHE.computeIfAbsent(clazz, k -> {
            List<Field> fields = new ArrayList<>();
            for (Field field : k.getDeclaredFields()) {
                field.setAccessible(true);
                fields.add(field);
            }
            return fields;
        });
    }
}
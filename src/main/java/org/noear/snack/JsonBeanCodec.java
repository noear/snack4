package org.noear.snack;

import java.lang.reflect.Constructor;
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
            // 添加详细错误日志
            System.err.println("Conversion failed for class: " + clazz.getName());
            System.err.println("JSON content: " + node.toJsonString());
            e.printStackTrace();
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
        Constructor<T> constructor = clazz.getDeclaredConstructor();
        constructor.setAccessible(true);
        T bean = constructor.newInstance();
        Map<String, Field> fields = getFields(clazz);

        for (Map.Entry<String, Field> entry : fields.entrySet()) {
            String fieldName = entry.getKey();
            Field field = entry.getValue();
            field.setAccessible(true);

            ONode fieldNode = node.get(fieldName);
            if (fieldNode != null && !fieldNode.isNull()) {
                Object value = convertValue(fieldNode, field.getGenericType());
                field.set(bean, value);
            } else {
                // 确保处理所有基本类型
                setDefaultValueIfPrimitive(bean, field);
            }
        }
        return bean;
    }

    private static ONode convertBeanToNode(Object bean) throws Exception {
        return convertBeanToNode(bean, new IdentityHashMap<>());
    }

    private static ONode convertBeanToNode(Object bean, Map<Object, Object> visited) throws Exception {
        if (visited.containsKey(bean)) {
            throw new StackOverflowError("Circular reference detected");
        }
        visited.put(bean, null);

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

    // 修改 convertValue 方法中的泛型处理逻辑
    private static Object convertValue(ONode node, Type targetType) {
        if (node.isNull()) return null;

        try {
            // 处理泛型类型
            if (targetType instanceof ParameterizedType) {
                ParameterizedType pType = (ParameterizedType) targetType;
                Class<?> rawType = (Class<?>) pType.getRawType();

                if (List.class.isAssignableFrom(rawType)) {
                    Type elementType = pType.getActualTypeArguments()[0];
                    return node.getArray().stream()
                            .map(itemNode -> convertValue(itemNode, elementType))
                            .collect(Collectors.toCollection(ArrayList::new)); // 明确使用ArrayList
                } else if (Map.class.isAssignableFrom(rawType)) {
                    Type[] typeArgs = pType.getActualTypeArguments();
                    Type keyType = typeArgs[0];
                    Type valueType = typeArgs[1];

                    Map<Object, Object> map = new LinkedHashMap<>();
                    node.getObject().forEach((key, valueNode) -> {
                        Object k = convertKey(key, keyType);
                        Object v = convertValue(valueNode, valueType);
                        map.put(k, v);
                    });
                    return map;
                }
            }

            // 处理基本类型与包装类型
            Class<?> clazz = (Class<?>) (targetType instanceof Class ? targetType :
                    ((ParameterizedType) targetType).getRawType());

            // 新增类型转换逻辑
            if (clazz == String.class) {
                return node.getString();
            } else if (clazz == Integer.class || clazz == int.class) {
                return node.getNumber().intValue();
            } else if (clazz == Long.class || clazz == long.class) {
                return node.getNumber().longValue();
            } else if (clazz == Double.class || clazz == double.class) {
                return node.getNumber().doubleValue();
            } else if (clazz == Boolean.class || clazz == boolean.class) {
                return node.getBoolean();
            } else if (clazz.isEnum()) {
                String enumValue = node.getString();
                try {
                    return Enum.valueOf((Class<Enum>) clazz, enumValue);
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException(
                            "Invalid enum value '" + enumValue + "' for " + clazz.getName(),
                            e
                    );
                }
            }

            // 处理嵌套对象
            return toBean(node, clazz);
        } catch (Exception e) {
            throw new RuntimeException("Convert error for type: " + targetType, e);
        }
    }

    // 新增键值转换方法
    private static Object convertKey(String key, Type keyType) {
        if (keyType == String.class) return key;
        if (keyType == Integer.class || keyType == int.class) return Integer.parseInt(key);
        if (keyType == Long.class || keyType == long.class) return Long.parseLong(key);
        throw new IllegalArgumentException("Unsupported map key type: " + keyType);
    }

    private static ONode convertValueToNode(Object value) {
        if (value == null) return new ONode(null);
        if (value instanceof ONode) return (ONode) value;

        // 处理所有数值类型
        if (value instanceof Number) {
            return new ONode((Number) value);
        } else if (value instanceof Boolean) {
            return new ONode((Boolean) value);
        } else if (value instanceof String) {
            return new ONode((String) value);
        } else if (value instanceof List) {
            return convertListToNode((List<?>) value);
        } else if (value instanceof Map) {
            return convertMapToNode((Map<?, ?>) value);
        } else if (value.getClass().isEnum()) {
            return new ONode(((Enum<?>) value).name());
        } else {
            return toNode(value);
        }
    }

    private static void setDefaultValueIfPrimitive(Object bean, Field field) throws IllegalAccessException {
        Class<?> type = field.getType();
        if (type.isPrimitive()) {
            if (type == int.class) {
                field.setInt(bean, 0);
            } else if (type == long.class) {
                field.setLong(bean, 0L);
            } else if (type == boolean.class) {
                field.setBoolean(bean, false);
            } else if (type == double.class) {
                field.setDouble(bean, 0.0);
            } else if (type == float.class) {
                field.setFloat(bean, 0.0f);
            } else if (type == short.class) {
                field.setShort(bean, (short) 0);
            } else if (type == byte.class) {
                field.setByte(bean, (byte) 0);
            } else if (type == char.class) {
                field.setChar(bean, '\u0000');
            }
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
            Class<?> current = clazz;
            while (current != null) {
                for (Field field : current.getDeclaredFields()) {
                    try {
                        // 强制突破访问限制
                        field.setAccessible(true);
                        fields.put(field.getName(), field);
                    } catch (Exception e) {
                        System.err.println("Field access error: " + field.getName());
                    }
                }
                current = current.getSuperclass();
            }
            return fields;
        });
    }
}
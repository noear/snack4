package org.noear.snack.format;

import org.noear.snack.ONode;
import org.noear.snack.core.Options;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class BeanCodec {
    private static final Map<Class<?>, Map<String, Field>> FIELD_CACHE = new ConcurrentHashMap<>();

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

    // 反序列化：ONode转对象
    public static <T> T deserialize(ONode node, Class<T> clazz) {
        return deserialize(node,clazz, Options.def());
    }

    public static <T> T deserialize(ONode node, Class<T> clazz, Options opts) {
        if (node == null || clazz == null) {
            return null;
        }
        try {
            return convertNodeToBean(node, clazz, new IdentityHashMap<>(), opts);
        } catch (Throwable e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException(e);
        }
    }

    //-- 私有方法 --//

    // 对象转ONode核心逻辑
    private static ONode convertBeanToNode(Object bean, Map<Object, Object> visited, Options opts) throws Exception {
        if (bean == null) {
            return new ONode(null);
        }

        // 优先使用自定义编解码器
        Codec<Object> codec = (Codec<Object>) opts.getCodecRegistry().get(bean.getClass());
        if (codec != null) {
            ONode node = new ONode();
            codec.encode(node, bean);
            return node;
        }

        // 循环引用检测
        if (visited.containsKey(bean)) {
            throw new StackOverflowError("Circular reference detected: " + bean.getClass().getName());
        }
        visited.put(bean, null);

        Class<?> clazz = bean.getClass();
        ONode node = new ONode(new LinkedHashMap<>());

        for (Field field : getFields(clazz).values()) {
            field.setAccessible(true);
            Object value = field.get(bean);
            ONode fieldNode = convertValueToNode(value, visited, opts);
            node.set(field.getName(), fieldNode);
        }

        return node;
    }

    // 值转ONode处理
    private static ONode convertValueToNode(Object value, Map<Object, Object> visited, Options opts) throws Exception {
        if (value == null) {
            return new ONode(null);
        }

        // 优先使用自定义编解码器
        Codec<Object> codec = (Codec<Object>) opts.getCodecRegistry().get(value.getClass());
        if (codec != null) {
            ONode node = new ONode();
            codec.encode(node, value);
            return node;
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
            return convertBeanToNode(value, visited, opts);
        }
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

    //-- 反序列化逻辑 --//

    @SuppressWarnings("unchecked")
    private static <T> T convertNodeToBean(ONode node, Class<T> clazz, Map<Object, Object> visited, Options opts) throws Exception {
        // 优先使用自定义编解码器
        Codec<T> codec = (Codec<T>) opts.getCodecRegistry().get(clazz);
        if (codec != null) {
            return codec.decode(node);
        }

        Constructor<T> constructor = clazz.getDeclaredConstructor();
        constructor.setAccessible(true);
        T bean = constructor.newInstance();

        for (String fieldName : getFields(clazz).keySet()) {
            Field field = getFields(clazz).get(fieldName);
            ONode fieldNode = node.get(fieldName);

            if (fieldNode != null && !fieldNode.isNull()) {
                Object value = convertValue(fieldNode, field.getGenericType(), visited, opts);
                field.set(bean, value);
            } else {
                setPrimitiveDefault(field, bean);
            }
        }
        return bean;
    }

    // 类型转换核心
    private static Object convertValue(ONode node, Type targetType, Map<Object, Object> visited, Options opts) throws Exception {
        if (node.isNull()) {
            return null;
        }

        // 处理泛型类型
        if (targetType instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) targetType;
            Type rawType = pType.getRawType();

            if (List.class.isAssignableFrom((Class<?>) rawType)) {
                return convertToList(node, pType.getActualTypeArguments()[0], visited, opts);
            } else if (Map.class.isAssignableFrom((Class<?>) rawType)) {
                Type[] typeArgs = pType.getActualTypeArguments();
                return convertToMap(node, typeArgs[0], typeArgs[1], visited, opts);
            }
        }

        // 处理基本类型
        Class<?> clazz = (Class<?>) (targetType instanceof Class ? targetType : ((ParameterizedType) targetType).getRawType());

        // 优先使用自定义编解码器
        Codec<?> codec = opts.getCodecRegistry().get(clazz);
        if (codec != null) {
            return codec.decode(node);
        }

        if (clazz == String.class) return node.getString();
        if (clazz == Integer.class || clazz == int.class) return node.getInt();
        if (clazz == Long.class || clazz == long.class) return node.getLong();
        if (clazz == Double.class || clazz == double.class) return node.getDouble();
        if (clazz == Boolean.class || clazz == boolean.class) return node.getBoolean();
        if (clazz.isEnum()) return convertToEnum(node, clazz);
        if (clazz == Object.class) return node.getValue();

        // 处理嵌套对象
        return convertNodeToBean(node, clazz, visited, opts);
    }

    //-- 辅助方法 --//

    // 枚举转换
    private static <E extends Enum<E>> E convertToEnum(ONode node, Class<?> clazz) {
        String value = node.getString();
        try {
            return Enum.valueOf((Class<E>) clazz, value);
        } catch (IllegalArgumentException e) {
            E[] constants = ((Class<E>) clazz).getEnumConstants();
            String valid = Arrays.stream(constants)
                    .map(Enum::name)
                    .collect(Collectors.joining(", "));
            throw new IllegalArgumentException(
                    "Invalid enum value: '" + value + "'. Valid values: " + valid, e);
        }
    }

    // 处理List泛型
    private static List<?> convertToList(ONode node, Type elementType, Map<Object, Object> visited, Options opts) throws Exception {
        List<Object> list = new ArrayList<>();
        for (ONode itemNode : node.getArray()) {
            list.add(convertValue(itemNode, elementType, visited, opts));
        }
        return list;
    }

    // 处理Map泛型
    private static Map<?, ?> convertToMap(ONode node, Type keyType, Type valueType, Map<Object, Object> visited, Options opts) throws Exception {
        Map<Object, Object> map = new LinkedHashMap<>();

        for (Map.Entry<String, ONode> kv : node.getObject().entrySet()) {
            Object k = convertKey(kv.getKey(), keyType);
            Object v = convertValue(kv.getValue(), valueType, visited, opts);
            map.put(k, v);
        }

        return map;
    }

    // Map键类型转换
    private static Object convertKey(String key, Type keyType) {
        if (keyType == String.class) return key;
        if (keyType == Integer.class || keyType == int.class) return Integer.parseInt(key);
        if (keyType == Long.class || keyType == long.class) return Long.parseLong(key);
        throw new IllegalArgumentException("Unsupported map key type: " + keyType);
    }

    // 基本类型默认值
    private static void setPrimitiveDefault(Field field, Object bean) throws IllegalAccessException {
        Class<?> type = field.getType();
        if (!type.isPrimitive()) return;

        if (type == int.class) field.setInt(bean, 0);
        else if (type == long.class) field.setLong(bean, 0L);
        else if (type == boolean.class) field.setBoolean(bean, false);
        else if (type == double.class) field.setDouble(bean, 0.0);
        else if (type == float.class) field.setFloat(bean, 0.0f);
        else if (type == short.class) field.setShort(bean, (short) 0);
        else if (type == byte.class) field.setByte(bean, (byte) 0);
        else if (type == char.class) field.setChar(bean, '\u0000');
    }

    // 获取类字段（包含继承字段）
    private static Map<String, Field> getFields(Class<?> clazz) {
        return FIELD_CACHE.computeIfAbsent(clazz, k -> {
            Map<String, Field> fields = new LinkedHashMap<>();
            Class<?> current = clazz;
            while (current != null) {
                for (Field field : current.getDeclaredFields()) {
                    field.setAccessible(true);
                    fields.put(field.getName(), field);
                }
                current = current.getSuperclass();
            }
            return fields;
        });
    }
}
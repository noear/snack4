package org.noear.snack4.core;

import org.noear.snack4.ONode;
import org.noear.snack4.core.util.FieldWrapper;
import org.noear.snack4.core.util.ReflectionUtil;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Bean 解码器
 *
 * @author noear
 * @since 4.0
 */
public class BeanDecoder {
    // 反序列化：ONode转对象
    public static <T> T deserialize(ONode node, Type type) {
        return deserialize(node, type, Options.def());
    }

    public static <T> T deserialize(ONode node, Type type, Options opts) {
        if (node == null || type == null) {
            return null;
        }

        if (type instanceof Class) {
            Class<?> clazz = (Class<?>) type;
            if (clazz.isAnonymousClass()) {
                type = clazz.getGenericSuperclass();
            }
        }

        try {
            return (T) convertNodeToBean(node, type, new IdentityHashMap<>(), opts);
        } catch (Throwable e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> Object convertNodeToBean(ONode node, Type type, Map<Object, Object> visited, Options opts) throws Exception {
        Class<?> clazz = null;
        if (type instanceof Class<?>) {
            clazz = (Class<?>) type;
        } else if (type instanceof ParameterizedType) {
            clazz = (Class<?>) ((ParameterizedType) type).getRawType();
        } else if (type instanceof GenericArrayType) {
            clazz = (Class<?>) ((GenericArrayType) type).getGenericComponentType();
        } else if (type instanceof TypeVariable) {
            clazz = (Class<?>) ((TypeVariable) type).getBounds()[0];
        }

        if (clazz == null) {
            throw new IllegalArgumentException("can not convert bean to type: " + type);
        }

        // 优先使用自定义编解码器
        Codec<T> codec = (Codec<T>) opts.getCodecRegistry().get(clazz);
        if (codec != null) {
            return codec.decode(node);
        }

        // 处理Properties类型
        if (clazz == Properties.class) {
            return convertNodeToProperties(node);
        } else if (clazz == String.class) {
            return node.getString();
        } else if (clazz == Double.class || clazz == Double.TYPE) {
            return node.getNumber().doubleValue();
        } else if (clazz == Float.class || clazz == Float.TYPE) {
            return node.getNumber().floatValue();
        } else if (clazz == Long.class || clazz == Long.TYPE) {
            return node.getNumber().longValue();
        } else if (clazz == Integer.class || clazz == Integer.TYPE) {
            return node.getNumber().intValue();
        } else if (clazz == Short.class || clazz == Short.TYPE) {
            return node.getNumber().shortValue();
        } else if (clazz.isArray()) {
            return convertNodeToArray(node, clazz);
        } else if (clazz.isEnum()) {
            return Enum.valueOf((Class<? extends Enum>) clazz, node.getString());
        }

        Object bean = null;
        if (clazz.isInterface()) {
            if (Map.class.isAssignableFrom(clazz)) {
                clazz = LinkedHashMap.class;
            } else if (Collection.class.isAssignableFrom(clazz)) {
                if (Set.class.isAssignableFrom(clazz)) {
                    clazz = HashSet.class;
                } else {
                    clazz = ArrayList.class;
                }
            } else {
                throw new IllegalArgumentException("can not convert bean to type: " + clazz);
            }
        }

        bean = ReflectionUtil.newInstance(clazz);

        if (bean instanceof Map) {
            if (node.isNull()) {
                bean = null;
            } else if (node.isObject()) {
                Type itemType = Object.class;
                if (type instanceof ParameterizedType) {
                    itemType = ((ParameterizedType) type).getActualTypeArguments()[1];
                }

                Map map = (Map) bean;

                for (Map.Entry<String, ONode> entry : node.getObject().entrySet()) {
                    map.put(entry.getKey(), convertNodeToBean(entry.getValue(), itemType, visited, opts));
                }
            } else {
                throw new IllegalArgumentException("The type of node " + node.getType() + " cannot be converted to map.");
            }
        } else if (bean instanceof Collection) {
            Type itemType = Object.class;
            if (type instanceof ParameterizedType) {
                itemType = ((ParameterizedType) type).getActualTypeArguments()[0];
            }

            if (node.isNull()) {
                bean = null;
            } else if (node.isArray()) {
                if (Set.class.isAssignableFrom(clazz)) {
                    Set tmp = (Set) bean;

                    for (ONode n1 : node.getArray()) {
                        Object item = convertNodeToBean(n1, itemType, visited, opts);
                        if (item != null) {
                            tmp.add(item);
                        }
                    }
                } else {
                    List tmp = (List) bean;

                    for (ONode n1 : node.getArray()) {
                        Object item = convertNodeToBean(n1, itemType, visited, opts);
                        if (item != null) {
                            tmp.add(item);
                        }
                    }
                }
            }
        } else {
            for (FieldWrapper field : ReflectionUtil.getDeclaredFields(clazz)) {
                ONode fieldNode = node.get(field.getAliasName());

                if (fieldNode != null && !fieldNode.isNull()) {
                    Object value = convertValue(fieldNode, field.getField().getGenericType(), visited, opts);
                    field.getField().set(bean, value);
                } else {
                    setPrimitiveDefault(field.getField(), bean);
                }
            }
        }

        return bean;
    }

    // 处理Properties类型
    private static Properties convertNodeToProperties(ONode node) {
        Properties properties = new Properties();
        flattenNodeToProperties(node, properties, "");
        return properties;
    }

    private static Object convertNodeToArray(ONode node, Class<?> clazz) {
        Class<?> itemType = clazz.getComponentType();
        Object array = Array.newInstance(itemType, node.size());

        for (int i = 0; i < node.size(); i++) {
            Array.set(array, i, node.get(i).toBean(itemType));
        }

        return array;
    }

    // 将嵌套的ONode扁平化为Properties
    private static void flattenNodeToProperties(ONode node, Properties properties, String prefix) {
        if (node.isObject()) {
            for (Map.Entry<String, ONode> entry : node.getObject().entrySet()) {
                String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
                flattenNodeToProperties(entry.getValue(), properties, key);
            }
        } else if (node.isArray()) {
            List<ONode> array = node.getArray();
            for (int i = 0; i < array.size(); i++) {
                String key = prefix + "[" + i + "]";
                flattenNodeToProperties(array.get(i), properties, key);
            }
        } else {
            properties.setProperty(prefix, node.getString());
        }
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
}
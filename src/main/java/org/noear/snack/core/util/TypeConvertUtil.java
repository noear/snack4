package org.noear.snack.core.util;

import org.noear.snack.exception.TypeConvertException;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TypeConvertUtil {
    private static final Map<Class<?>, Function<String, ?>> CONVERTERS = new ConcurrentHashMap<>();

    static {
        // 注册基本类型转换器
        CONVERTERS.put(int.class, Integer::parseInt);
        CONVERTERS.put(Integer.class, Integer::parseInt);
        CONVERTERS.put(long.class, Long::parseLong);
        CONVERTERS.put(Long.class, Long::parseLong);
        CONVERTERS.put(double.class, Double::parseDouble);
        CONVERTERS.put(Double.class, Double::parseDouble);
        CONVERTERS.put(boolean.class, Boolean::parseBoolean);
        CONVERTERS.put(Boolean.class, Boolean::parseBoolean);
    }

    // 安全转换入口
    public static Object convert(String value, Type targetType) {
        if (value == null) return null;

        Class<?> rawType = GenericUtil.resolveRawType(targetType);
        Function<String, ?> converter = CONVERTERS.get(rawType);

        if (converter != null) {
            return converter.apply(value);
        } else if (rawType.isEnum()) {
            return convertToEnum(value, rawType);
        }
        throw new TypeConvertException("Unsupported type: " + rawType.getName());
    }

    // 枚举转换逻辑
    private static <E extends Enum<E>> E convertToEnum(String value, Class<?> enumType) {
        try {
            return Enum.valueOf((Class<E>) enumType, value);
        } catch (IllegalArgumentException e) {
            E[] constants = ((Class<E>) enumType).getEnumConstants();
            String valid = Arrays.stream(constants)
                    .map(Enum::name)
                    .collect(Collectors.joining(", "));
            throw new TypeConvertException("Invalid enum value: '" + value + "'. Valid values: " + valid);
        }
    }
}
package org.noear.snack.core.util;

import org.noear.snack.annotation.ONodeAttr;
import org.noear.snack.core.Codec;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class AnnotationUtil {
    // 获取字段别名映射
    public static Map<String, String> getFieldAliasMap(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(ONodeAttr.class))
                .collect(Collectors.toMap(
                        Field::getName,
                        f -> {
                            ONodeAttr attr = f.getAnnotation(ONodeAttr.class);
                            return attr.alias().isEmpty() ? f.getName() : attr.alias();
                        }
                ));
    }

    // 检查字段是否需要忽略
    public static boolean isFieldIgnored(Field field) {
        return field.isAnnotationPresent(ONodeAttr.class) &&
                field.getAnnotation(ONodeAttr.class).ignore();
    }

    // 获取字段对应的编解码器
    public static Optional<Codec<?>> getFieldCodec(Field field) {
        if (field.isAnnotationPresent(ONodeAttr.class)) {
            Class<? extends Codec> codecClass = field.getAnnotation(ONodeAttr.class).codec();
            if (codecClass != Codec.class) {
                try {
                    return Optional.of(codecClass.newInstance());
                } catch (Exception e) {
                    throw new AnnotationProcessException("Failed to create codec for field: " + field.getName(), e);
                }
            }
        }
        return Optional.empty();
    }

    // 自定义异常
    public static class AnnotationProcessException extends RuntimeException {
        public AnnotationProcessException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
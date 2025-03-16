package org.noear.snack.core.util;

import org.noear.snack.exception.ReflectionException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

// file: ReflectionUtil.java
public class ReflectionUtil {
    // 带缓存的字段获取
    private static final Map<Class<?>, Collection<Field>> FIELD_CACHE = new ConcurrentHashMap<>();

    public static Collection<Field> getDeclaredFields(Class<?> clazz) {
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
            return fields.values();
        });
    }

    // 安全创建实例
    public static <T> T newInstance(Class<T> clazz) {
        try {
            Constructor<T> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception e) {
            throw new ReflectionException("Create instance failed: " + clazz.getName(), e);
        }
    }

    // 带错误处理的字段设值
    public static void setField(Object target, Field field, Object value) {
        try {
            field.setAccessible(true);
            field.set(target, value);
        } catch (IllegalAccessException e) {
            throw new ReflectionException("Set field failed: " + field.getName(), e);
        }
    }
}
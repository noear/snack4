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
package org.noear.snack4.codec;

import org.noear.snack4.ONode;
import org.noear.snack4.Options;
import org.noear.snack4.annotation.ONodeAttr;
import org.noear.snack4.codec.encode.*;
import org.noear.snack4.codec.util.FieldWrapper;
import org.noear.snack4.codec.util.ReflectionUtil;

import java.io.File;
import java.lang.reflect.Array;
import java.net.InetSocketAddress;
import java.sql.Clob;
import java.text.SimpleDateFormat;
import java.time.*;
import java.util.*;

/**
 * 对象编码器
 *
 * @author noear
 * @since 4.0
 */
public class ObjectEncoder {
    private static final Map<Class<?>, NodeEncoder<?>> ENCODERS = new HashMap<>();
    private static final List<NodePatternEncoder<?>> PATTERN_ENCODERS = new ArrayList<>();
    static {
        PATTERN_ENCODERS.add(new CalendarPatternEncoder());
        PATTERN_ENCODERS.add(new ClobPatternEncoder());

        ENCODERS.put(ONode.class, new ONodeEncoder());
        ENCODERS.put(Properties.class, new PropertiesEncoder());
        ENCODERS.put(InetSocketAddress.class, new InetSocketAddressEncoder());
        ENCODERS.put(SimpleDateFormat.class, new SimpleDateFormatEncoder());
        ENCODERS.put(File.class, new FileEncoder());
        ENCODERS.put(Calendar.class, new CalendarPatternEncoder());
        ENCODERS.put(Class.class, new ClassEncoder());
        ENCODERS.put(Clob.class, new ClobPatternEncoder());
        ENCODERS.put(Currency.class, new CurrencyEncoder());
        ENCODERS.put(TimeZone.class, new TimeZoneEncoder());
        ENCODERS.put(UUID.class, new UUIDEncoder());

        ENCODERS.put(String.class, new StringEncoder());

        ENCODERS.put(Date.class, new DateEncoder());
        ENCODERS.put(ZonedDateTime.class, new ZonedDateTimeEncoder());

        ENCODERS.put(OffsetDateTime.class, new OffsetDateTimeEncoder());
        ENCODERS.put(OffsetTime.class, new OffsetTimeEncoder());

        ENCODERS.put(LocalDateTime.class, new LocalDateTimeEncoder());
        ENCODERS.put(LocalDate.class, new LocalDateEncoder());
        ENCODERS.put(LocalTime.class, new LocalTimeEncoder());


        ENCODERS.put(Boolean.class, new BooleanEncoder());
        ENCODERS.put(Boolean.TYPE, new BooleanEncoder());

        ENCODERS.put(Double.class, new DoubleEncoder());
        ENCODERS.put(Double.TYPE, new DoubleEncoder());

        ENCODERS.put(Float.class, new FloatEncoder());
        ENCODERS.put(Float.TYPE, new FloatEncoder());

        ENCODERS.put(Long.class, new LongEncoder());
        ENCODERS.put(Long.TYPE, new LongEncoder());

        ENCODERS.put(Integer.class, new IntegerEncoder());
        ENCODERS.put(Integer.TYPE, new IntegerEncoder());

        ENCODERS.put(Short.class, new ShortEncoder());
        ENCODERS.put(Short.TYPE, new ShortEncoder());
    }

    private static NodeEncoder getNodeEncoder(Options opts, Class<?> clazz) {
        NodeEncoder encoder = opts.getNodeEncoder(clazz);
        if (encoder == null) {
            encoder = ENCODERS.get(clazz);
        }

        if (encoder == null) {
            for (NodePatternEncoder encoder1 : PATTERN_ENCODERS) {
                if (encoder1.canEncode(clazz)) {
                    return encoder1;
                }
            }
        }

        return encoder;
    }

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
        NodeEncoder encoder = getNodeEncoder(opts, bean.getClass());
        if (encoder != null) {
            return encoder.encode(opts, null, bean);
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
                ONode fieldNode = convertValueToNode(value, field.getAttr(), visited, opts);
                node.set(field.getAliasName(), fieldNode);
            }

            return node;
        }
    }

    // 值转ONode处理
    private static ONode convertValueToNode(Object value, ONodeAttr attr, Map<Object, Object> visited, Options opts) throws Exception {
        if (value == null) {
            return new ONode(null);
        }

        // 优先使用自定义编解码器
        NodeEncoder<Object> codec = (NodeEncoder<Object>) opts.getNodeEncoder(value.getClass());
        if (codec != null) {
            return codec.encode(opts, attr, value);
        }

        if (value instanceof Enum) {
            return _EnumEncoder.getInstance().encode(opts, attr, (Enum) value);
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
            arrayNode.add(convertValueToNode(element, null, visited, opts));
        }
        return arrayNode;
    }

    // 处理集合类型
    private static ONode convertCollectionToNode(Collection<?> collection, Map<Object, Object> visited, Options opts) throws Exception {
        ONode arrayNode = new ONode(new ArrayList<>());
        for (Object item : collection) {
            arrayNode.add(convertValueToNode(item, null, visited, opts));
        }
        return arrayNode;
    }

    // 处理Map类型
    private static ONode convertMapToNode(Map<?, ?> map, Map<Object, Object> visited, Options opts) throws Exception {
        ONode objNode = new ONode(new LinkedHashMap<>());
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = entry.getKey().toString();
            ONode valueNode = convertValueToNode(entry.getValue(), null, visited, opts);
            objNode.set(key, valueNode);
        }
        return objNode;
    }
}
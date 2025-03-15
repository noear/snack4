package org.noear.snack;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * JSON节点抽象，支持多种数据类型操作
 */
public final class ONode {
    private Object value;
    private int type;

    /**
     * 节点类型常量
     */
    public static final int TYPE_NULL = 0;
    public static final int TYPE_BOOLEAN = 1;
    public static final int TYPE_NUMBER = 2;
    public static final int TYPE_STRING = 3;
    public static final int TYPE_ARRAY = 4;
    public static final int TYPE_OBJECT = 5;

    public ONode(Object value) {
        this.value = value;
        this.type = determineType(value);
    }

    private int determineType(Object value) {
        if (value == null) return TYPE_NULL;
        if (value instanceof Boolean) return TYPE_BOOLEAN;
        if (value instanceof Number) return TYPE_NUMBER;
        if (value instanceof String) return TYPE_STRING;
        if (value instanceof List) return TYPE_ARRAY;
        if (value instanceof Map) return TYPE_OBJECT;
        throw new IllegalArgumentException("Unsupported type");
    }

    // Getters
    public boolean isNull() {
        return type == TYPE_NULL;
    }

    public boolean isBoolean() {
        return type == TYPE_BOOLEAN;
    }

    public boolean isNumber() {
        return type == TYPE_NUMBER;
    }

    public boolean isInteger() {
        if (value instanceof Integer || value instanceof Long) {
            return true;
        }
        if (value instanceof Double) {
            double d = (Double) value;
            return d == Math.floor(d) && !Double.isInfinite(d);
        }
        return false;
    }

    public boolean isString() {
        return type == TYPE_STRING;
    }

    public boolean isArray() {
        return type == TYPE_ARRAY;
    }

    public boolean isObject() {
        return type == TYPE_OBJECT;
    }

    public Object getValue() {
        return value;
    }

    public Boolean getBoolean() {
        return (Boolean) value;
    }

    public Number getNumber() {
        return (Number) value;
    }

    public String getString() {
        return (String) value;
    }

    @SuppressWarnings("unchecked")
    public List<ONode> getArray() {
        return (List<ONode>) value;
    }

    @SuppressWarnings("unchecked")
    public Map<String, ONode> getObject() {
        return (Map<String, ONode>) value;
    }

    // 值转换方法
    public int getInt() {
        return getNumber().intValue();
    }

    public long getLong() {
        return getNumber().longValue();
    }

    public double getDouble() {
        return getNumber().doubleValue();
    }

    public ONode get(String key) {
        return getObject().get(key);
    }

    public void set(String key, ONode value) {
        getObject().put(key, value);
    }

    public ONode get(int index) {
        return getArray().get(index);
    }

    public void add(ONode value) {
        getArray().add(value);
    }

    // Optional访问
    public Optional<ONode> getOptional(String key) {
        return isObject() ? Optional.ofNullable(getObject().get(key)) : Optional.empty();
    }

    public Optional<ONode> getOptional(int index) {
        if (!isArray()) return Optional.empty();
        List<ONode> arr = getArray();
        return (index >= 0 && index < arr.size())
                ? Optional.of(arr.get(index))
                : Optional.empty();
    }

    // 序列化
    public String toJsonString() {
        return JsonSerializer.toJsonString(this);
    }

    public int size() {
        if (isArray()) {
            return getArray().size();
        } else if (isObject()) {
            return getObject().size();
        } else {
            return 1;
        }
    }

    // 对象池支持
    ONode reset(Object value) {
        this.value = value;
        this.type = determineType(value);
        return this;
    }

    ONode clear() {
        if (isObject()) ((Map<?, ?>) value).clear();
        if (isArray()) ((List<?>) value).clear();
        return reset(null);
    }

    // 在 JsonParser.ONode 类中添加以下方法
    public int getType() {
        return type;
    }

    public boolean hasKey(String key) {
        return isObject() && getObject().containsKey(key);
    }

    public static String typeToString(int type) {
        switch (type) {
            case TYPE_NULL: return "null";
            case TYPE_BOOLEAN: return "boolean";
            case TYPE_NUMBER: return "number";
            case TYPE_STRING: return "string";
            case TYPE_ARRAY: return "array";
            case TYPE_OBJECT: return "object";
            default: return "unknown";
        }
    }
}
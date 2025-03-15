package org.noear.snack;

import org.noear.snack.codec.BeanCodec;
import org.noear.snack.codec.JsonParser;
import org.noear.snack.codec.JsonSerializer;
import org.noear.snack.schema.validator.SchemaValidator;

import java.io.IOException;
import java.io.StringReader;
import java.util.*;

/**
 * 高性能 JSON 节点抽象
 */
public final class ONode {
    private Object value;
    private int type;

    public static final ONode NULL = new ONode();

    public static final int TYPE_NULL = 0;
    public static final int TYPE_BOOLEAN = 1;
    public static final int TYPE_NUMBER = 2;
    public static final int TYPE_STRING = 3;
    public static final int TYPE_ARRAY = 4;
    public static final int TYPE_OBJECT = 5;

    public ONode() {
        this.type = TYPE_NULL;
    }

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

    // Getters and Setters
    public boolean isNull() { return type == TYPE_NULL; }
    public boolean isBoolean() { return type == TYPE_BOOLEAN; }
    public boolean isNumber() { return type == TYPE_NUMBER; }
    public boolean isString() { return type == TYPE_STRING; }
    public boolean isArray() { return type == TYPE_ARRAY; }
    public boolean isObject() { return type == TYPE_OBJECT; }

    public Object getValue() { return value; }
    public Boolean getBoolean() { return (Boolean) value; }
    public Number getNumber() { return (Number) value; }
    public String getString() { return (String) value; }

    @SuppressWarnings("unchecked")
    public List<ONode> getArray() { return (List<ONode>) value; }

    @SuppressWarnings("unchecked")
    public Map<String, ONode> getObject() { return (Map<String, ONode>) value; }
    public ONode newObject() {
        if (value == null) {
            value = new HashMap<>();
        }

        return this;
    }

    public ONode newArray() {
        if (value == null) {
            value = new ArrayList<>();
        }

        return this;
    }

    public int getInt() { return getNumber().intValue(); }
    public long getLong() { return getNumber().longValue(); }
    public double getDouble() { return getNumber().doubleValue(); }

    public void setValue(Object value) {
        this.value = value;
    }

    public ONode get(String key) { return getObject().get(key); }
    public void set(String key, ONode value) { getObject().put(key, value); }

    public ONode get(int index) { return getArray().get(index); }
    public void add(ONode value) { getArray().add(value); }

    public Optional<ONode> getOptional(String key) {
        return isObject() ? Optional.ofNullable(getObject().get(key)) : Optional.empty();
    }

    public Optional<ONode> getOptional(int index) {
        if (!isArray()) return Optional.empty();
        List<ONode> arr = getArray();
        return (index >= 0 && index < arr.size()) ? Optional.of(arr.get(index)) : Optional.empty();
    }

    public String toJsonString() {
        return JsonSerializer.toJsonString(this);
    }

    public int size() {
        if (isArray()) return getArray().size();
        if (isObject()) return getObject().size();
        return 1;
    }

    public ONode reset(Object value) {
        this.value = value;
        this.type = determineType(value);
        return this;
    }

    public ONode clear() {
        if (isObject()) ((Map<?, ?>) value).clear();
        if (isArray()) ((List<?>) value).clear();
        return reset(null);
    }

    public int getType() { return type; }

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

    /// /////////////

    public static ONode load(String json) throws IOException {
        return new JsonParser(new StringReader(json)).parse();
    }

    public String toJson() {
        return JsonSerializer.toJsonString(this);
    }

    public <T> T toBean(Class<T> clazz) {
        return BeanCodec.deserialize(this, clazz);
    }

    public void validate(SchemaValidator schema) {
        schema.validate(this);
    }
}
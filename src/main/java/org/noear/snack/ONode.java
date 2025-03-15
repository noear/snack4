package org.noear.snack;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
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

    public ONode get(String key){
        return getObject().get(key);
    }

    public ONode get(int index){
        return getArray().get(index);
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
        try (StringWriter writer = new StringWriter()) {
            serialize(writer);
            return writer.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void serialize(Writer writer) throws IOException {
        switch (type) {
            case TYPE_OBJECT:
                serializeObject(writer);
                break;
            case TYPE_ARRAY:
                serializeArray(writer);
                break;
            case TYPE_STRING:
                writer.write('"' + escapeString(getString()) + '"');
                break;
            case TYPE_NUMBER:
                writer.write(getNumber().toString());
                break;
            case TYPE_BOOLEAN:
                writer.write(getBoolean() ? "true" : "false");
                break;
            case TYPE_NULL:
                writer.write("null");
                break;
        }
    }

    private void serializeObject(Writer writer) throws IOException {
        writer.write('{');
        boolean first = true;
        for (Map.Entry<String, ONode> entry : getObject().entrySet()) {
            if (!first) writer.write(',');
            writer.write('"' + escapeString(entry.getKey()) + "\":");
            entry.getValue().serialize(writer);
            first = false;
        }
        writer.write('}');
    }

    private void serializeArray(Writer writer) throws IOException {
        writer.write('[');
        boolean first = true;
        for (ONode item : getArray()) {
            if (!first) writer.write(',');
            item.serialize(writer);
            first = false;
        }
        writer.write(']');
    }

    private String escapeString(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
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
            case TYPE_NULL:
                return "null";
            case TYPE_BOOLEAN:
                return "boolean";
            case TYPE_NUMBER:
                return "number";
            case TYPE_STRING:
                return "string";
            case TYPE_ARRAY:
                return "array";
            case TYPE_OBJECT:
                return "object";
            default:
                return "unknown";
        }
    }
}
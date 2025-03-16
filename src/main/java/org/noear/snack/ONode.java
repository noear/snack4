package org.noear.snack;

import org.noear.snack.core.*;
import org.noear.snack.core.JsonPath;
import org.noear.snack.schema.SchemaValidator;

import java.io.StringReader;
import java.util.*;

/**
 * 高性能 JSON 节点抽象
 */
public final class ONode {
    private Object value;
    private int type;

    public ONode() {
        this.type = JsonTypes.TYPE_NULL;
    }

    public ONode(Object value) {
        this.value = value;
        this.type = JsonTypes.resolveType(value);
    }

    // Getters and Setters
    public boolean isNull() {
        return type == JsonTypes.TYPE_NULL;
    }

    public boolean isNullOrEmpty() {
        return type == JsonTypes.TYPE_NULL ||
                (type == JsonTypes.TYPE_OBJECT && getObject().isEmpty()) ||
                (type == JsonTypes.TYPE_ARRAY && getArray().isEmpty());
    }

    public boolean isBoolean() {
        return type == JsonTypes.TYPE_BOOLEAN;
    }

    public boolean isNumber() {
        return type == JsonTypes.TYPE_NUMBER;
    }

    public boolean isString() {
        return type == JsonTypes.TYPE_STRING;
    }

    public boolean isArray() {
        return type == JsonTypes.TYPE_ARRAY;
    }

    public boolean isObject() {
        return type == JsonTypes.TYPE_OBJECT;
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

    public ONode newObject() {
        if (value == null) {
            value = new LinkedHashMap<>();
            type = JsonTypes.TYPE_OBJECT;
        }

        return this;
    }

    public ONode newArray() {
        if (value == null) {
            value = new ArrayList<>();
            type = JsonTypes.TYPE_ARRAY;
        }

        return this;
    }

    public int getInt() {
        return getNumber().intValue();
    }

    public long getLong() {
        return getNumber().longValue();
    }

    public double getDouble() {
        return getNumber().doubleValue();
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public ONode get(String key) {
        return getObject().get(key);
    }

    public ONode set(String key, Object value) {
        ONode oNode;
        if (value instanceof ONode) {
            oNode = (ONode) value;
        } else if (value instanceof Collection) {
            oNode = BeanCodec.serialize(value);
        } else if (value instanceof Map) {
            oNode = BeanCodec.serialize(value);
        } else {
            oNode = new ONode(value);
        }

        return set0(key, oNode);
    }

    private ONode set0(String key, ONode value) {
        if (type == JsonTypes.TYPE_NULL) {
            newObject();
        }

        getObject().put(key, value);
        return this;
    }

    public ONode get(int index) {
        if (index < 0) {
            int pos = getArray().size() + index;
            return getArray().get(pos);
        } else {
            return getArray().get(index);
        }
    }

    public ONode add(Object value) {
        ONode oNode;
        if (value instanceof ONode) {
            oNode = (ONode) value;
        } else if (value instanceof Collection) {
            oNode = BeanCodec.serialize(value);
        } else if (value instanceof Map) {
            oNode = BeanCodec.serialize(value);
        } else {
            oNode = new ONode(value);
        }

        add0(oNode);
        return this;
    }

    private ONode add0(ONode value) {
        if (type == JsonTypes.TYPE_NULL) {
            newArray();
        }

        getArray().add(value);
        return this;
    }

    public Optional<ONode> getOptional(String key) {
        return isObject() ? Optional.ofNullable(getObject().get(key)) : Optional.empty();
    }

    public Optional<ONode> getOptional(int index) {
        if (!isArray()) return Optional.empty();
        List<ONode> arr = getArray();
        return (index >= 0 && index < arr.size()) ? Optional.of(arr.get(index)) : Optional.empty();
    }

    public int size() {
        if (isArray()) return getArray().size();
        if (isObject()) return getObject().size();
        return 1;
    }

    public ONode reset(Object value) {
        this.value = value;
        this.type = JsonTypes.resolveType(value);
        return this;
    }

    public ONode clear() {
        if (isObject()) ((Map<?, ?>) value).clear();
        if (isArray()) ((List<?>) value).clear();
        return reset(null);
    }

    public int getType() {
        return type;
    }

    public boolean hasKey(String key) {
        return isObject() && getObject().containsKey(key);
    }

    /// /////////////

    /**
     * 根据 jsonpath 查询
     */
    public ONode select(String jsonpath) {
        return JsonPath.select(this, jsonpath);
    }

    /**
     * 根据 jsonpath 删除
     */
    public void delete(String jsonpath) {
        JsonPath.delete(this, jsonpath);
    }

    /**
     * 根据 jsonpath 生成
     */
    public void create(String jsonpath) {
        JsonPath.create(this, jsonpath);
    }


    /// /////////////

    public static ONode loadBean(Object bean, Options opts) {
        return BeanCodec.serialize(bean, opts);
    }

    public static ONode loadBean(Object bean) {
        return BeanCodec.serialize(bean, Options.def());
    }

    // 添加带 Options 的静态方法
    public static ONode loadJson(String json, Options opts) {
        try {
            return new JsonReader(new StringReader(json), opts).read();
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }


    // 保持原有方法兼容性
    public static ONode loadJson(String json) {
        return loadJson(json, Options.def());
    }

    public String toJson(Options opts) {
        try {
            return JsonWriter.serialize(this, opts);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public String toJson() {
        return toJson(Options.def());
    }

    public <T> T toBean(Class<T> clazz, Options opts) {
        return BeanCodec.deserialize(this, clazz, opts);
    }

    public <T> T toBean(Class<T> clazz) {
        return toBean(clazz, Options.def());
    }

    public void validate(SchemaValidator schema) {
        schema.validate(this);
    }
}
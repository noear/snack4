package org.noear.snack.core;

import java.util.List;
import java.util.Map;

/**
 * @author noear 2025/3/16 created
 */
public enum JsonType {
    TYPE_NULL,
    TYPE_BOOLEAN,
    TYPE_NUMBER,
    TYPE_STRING,
    TYPE_ARRAY,
    TYPE_OBJECT,
    ;

    @Override
    public String toString() {
        return getTypeName(this);
    }

    public static String getTypeName(JsonType type) {
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

    public static boolean isValue(JsonType type) {
        return type.ordinal() > TYPE_NULL.ordinal() && type.ordinal() < TYPE_ARRAY.ordinal();
    }

    public static JsonType resolveType(Object value) {
        if (value == null) return JsonType.TYPE_NULL;
        if (value instanceof Boolean) return JsonType.TYPE_BOOLEAN;
        if (value instanceof Number) return JsonType.TYPE_NUMBER;
        if (value instanceof String) return JsonType.TYPE_STRING;
        if (value instanceof List) return JsonType.TYPE_ARRAY;
        if (value instanceof Map) return JsonType.TYPE_OBJECT;

        throw new IllegalArgumentException("Unsupported type");
    }
}
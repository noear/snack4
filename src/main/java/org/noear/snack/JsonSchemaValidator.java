package org.noear.snack;

import org.noear.snack.exception.SchemaException;

import java.util.List;
import java.util.Map;

public class JsonSchemaValidator {
    private final ONode schema;

    public JsonSchemaValidator(ONode schema) {
        if (!schema.isObject()) {
            throw new IllegalArgumentException("Schema must be a JSON object");
        }
        this.schema = schema;
    }

    public void validate(ONode data) throws SchemaException {
        validateNode(schema, data, "$");
    }

    private void validateNode(ONode schemaNode, ONode dataNode, String path) throws SchemaException {
        // 处理类型校验
        if (schemaNode.hasKey("type")) {
            validateType(schemaNode.get("type"), dataNode, path);
        }

        // 处理枚举校验
        if (schemaNode.hasKey("enum")) {
            validateEnum(schemaNode.get("enum"), dataNode, path);
        }

        // 处理对象属性校验
        if (dataNode.isObject() && schemaNode.hasKey("properties")) {
            validateProperties(schemaNode.get("properties"), dataNode, path);
        }

        // 处理数组校验
        if (dataNode.isArray()) {
            validateArrayConstraints(schemaNode, dataNode, path);
        }

        // 处理数值范围校验
        if (dataNode.isNumber()) {
            validateNumberRange(schemaNode, dataNode, path);
        }

        // 处理字符串格式校验
        if (dataNode.isString()) {
            validateStringConstraints(schemaNode, dataNode, path);
        }

        // 处理条件校验（anyOf/allOf/oneOf）
        validateConditional(schemaNode, dataNode, path);
    }

    private void validateType(ONode typeNode, ONode dataNode, String path) throws SchemaException {
        if (typeNode.isString()) {
            String expectedType = typeNode.getString();
            if (!matchType(dataNode, expectedType)) {
                throw typeMismatch(expectedType, dataNode, path);
            }
        } else if (typeNode.isArray()) {
            boolean matched = false;
            for (ONode typeOption : typeNode.getArray()) {
                if (matchType(dataNode, typeOption.getString())) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                throw new SchemaException("Type not in allowed types at " + path);
            }
        }
    }

    private boolean matchType(ONode node, String type) {
        switch (type) {
            case "string": return node.isString();
            case "number": return node.isNumber();
            case "integer": return node.isNumber() && isInteger(node.getNumber());
            case "boolean": return node.isBoolean();
            case "object": return node.isObject();
            case "array": return node.isArray();
            case "null": return node.isNull();
            default: return false;
        }
    }

    private boolean isInteger(Number num) {
        return num instanceof Integer || num instanceof Long ||
                (num instanceof Double && num.doubleValue() == num.longValue());
    }

    private void validateEnum(ONode enumNode, ONode dataNode, String path) throws SchemaException {
        if (!enumNode.isArray()) return;

        for (ONode allowedValue : enumNode.getArray()) {
            if (deepEquals(allowedValue, dataNode)) {
                return;
            }
        }
        throw new SchemaException("Value not in enum list at " + path);
    }

    private boolean deepEquals(ONode a, ONode b) {
        // 实现深度比较逻辑
        if (a.getType() != b.getType()) return false;

        switch (a.getType()) {
            case ONode.TYPE_NULL: return true;
            case ONode.TYPE_BOOLEAN: return a.getBoolean() == b.getBoolean();
            case ONode.TYPE_NUMBER: return a.getNumber().doubleValue() == b.getNumber().doubleValue();
            case ONode.TYPE_STRING: return a.getString().equals(b.getString());
            case ONode.TYPE_ARRAY:
                List<ONode> aArr = a.getArray();
                List<ONode> bArr = b.getArray();
                if (aArr.size() != bArr.size()) return false;
                for (int i = 0; i < aArr.size(); i++) {
                    if (!deepEquals(aArr.get(i), bArr.get(i))) return false;
                }
                return true;
            case ONode.TYPE_OBJECT:
                Map<String, ONode> aObj = a.getObject();
                Map<String, ONode> bObj = b.getObject();
                if (aObj.size() != bObj.size()) return false;
                for (Map.Entry<String, ONode> entry : aObj.entrySet()) {
                    if (!bObj.containsKey(entry.getKey()) ||
                            !deepEquals(entry.getValue(), bObj.get(entry.getKey()))) {
                        return false;
                    }
                }
                return true;
            default: return false;
        }
    }

    private void validateProperties(ONode propertiesNode, ONode dataNode, String path) throws SchemaException {
        Map<String, ONode> properties = propertiesNode.getObject();
        Map<String, ONode> dataObj = dataNode.getObject();

        // 校验必填字段
        if (schema.hasKey("required")) {
            for (ONode requiredField : schema.get("required").getArray()) {
                String field = requiredField.getString();
                if (!dataObj.containsKey(field)) {
                    throw new SchemaException("Missing required field: " + field + " at " + path);
                }
            }
        }

        // 校验每个属性
        for (Map.Entry<String, ONode> propEntry : properties.entrySet()) {
            String propName = propEntry.getKey();
            if (dataObj.containsKey(propName)) {
                validateNode(propEntry.getValue(), dataObj.get(propName), path + "." + propName);
            }
        }
    }

    private void validateArrayConstraints(ONode schemaNode, ONode dataNode, String path) throws SchemaException {
        List<ONode> items = dataNode.getArray();

        // 校验最小/最大长度
        if (schemaNode.hasKey("minItems")) {
            int min = schemaNode.get("minItems").getInt();
            if (items.size() < min) {
                throw new SchemaException("Array length " + items.size() + " < minItems(" + min + ") at " + path);
            }
        }
        if (schemaNode.hasKey("maxItems")) {
            int max = schemaNode.get("maxItems").getInt();
            if (items.size() > max) {
                throw new SchemaException("Array length " + items.size() + " > maxItems(" + max + ") at " + path);
            }
        }

        // 校验元素类型
        if (schemaNode.hasKey("items")) {
            ONode itemsSchema = schemaNode.get("items");
            for (int i = 0; i < items.size(); i++) {
                validateNode(itemsSchema, items.get(i), path + "[" + i + "]");
            }
        }
    }

    private void validateNumberRange(ONode schemaNode, ONode dataNode, String path) throws SchemaException {
        double value = dataNode.getDouble();

        if (schemaNode.hasKey("minimum")) {
            double min = schemaNode.get("minimum").getDouble();
            if (value < min) {
                throw new SchemaException("Value " + value + " < minimum(" + min + ") at " + path);
            }
        }
        if (schemaNode.hasKey("maximum")) {
            double max = schemaNode.get("maximum").getDouble();
            if (value > max) {
                throw new SchemaException("Value " + value + " > maximum(" + max + ") at " + path);
            }
        }
    }

    private void validateStringConstraints(ONode schemaNode, ONode dataNode, String path) throws SchemaException {
        String value = dataNode.getString();

        if (schemaNode.hasKey("minLength")) {
            int min = schemaNode.get("minLength").getInt();
            if (value.length() < min) {
                throw new SchemaException("String length " + value.length() + " < minLength(" + min + ") at " + path);
            }
        }
        if (schemaNode.hasKey("maxLength")) {
            int max = schemaNode.get("maxLength").getInt();
            if (value.length() > max) {
                throw new SchemaException("String length " + value.length() + " > maxLength(" + max + ") at " + path);
            }
        }
        if (schemaNode.hasKey("pattern")) {
            String pattern = schemaNode.get("pattern").getString();
            if (!value.matches(pattern)) {
                throw new SchemaException("String does not match pattern: " + pattern + " at " + path);
            }
        }
    }

    private void validateConditional(ONode schemaNode, ONode dataNode, String path) throws SchemaException {
        validateConditionalGroup(schemaNode, "anyOf", dataNode, path, false);
        validateConditionalGroup(schemaNode, "allOf", dataNode, path, true);
        validateConditionalGroup(schemaNode, "oneOf", dataNode, path, false);
    }

    private void validateConditionalGroup(ONode schemaNode, String key,
                                          ONode dataNode, String path,
                                          boolean requireAll) throws SchemaException {
        if (!schemaNode.hasKey(key)) return;

        List<ONode> schemas = schemaNode.get(key).getArray();
        int matchCount = 0;

        for (ONode subSchema : schemas) {
            try {
                validateNode(subSchema, dataNode, path);
                matchCount++;
            } catch (SchemaException e) {
                if (requireAll) throw e;
            }
        }

        if (requireAll && matchCount != schemas.size()) {
            throw new SchemaException("Failed to satisfy allOf constraints at " + path);
        }
        if (!requireAll && key.equals("anyOf") && matchCount == 0) {
            throw new SchemaException("Failed to satisfy anyOf constraints at " + path);
        }
        if (!requireAll && key.equals("oneOf") && matchCount != 1) {
            throw new SchemaException("Must satisfy exactly one of oneOf constraints at " + path);
        }
    }

    private SchemaException typeMismatch(String expected, ONode actual, String path) {
        return new SchemaException("Expected type " + expected + " but got " +
                getTypeName(actual.getType()) + " at " + path);
    }

    private static String getTypeName(int type) {
        switch (type) {
            case ONode.TYPE_NULL: return "null";
            case ONode.TYPE_BOOLEAN: return "boolean";
            case ONode.TYPE_NUMBER: return "number";
            case ONode.TYPE_STRING: return "string";
            case ONode.TYPE_ARRAY: return "array";
            case ONode.TYPE_OBJECT: return "object";
            default: return "unknown";
        }
    }
}
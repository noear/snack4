package org.noear.snack.schema.rule;

import org.noear.snack.ONode;
import org.noear.snack.exception.SchemaException;

import java.util.HashSet;
import java.util.Set;

/**
 * 类型验证规则实现
 */
public class TypeRule implements ValidationRule {
    private final Set<String> allowedTypes;

    public TypeRule(ONode typeNode) {
        this.allowedTypes = new HashSet<>();
        if (typeNode.isString()) {
            allowedTypes.add(typeNode.getString());
        } else if (typeNode.isArray()) {
            for (ONode t : typeNode.getArray()) {
                allowedTypes.add(t.getString());
            }
        }
    }

    @Override
    public void validate(ONode data) throws SchemaException {
        String actualType = getTypeName(data.getType());
        if (!allowedTypes.contains(actualType)) {
            throw new SchemaException("Type mismatch. Expected: " + allowedTypes + ", Actual: " + actualType);
        }
    }


    public static String getTypeName(int type) {
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
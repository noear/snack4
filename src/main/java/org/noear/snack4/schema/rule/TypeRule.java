package org.noear.snack4.schema.rule;

import org.noear.snack4.ONode;
import org.noear.snack4.core.JsonType;
import org.noear.snack4.exception.SchemaException;

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
        String actualType = JsonType.getTypeName(data.getType());
        if (!allowedTypes.contains(actualType)) {
            throw new SchemaException("Type mismatch. Expected: " + allowedTypes + ", Actual: " + actualType);
        }
    }
}
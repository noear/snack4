package org.noear.snack4.schema.rule;

import org.noear.snack4.ONode;
import org.noear.snack4.exception.SchemaException;

/**
 * 预编译规则接口
 */
public interface ValidationRule {
    void validate(ONode data) throws SchemaException;
}
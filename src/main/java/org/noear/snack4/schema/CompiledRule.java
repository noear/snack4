package org.noear.snack4.schema;

import org.noear.snack4.ONode;
import org.noear.snack4.exception.SchemaException;
import org.noear.snack4.schema.rule.ValidationRule;
import org.noear.snack4.core.PathTracker;

import java.util.List;

/**
 * 编译验证规则实现
 */
public class CompiledRule {
    private final List<ValidationRule> rules;

    public CompiledRule(List<ValidationRule> rules) {
        this.rules = rules;
    }

   public void validate(ONode data, PathTracker path) throws SchemaException {
        for (ValidationRule rule : rules) {
            rule.validate(data);
        }
    }
}
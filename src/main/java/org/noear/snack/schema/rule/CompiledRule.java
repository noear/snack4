package org.noear.snack.schema.rule;

import org.noear.snack.ONode;
import org.noear.snack.exception.SchemaException;
import org.noear.snack.util.PathTracker;

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
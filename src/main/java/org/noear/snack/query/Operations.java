package org.noear.snack.query;


import org.noear.snack.ONode;
import org.noear.snack.core.util.TextUtil;
import org.noear.snack.exception.PathResolutionException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

/**
 * 操作符处理库(支持动态注册)
 *
 * @author noear 2025/5/5 created
 */
public class Operations {
    private static final Map<String, BiFunction<ONode, Condition, Boolean>> LIB = new ConcurrentHashMap<>();

    static {
        // 操作函数
        register("startsWith", Operations::startsWith);
        register("endsWith", Operations::endsWith);
        register("contains", Operations::contains);
        register("in", Operations::in);
    }

    /**
     * 注册
     */
    public static void register(String name, BiFunction<ONode, Condition, Boolean> func) {
        LIB.put(name, func);
    }

    /**
     * 获取
     */
    public static BiFunction<ONode, Condition, Boolean> get(String funcName) {
        BiFunction<ONode, Condition, Boolean> tmp = LIB.get(funcName);

        if (tmp == null) {
            return Operations::def;
        } else {
            return tmp;
        }
    }

    /// /////////////////

    private static boolean startsWith(ONode node, Condition condition) {
        ONode leftNode = condition.getLeftNode(node);

        if (leftNode.isString()) {
            ONode rightNode = condition.getRightNode(node);
            if (rightNode == null) {
                return false;
            }

            return leftNode.getString().startsWith(rightNode.getString());
        }
        return false;
    }

    private static boolean endsWith(ONode node, Condition condition) {
        ONode leftNode = condition.getLeftNode(node);

        if (leftNode.isString()) {
            ONode rightNode = condition.getRightNode(node);
            if (rightNode == null) {
                return false;
            }

            return leftNode.getString().endsWith(rightNode.getString());
        }
        return false;
    }

    private static boolean contains(ONode node, Condition condition) {
        ONode leftNode = condition.getLeftNode(node);

        ONode expectedNode = condition.getRightNode(node);

        // 支持多类型包含检查
        if (leftNode.isArray()) {
            return leftNode.getArray().stream()
                    .anyMatch(item -> isValueMatch(item, expectedNode));
        } else if (leftNode.isString()) {
            if (expectedNode.isString()) {
                return leftNode.getString().contains(expectedNode.getString());
            } else {
                throw new IllegalArgumentException("expected string but got " + expectedNode);
            }
        }
        return false;
    }

    private static boolean in(ONode node, Condition condition) {
        ONode leftNode = condition.getLeftNode(node);

        ONode rightNode = condition.getRightNode(node);
        if (rightNode == null && rightNode.isArray() == false) {
            return false;
        }

        boolean found = rightNode.getArray().stream().anyMatch(v -> isValueMatch(leftNode, v));
        return found;
    }

    public static boolean def(ONode node, Condition condition) {
        ONode leftNode = condition.getLeftNode(node);

        // 处理存在性检查（如 @.price）
        if (TextUtil.isEmpty(condition.getOp()) && TextUtil.isEmpty(condition.getRight())) {
            return true;
        }

        ONode rightNode = condition.getRightNode(node);


        if ("=~".equals(condition.getOp()) && condition.getRight() != null) {
            if (leftNode.isString()) {
                if (rightNode.isString()) {
                    return leftNode.getString().matches(rightNode.getString().replace("\\/", "/"));
                }
            }

            return false;
        }


        // 类型判断逻辑
        if (condition.getRight().startsWith("'")) {
            if (leftNode.isString()) {
                if (rightNode.isString()) {
                    return compareString(condition.getOp(), leftNode.getString(), condition.getRight().substring(1, condition.getRight().length() - 1));
                }
            }

            return false;
        } else {
            if (leftNode.isNumber()) {
                if (rightNode.isNumber()) {
                    return compareNumber(condition.getOp(), leftNode.getDouble(), rightNode.getDouble());
                }
            }

            return false;
        }
    }

    /// ///////////////


    private static boolean compareString(String op, String a, String b) {
        switch (op) {
            case "==":
                return a.equals(b);
            case "!=":
                return !a.equals(b);
            default:
                throw new PathResolutionException("Unsupported operator for string: " + op);
        }
    }

    private static boolean compareNumber(String op, double a, double b) {
        switch (op) {
            case "==":
                return a == b;
            case "!=":
                return a != b;
            case ">":
                return a > b;
            case "<":
                return a < b;
            case ">=":
                return a >= b;
            case "<=":
                return a <= b;
            default:
                throw new PathResolutionException("Unsupported operator for number: " + op);
        }
    }

    private static boolean isValueMatch(ONode item, ONode expected) {
        if (item.isArray()) {
            return item.getArray().stream().anyMatch(one -> isValueMatch(one, expected));
        }

        if (item.isString()) {
            if (expected.isString()) {
                return item.getString().equals(expected.getString());
            }
        } else if (item.isNumber()) {
            if (expected.isNumber()) {
                double itemValue = item.getDouble();
                double expectedValue = expected.getNumber().doubleValue();
                return itemValue == expectedValue;
            }
        } else if (item.isBoolean()) {
            if (expected.isBoolean()) {
                Boolean itemBool = item.getBoolean();
                return itemBool == expected.getBoolean();
            }
        }

        return false;
    }
}
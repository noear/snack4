package org.noear.snack.query;


import org.noear.snack.ONode;
import org.noear.snack.core.util.TextUtil;
import org.noear.snack.exception.PathResolutionException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author noear 2025/5/5 created
 */
public class Operations {
    // 正则表达式更新（支持更复杂的键路径和转义字符）
    public static final Pattern CONDITION_PATTERN = Pattern.compile(
            "^@?\\.?" +
                    "(?<key>[\\w\\.\\[\\]]+)" +
                    "(\\s+" +
                    "(?<op>[\\w=~!><]+)" + //==|=~|!=|>=|<=|>|<|startsWith|endsWith|contains|in|\b
                    "(\\s+" +
                    "(?<right>.*?))?)?" +
                    "$", Pattern.CASE_INSENSITIVE
    );


    private static final Map<String, BiFunction<ONode, JsonPath.Factor, Boolean>> lib = new HashMap<>();

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
    public static void register(String name, BiFunction<ONode, JsonPath.Factor, Boolean> func) {
        lib.put(name, func);
    }

    /**
     * 获取
     */
    public static BiFunction<ONode, JsonPath.Factor, Boolean> get(String funcName) {
        BiFunction<ONode, JsonPath.Factor, Boolean> tmp = lib.get(funcName);

        if (tmp == null) {
            return Operations::def;
        } else {
            return tmp;
        }
    }

    /// /////////////////

    private static boolean startsWith(ONode node, JsonPath.Factor factor) {
        if (factor.right == null) {
            return false;
        }

        factor.right = factor.right.substring(1, factor.right.length() - 1);

        ONode target = resolveNestedPath(node, factor.keyPath);
        if (target == null) {
            return false;
        } else if (target.isString()) {
            return target.getString().startsWith(factor.right);
        }
        return false;
    }

    private static boolean endsWith(ONode node, JsonPath.Factor factor) {
        if (factor.right == null) {
            return false;
        }

        factor.right = factor.right.substring(1, factor.right.length() - 1);

        ONode target = resolveNestedPath(node, factor.keyPath);
        if (target == null) {
            return false;
        } else if (target.isString()) {
            return target.getString().endsWith(factor.right);
        }
        return false;
    }

    private static boolean contains(ONode node, JsonPath.Factor factor) {
        if(factor.right == null){
            return false;
        }

        String expectedRaw = factor.right.replaceAll("^'|'$", "");
        String expectedValue = expectedRaw.replace("\\'", "'");

        ONode target = resolveNestedPath(node, factor.keyPath);
        if (target == null) return false;

        // 支持多类型包含检查
        if (target.isArray()) {
            return target.getArray().stream()
                    .anyMatch(item -> isValueMatch(item, expectedValue));
        } else if (target.isString()) {
            return target.getString().contains(expectedValue);
        }
        return false;
    }

    private static boolean in(ONode node, JsonPath.Factor factor) {
        if (factor.right == null) {
            return false;
        }

        String arrayContent = factor.right;

        // 处理数组的方括号
        if (arrayContent.startsWith("[") && arrayContent.endsWith("]")) {
            arrayContent = arrayContent.substring(1, arrayContent.length() - 1).trim();
        }

        List<String> expectedValues = new ArrayList<>();
        if (!arrayContent.isEmpty()) {
            // 正则表达式匹配元素（单引号字符串、数值、其他）
            Pattern elementPattern = Pattern.compile("'((?:\\\\'|[^'])*)'|(-?\\d+\\.?\\d*)|([\\w-]+)");
            Matcher matcher = elementPattern.matcher(arrayContent);
            while (matcher.find()) {
                String element = matcher.group(0).trim();
                if (element.startsWith("'")) {
                    // 处理单引号字符串，转义单引号
                    element = element.substring(1, element.length() - 1).replace("\\'", "'");
                }
                expectedValues.add(element);
            }
        }

        ONode target = resolveNestedPath(node, factor.keyPath);
        if (target == null) return false;

        boolean found = expectedValues.stream().anyMatch(v -> isValueMatch(target, v));
        return found;
    }

    public static boolean def(ONode node, JsonPath.Factor factor) {
        // 处理存在性检查（如 @.price）
        if (TextUtil.isEmpty(factor.op) && TextUtil.isEmpty(factor.right)) {
            return resolveNestedPath(node, factor.keyPath) != null;
        }

        // 获取目标节点
        ONode target = resolveNestedPath(node, factor.keyPath);
        if (target == null) return false;

        if ("=~".equals(factor.op) && factor.right != null) {
            if (!target.isString()) return false;
            return target.getString().matches(factor.right.substring(1, factor.right.length() - 1).replace("\\/", "/"));
        }


        // 类型判断逻辑
        if (factor.right.startsWith("'")) {
            if (!target.isString()) return false;
            return compareString(factor.op, target.getString(), factor.right.substring(1, factor.right.length() - 1));
        } else {
            if (!target.isNumber()) return false;
            return compareNumber(factor.op, target.getDouble(), Double.parseDouble(factor.right));
        }
    }

    /// ///////////////
    ///
    ///


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

    private static boolean isValueMatch(ONode item, String expected) {
        if(item.isArray()){
            return item.getArray().stream().anyMatch(one -> isValueMatch(one, expected));
        }

        if (item.isString()) {
            return item.getString().equals(expected);
        } else if (item.isNumber()) {
            try {
                double itemValue = item.getDouble();
                double expectedValue = Double.parseDouble(expected);
                return itemValue == expectedValue;
            } catch (NumberFormatException e) {
                return false;
            }
        } else if (item.isBoolean()) {
            Boolean itemBool = item.getBoolean();
            if (expected.equalsIgnoreCase("true") || expected.equalsIgnoreCase("false")) {
                return itemBool == Boolean.parseBoolean(expected);
            }
            return false;
        }
        return false;
    }


    private static ONode resolveNestedPath(ONode node, String keyPath) {
        String[] keys = keyPath.split("\\.|\\[");
        ONode current = node;
        for (String key : keys) {
            if(key.endsWith("]")) {
                key = key.substring(0, key.length() - 1).trim();
            }

            if (current.isObject()) {
                current = current.get(key);
            } else if (current.isArray()) {
                try {
                    int index = Integer.parseInt(key);
                    current = current.get(index);
                } catch (NumberFormatException e) {
                    return null;
                }
            } else {
                return null;
            }
            if (current == null) return null;
        }
        return current;
    }
}

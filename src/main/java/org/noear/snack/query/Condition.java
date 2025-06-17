package org.noear.snack.query;

/**
 * 
 * @author noear 2025/5/5 created
 * */
import org.noear.snack.ONode;
import org.noear.snack.core.util.TextUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * 条件描述
 */
public class Condition {
    private static Map<String, Condition> conditionMap = new ConcurrentHashMap<>();

    public static Condition get(String conditionStr) {
        return conditionMap.computeIfAbsent(conditionStr, Condition::new);
    }

    /// ///////////////////


    private final String left;
    private final String op;
    private final String right;

    private final ONode leftValue;
    private final ONode rightValue;

    private Condition(String conditionStr) {
        String[] parts = new String[3];

        int spaceIdx = conditionStr.indexOf(' ');
        if (spaceIdx < 0) {
            //没有空隔
            parts[0] = conditionStr;
        } else {
            //有空隔
            parts[0] = conditionStr.substring(0, spaceIdx);
            parts[1] = conditionStr.substring(spaceIdx + 1).trim();
            spaceIdx = parts[1].indexOf(' ');
            if (spaceIdx > 0) {
                //有第二个空隔
                parts[2] = parts[1].substring(spaceIdx + 1).trim();
                parts[1] = parts[1].substring(0, spaceIdx);
            }
        }

        this.left = parts[0];
        this.op = parts[1];
        this.right = parts[2];

        this.leftValue = resolveConstantNode(left);
        this.rightValue = resolveConstantNode(right);
    }

    public String getLeft() {
        return left;
    }

    public String getOp() {
        return op;
    }

    public String getRight() {
        return right;
    }

    public ONode getLeftNode(ONode node, ONode root) {
        if (TextUtil.isEmpty(left)) {
            return null;
        } else {
            if (leftValue == null) {
                return resolveNestedPath(node, left, root);
            } else {
                return leftValue;
            }
        }
    }


    public ONode getRightNode(ONode node, ONode root) {
        if (TextUtil.isEmpty(right)) {
            return null;
        } else {
            if (rightValue == null) {
                return resolveNestedPath(node, right, root);
            } else {
                return rightValue;
            }
        }
    }


    @Override
    public String toString() {
        return "Condition{" +
                "left='" + left + '\'' +
                ", op='" + op + '\'' +
                ", right='" + right + '\'' +
                '}';
    }

    /**
     * 分析常量节点
     */
    private static ONode resolveConstantNode(String value) {
        if (TextUtil.isEmpty(value)) {
            return null;
        } else {
            char ch = value.charAt(0);
            if (ch == '@' || ch == '$') {
                return null;
            } else {
                if (ch == '\'') {
                    return new ONode(value.substring(1, value.length() - 1));
                } else if (ch == '/') {
                    return new ONode(value);
                } else {
                    return ONode.load(value);
                }
            }
        }
    }

    private static ONode resolveNestedPath(ONode node, String keyPath, ONode root) {
        if (keyPath.startsWith("$")) {
            return root.select(keyPath);
        }


        String[] keys = keyPath.split("\\.|\\[");
        ONode current = node;
        for (String key : keys) {
            if (key.length() == 1 && '@' == key.charAt(0)) {
                continue;
            }

            if (key.endsWith("]")) {
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

    public static Pattern parseJsRegex(String jsRegex) {
        // 1. 检查输入是否以 / 开头和结尾
        if (!jsRegex.startsWith("/") || !jsRegex.contains("/")) {
            throw new IllegalArgumentException("Invalid JavaScript regex format: " + jsRegex);
        }

        // 2. 分离正则主体和修饰符
        int lastSlashIndex = jsRegex.lastIndexOf('/');
        String regexBody = jsRegex.substring(1, lastSlashIndex);
        String flags = jsRegex.substring(lastSlashIndex + 1);

        // 3. 转换修饰符为 Java 的 Pattern 标志
        int javaFlags = 0;
        for (char flag : flags.toCharArray()) {
            switch (flag) {
                case 'i':
                    javaFlags |= Pattern.CASE_INSENSITIVE;
                    break;
                case 'm':
                    javaFlags |= Pattern.MULTILINE;
                    break;
                case 's':
                    javaFlags |= Pattern.DOTALL;
                    break;
                // 忽略 g（全局匹配），Java 通过 Matcher 循环实现
                case 'g':
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported flag: " + flag);
            }
        }

        // 4. 创建 Pattern
        return Pattern.compile(regexBody, javaFlags);
    }
}
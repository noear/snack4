package org.noear.snack.query;

/**
 * 
 * @author noear 2025/5/5 created
 * */
import org.noear.snack.ONode;
import org.noear.snack.core.util.TextUtil;

/**
 * 条件描述
 */
public class Condition {
    public static Condition parse(String conditionStr) {
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

        return new Condition(parts[0], parts[1], parts[2]);
    }


    private final String left;
    private final String op;
    private final String right;

    private final ONode leftValue;
    private final ONode rightValue;

    private Condition(String left, String op, String right) {
        this.left = left;
        this.op = op;
        this.right = right;

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
                if (ch == '\'' || ch == '/') {
                    return new ONode(value.substring(1, value.length() - 1));
                } else {
                    return ONode.loadJson(value);
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
}
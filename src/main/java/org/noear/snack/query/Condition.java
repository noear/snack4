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
    private String left;
    private String op;
    private String right;

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
            char ch = left.charAt(0);
            if (ch == '@' || ch == '$') {
                return resolveNestedPath(node, left, root);
            } else {
                if (ch == '\'' || ch == '/') {
                    return new ONode(left.substring(1, left.length() - 1));
                } else {
                    return ONode.loadJson(left);
                }
            }
        }
    }


    public ONode getRightNode(ONode node, ONode root) {
        if (TextUtil.isEmpty(right)) {
            return null;
        } else {
            char ch = right.charAt(0);
            if (ch == '@' || ch == '$') {
                return resolveNestedPath(node, right, root);
            } else {
                if (ch == '\'' || ch == '/') {
                    return new ONode(right.substring(1, right.length() - 1));
                } else {
                    return ONode.loadJson(right);
                }
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

    public static Condition parse(String conditionStr) {
        Condition f = new Condition();

        int spaceIdx = conditionStr.indexOf(' ');
        if (spaceIdx < 0) {
            //没有空隔
            f.left = conditionStr;
        } else {
            //有空隔
            f.left = conditionStr.substring(0, spaceIdx);
            f.op = conditionStr.substring(spaceIdx + 1).trim();
            spaceIdx = f.op.indexOf(' ');
            if (spaceIdx > 0) {
                //有第二个空隔
                f.right = f.op.substring(spaceIdx + 1).trim();
                f.op = f.op.substring(0, spaceIdx);
            }
        }

        return f;
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
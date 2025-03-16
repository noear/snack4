package org.noear.snack.schema;

import org.noear.snack.ONode;
import org.noear.snack.exception.PathResolutionException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * JSON路径查询工具类
 */
public class JsonPath {
    private static final Map<String, Function<ONode, ONode>> FUNCTIONS = new HashMap<>();

    static {
        // 注册内置函数
        FUNCTIONS.put("count", node -> new ONode(node.isArray() ? node.size() : 1));
        FUNCTIONS.put("sum", node -> {
            if (!node.isArray()) throw new PathResolutionException("sum() function requires an array");
            double sum = 0;
            for (ONode item : node.getArray()) {
                sum += item.getDouble();
            }
            return new ONode(sum);
        });
    }

    public static ONode query(ONode node, String path) {
        if (node == null) throw new PathResolutionException("Null input node");
        if (!path.startsWith("$")) throw new PathResolutionException("Path must start with $");

        PathParser parser = new PathParser(path);
        return parser.evaluate(node);
    }

    private static class PathParser {
        private final String path;
        private int index;

        PathParser(String path) {
            this.path = path;
            this.index = 1; // 跳过 $
        }

        ONode evaluate(ONode root) {
            ONode current = root;
            while (index < path.length()) {
                char ch = path.charAt(index);
                switch (ch) {
                    case '.':
                        index++;
                        if (index < path.length() && path.charAt(index) == '.') {
                            index++;
                            current = resolveRecursive(current);
                        } else {
                            current = resolveKey(current);
                        }
                        break;
                    case '[':
                        index++;
                        current = resolveIndex(current);
                        break;
                    case '*':
                        index++;
                        current = resolveWildcard(current);
                        break;
                    default:
                        throw new PathResolutionException("Invalid path syntax at index " + index);
                }
            }
            return current;
        }

        private ONode resolveKey(ONode node) {
            String key = parseSegment();
            if (key.endsWith("()")) {
                return resolveFunction(node, key.substring(0, key.length() - 2));
            }
            return getNodeByKey(node, key);
        }

        private ONode resolveIndex(ONode node) {
            String indexStr = parseSegment();
            if (indexStr.endsWith("]")) {
                indexStr = indexStr.substring(0, indexStr.length() - 1); // 去掉末尾的 ]
            }
            if ("*".equals(indexStr)) {
                return resolveWildcard(node);
            }
            if (indexStr.startsWith("?")) {
                return resolveFilter(node, indexStr.substring(1));
            }
            return getNodeByIndex(node, indexStr);
        }

        private ONode resolveWildcard(ONode node) {
            if (node.isArray()) {
                return node;
            } else if (node.isObject()) {
                return new ONode(node.getObject().values());
            }
            throw new PathResolutionException("Wildcard can only be used on arrays or objects");
        }

        private ONode resolveRecursive(ONode node) {
            List<ONode> results = new ArrayList<>();
            recursiveSearch(node, results);
            return new ONode(results);
        }

        private void recursiveSearch(ONode node, List<ONode> results) {
            if (node.isArray()) {
                for (ONode item : node.getArray()) {
                    recursiveSearch(item, results);
                }
            } else if (node.isObject()) {
                for (Map.Entry<String, ONode> entry : node.getObject().entrySet()) {
                    recursiveSearch(entry.getValue(), results);
                }
            } else {
                results.add(node);
            }
        }

        private String parseSegment() {
            StringBuilder segment = new StringBuilder();
            while (index < path.length()) {
                char ch = path.charAt(index);
                if (ch == '.' || ch == '[' || ch == ']' || ch == '*') break;
                segment.append(ch);
                index++;
            }
            return segment.toString();
        }

        private ONode getNodeByKey(ONode node, String key) {
            if (!node.isObject()) throw new PathResolutionException("Not an object at key access");
            Map<String, ONode> obj = node.getObject();
            if (!obj.containsKey(key)) {
                throw new PathResolutionException("Missing key: " + key);
            }
            return obj.get(key);
        }

        private ONode getNodeByIndex(ONode node, String indexStr) {
            if (!node.isArray()) throw new PathResolutionException("Not an array at index access");
            try {
                int idx = Integer.parseInt(indexStr);
                List<ONode> arr = node.getArray();

                // 支持负数索引
                if (idx < 0) {
                    idx = arr.size() + idx; // 例如：-1 表示最后一个元素
                }

                if (idx < 0 || idx >= arr.size()) {
                    throw new PathResolutionException("Index out of bounds: " + idx);
                }

                return arr.get(idx);
            } catch (NumberFormatException e) {
                throw new PathResolutionException("Invalid array index: " + indexStr);
            }
        }

        private ONode resolveFunction(ONode node, String functionName) {
            Function<ONode, ONode> function = FUNCTIONS.get(functionName);
            if (function == null) {
                throw new PathResolutionException("Unknown function: " + functionName);
            }
            return function.apply(node);
        }

        private ONode resolveFilter(ONode node, String filter) {
            if (!node.isArray()) throw new PathResolutionException("Filter can only be used on arrays");
            List<ONode> results = new ArrayList<>();
            for (ONode item : node.getArray()) {
                if (evaluateFilter(item, filter)) {
                    results.add(item);
                }
            }
            return new ONode(results);
        }

        private boolean evaluateFilter(ONode node, String filter) {
            // 简单的过滤器实现，支持基本的条件表达式
            if (filter.startsWith("@.")) {
                String[] parts = filter.substring(2).split(" ");
                if (parts.length == 3) {
                    String key = parts[0];
                    String operator = parts[1];
                    String value = parts[2].replaceAll("['\"]", ""); // 去掉引号
                    ONode target = node.get(key);
                    if (target == null) return false;
                    switch (operator) {
                        case "==":
                            return target.getString().equals(value);
                        case ">":
                            return target.getDouble() > Double.parseDouble(value);
                        case "<":
                            return target.getDouble() < Double.parseDouble(value);
                        case ">=":
                            return target.getDouble() >= Double.parseDouble(value);
                        case "<=":
                            return target.getDouble() <= Double.parseDouble(value);
                        case "!=":
                            return !target.getString().equals(value);
                        default:
                            throw new PathResolutionException("Unsupported operator: " + operator);
                    }
                }
            }
            throw new PathResolutionException("Invalid filter syntax: " + filter);
        }
    }
}
package org.noear.snack.core;

import org.noear.snack.ONode;
import org.noear.snack.exception.PathResolutionException;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * JSON路径查询工具类
 */
public class JsonPath {
    private static final Map<String, Function<ONode, ONode>> FUNCTIONS = new HashMap<>();

    static {
        FUNCTIONS.put("count", node -> new ONode(node.isArray() ? node.size() : 1));
        FUNCTIONS.put("sum", node -> {
            if (!node.isArray()) throw new PathResolutionException("sum() requires an array");
            double sum = node.getArray().stream()
                    .filter(ONode::isNumber)
                    .mapToDouble(ONode::getDouble)
                    .sum();
            return new ONode(sum);
        });
    }

    /**
     * 根据 jsonpath 查询
     */
    public static ONode select(ONode root, String path) {
        if (!path.startsWith("$")) throw new PathResolutionException("Path must start with $");
        return new PathParser(path).select(root);
    }

    /**
     * 根据 jsonpath 删除
     */
    public static void delete(ONode root, String path) {
        if (!path.startsWith("$")) throw new PathResolutionException("Path must start with $");
        new PathParser(path).delete(root);
    }

    /**
     * 根据 jsonpath 生成
     */
    public static void create(ONode root, String path) {
        if (!path.startsWith("$")) throw new PathResolutionException("Path must start with $");
        new PathParser(path).create(root);
    }

    private static class PathParser {
        private final String path;
        private int index;

        PathParser(String path) {
            this.path = path;
            this.index = 0; // 起始位置为 $ 符号
        }

        // 主解析逻辑
        ONode select(ONode root) {
            List<ONode> currentNodes = Collections.singletonList(root);
            index++;

            while (index < path.length()) {
                skipWhitespace();
                if (index >= path.length()) break;

                char ch = path.charAt(index);
                if (ch == '.') {
                    currentNodes = handleDot(currentNodes);
                } else if (ch == '[') {
                    currentNodes = handleBracket(currentNodes);
                } else {
                    throw new PathResolutionException("Unexpected character '" + ch + "' at index " + index);
                }
            }

            return currentNodes.size() == 1 ? currentNodes.get(0) : new ONode(currentNodes);
        }

        // 删除节点
        void delete(ONode root) {
            List<ONode> currentNodes = Collections.singletonList(root);
            index++;

            while (index < path.length()) {
                skipWhitespace();
                if (index >= path.length()) break;

                char ch = path.charAt(index);
                if (ch == '.') {
                    currentNodes = handleDot(currentNodes);
                } else if (ch == '[') {
                    currentNodes = handleBracket(currentNodes);
                } else {
                    throw new PathResolutionException("Unexpected character '" + ch + "' at index " + index);
                }
            }

            if (currentNodes.size() == 1) {
                ONode node = currentNodes.get(0);
                if (node.isObject()) {
                    node.getObject().remove(path.substring(path.lastIndexOf('.') + 1));
                } else if (node.isArray()) {
                    node.getArray().remove(Integer.parseInt(path.substring(path.lastIndexOf('[') + 1, path.lastIndexOf(']'))));
                }
            }
        }

        // 创建节点
        void create(ONode root) {
            List<ONode> currentNodes = Collections.singletonList(root);
            index++;

            while (index < path.length()) {
                skipWhitespace();
                if (index >= path.length()) break;

                char ch = path.charAt(index);
                if (ch == '.') {
                    currentNodes = handleDot(currentNodes);
                } else if (ch == '[') {
                    currentNodes = handleBracket(currentNodes);
                } else {
                    throw new PathResolutionException("Unexpected character '" + ch + "' at index " + index);
                }
            }

            if (currentNodes.size() == 1) {
                ONode node = currentNodes.get(0);
                if (node.isObject()) {
                    node.set(path.substring(path.lastIndexOf('.') + 1), new ONode());
                } else if (node.isArray()) {
                    node.add(new ONode());
                }
            }
        }

        // 处理 '.' 或 '..'，返回新的节点集合
        private List<ONode> handleDot(List<ONode> currentNodes) {
            index++;
            if (index < path.length() && path.charAt(index) == '.') {
                index++;
                currentNodes = resolveRecursive(currentNodes);
                if (index < path.length() && path.charAt(index) != '.' && path.charAt(index) != '[') {
                    currentNodes = resolveKey(currentNodes, false);
                }
            } else {
                currentNodes = resolveKey(currentNodes, true);
            }
            return currentNodes;
        }

        // 处理 '[...]'
        private List<ONode> handleBracket(List<ONode> nodes) {
            index++; // 跳过'['
            String segment = parseSegment(']');
            while (index < path.length() && path.charAt(index) == ']') {
                index++;
            }
            if (segment.equals("*")) {
                return resolveWildcard(nodes);
            } else if (segment.startsWith("?")) {
                return resolveFilter(nodes, segment.substring(1));
            } else {
                return resolveIndex(nodes, segment);
            }
        }

        // 解析键名或函数调用（如 "store" 或 "count()"）
        private List<ONode> resolveKey(List<ONode> nodes, boolean strict) {
            String key = parseSegment('.', '[');
            if (key.endsWith("()")) {
                String funcName = key.substring(0, key.length() - 2);
                return nodes.stream()
                        .map(n -> FUNCTIONS.get(funcName).apply(n))
                        .collect(Collectors.toList());
            }
            return nodes.stream()
                    .map(n -> getChild(n, key, strict))
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());
        }

        private List<ONode> getChild(ONode node, String key, boolean strict) {
            if (node.isObject()) {
                ONode child = node.get(key);
                if (strict && child == null) {
                    throw new PathResolutionException("Missing key '" + key + "'");
                }
                return child != null ? Collections.singletonList(child) : Collections.emptyList();
            }
            return Collections.emptyList(); // 非对象节点直接跳过
        }

        // 处理通配符 *
        private List<ONode> resolveWildcard(List<ONode> nodes) {
            return nodes.stream()
                    .map(n -> {
                        if (n.isArray()) return n.getArray();
                        else if (n.isObject()) return new ArrayList<>(n.getObject().values());
                        else return Collections.<ONode>emptyList();
                    })
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());
        }

        // 处理精确索引（支持负数）
        private List<ONode> resolveIndex(List<ONode> nodes, String indexStr) {
            return nodes.stream()
                    .filter(ONode::isArray)
                    .map(arr -> {
                        int idx = Integer.parseInt(indexStr);
                        if (idx < 0) idx += arr.size();
                        if (idx < 0 || idx >= arr.size()) {
                            throw new PathResolutionException("Index out of bounds: " + idx);
                        }
                        return arr.get(idx);
                    })
                    .collect(Collectors.toList());
        }

        // 处理递归搜索 ..
        private List<ONode> resolveRecursive(List<ONode> nodes) {
            List<ONode> tmp = new ArrayList<>();
            nodes.forEach(node -> collectRecursive(node, tmp));

            List<ONode> results = tmp;

            while (index < path.length()) {
                skipWhitespace();
                if (index >= path.length()) break;
                char ch = path.charAt(index);
                if (ch == '.' || ch == '[') {
                    if (ch == '.') {
                        results = handleDot(results);
                    } else if (ch == '[') {
                        results = handleBracket(results);
                    }
                } else {
                    break;
                }
            }
            return results;
        }

        private void collectRecursive(ONode node, List<ONode> results) {
            if (node.isArray()) {
                node.getArray().forEach(n -> collectRecursive(n, results));
            } else if (node.isObject()) {
                node.getObject().values().forEach(n -> collectRecursive(n, results));
            }
            results.add(node);
        }

        // 处理过滤器（如 [?(@.price > 10)]）
        private List<ONode> resolveFilter(List<ONode> nodes, String filter) {
            return nodes.stream()
                    .filter(n -> {
                        try {
                            return evaluateFilter(n, filter);
                        } catch (Exception e) {
                            return false; // 静默跳过异常
                        }
                    })
                    .collect(Collectors.toList());
        }

        private boolean evaluateFilter(ONode node, String filter) {
            if (filter.startsWith("@.")) {
                String[] parts = filter.substring(2).split("\\s+", 2);
                String keyPath = parts[0];
                if (parts.length == 1) {
                    return resolveNestedPath(node, keyPath) != null;
                }
                String opValue = parts[1];
                String[] opParts = opValue.split("\\s+", 2);
                if (opParts.length < 2) return false;
                String op = opParts[0];
                String value = opParts[1].replaceAll("['\"]", "");
                ONode target = resolveNestedPath(node, keyPath);
                if (target == null) return false;
                if (target.isNumber()) {
                    return compareNumber(op, target.getDouble(), Double.parseDouble(value));
                } else if (target.isString()) {
                    return compareString(op, target.getString(), value);
                }
            }
            return false;
        }

        private ONode resolveNestedPath(ONode node, String keyPath) {
            String[] keys = keyPath.split("\\.");
            ONode current = node;
            for (String key : keys) {
                current = current.get(key);
                if (current == null) return null;
            }
            return current;
        }

        private boolean compareString(String op, String a, String b) {
            switch (op) {
                case "==":
                    return a.equals(b);
                case "!=":
                    return !a.equals(b);
                default:
                    throw new PathResolutionException("Unsupported operator for string: " + op);
            }
        }

        private boolean compareNumber(String op, double a, double b) {
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

        // 解析路径段（支持终止符列表）
        private String parseSegment(char... terminators) {
            StringBuilder sb = new StringBuilder();
            while (index < path.length()) {
                char ch = path.charAt(index);
                if (ch == ']') {
                    index++; // 强制跳过闭合的 ]
                    break;
                }
                if (isTerminator(ch, terminators)) break;
                sb.append(ch);
                index++;
            }
            return sb.toString().trim();
        }

        private boolean isTerminator(char ch, char[] terminators) {
            for (char t : terminators) {
                if (ch == t) return true;
            }
            return ch == '.' || ch == '[' || ch == ']';
        }

        // 跳过空白字符
        private void skipWhitespace() {
            while (index < path.length() && Character.isWhitespace(path.charAt(index))) {
                index++;
            }
        }
    }
}
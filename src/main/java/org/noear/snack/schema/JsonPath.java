package org.noear.snack.schema;

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
                    .filter(o -> o.isNumber())
                    .mapToDouble(n -> n.getDouble()) // 安全获取数值
                    .sum();
            return new ONode(sum); // 返回数值类型节点
        });
    }

    public static ONode query(ONode root, String path) {
        if (!path.startsWith("$")) throw new PathResolutionException("Path must start with $");
        return new PathParser(path).evaluate(root);
    }

    private static class PathParser {
        private final String path;
        private int index;

        PathParser(String path) {
            this.path = path;
            this.index = 0; // 起始位置为 $ 符号
        }

        // 主解析逻辑
        ONode evaluate(ONode root) {
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

        // 处理 '.' 或 '..'，返回新的节点集合
        private List<ONode> handleDot(List<ONode> currentNodes) {
            index++;
            if (index < path.length() && path.charAt(index) == '.') {
                index++;
                currentNodes = resolveRecursive(currentNodes);
                // 递归后继续处理后续路径（例如 $..price）
                if (index < path.length() && path.charAt(index) != '.' && path.charAt(index) != '[') {
                    currentNodes = resolveKey(currentNodes, false); // 宽松模式
                }
            } else {
                currentNodes = resolveKey(currentNodes, true); // 严格模式
            }
            return currentNodes;
        }

        // 处理 '[...]'
        private List<ONode> handleBracket(List<ONode> nodes) {
            index++; // 跳过'['
            String segment = parseSegment(']');
            // 确保跳过闭合的']'
            if (index < path.length() && path.charAt(index) == ']') {
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
            List<ONode> results = new ArrayList<>();
            nodes.forEach(node -> collectRecursive(node, results));
            return results;
        }

        private void collectRecursive(ONode node, List<ONode> results) {
            if (node.isArray()) {
                node.getArray().forEach(n -> collectRecursive(n, results));
            } else if (node.isObject()) {
                node.getObject().values().forEach(n -> collectRecursive(n, results));
            } else {
                results.add(node);
            }
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
                String[] parts = filter.substring(2).split("\\s+");
                if (parts.length >= 3) {
                    String key = parts[0];
                    String op = parts[1];
                    String value = parts[2].replaceAll("['\"]", "");
                    ONode target = node.get(key);
                    if (target == null) return false;

                    // 根据类型安全比较
                    if (target.isString()) {
                        String targetValue = target.getString();
                        return compareString(op, targetValue, value);
                    } else if (target.isNumber()) {
                        double targetValue = target.getDouble();
                        return compareNumber(op, targetValue, Double.parseDouble(value));
                    }
                }
            }
            throw new PathResolutionException("Invalid filter: " + filter);
        }

        private boolean compareString(String op, String a, String b) {
            switch (op) {
                case "==": return a.equals(b);
                case "!=": return !a.equals(b);
                default: throw new PathResolutionException("Unsupported operator for string: " + op);
            }
        }

        private boolean compareNumber(String op, double a, double b) {
            switch (op) {
                case "==": return a == b;
                case "!=": return a != b;
                case ">": return a > b;
                case "<": return a < b;
                case ">=": return a >= b;
                case "<=": return a <= b;
                default: throw new PathResolutionException("Unsupported operator for number: " + op);
            }
        }

        // 解析路径段（支持终止符列表）
        private String parseSegment(char... terminators) {
            StringBuilder sb = new StringBuilder();
            while (index < path.length()) {
                char ch = path.charAt(index);
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

        // 获取子节点
        private List<ONode> getChild(ONode node, String key) {
            if (node.isObject()) {
                ONode child = node.get(key);
                return child != null ? Collections.singletonList(child) : Collections.emptyList();
            }
            throw new PathResolutionException("Node is not an object at key '" + key + "'");
        }

        // 跳过空白字符
        private void skipWhitespace() {
            while (index < path.length() && Character.isWhitespace(path.charAt(index))) {
                index++;
            }
        }
    }
}
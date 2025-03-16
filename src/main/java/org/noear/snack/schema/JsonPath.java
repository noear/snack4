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
            double sum = node.getArray().stream().mapToDouble(ONode::getDouble).sum();
            return new ONode(sum);
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
            this.index = 1; // 跳过 $
        }

        // 核心解析逻辑
        ONode evaluate(ONode root) {
            List<ONode> currentNodes = Collections.singletonList(root);
            while (index < path.length()) {
                char ch = path.charAt(index);
                if (ch == '.') {
                    index++;
                    if (index < path.length() && path.charAt(index) == '.') {
                        index++;
                        currentNodes = resolveRecursive(currentNodes);
                    } else {
                        currentNodes = resolveKey(currentNodes);
                    }
                } else if (ch == '[') {
                    index++;
                    currentNodes = resolveIndex(currentNodes);
                } else {
                    throw new PathResolutionException("Invalid syntax at index " + index);
                }
            }
            return currentNodes.size() == 1 ? currentNodes.get(0) : new ONode(currentNodes);
        }

        // 处理递归搜索 `..`
        private List<ONode> resolveRecursive(List<ONode> nodes) {
            List<ONode> results = new ArrayList<>();
            for (ONode node : nodes) {
                collectRecursive(node, results);
            }
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

        // 处理键访问 `.key`
        private List<ONode> resolveKey(List<ONode> nodes) {
            String key = parseSegment();
            if (key.endsWith("()")) {
                return nodes.stream()
                        .map(n -> resolveFunction(n, key.substring(0, key.length() - 2)))
                        .collect(Collectors.toList());
            }
            return nodes.stream()
                    .map(n -> getNodeByKey(n, key))
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
        }

        // 处理索引 `[0]` 或过滤器 `[?()]`
        private List<ONode> resolveIndex(List<ONode> nodes) {
            String segment = parseSegment().replace("]", "");
            if ("*".equals(segment)) {
                return resolveWildcard(nodes);
            } else if (segment.startsWith("?")) {
                return resolveFilter(nodes, segment.substring(1));
            } else {
                return resolveIndex(nodes, segment);
            }
        }

        // 处理通配符 `*`
        private List<ONode> resolveWildcard(List<ONode> nodes) {
            return nodes.stream()
                    .map(n -> n.isArray() ? n.getArray() : n.isObject() ? new ArrayList<>(n.getObject().values()) : null)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());
        }

        // 处理过滤器 `[?(@.price > 10)]`
        private List<ONode> resolveFilter(List<ONode> nodes, String filter) {
            return nodes.stream()
                    .filter(n -> evaluateFilter(n, filter))
                    .collect(Collectors.toList());
        }

        // 处理数组索引 `[0]`
        private List<ONode> resolveIndex(List<ONode> nodes, String indexStr) {
            return nodes.stream()
                    .filter(n -> n.isArray())
                    .map(n -> {
                        int idx = Integer.parseInt(indexStr);
                        if (idx < 0) idx += n.size();
                        if (idx < 0 || idx >= n.size()) throw new PathResolutionException("Index out of bounds");
                        return n.get(idx);
                    })
                    .collect(Collectors.toList());
        }

        // 辅助方法：解析路径段
        private String parseSegment() {
            StringBuilder sb = new StringBuilder();
            while (index < path.length()) {
                char ch = path.charAt(index);
                if (ch == '.' || ch == '[' || ch == ']') break;
                sb.append(ch);
                index++;
            }
            return sb.toString();
        }

        // 辅助方法：处理键访问
        private List<ONode> getNodeByKey(ONode node, String key) {
            if (!node.isObject()) throw new PathResolutionException("Not an object at key " + key);
            ONode child = node.get(key);
            return child != null ? Collections.singletonList(child) : Collections.emptyList();
        }

        // 辅助方法：处理函数调用
        private ONode resolveFunction(ONode node, String funcName) {
            Function<ONode, ONode> func = FUNCTIONS.get(funcName);
            if (func == null) throw new PathResolutionException("Unknown function: " + funcName);
            return func.apply(node);
        }

        // 辅助方法：过滤器逻辑
        private boolean evaluateFilter(ONode node, String filter) {
            if (filter.startsWith("@.")) {
                String[] parts = filter.substring(2).split("\\s+");
                if (parts.length == 3) {
                    String key = parts[0];
                    String op = parts[1];
                    String value = parts[2].replaceAll("['\"]", "");
                    ONode target = node.get(key);
                    if (target == null) return false;
                    switch (op) {
                        case "==": return target.getString().equals(value);
                        case ">": return target.getDouble() > Double.parseDouble(value);
                        case "<": return target.getDouble() < Double.parseDouble(value);
                        // 其他操作符类似处理
                    }
                }
            }
            throw new PathResolutionException("Invalid filter: " + filter);
        }
    }
}
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
            this.index = 0; // 起始位置为 $ 符号
        }

        ONode evaluate(ONode root) {
            List<ONode> currentNodes = Collections.singletonList(root);
            index++; // 跳过 $

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

        // 处理键名（如 "store", "bicycle"）
        private List<ONode> resolveKey(List<ONode> nodes) {
            String key = parseSegment();
            if (key.endsWith("()")) {
                String funcName = key.substring(0, key.length() - 2);
                return nodes.stream()
                        .map(n -> resolveFunction(n, funcName))
                        .collect(Collectors.toList());
            }
            return nodes.stream()
                    .map(n -> getChild(n, key))
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());
        }

        // 处理索引（如 [0], [*], [?()]）
        private List<ONode> resolveIndex(List<ONode> nodes) {
            String segment = parseSegment().replace("]", "");
            if (segment.equals("*")) {
                return resolveWildcard(nodes);
            } else if (segment.startsWith("?")) {
                return resolveFilter(nodes, segment.substring(1));
            } else {
                return resolveExactIndex(nodes, segment);
            }
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

        // 处理精确索引（如 [0], [-1]）
        private List<ONode> resolveExactIndex(List<ONode> nodes, String indexStr) {
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

        // 解析路径段（如 "store", "book[0]"）
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

        // 获取子节点
        private List<ONode> getChild(ONode node, String key) {
            if (node.isObject()) {
                ONode child = node.get(key);
                return child != null ? Collections.singletonList(child) : Collections.emptyList();
            } else {
                throw new PathResolutionException("Not an object at key '" + key + "'");
            }
        }

        // 处理函数调用（如 count()）
        private ONode resolveFunction(ONode node, String funcName) {
            Function<ONode, ONode> func = FUNCTIONS.get(funcName);
            if (func == null) throw new PathResolutionException("Unknown function: " + funcName);
            return func.apply(node);
        }

        // 处理过滤器（如 [?(@.price > 10)]）
        private List<ONode> resolveFilter(List<ONode> nodes, String filter) {
            return nodes.stream()
                    .filter(n -> evaluateFilter(n, filter))
                    .collect(Collectors.toList());
        }

        private boolean evaluateFilter(ONode node, String filter) {
            // 简化版过滤器实现（支持 @.key OP value）
            if (filter.startsWith("@.")) {
                String[] parts = filter.substring(2).split("\\s+");
                if (parts.length >= 3) {
                    String key = parts[0];
                    String op = parts[1];
                    String value = parts[2].replaceAll("['\"]", "");
                    ONode target = node.get(key);
                    if (target == null) return false;
                    switch (op) {
                        case "==": return target.getString().equals(value);
                        case ">": return target.getDouble() > Double.parseDouble(value);
                        case "<": return target.getDouble() < Double.parseDouble(value);
                        // 扩展其他操作符...
                    }
                }
            }
            throw new PathResolutionException("Unsupported filter: " + filter);
        }
    }
}
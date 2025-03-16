package org.noear.snack.schema;

import org.noear.snack.ONode;

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
            return getNodeByKey(node, key);
        }

        private ONode resolveIndex(ONode node) {
            StringBuilder indexStr = new StringBuilder();
            while (index < path.length()) {
                char ch = path.charAt(index);
                if (ch == ']') {
                    index++; // 跳过 ]
                    break;
                }
                indexStr.append(ch);
                index++;
            }
            return getNodeByIndex(node, indexStr.toString());
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
    }

    public static class PathResolutionException extends RuntimeException {
        public PathResolutionException(String message) {
            super(message);
        }
    }
}
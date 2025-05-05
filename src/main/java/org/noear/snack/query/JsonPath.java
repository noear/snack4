package org.noear.snack.query;

import org.noear.snack.ONode;
import org.noear.snack.core.JsonSource;
import org.noear.snack.exception.PathResolutionException;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * JSON路径查询工具类
 */
public class JsonPath {
    /**
     * 根据 jsonpath 查询
     */
    public static ONode select(ONode root, String path) {
        if (!path.startsWith("$")) throw new PathResolutionException("Path must start with $");
        return new PathParser(path).select(root);
    }

    /**
     * 根据 jsonpath 生成
     */
    public static ONode create(ONode root, String path) {
        if (!path.startsWith("$")) throw new PathResolutionException("Path must start with $");
        return new PathParser(path).create(root);
    }

    /**
     * 根据 jsonpath 删除
     */
    public static void delete(ONode root, String path) {
        if (!path.startsWith("$")) throw new PathResolutionException("Path must start with $");
        new PathParser(path).delete(root);
    }

    private static class PathParser {
        private final String path;
        private int index;
        private boolean isCreateMode;
        private boolean isDeleteMode;

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
                    currentNodes = handleDot(currentNodes, root);
                } else if (ch == '[') {
                    currentNodes = handleBracket(currentNodes, root);
                } else {
                    throw new PathResolutionException("Unexpected character '" + ch + "' at index " + index);
                }
            }

            if (currentNodes.size() > 1) {
                return new ONode(currentNodes);
            } else {
                if ((path.indexOf('?') < 0 && path.indexOf('*') < 0 && path.indexOf("..") < 0 && path.indexOf(",") < 0 && path.indexOf(":") < 0) || path.indexOf("()") > 0) {
                    if (currentNodes.size() > 0) {
                        return currentNodes.get(0);
                    } else {
                        return new ONode();
                    }
                } else {
                    return new ONode(currentNodes);
                }
            }
        }



        // 创建节点
        ONode create(ONode root) {
            isCreateMode = true;
            List<ONode> currentNodes = Collections.singletonList(root);
            index++;

            while (index < path.length()) {
                skipWhitespace();
                if (index >= path.length()) break;

                char ch = path.charAt(index);
                if (ch == '.') {
                    currentNodes = handleDot(currentNodes, root);
                } else if (ch == '[') {
                    currentNodes = handleBracket(currentNodes, root);
                } else {
                    throw new PathResolutionException("Unexpected character '" + ch + "' at index " + index);
                }
            }

            for (ONode node : currentNodes) {
                if (node.isObject()) {
                    node.set(path.substring(path.lastIndexOf('.') + 1), new ONode());
                } else if (node.isArray()) {
                    node.add(new ONode());
                }
            }

            if (currentNodes.size() > 1) {
                return new ONode(currentNodes);
            } else {
                if ((path.indexOf('?') < 0 && path.indexOf('*') < 0 && path.indexOf("..") < 0 && path.indexOf(",") < 0 && path.indexOf(":") < 0) || path.indexOf("()") > 0) {
                    if (currentNodes.size() > 0) {
                        return currentNodes.get(0);
                    } else {
                        return new ONode();
                    }
                } else {
                    return new ONode(currentNodes);
                }
            }
        }

        // 删除节点
        void delete(ONode root) {
            isDeleteMode = true;
            List<ONode> currentNodes = Collections.singletonList(root);
            index++;

            while (index < path.length()) {
                skipWhitespace();
                if (index >= path.length()) break;

                char ch = path.charAt(index);
                if (ch == '.') {
                    currentNodes = handleDot(currentNodes, root);
                } else if (ch == '[') {
                    currentNodes = handleBracket(currentNodes, root);
                } else {
                    throw new PathResolutionException("Unexpected character '" + ch + "' at index " + index);
                }
            }

            if (currentNodes.size() == 1) {
                for (ONode n : currentNodes) {
                    if (n.source != null) {
                        if (n.source.key != null) {
                            if ("*".equals(n.source.key)) {
                                n.source.parent.clear();
                            } else {
                                n.source.parent.remove(n.source.key);
                            }
                        } else {
                            n.source.parent.remove(n.source.index);
                        }
                    }
                }
            }
        }

        // 处理 '.' 或 '..'，返回新的节点集合
        private List<ONode> handleDot(List<ONode> currentNodes, ONode root) {
            index++;
            if (index < path.length() && path.charAt(index) == '.') {
                index++;
                currentNodes = resolveRecursive(currentNodes, root);
                if (index < path.length() && path.charAt(index) != '.' && path.charAt(index) != '[') {
                    currentNodes = resolveKey(currentNodes, false);
                }
            } else {
                currentNodes = resolveKey(currentNodes, false);
            }
            return currentNodes;
        }

        // 处理 '[...]'
        private List<ONode> handleBracket(List<ONode> currentNodes, ONode root) {
            char expect = path.charAt(index - 1);

            index++; // 跳过'['
            String segment = parseSegment(']');
            while (index < path.length() && path.charAt(index) == ']') {
                index++;
            }

            if (segment.equals("*")) {
                // 全选
                currentNodes = resolveWildcard(currentNodes);
            } else if (segment.startsWith("?")) {
                // 条件过滤，如 [?@id]
                // ..*[?...] 支持进一步深度展开
                // ..x[?...] 展开过，但查询后是新的结果可以再展开
                // ..[?...] 展开过，不需要再展开
                currentNodes = resolveFilter(currentNodes, segment.substring(1), expect != '.', root);
            } else if (segment.contains(",")) {
                // 多索引选择，如 [1,4]
                currentNodes = resolveMultiIndex(currentNodes, segment);
            } else if (segment.contains(":")) {
                // 范围选择，如 [1:4]
                currentNodes = resolveRangeIndex(currentNodes, segment);
            } else {
                // 属性选择
                currentNodes = resolveIndex(currentNodes, segment);
            }

            return currentNodes;
        }

        private List<ONode> resolveRangeIndex(List<ONode> nodes, String rangeStr) {
            String[] parts = rangeStr.split(":");
            if (parts.length != 2) {
                throw new PathResolutionException("Invalid range syntax: " + rangeStr);
            }

            return nodes.stream()
                    .filter(ONode::isArray)
                    .flatMap(arr -> {
                        int size = arr.size();
                        int start = parseRangeBound(parts[0], size);
                        int end = parseRangeBound(parts[1], size);

                        // 调整范围确保有效
                        start = Math.max(0, Math.min(start, size));
                        end = Math.max(0, Math.min(end, size));

                        if (start >= end) {
                            return Stream.empty();
                        }

                        return IntStream.range(start, end)
                                .mapToObj(idx -> {
                                    ONode node = arr.get(idx);
                                    node.source = new JsonSource(arr, null, idx);
                                    return node;
                                });
                    })
                    .collect(Collectors.toList());
        }

        // 辅助方法：解析范围边界
        private int parseRangeBound(String boundStr, int size) {
            if (boundStr.isEmpty()) {
                return 0; // 默认开始
            }

            int bound = Integer.parseInt(boundStr.trim());
            if (bound < 0) {
                bound += size;
            }
            return bound;
        }

        // 新增方法：处理多索引选择
        private List<ONode> resolveMultiIndex(List<ONode> nodes, String indicesStr) {
            Stream<Integer> indexParts = Arrays.stream(indicesStr.split(","))
                    .map(String::trim)
                    .map(Integer::parseInt);

            return nodes.stream()
                    .filter(ONode::isArray)
                    .flatMap(arr -> {
                        return indexParts
                                .map(idx -> {
                                    if (idx < 0) idx += arr.size();
                                    if (idx < 0 || idx >= arr.size()) {
                                        throw new PathResolutionException("Index out of bounds: " + idx);
                                    }
                                    ONode node = arr.get(idx);
                                    node.source = new JsonSource(arr, null, idx);
                                    return node;
                                });
                    })
                    .collect(Collectors.toList());
        }

        // 解析键名或函数调用（如 "store" 或 "count()"）
        private List<ONode> resolveKey(List<ONode> nodes, boolean strict) {
            String key = parseSegment('.', '[');
            if (key.endsWith("()")) {
                String funcName = key.substring(0, key.length() - 2);
                return Collections.singletonList(
                        FunctionLib.get(funcName).apply(nodes) // 传入节点列表
                );
            } else if (key.equals("*")) {
                return nodes;
            } else {
                return nodes.stream()
                        .map(n -> getChild(n, key, strict))
                        .flatMap(Collection::stream)
                        .collect(Collectors.toList());
            }
        }

        private List<ONode> getChild(ONode node, String key, boolean strict) {
            if (isCreateMode) {
                node.newObject();
            }

            if (node.isObject()) {
                ONode child = node.get(key);
                if (child == null) {
                    if (isCreateMode) {
                        child = new ONode();
                        node.set(key, child);
                    } else if (strict) {
                        throw new PathResolutionException("Missing key '" + key + "'");
                    }
                } else {
                    child.source = new JsonSource(node, key, 0);
                }
                return child != null ? Collections.singletonList(child) : Collections.emptyList();
            }
            return Collections.emptyList(); // 非对象节点直接跳过
        }

        // 处理通配符 *
        private List<ONode> resolveWildcard(List<ONode> nodes) {
            return nodes.stream()
                    .map(n -> {
                        Collection<ONode> childs;
                        if (n.isArray()) {
                            childs = n.getArray();
                        } else if (n.isObject()) {
                            childs = n.getObject().values();
                        } else {
                            childs = Collections.<ONode>emptyList();
                        }

                        if (isDeleteMode) {
                            JsonSource source = new JsonSource(n, "*", 0);
                            for (ONode child : childs) {
                                child.source = source;
                            }
                        }

                        return childs;

                    })
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());
        }

        // 处理精确索引（支持负数）
        private List<ONode> resolveIndex(List<ONode> nodes, String indexStr) {
            return nodes.stream()
                    .filter(o -> {
                        if (isCreateMode) {
                            o.newArray();
                            return true;
                        } else {
                            return o.isArray();
                        }
                    })
                    .map(arr -> {
                        int idx = Integer.parseInt(indexStr);

                        if (isCreateMode) {
                            //0 算1个，-1 算1个（至少算1个）
                            int count = 0;
                            if (idx < 0) {
                                count = Math.abs(idx) - arr.size();
                            } else {
                                count = idx + 1 - arr.size();
                            }

                            for (int i = 0; i < count; i++) {
                                arr.add(new ONode());
                            }
                        } else {
                            if (idx < 0) idx += arr.size();
                            if (idx < 0 || idx >= arr.size()) {
                                throw new PathResolutionException("Index out of bounds: " + idx);
                            }
                        }
                        ONode rst = arr.get(idx);
                        rst.source = new JsonSource(arr, null, idx);

                        return rst;
                    })
                    .collect(Collectors.toList());
        }

        // 处理递归搜索 ..
        private List<ONode> resolveRecursive(List<ONode> nodes, ONode root) {
            List<ONode> tmp = new ArrayList<>();
            nodes.forEach(node -> collectRecursive(node, tmp));

            List<ONode> results = tmp;

            while (index < path.length()) {
                skipWhitespace();
                if (index >= path.length()) break;
                char ch = path.charAt(index);
                if (ch == '.' || ch == '[') {
                    if (ch == '.') {
                        results = handleDot(results, root);
                    } else if (ch == '[') {
                        results = handleBracket(results, root);
                    }
                } else {
                    break;
                }
            }
            return results;
        }

        private void collectRecursive(ONode node, List<ONode> results) {
            if (node.isArray()) {
                for (ONode n1 : node.getArray()) {
                    results.add(n1);
                    collectRecursive(n1, results);
                }
            } else if (node.isObject()) {
                for (ONode n1 : node.getObject().values()) {
                    results.add(n1);
                    collectRecursive(n1, results);
                }
            }
        }

        // 处理过滤器（如 [?(@.price > 10)]）
        private List<ONode> resolveFilter(List<ONode> nodes, String filterStr, boolean isFlatten, ONode root) {
            Expression expression = Expression.get(filterStr);

            if (isFlatten) {
                return nodes.stream()
                        .flatMap(n -> flattenNode(n)) // 使用递归展开多级数组
                        .filter(n -> expression.test(n, root))
                        .collect(Collectors.toList());
            } else {
                return nodes.stream()
                        .filter(n -> expression.test(n, root))
                        .collect(Collectors.toList());
            }
        }

        // 新增递归展开方法
        private Stream<ONode> flattenNode(ONode node) {
            if (node.isArray()) {
                return node.getArray().stream()
                        .flatMap(this::flattenNode)
                        .filter(n -> !n.isNull());
            }
            return Stream.of(node);
        }


        // 解析路径段（支持终止符列表）
        private String parseSegment(char... terminators) {
            StringBuilder sb = new StringBuilder();
            boolean inRegex = false; // 标记是否正在解析正则表达式
            boolean inBracket = false;

            while (index < path.length()) {
                char ch = path.charAt(index);

                // 处理正则表达式开始/结束标记
                if (ch == '/' && !inRegex) {
                    inRegex = true;
                    sb.append(ch);
                    index++;
                    continue;
                } else if (ch == '/' && inRegex) {
                    inRegex = false;
                    sb.append(ch);
                    index++;
                    continue;
                }

                // 处理[]内嵌表达式结束
                if (ch == ']' && inBracket) {
                    inBracket = false;
                    sb.append(ch);
                    index++;
                    continue;
                }

                // 如果在正则表达式内部，忽略终止符检查
                if (!inBracket && !inRegex && isTerminator(ch, terminators)) {
                    if (ch == ']') {
                        index++; // 跳过闭合的 ]
                    }
                    break;
                }

                // 处理[]内嵌表达式开始
                if (ch == '[' && !inBracket) {
                    inBracket = true;
                }

                sb.append(ch);
                index++;
            }

            return sb.toString().trim();
        }

        private boolean isTerminator(char ch, char[] terminators) {
            for (char t : terminators) {
                if (ch == t) return true;
            }
            return false;
        }

        // 跳过空白字符
        private void skipWhitespace() {
            while (index < path.length() && Character.isWhitespace(path.charAt(index))) {
                index++;
            }
        }
    }
}
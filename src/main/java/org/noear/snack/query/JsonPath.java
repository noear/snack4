package org.noear.snack.query;

import org.noear.snack.ONode;
import org.noear.snack.core.JsonSource;
import org.noear.snack.exception.PathResolutionException;

import java.util.*;
import java.util.stream.Collectors;
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
        return new PathParser(path).select(new Context(root));
    }

    /**
     * 根据 jsonpath 生成
     */
    public static ONode create(ONode root, String path) {
        if (!path.startsWith("$")) throw new PathResolutionException("Path must start with $");
        return new PathParser(path).create(new Context(root));
    }

    /**
     * 根据 jsonpath 删除
     */
    public static void delete(ONode root, String path) {
        if (!path.startsWith("$")) throw new PathResolutionException("Path must start with $");
        new PathParser(path).delete(new Context(root));
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
        ONode select(Context context) {
            List<ONode> currentNodes = Collections.singletonList(context.root);
            index++;

            while (index < path.length()) {
                skipWhitespace();
                if (index >= path.length()) break;

                char ch = path.charAt(index);
                if (ch == '.') {
                    currentNodes = handleDot(currentNodes, context);
                } else if (ch == '[') {
                    currentNodes = handleBracket(currentNodes, context);
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
        ONode create(Context context) {
            isCreateMode = true;
            List<ONode> currentNodes = Collections.singletonList(context.root);
            index++;

            while (index < path.length()) {
                skipWhitespace();
                if (index >= path.length()) break;

                char ch = path.charAt(index);
                if (ch == '.') {
                    currentNodes = handleDot(currentNodes, context);
                } else if (ch == '[') {
                    currentNodes = handleBracket(currentNodes, context);
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
        void delete(Context context) {
            isDeleteMode = true;
            List<ONode> currentNodes = Collections.singletonList(context.root);
            index++;

            while (index < path.length()) {
                skipWhitespace();
                if (index >= path.length()) break;

                char ch = path.charAt(index);
                if (ch == '.') {
                    currentNodes = handleDot(currentNodes, context);
                } else if (ch == '[') {
                    currentNodes = handleBracket(currentNodes, context);
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
        private List<ONode> handleDot(List<ONode> currentNodes, Context context) {
            index++;
            if (index < path.length() && path.charAt(index) == '.') {
                index++;

                if (path.charAt(index) == '*') {
                    index++;
                } else {
                    context.flattened = true;
                }

                currentNodes = resolveRecursive(currentNodes, context);

                if (index < path.length() && path.charAt(index) != '.' && path.charAt(index) != '[') {
                    currentNodes = resolveKey(currentNodes, false, true);
                    context.flattened = false;
                }
            } else {
                char ch = path.charAt(index);
                if (ch == '[') {
                    currentNodes = handleBracket(currentNodes, context);
                } else {
                    currentNodes = resolveKey(currentNodes, false, false);
                }
            }

            return currentNodes;
        }

        // 处理 '[...]'
        private List<ONode> handleBracket(List<ONode> currentNodes, Context context) {
            index++; // 跳过'['
            String segment = parseSegment(']');
            while (index < path.length() && path.charAt(index) == ']') {
                index++;
            }

            if (segment.equals("*")) {
                // 全选
                currentNodes = resolveWildcard(currentNodes);
            } else {
                if (segment.startsWith("?")) {
                    // 条件过滤，如 [?@id]
                    // ..*[?...] 支持进一步深度展开
                    // ..x[?...] 已展开过，但查询后是新的结果可以再展开
                    // ..[?...] 已展开过，不需要再展开
                    currentNodes = resolveFilter(currentNodes, segment.substring(1), context);
                } else if (segment.contains(",")) {
                    // 多索引选择，如 [1,4], ['a','b']
                    currentNodes = resolveMultiIndex(currentNodes, segment);
                } else if (segment.contains(":")) {
                    // 范围选择，如 [1:4]
                    currentNodes = resolveRangeIndex(currentNodes, segment);
                } else {
                    // 属性选择
                    currentNodes = resolveIndex(currentNodes, segment);
                }

                context.flattened = false;
            }

            return currentNodes;
        }

        private List<ONode> resolveRangeIndex(List<ONode> nodes, String rangeStr) {
            String[] parts = rangeStr.split(":", 3); //[start:end:step]
            if (parts.length == 1) {
                throw new PathResolutionException("Invalid range syntax: " + rangeStr);
            }

            final int step = (parts.length == 3 && parts[2].length() > 0) ? Integer.parseInt(parts[2]) : 1;

            if (step == 0) {
                return Collections.emptyList();
            }

            return nodes.stream()
                    .filter(ONode::isArray)
                    .flatMap(arr -> {
                        int size = arr.size();
                        int start = parseRangeBound(parts[0], (step > 0 ? 0 : size - 1), size);
                        int end = parseRangeBound(parts[1], (step > 0 ? size : -1), size);

                        // 调整范围确保有效
                        RangeUtil.Bounds bounds = RangeUtil.bounds(start, end, step, size);

                        List<ONode> result = new ArrayList<>();
                        if (step > 0) {
                            int i = bounds.lower;
                            while (i < bounds.upper) {
                                ONode node = arr.get(i);
                                node.source = new JsonSource(arr, null, i);
                                result.add(node);
                                i += step;
                            }
                        } else {
                            int i = bounds.upper;
                            while (bounds.lower < i) {
                                ONode node = arr.get(i);
                                node.source = new JsonSource(arr, null, i);
                                result.add(node);
                                i += step;
                            }
                        }
                        return result.stream();
                    })
                    .collect(Collectors.toList());
        }

        // 辅助方法：解析范围边界
        private int parseRangeBound(String boundStr, int def, int size) {
            if (boundStr.isEmpty()) {
                return def; // 默认开始
            }

            int bound = Integer.parseInt(boundStr.trim());
            if (bound < 0) {
                bound += size;
            }
            return bound;
        }

        // 新增方法：处理多索引选择
        private List<ONode> resolveMultiIndex(List<ONode> nodes, String indicesStr) {
            Stream<String> indexParts = Arrays.stream(indicesStr.split(","))
                    .map(String::trim);


            return nodes.stream()
                    .flatMap(n -> {
                        return indexParts.flatMap(key->{
                            char ch = key.charAt(0);
                            if (ch == '\'') {
                                //string
                                if (n.isObject()) {
                                    ONode p = n.get(key.substring(1, key.length() - 1));
                                    if (p != null) {
                                        return Stream.of(p);
                                    }
                                }

                                return Stream.empty();
                            } else if (ch == '*') {
                                //*
                                if (n.isArray()) {
                                    return n.getArray().stream();
                                } else if (n.isObject()) {
                                    return n.getObject().values().stream();
                                }

                                return Stream.empty();
                            } else {
                                //num
                                if (n.isArray()) {
                                    int idx = Integer.parseInt(key);
                                    if (idx < 0) idx += n.size();
                                    if (idx < 0 || idx >= n.size()) {
                                        throw new PathResolutionException("Index out of bounds: " + idx);
                                    }
                                    ONode node = n.get(idx);
                                    node.source = new JsonSource(n, null, idx);
                                    return Stream.of(node);
                                }

                                return Stream.empty();
                            }
                        });
                    })
                    .collect(Collectors.toList());

        }

        // 解析键名或函数调用（如 "store" 或 "count()"）
        private List<ONode> resolveKey(List<ONode> nodes, boolean strict, boolean flattened) {
            String key = parseSegment('.', '[');
            if (key.endsWith("()")) {
                String funcName = key.substring(0, key.length() - 2);
                return Collections.singletonList(
                        FunctionLib.get(funcName).apply(nodes) // 传入节点列表
                );
            } else if (key.equals("*")) {
                if (flattened) {
                    return nodes;
                } else {
                    return resolveWildcard(nodes);
                }
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
            if (indexStr.startsWith("'")) {
                final String key = indexStr.substring(1, indexStr.length() - 1);

                return nodes.stream()
                        .filter(o -> {
                            if (isCreateMode) {
                                o.newObject();
                                return true;
                            } else {
                                return o.isObject();
                            }
                        })
                        .map(obj -> {
                            if (isCreateMode) {
                                obj.getOrNew(key);
                            }

                            ONode rst = obj.get(key);
                            rst.source = new JsonSource(obj, key, 0);

                            return rst;
                        })
                        .collect(Collectors.toList());
            } else {
                final int index = Integer.parseInt(indexStr);

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
                            int idx = index;
                            if (idx < 0) {
                                idx = arr.size() + idx;
                            }

                            if (isCreateMode) {
                                int count = idx + 1 - arr.size();
                                for (int i = 0; i < count; i++) {
                                    arr.add(new ONode());
                                }
                            }

                            if (idx < 0 || idx >= arr.size()) {
                                throw new PathResolutionException("Index out of bounds: " + idx);
                            }

                            ONode rst = arr.get(idx);
                            rst.source = new JsonSource(arr, null, idx);

                            return rst;
                        })
                        .collect(Collectors.toList());
            }
        }

        // 处理递归搜索 ..
        private List<ONode> resolveRecursive(List<ONode> nodes, Context context) {
            List<ONode> tmp = new ArrayList<>();
            nodes.forEach(node -> collectRecursive(node, tmp));

            List<ONode> results = tmp;

            while (index < path.length()) {
                skipWhitespace();
                if (index >= path.length()) break;
                char ch = path.charAt(index);
                if (ch == '.' || ch == '[') {
                    if (ch == '.') {
                        results = handleDot(results, context);
                    } else if (ch == '[') {
                        results = handleBracket(results, context);
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
        private List<ONode> resolveFilter(List<ONode> nodes, String filterStr, Context context) {
            Expression expression = Expression.get(filterStr);

            if (context.flattened) {
                return nodes.stream()
                        .filter(n -> expression.test(n, context.root))
                        .collect(Collectors.toList());
            } else {
                return nodes.stream()
                        .flatMap(n -> flattenNode(n)) // 使用递归展开多级数组
                        .filter(n -> expression.test(n, context.root))
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

    static class Context {
        public boolean flattened = false;
        public final ONode root;

        public Context(ONode root) {
            this.root = root;
        }
    }
}
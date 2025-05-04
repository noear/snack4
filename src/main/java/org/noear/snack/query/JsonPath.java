package org.noear.snack.query;

import org.noear.snack.ONode;
import org.noear.snack.core.JsonSource;
import org.noear.snack.exception.PathResolutionException;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * JSON路径查询工具类（带预解析优化）
 */
public class JsonPath {
    /**
     * 路径编译器（带缓存）
     */
    private static final PathCompiler pathCompiler = new PathCompiler();

    public static ONode select(ONode root, String path) {
        List<PathSegment> segments = pathCompiler.compile(path);
        return new PathProcessor(segments).processSelect(root);
    }

    public static void delete(ONode root, String path) {
        List<PathSegment> segments = pathCompiler.compile(path);
        new PathProcessor(segments).processDelete(root);
    }

    public static void create(ONode root, String path) {
        List<PathSegment> segments = pathCompiler.compile(path);
        new PathProcessor(segments).processCreate(root);
    }

    /**
     * 路径片段定义
     */
    static class PathSegment {
        enum Type {ROOT, RECURSIVE, PROPERTY, WILDCARD, INDEX, FILTER, FUNCTION}

        final Type type;
        final String value;
        final Predicate<ONode> filter;
        final int index;

        PathSegment(Type type) {
            this(type, null, null, -1);
        }

        PathSegment(Type type, String value) {
            this(type, value, null, -1);
        }

        PathSegment(Predicate<ONode> filter) {
            this(Type.FILTER, null, filter, -1);
        }

        PathSegment(int index) {
            this(Type.INDEX, null, null, index);
        }

        private PathSegment(Type type, String value, Predicate<ONode> filter, int index) {
            this.type = type;
            this.value = value;
            this.filter = filter;
            this.index = index;
        }
    }

    /**
     * 路径编译器
     */
    static class PathCompiler {
        private static final int MAX_CACHE_SIZE = 1024;
        private final Map<String, List<PathSegment>> cache = new LinkedHashMap<String, List<PathSegment>>() {
            @Override
            protected boolean removeEldestEntry(Map.Entry eldest) {
                return size() > MAX_CACHE_SIZE;
            }
        };

        public List<PathSegment> compile(String path) {
            return cache.computeIfAbsent(path, k -> doCompile(k));
        }

        private List<PathSegment> doCompile(String path) {
            List<PathSegment> segments = new ArrayList<>();
            ParserContext ctx = new ParserContext(path);

            if (ctx.current() != '$') {
                throw new PathResolutionException("Path must start with $");
            }
            segments.add(new PathSegment(PathSegment.Type.ROOT));
            ctx.consume();

            while (ctx.hasNext()) {
                ctx.skipSpaces();
                char c = ctx.current();

                if (c == '.') {
                    parseDot(ctx, segments);
                } else if (c == '[') {
                    parseBracket(ctx, segments);
                } else {
                    throw new PathResolutionException("Unexpected char '" + c + "' at " + ctx.pos());
                }
            }

            return segments;
        }

        private void parseDot(ParserContext ctx, List<PathSegment> segs) {
            ctx.consume(); // 跳过 .
            if (ctx.current() == '.') { // 递归 ..
                ctx.consume();
                segs.add(new PathSegment(PathSegment.Type.RECURSIVE));
                ctx.skipSpaces();
                if (ctx.current() != '[' && ctx.current() != '.') {
                    parseProperty(ctx, segs);
                }
            } else { // 普通属性
                parseProperty(ctx, segs);
            }
        }

        private void parseProperty(ParserContext ctx, List<PathSegment> segs) {
            StringBuilder sb = new StringBuilder();
            while (ctx.hasNext()) {
                char c = ctx.current();
                if (c == '.' || c == '[' || Character.isWhitespace(c)) break;
                sb.append(c);
                ctx.consume();
            }
            String prop = sb.toString().trim();
            if (prop.endsWith("()")) { // 函数
                segs.add(new PathSegment(PathSegment.Type.FUNCTION, prop.substring(0, prop.length() - 2)));
            } else {
                segs.add(new PathSegment(PathSegment.Type.PROPERTY, prop));
            }
        }

        private void parseBracket(ParserContext ctx, List<PathSegment> segs) {
            ctx.consume(); // 跳过 [
            ctx.skipSpaces();
            char c = ctx.current();

            if (c == '*') { // 通配符
                ctx.consume();
                segs.add(new PathSegment(PathSegment.Type.WILDCARD));
            } else if (c == '?') { // 过滤器
                String filterExpr = parseFilter(ctx);
                segs.add(new PathSegment(compileFilter(filterExpr)));
            } else if (Character.isDigit(c) || c == '-') { // 索引
                int index = parseIndex(ctx);
                segs.add(new PathSegment(index));
            }
            ctx.skipUntil(']');
            ctx.consume(); // 跳过 ]
        }

        private String parseFilter(ParserContext ctx) {
            ctx.consume(); // 跳过 ?
            ctx.skipSpaces();
            StringBuilder sb = new StringBuilder();
            while (ctx.hasNext() && ctx.current() != ']') {
                sb.append(ctx.current());
                ctx.consume();
            }
            return sb.toString().trim();
        }

        private int parseIndex(ParserContext ctx) {
            StringBuilder sb = new StringBuilder();
            while (ctx.hasNext() && (Character.isDigit(ctx.current()) || ctx.current() == '-')) {
                sb.append(ctx.current());
                ctx.consume();
            }
            return Integer.parseInt(sb.toString());
        }

        private Predicate<ONode> compileFilter(String expr) {
            // 示例：实现等于比较
            Matcher matcher = Pattern.compile("@\\.?(\\w+)\\s*==\\s*'?(.*?)'?").matcher(expr);
            if (matcher.find()) {
                String key = matcher.group(1);
                String value = matcher.group(2).replace("'", "");
                return node -> {
                    ONode target = node.get(key);
                    return target != null && target.getString().equals(value);
                };
            }
            return node -> false;
        }
    }

    /**
     * 路径处理器
     */
    static class PathProcessor {
        private final List<PathSegment> segments;
        private boolean isCreateMode;
        private boolean isDeleteMode;

        PathProcessor(List<PathSegment> segments) {
            this.segments = segments;
        }

        ONode processSelect(ONode root) {
            List<ONode> current = Collections.singletonList(root);
            for (PathSegment seg : segments.subList(1, segments.size())) {
                current = processSegment(current, seg);
                if (current.isEmpty()) break;
            }
            return buildResult(current);
        }

        void processDelete(ONode root) {
            isDeleteMode = true;
            List<ONode> current = Collections.singletonList(root);
            for (int i = 1; i < segments.size() - 1; i++) {
                current = processSegment(current, segments.get(i));
            }
            deleteNodes(processSegment(current, segments.get(segments.size() - 1)));
        }

        void processCreate(ONode root) {
            isCreateMode = true;
            List<ONode> current = Collections.singletonList(root);
            for (int i = 1; i < segments.size(); i++) {
                current = processSegment(current, segments.get(i));
            }
        }

        private List<ONode> processSegment(List<ONode> nodes, PathSegment seg) {
            switch (seg.type) {
                case PROPERTY:
                    return resolveProperty(nodes, seg.value);
                case RECURSIVE:
                    return resolveRecursive(nodes);
                case WILDCARD:
                    return resolveWildcard(nodes);
                case FILTER:
                    return resolveFilter(nodes, seg.filter);
                case INDEX:
                    return resolveIndex(nodes, seg.index);
                case FUNCTION:
                    return resolveFunction(nodes, seg.value);
                default:
                    return Collections.emptyList();
            }
        }

        // 处理属性访问
        private List<ONode> resolveProperty(List<ONode> nodes, String prop) {
            return nodes.stream()
                    .map(node -> getChild(node, prop))
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
        }

        private List<ONode> getChild(ONode node, String key) {
            if (isCreateMode) {
                if (!node.isObject()) node.newObject();
                if (!node.hasKey(key)) node.set(key, new ONode());
            }

            ONode child = node.get(key);
            if (child == null) {
                return Collections.emptyList();
            }
            if (isDeleteMode) {
                child.source = new JsonSource(node, key, -1);
            }
            return Collections.singletonList(child);
        }

        // 处理通配符
        private List<ONode> resolveWildcard(List<ONode> nodes) {
            return nodes.stream()
                    .flatMap(node -> {
                        if (node.isArray()) return node.getArray().stream();
                        if (node.isObject()) return node.getObject().values().stream();
                        return Stream.empty();
                    })
                    .peek(child -> {
                        if (isDeleteMode) child.source = new JsonSource(null, "*", -1);
                    })
                    .collect(Collectors.toList());
        }

        // 处理索引
        private List<ONode> resolveIndex(List<ONode> nodes, int index) {
            return nodes.stream()
                    .filter(ONode::isArray)
                    .map(arr -> {
                        if (isCreateMode) {
                            while (arr.size() <= index) arr.add(new ONode());
                        }
                        int idx = index >= 0 ? index : arr.size() + index;
                        return arr.get(idx);
                    })
                    .collect(Collectors.toList());
        }

        // 处理递归搜索
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
            }
            results.add(node);
        }

        // 处理过滤器
        private List<ONode> resolveFilter(List<ONode> nodes, Predicate<ONode> filter) {
            return nodes.stream()
                    .flatMap(n -> flatten(n))
                    .filter(filter)
                    .collect(Collectors.toList());
        }

        private Stream<ONode> flatten(ONode node) {
            if (node.isArray()) {
                return node.getArray().stream().flatMap(this::flatten);
            }
            return Stream.of(node);
        }

        // 处理函数
        private List<ONode> resolveFunction(List<ONode> nodes, String func) {
            Function<List<ONode>, ONode> fn = Functions.get(func);
            return Collections.singletonList(fn.apply(nodes));
        }

        // 构建结果
        private ONode buildResult(List<ONode> nodes) {
            if (nodes.isEmpty()) return new ONode();
            if (nodes.size() == 1) return nodes.get(0);
            return new ONode(nodes);
        }

        // 删除节点
        private void deleteNodes(List<ONode> nodes) {
            nodes.forEach(node -> {
                if (node.source != null) {
                    if (node.source.parent != null) {
                        if (node.source.key != null) {
                            node.source.parent.remove(node.source.key);
                        } else if (node.source.index >= 0) {
                            node.source.parent.remove(node.source.index);
                        }
                    }
                }
            });
        }
    }

    /**
     * 解析上下文辅助类
     */
    static class ParserContext {
        private final String input;
        private int pos;

        ParserContext(String input) {
            this.input = input;
            this.pos = 0;
        }

        char current() {
            return pos < input.length() ? input.charAt(pos) : 0;
        }

        boolean hasNext() {
            return pos < input.length();
        }

        void consume() {
            pos++;
        }

        int pos() {
            return pos;
        }

        void skipSpaces() {
            while (hasNext() && Character.isWhitespace(current())) consume();
        }

        void skipUntil(char end) {
            while (hasNext() && current() != end) consume();
        }
    }
}
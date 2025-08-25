package org.noear.snack.query;

import org.noear.snack.ONode;
import org.noear.snack.core.JsonSource;
import org.noear.snack.exception.PathResolutionException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * JSON路径查询工具类（支持路径预编译和步骤缓存）
 */
public class JsonPathTmp {
    // 缓存已编译的路径
    private static final Map<String, CompiledPath> PATH_CACHE = new ConcurrentHashMap<>();

    /**
     * 根据 jsonpath 查询
     */
    public static ONode select(ONode root, String path) {
        if (!path.startsWith("$")) throw new PathResolutionException("Path must start with $");
        CompiledPath compiled = compilePath(path);
        return compiled.select(new QueryContext(root));
    }

    /**
     * 根据 jsonpath 生成
     */
    public static ONode create(ONode root, String path) {
        if (!path.startsWith("$")) throw new PathResolutionException("Path must start with $");
        CompiledPath compiled = compilePath(path);
        return compiled.create(new QueryContext(root));
    }

    /**
     * 根据 jsonpath 删除
     */
    public static void delete(ONode root, String path) {
        if (!path.startsWith("$")) throw new PathResolutionException("Path must start with $");
        CompiledPath compiled = compilePath(path);
        compiled.delete(new QueryContext(root));
    }

    /**
     * 编译路径并缓存
     */
    private static CompiledPath compilePath(String path) {
        return PATH_CACHE.computeIfAbsent(path, CompiledPath::new);
    }

    /**
     * 已编译的路径对象，包含可重用的步骤序列
     */
    private static class CompiledPath {
        private final String path;
        private final List<Step> steps;

        CompiledPath(String path) {
            this.path = path;
            this.steps = parseSteps(path);
        }

        ONode select(QueryContext context) {
            context.setMode(QueryMode.SELECT);
            return executeSteps(context);
        }

        ONode create(QueryContext context) {
            context.setMode(QueryMode.CREATE);
            return executeSteps(context);
        }

        void delete(QueryContext context) {
            context.setMode(QueryMode.DELETE);
            executeSteps(context);
        }

        private ONode executeSteps(QueryContext context) {
            List<ONode> currentNodes = Collections.singletonList(context.root);

            for (Step step : steps) {
                currentNodes = step.execute(currentNodes, context);

                if (currentNodes.isEmpty() && context.isEarlyTerminationAllowed()) {
                    break;
                }
            }

            return buildResult(currentNodes, context);
        }

        private ONode buildResult(List<ONode> nodes, QueryContext context) {
            if (nodes.size() > 1) {
                return new ONode(nodes);
            } else if (!nodes.isEmpty()) {
                return nodes.get(0);
            } else {
                return new ONode();
            }
        }

        private List<Step> parseSteps(String path) {
            PathParser parser = new PathParser(path);
            return parser.parseSteps();
        }
    }

    /**
     * 查询上下文，管理执行状态
     */
    private static class QueryContext {
        public final ONode root;
        private QueryMode mode;
        private boolean flattened = false;

        public QueryContext(ONode root) {
            this.root = root;
        }

        public void setMode(QueryMode mode) {
            this.mode = mode;
        }

        public QueryMode getMode() {
            return mode;
        }

        public boolean isFlattened() {
            return flattened;
        }

        public void setFlattened(boolean flattened) {
            this.flattened = flattened;
        }

        public boolean isEarlyTerminationAllowed() {
            return mode != QueryMode.CREATE;
        }
    }

    /**
     * 步骤接口，所有操作步骤实现此接口
     */
    private interface Step {
        List<ONode> execute(List<ONode> currentNodes, QueryContext context);
    }

    /**
     * 步骤工厂
     */
    private static class StepFactory {
        static Step createDotStep(String segment, boolean recursive) {
            if (recursive) {
                return new RecursiveDotStep(segment);
            } else if (segment.equals("*")) {
                return new WildcardStep();
            } else if (segment.endsWith("()")) {
                String funcName = segment.substring(0, segment.length() - 2);
                return new FunctionStep(funcName);
            } else {
                return new PropertyStep(segment);
            }
        }

        static Step createBracketStep(String segment) {
            if (segment.equals("*")) {
                return new WildcardStep();
            } else if (segment.startsWith("?")) {
                return new FilterStep(segment.substring(1));
            } else if (segment.contains(",")) {
                return new MultiIndexStep(segment);
            } else if (segment.contains(":")) {
                return new RangeStep(segment);
            } else {
                return new SingleIndexStep(segment);
            }
        }
    }

    // 具体步骤实现
    private static class PropertyStep implements Step {
        private final String key;

        PropertyStep(String key) {
            this.key = key;
        }

        @Override
        public List<ONode> execute(List<ONode> currentNodes, QueryContext context) {
            return currentNodes.stream()
                    .map(n -> getChild(n, key, context.getMode() == QueryMode.CREATE))
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());
        }

        private List<ONode> getChild(ONode node, String key, boolean createIfMissing) {
            if (createIfMissing) {
                node.newObject();
            }

            if (node.isObject()) {
                ONode child = node.get(key);
                if (child == null && createIfMissing) {
                    child = new ONode();
                    node.set(key, child);
                }

                if (child != null) {
                    child.source = new JsonSource(node, key, 0);
                    return Collections.singletonList(child);
                }
            }
            return Collections.emptyList();
        }
    }

    private static class WildcardStep implements Step {
        @Override
        public List<ONode> execute(List<ONode> currentNodes, QueryContext context) {
            return currentNodes.stream()
                    .map(n -> {
                        Collection<ONode> children;
                        if (n.isArray()) {
                            children = n.getArray();
                        } else if (n.isObject()) {
                            children = n.getObject().values();
                        } else {
                            children = Collections.emptyList();
                        }

                        if (context.getMode() == QueryMode.DELETE) {
                            JsonSource source = new JsonSource(n, "*", 0);
                            for (ONode child : children) {
                                child.source = source;
                            }
                        }

                        return children;
                    })
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());
        }
    }

    private static class FunctionStep implements Step {
        private final String funcName;

        FunctionStep(String funcName) {
            this.funcName = funcName;
        }

        @Override
        public List<ONode> execute(List<ONode> currentNodes, QueryContext context) {
            ONode result = FunctionLib.get(funcName).apply(currentNodes);
            return Collections.singletonList(result);
        }
    }

    private static class RecursiveDotStep implements Step {
        private final String segment;
        private final Step nextStep;

        RecursiveDotStep(String segment) {
            this.segment = segment;
            this.nextStep = StepFactory.createDotStep(segment, false);
        }

        @Override
        public List<ONode> execute(List<ONode> currentNodes, QueryContext context) {
            List<ONode> allNodes = new ArrayList<>();
            for (ONode node : currentNodes) {
                collectRecursive(node, allNodes);
            }

            context.setFlattened(true);
            return nextStep.execute(allNodes, context);
        }

        private void collectRecursive(ONode node, List<ONode> results) {
            if (node.isArray()) {
                for (ONode child : node.getArray()) {
                    results.add(child);
                    collectRecursive(child, results);
                }
            } else if (node.isObject()) {
                for (ONode child : node.getObject().values()) {
                    results.add(child);
                    collectRecursive(child, results);
                }
            }
        }
    }

    private static class FilterStep implements Step {
        private final String filterStr;
        private final Expression expression;

        FilterStep(String filterStr) {
            this.filterStr = filterStr;
            this.expression = Expression.get(filterStr);
        }

        @Override
        public List<ONode> execute(List<ONode> currentNodes, QueryContext context) {
            if (context.isFlattened()) {
                return currentNodes.stream()
                        .filter(n -> expression.test(n, context.root))
                        .collect(Collectors.toList());
            } else {
                return currentNodes.stream()
                        .flatMap(this::flattenNode)
                        .filter(n -> expression.test(n, context.root))
                        .collect(Collectors.toList());
            }
        }

        private Stream<ONode> flattenNode(ONode node) {
            if (node.isArray()) {
                return node.getArray().stream()
                        .flatMap(this::flattenNode)
                        .filter(n -> !n.isNull());
            }
            return Stream.of(node);
        }
    }

    private static class SingleIndexStep implements Step {
        private final String indexStr;

        SingleIndexStep(String indexStr) {
            this.indexStr = indexStr;
        }

        @Override
        public List<ONode> execute(List<ONode> currentNodes, QueryContext context) {
            if (indexStr.startsWith("'")) {
                final String key = indexStr.substring(1, indexStr.length() - 1);
                return resolveKeyIndex(currentNodes, key, context);
            } else {
                final int index = Integer.parseInt(indexStr);
                return resolveArrayIndex(currentNodes, index, context);
            }
        }

        private List<ONode> resolveKeyIndex(List<ONode> nodes, String key, QueryContext context) {
            return nodes.stream()
                    .filter(o -> {
                        if (context.getMode() == QueryMode.CREATE) {
                            o.newObject();
                            return true;
                        } else {
                            return o.isObject();
                        }
                    })
                    .map(obj -> {
                        if (context.getMode() == QueryMode.CREATE) {
                            obj.getOrNew(key);
                        }

                        ONode rst = obj.get(key);
                        if (rst != null) {
                            rst.source = new JsonSource(obj, key, 0);
                        }
                        return rst;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }

        private List<ONode> resolveArrayIndex(List<ONode> nodes, int index, QueryContext context) {
            return nodes.stream()
                    .filter(o -> {
                        if (context.getMode() == QueryMode.CREATE) {
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

                        if (context.getMode() == QueryMode.CREATE) {
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

    private static class MultiIndexStep implements Step {
        private final String indicesStr;
        private final List<String> indices;

        MultiIndexStep(String indicesStr) {
            this.indicesStr = indicesStr;
            this.indices = Arrays.stream(indicesStr.split(","))
                    .map(String::trim)
                    .collect(Collectors.toList());
        }

        @Override
        public List<ONode> execute(List<ONode> currentNodes, QueryContext context) {
            return currentNodes.stream()
                    .flatMap(n -> indices.stream()
                            .flatMap(key -> resolveKey(n, key, context).stream()))
                    .collect(Collectors.toList());
        }

        private List<ONode> resolveKey(ONode n, String key, QueryContext context) {
            char ch = key.charAt(0);
            if (ch == '\'') {
                if (n.isObject()) {
                    ONode p = n.get(key.substring(1, key.length() - 1));
                    if (p != null) {
                        return Collections.singletonList(p);
                    }
                }
            } else if (ch == '*') {
                if (n.isArray()) {
                    return n.getArray();
                } else if (n.isObject()) {
                    return new ArrayList<>(n.getObject().values());
                }
            } else {
                if (n.isArray()) {
                    int idx = Integer.parseInt(key);
                    if (idx < 0) idx += n.size();
                    if (idx < 0 || idx >= n.size()) {
                        throw new PathResolutionException("Index out of bounds: " + idx);
                    }
                    ONode node = n.get(idx);
                    node.source = new JsonSource(n, null, idx);
                    return Collections.singletonList(node);
                }
            }
            return Collections.emptyList();
        }
    }

    private static class RangeStep implements Step {
        private final String rangeStr;
        private final int[] rangeParams;

        RangeStep(String rangeStr) {
            this.rangeStr = rangeStr;
            this.rangeParams = parseRange(rangeStr);
        }

        @Override
        public List<ONode> execute(List<ONode> currentNodes, QueryContext context) {
            return currentNodes.stream()
                    .filter(ONode::isArray)
                    .flatMap(arr -> {
                        int size = arr.size();
                        int start = rangeParams[0] < 0 ? size + rangeParams[0] : rangeParams[0];
                        int end = rangeParams[1] < 0 ? size + rangeParams[1] : rangeParams[1];
                        int step = rangeParams[2];

                        RangeUtil.Bounds bounds = RangeUtil.bounds(start, end, step, size);

                        List<ONode> result = new ArrayList<>();
                        if (step > 0) {
                            for (int i = bounds.lower; i < bounds.upper; i += step) {
                                ONode node = arr.get(i);
                                node.source = new JsonSource(arr, null, i);
                                result.add(node);
                            }
                        } else {
                            for (int i = bounds.upper; bounds.lower < i; i += step) {
                                ONode node = arr.get(i);
                                node.source = new JsonSource(arr, null, i);
                                result.add(node);
                            }
                        }
                        return result.stream();
                    })
                    .collect(Collectors.toList());
        }

        private int[] parseRange(String rangeStr) {
            String[] parts = rangeStr.split(":", 3);
            if (parts.length < 2) {
                throw new PathResolutionException("Invalid range syntax: " + rangeStr);
            }

            int start = parts[0].isEmpty() ? 0 : Integer.parseInt(parts[0]);
            int end = parts[1].isEmpty() ? Integer.MAX_VALUE : Integer.parseInt(parts[1]);
            int step = (parts.length == 3 && !parts[2].isEmpty()) ? Integer.parseInt(parts[2]) : 1;

            return new int[]{start, end, step};
        }
    }

    /**
     * 路径解析器（优化版）
     */
    private static class PathParser {
        private final String path;
        private int index;

        PathParser(String path) {
            this.path = path;
            this.index = 0;
        }

        List<Step> parseSteps() {
            List<Step> steps = new ArrayList<>();
            index++; // 跳过$

            while (index < path.length()) {
                skipWhitespace();
                if (index >= path.length()) break;

                char ch = path.charAt(index);
                if (ch == '.') {
                    steps.add(parseDotStep());
                } else if (ch == '[') {
                    steps.add(parseBracketStep());
                } else {
                    throw new PathResolutionException("Unexpected character '" + ch + "' at index " + index);
                }
            }

            return steps;
        }

        private Step parseDotStep() {
            index++; // 跳过.
            boolean recursive = false;

            if (index < path.length() && path.charAt(index) == '.') {
                index++;
                recursive = true;
            }

            String segment = parseSegment('.', '[');
            return StepFactory.createDotStep(segment, recursive);
        }

        private Step parseBracketStep() {
            index++; // 跳过[
            String segment = parseSegment(']');
            while (index < path.length() && path.charAt(index) == ']') {
                index++;
            }

            return StepFactory.createBracketStep(segment);
        }

        private String parseSegment(char... terminators) {
            StringBuilder sb = new StringBuilder();
            boolean inRegex = false;
            boolean inBracket = false;

            while (index < path.length()) {
                char ch = path.charAt(index);

                if (ch == '/' && !inRegex) {
                    inRegex = true;
                } else if (ch == '/' && inRegex) {
                    inRegex = false;
                }

                if (ch == ']' && inBracket) {
                    inBracket = false;
                }

                if (!inBracket && !inRegex && isTerminator(ch, terminators)) {
                    if (ch == ']') {
                        index++;
                    }
                    break;
                }

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

        private void skipWhitespace() {
            while (index < path.length() && Character.isWhitespace(path.charAt(index))) {
                index++;
            }
        }
    }

    private enum QueryMode {
        SELECT, CREATE, DELETE
    }
}
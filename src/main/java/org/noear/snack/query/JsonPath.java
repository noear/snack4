package org.noear.snack.query;

import org.noear.snack.ONode;
import org.noear.snack.core.JsonSource;
import org.noear.snack.exception.PathResolutionException;

import java.util.*;
import java.util.regex.Matcher;
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
                    currentNodes = handleDot(currentNodes);
                } else if (ch == '[') {
                    currentNodes = handleBracket(currentNodes);
                } else {
                    throw new PathResolutionException("Unexpected character '" + ch + "' at index " + index);
                }
            }

            if ((path.indexOf('?') < 0 && path.indexOf('*') < 0 && path.indexOf("..") < 0) || path.indexOf("()") > 0) {
                if (currentNodes.size() > 0) {
                    return currentNodes.get(0);
                } else {
                    return new ONode();
                }
            } else {
                return new ONode(currentNodes);
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
                    currentNodes = handleDot(currentNodes);
                } else if (ch == '[') {
                    currentNodes = handleBracket(currentNodes);
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

        // 创建节点
        void create(ONode root) {
            isCreateMode = true;
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
                currentNodes = resolveKey(currentNodes, false);
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
                return Collections.singletonList(
                        Functions.get(funcName).apply(nodes) // 传入节点列表
                );
            }
            return nodes.stream()
                    .map(n -> getChild(n, key, strict))
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());
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
                        List<ONode> childs;
                        if (n.isArray()) childs = n.getArray();
                        else if (n.isObject()) childs = new ArrayList<>(n.getObject().values());
                        else childs = Collections.<ONode>emptyList();

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
                            int count = Math.max(Math.abs(idx), 1) - arr.size();
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
                    .flatMap(n -> flattenNode(n)) // 使用递归展开多级数组
                    .filter(n -> {
                        try {
                            return evaluateFilter(n, filter);
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .collect(Collectors.toList());
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

        private boolean evaluateFilter(ONode node, String filter) {
            try {
                List<Token> tokens = tokenize(filter);
                List<Token> rpn = convertToRPN(tokens);
                return evaluateRPN(node, rpn);
            } catch (Exception e) {
                return false;
            }
        }

        // 新增分词器
        private List<Token> tokenize(String filter) {
            List<Token> tokens = new ArrayList<>();
            int index = 0;
            int len = filter.length();

            while (index < len) {
                char c = filter.charAt(index);
                if (Character.isWhitespace(c)) {
                    index++;
                    continue;
                }

                if (c == '(') {
                    tokens.add(new Token(TokenType.LPAREN, "("));
                    index++;
                } else if (c == ')') {
                    tokens.add(new Token(TokenType.RPAREN, ")"));
                    index++;
                } else if (c == '&' && index + 1 < len && filter.charAt(index + 1) == '&') {
                    tokens.add(new Token(TokenType.AND, "&&"));
                    index += 2;
                } else if (c == '|' && index + 1 < len && filter.charAt(index + 1) == '|') {
                    tokens.add(new Token(TokenType.OR, "||"));
                    index += 2;
                } else {
                    int start = index;
                    while (index < len) {
                        char curr = filter.charAt(index);
                        if (curr == '(' || curr == ')' || curr == '&' || curr == '|') {
                            break;
                        }
                        if ((curr == '&' || curr == '|') && index + 1 < len && filter.charAt(index + 1) == curr) {
                            break;
                        }
                        index++;
                    }
                    String atom = filter.substring(start, index).trim();
                    if (!atom.isEmpty()) {
                        tokens.add(new Token(TokenType.ATOM, atom));
                    }
                }
            }
            return tokens;
        }

        // 转换为逆波兰式
        private List<Token> convertToRPN(List<Token> tokens) {
            List<Token> output = new ArrayList<>();
            Deque<Token> stack = new ArrayDeque<>();

            for (Token token : tokens) {
                switch (token.type) {
                    case ATOM:
                        output.add(token);
                        break;
                    case LPAREN:
                        stack.push(token);
                        break;
                    case RPAREN:
                        while (!stack.isEmpty() && stack.peek().type != TokenType.LPAREN) {
                            output.add(stack.pop());
                        }
                        stack.pop();
                        break;
                    case AND:
                    case OR:
                        while (!stack.isEmpty() && stack.peek().type != TokenType.LPAREN &&
                                precedence(token) <= precedence(stack.peek())) {
                            output.add(stack.pop());
                        }
                        stack.push(token);
                        break;
                }
            }

            while (!stack.isEmpty()) {
                output.add(stack.pop());
            }
            return output;
        }

        private int precedence(Token token) {
            return token.type == TokenType.AND ? 2 : token.type == TokenType.OR ? 1 : 0;
        }

        // 评估逆波兰式
        private boolean evaluateRPN(ONode node, List<Token> rpn) {
            Deque<Boolean> stack = new ArrayDeque<>();
            for (Token token : rpn) {
                if (token.type == TokenType.ATOM) {
                    stack.push(evaluateSingleCondition(node, token.value));
                } else if (token.type == TokenType.AND || token.type == TokenType.OR) {
                    boolean b = stack.pop();
                    boolean a = stack.pop();
                    stack.push(token.type == TokenType.AND ? a && b : a || b);
                }
            }
            return stack.pop();
        }


        private boolean evaluateSingleCondition(ONode node, String condition) {
            if (condition.startsWith("!")) {
                //非运行
                return !evaluateSingleCondition(node, condition.substring(1));
            }

            Matcher matcher = Operations.CONDITION_PATTERN.matcher(condition);
            if (!matcher.matches()) return false;

            Factor factor = new Factor();
            factor.keyPath = matcher.group("key");
            factor.op = matcher.group("op");
            factor.right = matcher.group("right");


            return Operations.get(factor.op).apply(node, factor);
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


    private enum TokenType {ATOM, AND, OR, LPAREN, RPAREN}

    private static class Token {
        final TokenType type;
        final String value;

        Token(TokenType type, String value) {
            this.type = type;
            this.value = value;
        }
    }

    public static class Factor {
        public String keyPath;
        public String op;
        public String right;
    }
}
package org.noear.snack.query;

import org.noear.snack.ONode;
import org.noear.snack.core.JsonSource;
import org.noear.snack.core.util.TextUtil;
import org.noear.snack.exception.PathResolutionException;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

                // 处理 in 操作符
                if (c == 'i' && index + 2 < len && filter.substring(index, index + 3).equals("in ")) {
                    tokens.add(new Token(TokenType.IN, "in"));
                    index += 3;
                    continue;
                }

                // 处理数组字面量 [..]
                if (c == '[') {
                    int start = index;
                    int bracketCount = 1;
                    index++;
                    while (index < len && bracketCount > 0) {
                        c = filter.charAt(index);
                        if (c == '[') bracketCount++;
                        if (c == ']') bracketCount--;
                        index++;
                    }
                    tokens.add(new Token(TokenType.ARRAY, filter.substring(start, index)));
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

            if (condition.contains(" in ")) {
                String[] parts = condition.split("\\s+in\\s+", 2);
                if (parts.length != 2) return false;

                String keyPath = parts[0].replace("@.", "").trim();
                String arrayStr = parts[1].trim();

                if (!arrayStr.startsWith("[") || !arrayStr.endsWith("]")) {
                    return false;
                }

                ONode target = resolveNestedPath(node, keyPath);
                if (target == null) return false;

                List<String> expectedValues = parseArrayLiteral(arrayStr)
                        .stream()
                        .map(s -> s.replaceAll("^'|'$", ""))
                        .collect(Collectors.toList());

                return expectedValues.stream().anyMatch(v -> isValueMatch(target, v));
            }

            // 特殊处理包含操作符
            if (condition.contains(" contains ")) {
                return evaluateContains(node, condition);
            } else if (condition.contains(" in ")) {
                return evaluateIn(node, condition, false);
            } else if (condition.contains(" nin ")) {
                return evaluateIn(node, condition, true);
            } else {
                return evaluateComparison(node, condition);
            }
        }

        private List<String> parseArrayLiteral(String arrayStr) {
            List<String> items = new ArrayList<>();
            String content = arrayStr.substring(1, arrayStr.length()-1).trim();
            if (content.isEmpty()) return items;

            boolean inString = false;
            StringBuilder current = new StringBuilder();
            for (char c : content.toCharArray()) {
                if (c == '\'' && !inString) {
                    inString = true;
                } else if (c == '\'' && inString) {
                    inString = false;
                    items.add(current.toString());
                    current = new StringBuilder();
                } else if (c == ',' && !inString) {
                    if (current.length() > 0) {
                        items.add(current.toString().trim());
                        current = new StringBuilder();
                    }
                } else {
                    current.append(c);
                }
            }
            if (current.length() > 0) {
                items.add(current.toString().trim());
            }
            return items;
        }

        private boolean evaluateComparison(ONode node, String condition) {
            Matcher matcher = CONDITION_PATTERN.matcher(condition);
            if (!matcher.matches()) return false;

            String keyPath = matcher.group("key");
            String op = matcher.group("op");
            String regex = matcher.group("regex");
            String strValue = matcher.group("str");
            String numValue = matcher.group("num");

            // 处理存在性检查（如 @.price）
            if (TextUtil.isEmpty(op) && strValue == null && numValue == null) {
                return resolveNestedPath(node, keyPath) != null;
            }

            // 获取目标节点
            ONode target = resolveNestedPath(node, keyPath);
            if (target == null) return false;

            if ("=~".equals(op) && regex != null) {
                if (!target.isString()) return false;
                return target.getString().matches(regex.replace("\\/", "/"));
            }

            // 处理转义字符
            String value = strValue != null ?
                    strValue.replaceAll("\\\\(.)", "$1") :
                    numValue;

            // 类型判断逻辑
            if (numValue != null) {
                if (!target.isNumber()) return false;
                return compareNumber(op, target.getDouble(), Double.parseDouble(numValue));
            } else {
                if (!target.isString()) return false;
                return compareString(op, target.getString(), value);
            }
        }

        // 新增 contains 操作符处理
        private boolean evaluateContains(ONode node, String condition) {
            String[] parts = condition.split("\\s+contains\\s+", 2);
            if (parts.length != 2) return false;

            String keyPath = parts[0].replace("@.", "").trim();
            String expectedRaw = parts[1].replaceAll("^'|'$", "");
            String expectedValue = expectedRaw.replace("\\'", "'");

            ONode target = resolveNestedPath(node, keyPath);
            if (target == null) return false;

            // 支持多类型包含检查
            if (target.isArray()) {
                return target.getArray().stream()
                        .anyMatch(item -> isValueMatch(item, expectedValue));
            } else if (target.isString()) {
                return target.getString().contains(expectedValue);
            }
            return false;
        }

        private boolean isValueMatch(ONode item, String expected) {
            if (item.isString()) {
                return item.getString().equals(expected);
            } else if (item.isNumber()) {
                try {
                    return item.getDouble() == Double.parseDouble(expected);
                } catch (NumberFormatException e) {
                    return false;
                }
            } else if (item.isBoolean()) {
                return item.getBoolean() == Boolean.parseBoolean(expected);
            }
            return false;
        }

        // 新增 in/nin 操作符处理
        private boolean evaluateIn(ONode node, String condition, boolean negate) {
            String[] parts = condition.split("\\s+(in|nin)\\s+", 2);
            if (parts.length != 2) return false;

            String keyPath = parts[0].replace("@.", "").trim();
            String valuesStr = parts[1].replaceAll("^'|'$", "");
            List<String> expectedValues = Arrays.stream(valuesStr.split(","))
                    .map(String::trim)
                    .collect(Collectors.toList());

            ONode target = resolveNestedPath(node, keyPath);
            if (target == null) return false;

            // 自动类型转换比较
            return negate ^ expectedValues.stream()
                    .anyMatch(v -> isValueMatch(target, v));
        }

        // 正则表达式更新（支持更复杂的键路径和转义字符）
        private static final Pattern CONDITION_PATTERN = Pattern.compile(
                "^@?\\.?" +
                        "(?<key>[\\w\\.]+)" +
                        "\\s*" +
                        "(?<op>==|=~|!=|>=|<=|>|<|contains|in|nin|\\b)" +
                        "\\s*" +
                        "(?:/(?<regex>.*?)/|'((?<str>.*?)(?<!\\\\))'|(?<num>[+-]?\\d+\\.?\\d*))?" +
                        "$", Pattern.CASE_INSENSITIVE
        );

        private ONode resolveNestedPath(ONode node, String keyPath) {
            String[] keys = keyPath.split("\\.");
            ONode current = node;
            for (String key : keys) {
                if (current.isObject()) {
                    current = current.get(key);
                } else if (current.isArray()) {
                    try {
                        int index = Integer.parseInt(key);
                        current = current.get(index);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                } else {
                    return null;
                }
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
            boolean inRegex = false; // 标记是否正在解析正则表达式

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

                // 如果在正则表达式内部，忽略终止符检查
                if (!inRegex && isTerminator(ch, terminators)) {
                    if (ch == ']') {
                        index++; // 跳过闭合的 ]
                    }
                    break;
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


    private enum TokenType {ATOM, AND, OR, IN, ARRAY, LPAREN, RPAREN}

    private static class Token {
        final TokenType type;
        final String value;

        Token(TokenType type, String value) {
            this.type = type;
            this.value = value;
        }
    }
}
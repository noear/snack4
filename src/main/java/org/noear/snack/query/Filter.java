package org.noear.snack.query;

import org.noear.snack.ONode;
import org.noear.snack.core.util.TextUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * @author noear 2025/5/5 created
 */
public class Filter implements Predicate<ONode> {
    private static Map<String, Filter> filters = new ConcurrentHashMap<>();

    public static Filter get(String filterStr) {
        return filters.computeIfAbsent(filterStr, k -> new Filter(filterStr));
    }

    /// ///////////////////

    private final List<Token> rpn;

    public Filter(String filterStr) {
        List<Token> tokens = tokenize(filterStr);
        this.rpn = convertToRPN(tokens);
    }

    // 评估逆波兰式
    @Override
    public boolean test(ONode node) {
        try {
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
        } catch (Throwable ex) {
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


    private boolean evaluateSingleCondition(ONode node, String conditionStr) {
        if (conditionStr.startsWith("!")) {
            //非运行
            return !evaluateSingleCondition(node, conditionStr.substring(1));
        }

        Condition condition = Condition.parse(conditionStr);

        // 过滤空条件（操作符处理时，就不需要再过滤了）
        if (TextUtil.isEmpty(condition.getLeft())) {
            return false;
        }

        // 单元操作（如 @.price）
        if (condition.getRight() == null) {
            if (condition.getOp() == null) {
                return condition.getLeftNode(node) != null;
            } else {
                return false;
            }
        }

        return Operations.get(condition.getOp()).apply(node, condition);
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
}
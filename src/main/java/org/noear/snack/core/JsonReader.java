package org.noear.snack.core;

import org.noear.snack.ONode;
import org.noear.snack.exception.ParseException;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class JsonReader {

    private final Options opts;
    private final ParserState state;

    public JsonReader(Reader reader) {
        this(reader, Options.def());
    }

    public JsonReader(Reader reader, Options opts) {
        this.state = new ParserState(reader);
        this.opts = opts != null ? opts : Options.def();
    }

    // 新增带 Options 的快捷方法
    public static ONode read(String json) throws IOException {
        return read(json, Options.def());
    }

    public static ONode read(String json, Options opts) throws IOException {
        return new JsonReader(new StringReader(json), opts).read();
    }

    public ONode read() throws IOException {
        state.fillBuffer();
        ONode node = parseValue();
        state.skipWhitespace();
        if (state.bufferPosition < state.bufferLimit) throw state.error("Unexpected data after json root");
        return node;
    }

    private ONode parseValue() throws IOException {
        state.skipWhitespace();
        char c = state.nextChar();
        state.bufferPosition--; // 回退进行类型判断

        if (opts.isFeatureEnabled(Feature.Input_AllowComment)) {
            skipComments();
        }

        if (c == '{') return parseObject();
        if (c == '[') return parseArray();
        if (c == '"') return new ONode(parseString());
        if (c == '-' || (c >= '0' && c <= '9')) return new ONode(parseNumber());
        if (c == 't') return parseKeyword("true", true);
        if (c == 'f') return parseKeyword("false", false);
        if (c == 'n') return parseKeyword("null", null);
        throw state.error("Unexpected character: " + c);
    }

    private void skipComments() throws IOException {
        char c = state.peekChar();
        if (c == '/') {
            state.bufferPosition++;
            char next = state.peekChar();
            if (next == '/') {
                skipLineComment();
            } else if (next == '*') {
                skipBlockComment();
            }
        }
    }

    private void skipLineComment() throws IOException {
        while (state.bufferPosition < state.bufferLimit && state.buffer[state.bufferPosition] != '\n') {
            state.bufferPosition++;
        }
    }

    private void skipBlockComment() throws IOException {
        state.bufferPosition++;
        while (true) {
            if (state.bufferPosition >= state.bufferLimit && !state.fillBuffer()) break;
            char c = state.buffer[state.bufferPosition++];
            if (c == '*' && state.peekChar() == '/') {
                state.bufferPosition++;
                break;
            }
        }
    }

    private ONode parseObject() throws IOException {
        Map<String, ONode> map = new LinkedHashMap<>();
        expect('{');
        while (true) {
            state.skipWhitespace();
            if (state.peekChar() == '}') {
                state.bufferPosition++;
                break;
            }

            // 修复：允许空键
            String key = parseString();
            if (key.isEmpty()) throw state.error("Empty key in object");

            state.skipWhitespace();
            expect(':');
            ONode value = parseValue();
            map.put(key, value);

            state.skipWhitespace();
            if (state.peekChar() == ',') {
                state.bufferPosition++;
                state.skipWhitespace();
                if (state.peekChar() == '}') throw state.error("Trailing comma in object");
            } else if (state.peekChar() == '}') {
                // Continue to closing
            } else {
                throw state.error("Expected ',' or '}'");
            }
        }
        return new ONode(map);
    }

    private ONode parseArray() throws IOException {
        ArrayList<ONode> list = new ArrayList<>();
        expect('[');
        while (true) {
            state.skipWhitespace();
            if (state.peekChar() == ']') {
                state.bufferPosition++;
                break;
            }

            list.add(parseValue());

            state.skipWhitespace();
            if (state.peekChar() == ',') {
                state.bufferPosition++;
                state.skipWhitespace();
                if (state.peekChar() == ']') throw state.error("Trailing comma in array");
            } else if (state.peekChar() == ']') {
                // Continue to closing
            } else {
                throw state.error("Expected ',' or ']'");
            }
        }
        return new ONode(list);
    }

    private String parseString() throws IOException {
        expect('"');
        StringBuilder sb = new StringBuilder();
        while (true) {
            char c = state.nextChar();
            if (c == '"') break;

            if (c == '\\') {
                c = state.nextChar();
                switch (c) {
                    case '"':
                        sb.append('"');
                        break;
                    case '\\':
                        sb.append('\\');
                        break;
                    case '/':
                        sb.append('/');
                        break;
                    case 'b':
                        sb.append('\b');
                        break;
                    case 'f':
                        sb.append('\f');
                        break;
                    case 'n':
                        sb.append('\n');
                        break;
                    case 'r':
                        sb.append('\r');
                        break;
                    case 't':
                        sb.append('\t');
                        break;
                    case 'u':
                        char[] hex = new char[4];
                        for (int i = 0; i < 4; i++) {
                            hex[i] = state.nextChar();
                            if (!isHex(hex[i])) throw state.error("Invalid Unicode escape");
                        }
                        sb.append((char) Integer.parseInt(new String(hex), 16));
                        break;
                    default:
                        throw state.error("Invalid escape character: \\" + c);
                }
            } else {
                if (c < 0x20) throw state.error("Unescaped control character: 0x" + Integer.toHexString(c));
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private Number parseNumber() throws IOException {
        StringBuilder sb = new StringBuilder();
        char c = state.peekChar();

        // 处理负数
        if (c == '-') {
            sb.append(c);
            state.bufferPosition++;
        }

        // 解析整数部分
        if (state.peekChar() == '0') {
            sb.append(state.nextChar());
            if (isDigit(state.peekChar())) {
                throw state.error("Leading zeros not allowed");
            }
        } else if (isDigit(state.peekChar())) {
            while (isDigit(state.peekChar())) {
                sb.append(state.nextChar());
            }
        } else {
            throw state.error("Invalid number format");
        }

        // 解析小数部分
        if (state.peekChar() == '.') {
            sb.append(state.nextChar());
            if (!isDigit(state.peekChar())) {
                throw state.error("Invalid decimal format");
            }
            while (isDigit(state.peekChar())) {
                sb.append(state.nextChar());
            }
        }

        // 解析指数部分
        if (state.peekChar() == 'e' || state.peekChar() == 'E') {
            sb.append(state.nextChar());
            if (state.peekChar() == '+' || state.peekChar() == '-') {
                sb.append(state.nextChar());
            }
            if (!isDigit(state.peekChar())) {
                throw state.error("Invalid exponent format");
            }
            while (isDigit(state.peekChar())) {
                sb.append(state.nextChar());
            }
        }

        String numStr = sb.toString();
        try {
            if (numStr.contains(".") || numStr.contains("e") || numStr.contains("E")) {
                return Double.parseDouble(numStr);
            } else {
                long longVal = Long.parseLong(numStr);
                if (longVal <= Integer.MAX_VALUE && longVal >= Integer.MIN_VALUE) {
                    return (int) longVal;
                }
                return longVal;
            }
        } catch (NumberFormatException e) {
            throw state.error("Invalid number: " + numStr);
        }
    }

    private ONode parseKeyword(String expect, Object value) throws IOException {
        for (int i = 0; i < expect.length(); i++) {
            char c = state.nextChar();
            if (c != expect.charAt(i)) {
                throw state.error("Unexpected keyword: expected '" + expect + "'");
            }
        }
        return new ONode(value);
    }

    private void expect(char expected) throws IOException {
        char c = state.nextChar();
        if (c != expected) {
            throw state.error("Expected '" + expected + "' but found '" + c + "'");
        }
    }


    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isHex(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }


    static class ParserState {
        private static final int BUFFER_SIZE = 8192;
        private final Reader reader;
        private long line = 1;
        private long column = 0;

        private final char[] buffer = new char[BUFFER_SIZE];
        private int bufferPosition;
        private int bufferLimit;

        public ParserState(Reader reader) {
            this.reader = reader;
        }


        private void skipWhitespace() throws IOException {
            while (bufferPosition < bufferLimit || fillBuffer()) {
                char c = buffer[bufferPosition];
                if (c == '\n') {
                    line++;
                    column = 0;
                } else if (c == '\r') {
                    // Handle CRLF
                    if (peekChar(1) == '\n') bufferPosition++;
                    line++;
                    column = 0;
                } else if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                    // Continue
                } else {
                    break;
                }
                bufferPosition++;
                column++;
            }
        }

        private char nextChar() throws IOException {
            if (bufferPosition >= bufferLimit && !fillBuffer()) {
                throw error("Unexpected end of input");
            }
            char c = buffer[bufferPosition++];
            column++;
            return c;
        }

        private char peekChar() throws IOException {
            return peekChar(0);
        }

        private char peekChar(int offset) throws IOException {
            if (bufferPosition + offset >= bufferLimit && !fillBuffer()) {
                return 0;
            }
            return (bufferPosition + offset < bufferLimit) ? buffer[bufferPosition + offset] : 0;
        }

        private boolean fillBuffer() throws IOException {
            if (bufferPosition < bufferLimit) return true;
            bufferLimit = reader.read(buffer);
            bufferPosition = 0;
            return bufferLimit > 0;
        }

        private ParseException error(String message) {
            return new ParseException(message + " at line " + line + " column " + column);
        }
    }
}
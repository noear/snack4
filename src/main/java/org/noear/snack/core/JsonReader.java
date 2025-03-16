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
    private static final int BUFFER_SIZE = 8192;
    private final Reader reader;
    private long line = 1;
    private long column = 0;

    private final char[] buffer = new char[BUFFER_SIZE];
    private int bufferPosition;
    private int bufferLimit;

    private final Options opts;

    public JsonReader(Reader reader) {
        this(reader, Options.def());
    }

    public JsonReader(Reader reader, Options opts) {
        this.reader = reader;
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
        fillBuffer();
        ONode node = parseValue();
        skipWhitespace();
        if (bufferPosition < bufferLimit) throw error("Unexpected data after json root");
        return node;
    }

    private ONode parseValue() throws IOException {
        skipWhitespace();
        char c = nextChar();
        bufferPosition--; // 回退进行类型判断

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
        throw error("Unexpected character: " + c);
    }

    private void skipComments() throws IOException {
        char c = peekChar();
        if (c == '/') {
            bufferPosition++;
            char next = peekChar();
            if (next == '/') {
                skipLineComment();
            } else if (next == '*') {
                skipBlockComment();
            }
        }
    }

    private void skipLineComment() throws IOException {
        while (bufferPosition < bufferLimit && buffer[bufferPosition] != '\n') {
            bufferPosition++;
        }
    }

    private void skipBlockComment() throws IOException {
        bufferPosition++;
        while (true) {
            if (bufferPosition >= bufferLimit && !fillBuffer()) break;
            char c = buffer[bufferPosition++];
            if (c == '*' && peekChar() == '/') {
                bufferPosition++;
                break;
            }
        }
    }

    private ONode parseObject() throws IOException {
        Map<String, ONode> map = new LinkedHashMap<>();
        expect('{');
        while (true) {
            skipWhitespace();
            if (peekChar() == '}') {
                bufferPosition++;
                break;
            }

            // 修复：允许空键
            String key = parseString();
            if (key.isEmpty()) throw error("Empty key in object");

            skipWhitespace();
            expect(':');
            ONode value = parseValue();
            map.put(key, value);

            skipWhitespace();
            if (peekChar() == ',') {
                bufferPosition++;
                skipWhitespace();
                if (peekChar() == '}') throw error("Trailing comma in object");
            } else if (peekChar() == '}') {
                // Continue to closing
            } else {
                throw error("Expected ',' or '}'");
            }
        }
        return new ONode(map);
    }

    private ONode parseArray() throws IOException {
        ArrayList<ONode> list = new ArrayList<>();
        expect('[');
        while (true) {
            skipWhitespace();
            if (peekChar() == ']') {
                bufferPosition++;
                break;
            }

            list.add(parseValue());

            skipWhitespace();
            if (peekChar() == ',') {
                bufferPosition++;
                skipWhitespace();
                if (peekChar() == ']') throw error("Trailing comma in array");
            } else if (peekChar() == ']') {
                // Continue to closing
            } else {
                throw error("Expected ',' or ']'");
            }
        }
        return new ONode(list);
    }

    private String parseString() throws IOException {
        expect('"');
        StringBuilder sb = new StringBuilder();
        while (true) {
            char c = nextChar();
            if (c == '"') break;

            if (c == '\\') {
                c = nextChar();
                switch (c) {
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/': sb.append('/'); break;
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    case 'u':
                        char[] hex = new char[4];
                        for (int i = 0; i < 4; i++) {
                            hex[i] = nextChar();
                            if (!isHex(hex[i])) throw error("Invalid Unicode escape");
                        }
                        sb.append((char) Integer.parseInt(new String(hex), 16));
                        break;
                    default:
                        throw error("Invalid escape character: \\" + c);
                }
            } else {
                if (c < 0x20) throw error("Unescaped control character: 0x" + Integer.toHexString(c));
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private Number parseNumber() throws IOException {
        StringBuilder sb = new StringBuilder();
        char c = peekChar();

        // 处理负数
        if (c == '-') {
            sb.append(c);
            bufferPosition++;
        }

        // 解析整数部分
        if (peekChar() == '0') {
            sb.append(nextChar());
            if (isDigit(peekChar())) {
                throw error("Leading zeros not allowed");
            }
        } else if (isDigit(peekChar())) {
            while (isDigit(peekChar())) {
                sb.append(nextChar());
            }
        } else {
            throw error("Invalid number format");
        }

        // 解析小数部分
        if (peekChar() == '.') {
            sb.append(nextChar());
            if (!isDigit(peekChar())) {
                throw error("Invalid decimal format");
            }
            while (isDigit(peekChar())) {
                sb.append(nextChar());
            }
        }

        // 解析指数部分
        if (peekChar() == 'e' || peekChar() == 'E') {
            sb.append(nextChar());
            if (peekChar() == '+' || peekChar() == '-') {
                sb.append(nextChar());
            }
            if (!isDigit(peekChar())) {
                throw error("Invalid exponent format");
            }
            while (isDigit(peekChar())) {
                sb.append(nextChar());
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
            throw error("Invalid number: " + numStr);
        }
    }

    private ONode parseKeyword(String expect, Object value) throws IOException {
        for (int i = 0; i < expect.length(); i++) {
            char c = nextChar();
            if (c != expect.charAt(i)) {
                throw error("Unexpected keyword: expected '" + expect + "'");
            }
        }
        return new ONode(value);
    }

    private void expect(char expected) throws IOException {
        char c = nextChar();
        if (c != expected) {
            throw error("Expected '" + expected + "' but found '" + c + "'");
        }
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

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isHex(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    private ParseException error(String message) {
        return new ParseException(message + " at line " + line + " column " + column);
    }
}
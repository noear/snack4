package org.noear.snack;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

/**
 * 高性能 JSON 序列化器
 */
public class JsonSerializer {
    public static String toJsonString(ONode node) {
        try (StringWriter writer = new StringWriter()) {
            serialize(node, writer);
            return writer.toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize ONode to JSON", e);
        }
    }

    public static void serialize(ONode node, Writer writer) throws IOException {
        switch (node.getType()) {
            case ONode.TYPE_OBJECT:
                serializeObject(node, writer);
                break;
            case ONode.TYPE_ARRAY:
                serializeArray(node, writer);
                break;
            case ONode.TYPE_STRING:
                writer.write('"' + escapeString(node.getString()) + '"');
                break;
            case ONode.TYPE_NUMBER:
                writer.write(node.getNumber().toString());
                break;
            case ONode.TYPE_BOOLEAN:
                writer.write(node.getBoolean() ? "true" : "false");
                break;
            case ONode.TYPE_NULL:
                writer.write("null");
                break;
        }
    }

    private static void serializeObject(ONode node, Writer writer) throws IOException {
        writer.write('{');
        boolean first = true;
        for (Map.Entry<String, ONode> entry : node.getObject().entrySet()) {
            if (!first) writer.write(',');
            writer.write('"' + escapeString(entry.getKey()) + "\":");
            serialize(entry.getValue(), writer);
            first = false;
        }
        writer.write('}');
    }

    private static void serializeArray(ONode node, Writer writer) throws IOException {
        writer.write('[');
        boolean first = true;
        for (ONode item : node.getArray()) {
            if (!first) writer.write(',');
            serialize(item, writer);
            first = false;
        }
        writer.write(']');
    }

    private static String escapeString(String s) {
        StringBuilder sb = new StringBuilder(s.length() * 2);
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }
}
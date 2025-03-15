package org.noear.snack.codec;

import org.noear.snack.ONode;
import org.noear.snack.core.Feature;
import org.noear.snack.core.Options;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;

public class JsonSerializer {
    public static String serialize(ONode node, Options opts) {
        return new SerializerContext(opts).serialize(node);
    }

    private static class SerializerContext {
        final Options opts;
        final Writer writer;
        int depth = 0;

        SerializerContext(Options opts) {
            this.opts = opts != null ? opts : Options.def();
            this.writer = new java.io.StringWriter();
        }

        String serialize(ONode node) {
            try {
                writeValue(node);
                return writer.toString();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private void writeValue(ONode node) throws IOException {
            switch (node.getType()) {
                case ONode.TYPE_OBJECT:
                    writeObject(node.getObject());
                    break;
                case ONode.TYPE_ARRAY:
                    writeArray(node.getArray());
                    break;
                case ONode.TYPE_STRING:
                    writeString(node.getString());
                    break;
                case ONode.TYPE_NUMBER:
                    writeNumber(node.getNumber());
                    break;
                case ONode.TYPE_BOOLEAN:
                    writer.write(node.getBoolean() ? "true" : "false");
                    break;
                case ONode.TYPE_NULL:
                    writer.write("null");
                    break;
            }
        }

        private void writeObject(Map<String, ONode> map) throws IOException {
            writer.write('{');
            boolean first = true;
            for (Map.Entry<String, ONode> entry : map.entrySet()) {
                if (opts.hasFeature(Feature.Output_SkipNullValue) && entry.getValue().isNull()) {
                    continue;
                }

                if (!first) writer.write(',');
                newLine();

                String key = opts.hasFeature(Feature.Output_UseUnderlineStyle) ?
                        toUnderlineName(entry.getKey()) : entry.getKey();
                writeString(key);
                writer.write(':');
                writeValue(entry.getValue());
                first = false;
            }
            newLine();
            writer.write('}');
        }

        private void writeArray(List<ONode> list) throws IOException {
            writer.write('[');
            boolean first = true;
            for (ONode item : list) {
                if (!first) writer.write(',');
                newLine();
                writeValue(item);
                first = false;
            }
            newLine();
            writer.write(']');
        }

        private void newLine() throws IOException {
            if (opts.hasFeature(Feature.Output_PrettyFormat)) {
                writer.write('\n');
                for (int i = 0; i < depth; i++) {
                    writer.write(opts.getIndent());
                }
            }
        }

        private void writeNumber(Number num) throws IOException {
            if (opts.hasFeature(Feature.UseBigNumberMode) && num instanceof Double) {
                writer.write('"' + num.toString() + '"');
            } else {
                writer.write(num.toString());
            }
        }

        private void writeString(String s) throws IOException {
            writer.write('"');
            writer.write(escapeString(s, opts));
            writer.write('"');
        }

        private static String escapeString(String s, Options opts) {
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

        private String toUnderlineName(String camelName) {
            // 实现驼峰转下划线逻辑
            return camelName;
        }
    }
}
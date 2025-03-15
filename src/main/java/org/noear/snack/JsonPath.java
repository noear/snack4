package org.noear.snack;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JsonPath {
    private static final Pattern PATH_PATTERN = Pattern.compile(
            "\\$|(?<=\\.|\\[)([^\\]\\[.]+)(?=\\.|\\[|$)|\\[\\s*'?([^'\\]]+)'?\\s*\\]"
    );

    public static ONode query(ONode node, String path) {
        if (node == null) throw new PathResolutionException("Null input node");
        if (!path.startsWith("$")) throw new PathResolutionException("Path must start with $");

        ONode current = node;
        Matcher matcher = PATH_PATTERN.matcher(path);
        matcher.find(); // Skip root $

        while (matcher.find()) {
            String keySegment = matcher.group(1);
            String indexSegment = matcher.group(2);

            if (indexSegment != null) {
                current = resolveIndex(current, indexSegment);
            } else if (keySegment != null) {
                current = resolveKey(current, keySegment);
            }
        }
        return current;
    }

    private static ONode resolveIndex(ONode node, String index) {
        if (!node.isArray()) throw new PathResolutionException("Not an array at index access");
        try {
            int idx = Integer.parseInt(index);
            List<ONode> arr = node.getArray();
            if (idx < 0 || idx >= arr.size()) {
                throw new PathResolutionException("Index out of bounds: " + idx);
            }
            return arr.get(idx);
        } catch (NumberFormatException e) {
            throw new PathResolutionException("Invalid array index: " + index);
        }
    }

    private static ONode resolveKey(ONode node, String key) {
        if (!node.isObject()) throw new PathResolutionException("Not an object at key access");
        Map<String, ONode> obj = node.getObject();
        if (!obj.containsKey(key)) {
            throw new PathResolutionException("Missing key: " + key);
        }
        return obj.get(key);
    }

    public static class PathResolutionException extends RuntimeException {
        public PathResolutionException(String message) { super(message); }
    }
}
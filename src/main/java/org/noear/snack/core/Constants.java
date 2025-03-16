package org.noear.snack.core;

/**
 * @author noear 2025/3/16 created
 */
public interface Constants {
    int TYPE_NULL = 0;
    int TYPE_BOOLEAN = 1;
    int TYPE_NUMBER = 2;
    int TYPE_STRING = 3;
    int TYPE_ARRAY = 4;
    int TYPE_OBJECT = 5;

    static String typeToString(int type) {
        switch (type) {
            case Constants.TYPE_NULL:
                return "null";
            case Constants.TYPE_BOOLEAN:
                return "boolean";
            case Constants.TYPE_NUMBER:
                return "number";
            case Constants.TYPE_STRING:
                return "string";
            case Constants.TYPE_ARRAY:
                return "array";
            case Constants.TYPE_OBJECT:
                return "object";
            default:
                return "unknown";
        }
    }
}
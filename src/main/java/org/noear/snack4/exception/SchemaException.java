package org.noear.snack4.exception;

/**
 * 模式验证异常
 */
public class SchemaException extends RuntimeException {
    public SchemaException(String message) {
        super(message);
    }

    public SchemaException(String message, Throwable cause) {
        super(message, cause);
    }
}

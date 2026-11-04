package com.buildsync.common.exception;

/**
 * Exception thrown when uploaded file fails validation rules (size, type, dimensions, content integrity).
 */
public class FileValidationException extends RuntimeException {

    public FileValidationException(String message) {
        super(message);
    }

    public FileValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}


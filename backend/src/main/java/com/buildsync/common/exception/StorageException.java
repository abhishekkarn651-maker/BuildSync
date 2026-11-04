package com.buildsync.common.exception;

/**
 * Exception thrown when file storage operations (read, write, delete) encounter I/O errors.
 */
public class StorageException extends RuntimeException {

    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}


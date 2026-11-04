package com.buildsync.common.exception;

/**
 * Thrown when attempting to register or create an entity that conflicts with existing unique constraints.
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}


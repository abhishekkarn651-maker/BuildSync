package com.buildsync.common.exception;

/**
 * Thrown when an expected entity (user, role, etc.) cannot be found.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}


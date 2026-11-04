package com.buildsync.common.exception;

/**
 * Thrown when refresh token validation, expiration, or reuse detection fails.
 */
public class TokenRefreshException extends RuntimeException {

    public TokenRefreshException(String message) {
        super(message);
    }
}


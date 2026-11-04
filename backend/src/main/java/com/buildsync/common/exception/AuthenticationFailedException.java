package com.buildsync.common.exception;

/**
 * Thrown on invalid credentials or when an account is disabled/unauthorized.
 */
public class AuthenticationFailedException extends RuntimeException {

    public AuthenticationFailedException(String message) {
        super(message);
    }
}


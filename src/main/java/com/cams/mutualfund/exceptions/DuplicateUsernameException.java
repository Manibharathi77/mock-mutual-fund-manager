package com.cams.mutualfund.exceptions;

/**
 * This is just an idea of custom exception for different use case
 * for better readability and clarity.
 */
public class DuplicateUsernameException extends RuntimeException {

    public DuplicateUsernameException(String username) {
        super("Username '" + username + "' already exists");
    }
}

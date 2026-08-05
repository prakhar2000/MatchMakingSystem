package com.matchmaking.exceptions;

public class DuplicatePlayerException extends RuntimeException {

    public DuplicatePlayerException(String message) {
        super(message);
    }

    public DuplicatePlayerException(String message, Throwable cause) {
        super(message, cause);
    }
}

package com.matchmaking.exceptions;

public class PlayerAlreadyInQueueException extends RuntimeException {

    public PlayerAlreadyInQueueException(String message) {
        super(message);
    }

    public PlayerAlreadyInQueueException(String message, Throwable cause) {
        super(message, cause);
    }
}

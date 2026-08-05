package com.matchmaking.exceptions;

public class PlayerNotInQueueException extends RuntimeException {

    public PlayerNotInQueueException(String message) {
        super(message);
    }

    public PlayerNotInQueueException(String message, Throwable cause) {
        super(message, cause);
    }
}

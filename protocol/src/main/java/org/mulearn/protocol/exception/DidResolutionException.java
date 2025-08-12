package org.mulearn.protocol.exception;

public class DidResolutionException extends RuntimeException {

    public DidResolutionException(String message) {
        super(message);
    }

    public DidResolutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
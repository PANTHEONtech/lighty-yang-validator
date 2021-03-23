package io.lighty.yang.validator.exceptions;

public class LyvApplicationException extends Exception {

    public LyvApplicationException(String message) {
        super(message);
    }

    public LyvApplicationException(String message, Throwable cause) {
        super(message, cause);
    }
}

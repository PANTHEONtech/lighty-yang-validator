package io.lighty.yang.validator.exceptions;

public class LyvApplicationException extends Exception {

    public LyvApplicationException(final String message) {
        super(message);
    }

    public LyvApplicationException(final String message, final Throwable cause) {
        super(message, cause);
    }
}

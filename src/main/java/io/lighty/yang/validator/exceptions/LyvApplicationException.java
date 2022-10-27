package io.lighty.yang.validator.exceptions;

public class LyvApplicationException extends Exception {
    @java.io.Serial
    private static final long serialVersionUID = 2775131351016822902L;

    public LyvApplicationException(final String message) {
        super(message);
    }

    public LyvApplicationException(final String message, final Throwable cause) {
        super(message, cause);
    }
}

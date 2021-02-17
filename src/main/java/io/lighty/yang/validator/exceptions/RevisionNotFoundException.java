package io.lighty.yang.validator.exceptions;

public class RevisionNotFoundException extends RuntimeException {
    public RevisionNotFoundException(String errorMessage) {
        super(errorMessage);
    }
}

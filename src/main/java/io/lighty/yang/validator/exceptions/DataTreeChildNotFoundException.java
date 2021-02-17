package io.lighty.yang.validator.exceptions;

public class DataTreeChildNotFoundException extends RuntimeException {
    public DataTreeChildNotFoundException(String errorMessage) {
        super(errorMessage);
    }
}

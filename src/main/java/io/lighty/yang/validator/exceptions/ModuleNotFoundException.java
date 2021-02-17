package io.lighty.yang.validator.exceptions;

public class ModuleNotFoundException extends RuntimeException {
    public ModuleNotFoundException(String errorMessage) {
        super(errorMessage);
    }
}

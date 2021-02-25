package io.lighty.yang.validator.exceptions;

/**
 * Indicating that expected element was not found by lyv, for example when expected
 * revision or DataSchemaNode was not found
 */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String customMessage, String name) {
        super(customMessage + ", " + name + " not found.");
    }
}

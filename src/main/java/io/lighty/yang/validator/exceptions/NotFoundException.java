package io.lighty.yang.validator.exceptions;

/**
 * Indicating that expected element was not found by lyv. For example when expected revision or DataSchemaNode was not
 * found.
 */
public class NotFoundException extends RuntimeException {
    @java.io.Serial
    private static final long serialVersionUID = -850705943805156370L;

    public NotFoundException(final String customMessage, final String name) {
        super(customMessage + ", " + name + " not found.");
    }
}

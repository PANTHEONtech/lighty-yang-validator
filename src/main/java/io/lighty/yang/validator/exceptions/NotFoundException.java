package io.lighty.yang.validator.exceptions;

/**
 * This exception is thrown when value of Optional<> variable is not present.
 * This excpetion is for handlig 3 types of cases when module is not found,
 * revision of module is not found or when some node is missing.
 * Takes two parameters. First is customMessage. It should describe what is not found(module, revision or node)
 * Second parameter is name of this what is not found.
 */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String customMessage, String name) {
        super(customMessage + name + " not found.");
    }
}

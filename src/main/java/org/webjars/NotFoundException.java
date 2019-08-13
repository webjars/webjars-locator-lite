package org.webjars;

/**
 * Thrown when a path matches multiple assets.
 */
public class NotFoundException extends IllegalArgumentException {

    public NotFoundException(final String message) {
        super(message);
    }

}

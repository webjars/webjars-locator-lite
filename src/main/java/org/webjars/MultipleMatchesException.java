package org.webjars;

/**
 * Thrown when a path matches multiple assets.
 */
public class MultipleMatchesException extends IllegalArgumentException {

    public MultipleMatchesException(final String message) {
        super(message);
    }

}
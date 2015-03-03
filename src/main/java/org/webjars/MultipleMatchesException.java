package org.webjars;

import java.util.List;

/**
 * Thrown when a path matches multiple assets.
 */
public class MultipleMatchesException extends IllegalArgumentException {

    private final List<String> matches;

    public MultipleMatchesException(final String message, List<String> matches) {
        super(message);
        this.matches = matches;
    }

    public List<String> getMatches() {
      return matches;
    }

}
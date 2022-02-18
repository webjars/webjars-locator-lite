package org.webjars;

import java.util.List;
import javax.annotation.Nullable;

/**
 * Thrown when a path matches multiple assets.
 */
public class MultipleMatchesException extends IllegalArgumentException {

    private static final long serialVersionUID = -5499824108129968936L;

    private final List<String> matches;

    public MultipleMatchesException(@Nullable final String message, @Nullable List<String> matches) {
        super(message);
        this.matches = matches;
    }

    @Nullable
    public List<String> getMatches() {
        return matches;
    }

}

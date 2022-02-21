package org.webjars;

import javax.annotation.Nullable;

/**
 * Thrown when a path matches multiple assets.
 */
public class NotFoundException extends IllegalArgumentException {

    private static final long serialVersionUID = -3946078288741435789L;

    public NotFoundException(@Nullable final String message) {
        super(message);
    }

}

package org.webjars;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.jar.JarFile;

/**
 * Utility for closing resources without throwing an exception (because you should never throw an exception from a
 * finally block).
 */
public class CloseQuietly {
    private static final Logger log = LoggerFactory.getLogger(CloseQuietly.class);

    static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                log.debug("Exception while closing resource", e);
            }
        }
    }

    static void closeQuietly(JarFile closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                log.debug("Exception while closing resource", e);
            }
        }
    }
}

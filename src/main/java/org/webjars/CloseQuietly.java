package org.webjars;

import java.io.Closeable;
import java.io.IOException;
import java.util.jar.JarFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility for closing resources without throwing an exception (because you should never throw an exception from a
 * finally block).
 */
public class CloseQuietly {
    private static final Logger log = LoggerFactory.getLogger(CloseQuietly.class);

    public static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                log.debug("Exception while closing resource", e);
            }
        }
    }
    
    public static void closeQuietly(JarFile jarFile) {
        if (jarFile != null) {
            try {
                jarFile.close();
            } catch (IOException e) {
                log.debug("Exception while closing resource", e);
            }
        }
    }

}

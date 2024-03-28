package org.webjars;


import org.jspecify.annotations.Nullable;

/**
 * WebJar Locator Cache Interface
 * Since classpath resources are essentially immutable, the WebJarsCache does not have the concept of expiry.
 * Cache keys and values are Strings because that is all that is needed.
 */
public interface WebJarCache {

    @Nullable String get(final String key);

    void put(final String key, final String value);

}

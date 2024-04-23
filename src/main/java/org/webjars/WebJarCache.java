package org.webjars;


import org.jspecify.annotations.Nullable;

import java.util.function.Function;

/**
 * WebJar Locator Cache Interface
 * Since classpath resources are essentially immutable, the WebJarsCache does not have the concept of expiry.
 * Cache keys and values are Strings because that is all that is needed.
 */
public interface WebJarCache {

    // todo: null can't be cached but if the locator can't find something, it never will, so consider having the compute function return Optional<String> so that we can cache the non-existence
    @Nullable String computeIfAbsent(String key, Function<String, String> function);

}

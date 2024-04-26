package org.webjars;

import org.jspecify.annotations.NullMarked;

import java.util.Optional;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;


@NullMarked
public class WebJarCacheDefault implements WebJarCache {

    final ConcurrentMap<String, Optional<String>> cache;

    public WebJarCacheDefault(ConcurrentMap<String, Optional<String>> cache) {
        this.cache = cache;
    }

    @Override
    public Optional<String> computeIfAbsent(String key, Function<String, Optional<String>> function) {
        return cache.computeIfAbsent(key, function);
    }

}

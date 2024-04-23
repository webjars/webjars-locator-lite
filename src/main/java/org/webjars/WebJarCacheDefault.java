package org.webjars;

import org.jspecify.annotations.Nullable;

import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

public class WebJarCacheDefault implements WebJarCache {

    final ConcurrentMap<String, String> cache;

    public WebJarCacheDefault(ConcurrentMap<String, String> cache) {
        this.cache = cache;
    }

    @Override
    public @Nullable String computeIfAbsent(String key, Function<String, String> function) {
        return cache.computeIfAbsent(key, function);
    }

}

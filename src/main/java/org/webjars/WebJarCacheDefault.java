package org.webjars;

import org.jspecify.annotations.Nullable;

import java.util.concurrent.ConcurrentHashMap;

public class WebJarCacheDefault implements WebJarCache {

    final ConcurrentHashMap<String, String> cache;

    public WebJarCacheDefault(ConcurrentHashMap<String, String> cache) {
        this.cache = cache;
    }

    @Override
    public @Nullable String get(String key) {
        return cache.get(key);
    }

    @Override
    public void put(String key, String value) {
        cache.put(key, value);
    }

}

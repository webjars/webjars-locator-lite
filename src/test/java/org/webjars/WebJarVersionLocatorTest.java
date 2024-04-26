package org.webjars;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.jspecify.annotations.NullMarked;
import org.junit.Test;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class WebJarVersionLocatorTest {

    @Test
    public void invalid_webjar_path_should_return_null() {
        assertNull(new WebJarVersionLocator().version("foo"));
    }

    @Test
    public void should_get_a_webjar_version() {
        assertEquals("3.1.1", new WebJarVersionLocator().version("bootswatch-yeti"));
    }

    @Test
    public void webjar_version_doesnt_match_path() {
        assertEquals("3.1.1", new WebJarVersionLocator().version("bootstrap"));
    }

    @Test
    public void full_path_exists_version_not_supplied() {
        assertEquals(WebJarVersionLocator.WEBJARS_PATH_PREFIX + "/bootstrap/3.1.1/js/bootstrap.js", new WebJarVersionLocator().fullPath("bootstrap", "js/bootstrap.js"));
    }

    @Test
    public void path_exists_version_not_supplied() {
        assertEquals("bootstrap/3.1.1/js/bootstrap.js", new WebJarVersionLocator().path("bootstrap", "js/bootstrap.js"));
    }

    @Test
    public void full_path_exists_version_supplied() {
        assertEquals(WebJarVersionLocator.WEBJARS_PATH_PREFIX + "/bootstrap/3.1.1/js/bootstrap.js", new WebJarVersionLocator().fullPath("bootstrap", "3.1.1/js/bootstrap.js"));
    }

    @Test
    public void cache_is_populated_on_lookup() {
        AtomicInteger numLookups = new AtomicInteger(0);

        @NullMarked
        class InspectableCache implements WebJarCache {
            final ConcurrentHashMap<String, Optional<String>> cache = new ConcurrentHashMap<>();

            @Override
            public Optional<String> computeIfAbsent(String key, Function<String, Optional<String>> function) {
                Function<String, Optional<String>> inspectableFunction = function.andThen((value) -> {
                    numLookups.incrementAndGet();
                    return value;
                });
                return cache.computeIfAbsent(key, inspectableFunction);
            }
        }

        final WebJarVersionLocator webJarVersionLocator = new WebJarVersionLocator(new InspectableCache());

        assertEquals("3.1.1", webJarVersionLocator.version("bootstrap"));
        assertEquals(1, numLookups.get());
        // should hit the cache and produce the same value
        assertEquals("3.1.1", webJarVersionLocator.version("bootstrap"));
        assertEquals(1, numLookups.get());

        // version is already cached so we shouldn't hit it again
        assertEquals(WebJarVersionLocator.WEBJARS_PATH_PREFIX + "/bootstrap/3.1.1/js/bootstrap.js", webJarVersionLocator.fullPath("bootstrap", "js/bootstrap.js"));
        assertEquals(1, numLookups.get());

        // make sure we don't hit the cache for another file in the already resolved WebJar
        assertEquals(WebJarVersionLocator.WEBJARS_PATH_PREFIX + "/bootstrap/3.1.1/css/bootstrap.css", webJarVersionLocator.fullPath("bootstrap", "css/bootstrap.css"));
        assertEquals(1, numLookups.get());

        // another WebJar should hit the cache but only once
        assertEquals("3.1.1", webJarVersionLocator.version("bootswatch-yeti"));
        assertEquals(2, numLookups.get());

        assertEquals("3.1.1", webJarVersionLocator.version("bootswatch-yeti"));
        assertEquals(2, numLookups.get());

        assertNull(webJarVersionLocator.version("asdf"));
        assertEquals(3, numLookups.get());

        assertNull(webJarVersionLocator.version("asdf"));
        assertEquals(3, numLookups.get());
    }
}

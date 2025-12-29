package org.webjars;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

class WebJarVersionLocatorTest {

    @Test
    void invalid_webjar_path_should_return_null() {
        assertNull(new WebJarVersionLocator().version("foo"));
    }

    @Test
    void should_get_a_webjar_version() {
        assertEquals("3.1.1", new WebJarVersionLocator().version("bootswatch-yeti"));
    }

    @Test
    void should_find_good_custom_webjar_version() {
        assertEquals("3.2.1", new WebJarVersionLocator().version("goodwebjar"));
    }

    @Test
    void should_not_find_bad_custom_webjar_version() {
        assertNull(new WebJarVersionLocator().version("badwebjar"));
    }

    @Test
    void should_find_bower_webjar_version() {
        assertEquals("2.3.2", new WebJarVersionLocator().version("js-base64"));
    }

    @Test
    void webjar_version_doesnt_match_path() {
        assertEquals("3.1.1", new WebJarVersionLocator().version("bootstrap"));
    }

    @Test
    void full_path_exists_version_not_supplied() {
        assertEquals(WebJarVersionLocator.WEBJARS_PATH_PREFIX + "/bootstrap/3.1.1/js/bootstrap.js", new WebJarVersionLocator().fullPath("bootstrap", "js/bootstrap.js"));
    }

    @Test
    void path_exists_version_not_supplied() {
        assertEquals("bootstrap/3.1.1/js/bootstrap.js", new WebJarVersionLocator().path("bootstrap", "js/bootstrap.js"));
    }

    @Test
    void full_path_exists_with_version_supplied() {
        assertEquals(WebJarVersionLocator.WEBJARS_PATH_PREFIX + "/bootstrap/3.1.1/js/bootstrap.js", new WebJarVersionLocator().fullPath("bootstrap", "3.1.1/js/bootstrap.js"));
    }

    @Test
    void path_exists_with_version_supplied() {
        assertEquals("bootstrap/3.1.1/js/bootstrap.js", new WebJarVersionLocator().path("bootstrap", "3.1.1/js/bootstrap.js"));
    }

    @Test
    void cache_is_populated_on_lookup() {
        AtomicBoolean shouldInspect = new AtomicBoolean(false);
        AtomicInteger numLookups = new AtomicInteger(0);

        @NullMarked
        class InspectableCache implements WebJarCache {
            final ConcurrentHashMap<String, Optional<String>> cache = new ConcurrentHashMap<>();

            @Override
            public Optional<String> computeIfAbsent(String key, Function<String, Optional<String>> function) {
                Function<String, Optional<String>> inspectableFunction = function.andThen((value) -> {
                    if(shouldInspect.get()) {
                        numLookups.incrementAndGet();
                    }
                    return value;
                });
                return cache.computeIfAbsent(key, inspectableFunction);
            }
        }

        final WebJarVersionLocator webJarVersionLocator = new WebJarVersionLocator(new InspectableCache());
        // enable inspection after webJarVersionLocator has been constructed, to ignore lookups caused by loading webjars-locator.properties
        shouldInspect.set(true);

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

    @Test
    void groupid_works_for_classic() {
        assertEquals("org.webjars", new WebJarVersionLocator().groupId("bootstrap"));
    }

    @Test
    void groupid_works_for_npm() {
        assertEquals("org.webjars.npm", new WebJarVersionLocator().groupId("jquery"));
    }

    @Test
    void version_works_for_qrcodejs() {
        final WebJarVersionLocator webJarVersionLocator = new WebJarVersionLocator();
        assertEquals("2015.11.25-04f46c6", webJarVersionLocator.version("qrcodejs"));
    }
}

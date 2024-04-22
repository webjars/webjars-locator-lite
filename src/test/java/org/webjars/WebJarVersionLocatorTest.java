package org.webjars;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.concurrent.ConcurrentHashMap;

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
        final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();
        final WebJarVersionLocator webJarVersionLocator = new WebJarVersionLocator(new WebJarCacheDefault(cache));

        assertEquals("3.1.1", webJarVersionLocator.version("bootstrap"));
        assertEquals(1, cache.size());
        // should hit the cache and produce the same value
        // todo: test that it was actually a cache hit
        assertEquals("3.1.1", webJarVersionLocator.version("bootstrap"));

        assertEquals(WebJarVersionLocator.WEBJARS_PATH_PREFIX + "/bootstrap/3.1.1/js/bootstrap.js", webJarVersionLocator.fullPath("bootstrap", "js/bootstrap.js"));
        assertEquals(2, cache.size());
        // should hit the cache and produce the same value
        // todo: test that it was actually a cache hit
        assertEquals(WebJarVersionLocator.WEBJARS_PATH_PREFIX + "/bootstrap/3.1.1/js/bootstrap.js", webJarVersionLocator.fullPath("bootstrap", "js/bootstrap.js"));
    }
}

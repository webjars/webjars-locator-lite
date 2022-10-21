package org.webjars;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class WebJarVersionLocatorTest {

    @Test
    public void invalid_webjar_path_should_return_null() {
        assertNull(WebJarVersionLocator.webJarVersion("foo"));
    }

    @Test
    public void should_get_a_webjar_version() {
        assertEquals("3.1.1", WebJarVersionLocator.webJarVersion("bootswatch-yeti"));
    }

    @Test
    public void webjar_version_doesnt_match_path() {
        assertEquals("3.1.1", WebJarVersionLocator.webJarVersion("bootstrap"));
    }

    @Test
    public void full_path_exists_version_not_supplied() {
        assertEquals(WebJarVersionLocator.WEBJARS_PATH_PREFIX + "/bootstrap/3.1.1/js/bootstrap.js", WebJarVersionLocator.fullPath("bootstrap", "js/bootstrap.js"));
    }

    @Test
    public void full_path_exists_version_supplied() {
        assertEquals(WebJarVersionLocator.WEBJARS_PATH_PREFIX + "/bootstrap/3.1.1/js/bootstrap.js", WebJarVersionLocator.fullPath("bootstrap", "3.1.1/js/bootstrap.js"));
    }
}

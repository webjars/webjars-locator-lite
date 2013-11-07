package org.webjars;

import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.Set;

import org.junit.Test;

public class WebJarAssetLocatorTest {

    @Test
    public void get_paths_of_asset_in_nested_folder() {
        WebJarAssetLocator locator = new WebJarAssetLocator();
        String jsPath = locator.getFullPath("bootstrap.js");
        String cssPath = locator.getFullPath("bootstrap.css");

        assertEquals("META-INF/resources/webjars/bootstrap/2.2.2/js/bootstrap.js", jsPath);
        assertEquals("META-INF/resources/webjars/bootstrap/2.2.2/css/bootstrap.css", cssPath);
    }

    @Test
    public void get_full_path_of_asset_in_root_folder() {
        String jsFullPath = new WebJarAssetLocator().getFullPath("jquery.js");

        assertEquals("META-INF/resources/webjars/jquery/1.8.3/jquery.js", jsFullPath);
    }

    @Test
    public void get_a_file_when_another_file_exists_that_starts_with_the_same_string() {
        String fooJsPath = new WebJarAssetLocator().getFullPath("foo.js");
        assertEquals("META-INF/resources/webjars/foo/1.0.0/foo.js", fooJsPath);
    }

    @Test
    public void get_full_path_from_partial_path_with_folders() {
        WebJarAssetLocator locator = new WebJarAssetLocator();
        String jsPath1 = locator.getFullPath("js/bootstrap.js");
        String jsPath2 = locator.getFullPath("/2.2.2/js/bootstrap.js");
        String jsPath3 = locator.getFullPath("bootstrap/2.2.2/js/bootstrap.js");
        String jsPath4 = locator.getFullPath("/bootstrap/2.2.2/js/bootstrap.js");

        String expected = "META-INF/resources/webjars/bootstrap/2.2.2/js/bootstrap.js";
        assertEquals(expected, jsPath1);
        assertEquals(expected, jsPath2);
        assertEquals(expected, jsPath3);
        assertEquals(expected, jsPath4);
    }

    @Test
    public void should_throw_exception_when_asset_not_found() {
        try {
            new WebJarAssetLocator().getFullPath("asset-unknown.js");
            fail("Exception should have been thrown!");
        } catch (IllegalArgumentException e) {
            assertEquals("asset-unknown.js could not be found. Make sure you've added the corresponding WebJar and please check for typos.", e.getMessage());
        }

        try {
            new WebJarAssetLocator().getFullPath("unknown.js");
            fail("Exception should have been thrown!");
        } catch (IllegalArgumentException e) {
            assertEquals("unknown.js could not be found. Make sure you've added the corresponding WebJar and please check for typos.", e.getMessage());
        }
    }

    @Test
    public void should_distinguish_between_multiple_versions() {
        WebJarAssetLocator locator = new WebJarAssetLocator();
        String v1Path = locator.getFullPath("1.0.0/multiple.js");
        String v2Path = locator.getFullPath("2.0.0/multiple.js");
        String moduleV2Path = locator.getFullPath("2.0.0/module/multiple_module.js");

        assertEquals("META-INF/resources/webjars/multiple/1.0.0/multiple.js", v1Path);
        assertEquals("META-INF/resources/webjars/multiple/2.0.0/multiple.js", v2Path);
        assertEquals("META-INF/resources/webjars/multiple/2.0.0/module/multiple_module.js", moduleV2Path);
    }

    @Test
    public void should_throw_exceptions_when_several_matches_found() {
        try {
            new WebJarAssetLocator().getFullPath("multiple.js");
            fail("Exception should have been thrown!");
        } catch (MultipleMatchesException e) {
            assertEquals("Multiple matches found for multiple.js. Please provide a more specific path, for example by including a version number.", e.getMessage());
        }
    }

    @Test
    public void should_throw_exceptions_when_several_matches_found_with_folder_in_path() {
        try {
            new WebJarAssetLocator().getFullPath("module/multiple_module.js");
            fail("Exception should have been thrown!");
        } catch (IllegalArgumentException e) {
            assertEquals("Multiple matches found for module/multiple_module.js. Please provide a more specific path, for example by including a version number.", e.getMessage());
        }
    }

    @Test
    public void should_list_assets_in_folder() {
        String fullPathPrefix = "META-INF/resources/webjars/multiple/1.0.0/";
        Set<String> assets = new WebJarAssetLocator().listAssets("/multiple/1.0.0");

        assertThat(assets, hasItems(fullPathPrefix + "multiple.js", fullPathPrefix + "module/multiple_module.js"));
    }
}

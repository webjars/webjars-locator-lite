package org.webjars;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

public class AssetLocatorTest {

  @Test
  public void get_paths_of_asset_in_nested_folder() {
    String jsPath = AssetLocator.getFullPath("bootstrap.js");
    String cssPath = AssetLocator.getFullPath("bootstrap.css");

    assertEquals("META-INF/resources/webjars/bootstrap/2.2.2/js/bootstrap.js", jsPath);
    assertEquals("META-INF/resources/webjars/bootstrap/2.2.2/css/bootstrap.css", cssPath);
  }

  @Test
  public void get_web_jar_path_of_asset_in_nested_folder() {
    String jsWebJarPath = AssetLocator.getWebJarPath("bootstrap.js");
    String cssWebJarPath = AssetLocator.getWebJarPath("bootstrap.css");

    assertEquals("webjars/bootstrap/2.2.2/js/bootstrap.js", jsWebJarPath);
    assertEquals("webjars/bootstrap/2.2.2/css/bootstrap.css", cssWebJarPath);
  }

  @Test
  public void get_full_path_of_asset_in_root_folder() {
    String jsFullPath = AssetLocator.getFullPath("jquery.js");

    assertEquals("META-INF/resources/webjars/jquery/1.8.3/jquery.js", jsFullPath);
  }

  @Test
  public void get_web_jar_path_of_asset_in_root_folder() {
    String jsWebJarPath = AssetLocator.getWebJarPath("jquery.js");

    assertEquals("webjars/jquery/1.8.3/jquery.js", jsWebJarPath);
  }

  @Test
  public void get_full_path_from_partial_path_with_folders() {
    String jsPath1 = AssetLocator.getFullPath("js/bootstrap.js");
    String jsPath2 = AssetLocator.getFullPath("/2.2.2/js/bootstrap.js");
    String jsPath3 = AssetLocator.getFullPath("bootstrap/2.2.2/js/bootstrap.js");
    String jsPath4 = AssetLocator.getFullPath("/bootstrap/2.2.2/js/bootstrap.js");

    String expected = "META-INF/resources/webjars/bootstrap/2.2.2/js/bootstrap.js";
    assertEquals(expected, jsPath1);
    assertEquals(expected, jsPath2);
    assertEquals(expected, jsPath3);
    assertEquals(expected, jsPath4);
  }

  @Test
  public void should_throw_exception_when_asset_not_found() {
    try {
      AssetLocator.getFullPath("unknown.js");
      fail("Exception should have been thrown!");
    } catch (IllegalArgumentException e) {
      assertEquals("unknown.js could not be found. Make sure you've added the corresponding WebJar and please check for typos.", e.getMessage());
    }
  }

  @Test
  public void should_distinguish_between_multiple_versions() {
    String v1Path = AssetLocator.getFullPath("1.0.0/multiple.js");
    String v2Path = AssetLocator.getFullPath("2.0.0/multiple.js");
    String moduleV2Path = AssetLocator.getFullPath("2.0.0/module/multiple_module.js");

    assertEquals("META-INF/resources/webjars/multiple/1.0.0/multiple.js", v1Path);
    assertEquals("META-INF/resources/webjars/multiple/2.0.0/multiple.js", v2Path);
    assertEquals("META-INF/resources/webjars/multiple/2.0.0/module/multiple_module.js", moduleV2Path);
  }

  @Test
  public void should_throw_exceptions_when_several_matches_found() {
    try {
      AssetLocator.getFullPath("multiple.js");
      fail("Exception should have been thrown!");
    } catch (IllegalArgumentException e) {
      assertEquals("Multiple matches found for multiple.js. Please provide a more specific path, for example by including a version number.", e.getMessage());
    }
  }

  @Test
  public void should_throw_exceptions_when_several_matches_found_with_folder_in_path() {
    try {
      AssetLocator.getFullPath("module/multiple_module.js");
      fail("Exception should have been thrown!");
    } catch (IllegalArgumentException e) {
      assertEquals("Multiple matches found for module/multiple_module.js. Please provide a more specific path, for example by including a version number.", e.getMessage());
    }
  }
}

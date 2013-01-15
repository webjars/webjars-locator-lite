package org.webjars;

import static org.junit.Assert.assertEquals;

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
}

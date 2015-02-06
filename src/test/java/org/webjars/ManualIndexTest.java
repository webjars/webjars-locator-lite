package org.webjars;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.HashSet;
import java.util.Map;

import org.junit.Test;

public class ManualIndexTest {

    @Test
    public void should_find_full_path() throws Exception {
        WebJarAssetLocator locator = new WebJarAssetLocator(new HashSet<String>(asList("META-INF/resources/myapp/app.js", "assets/users/login.css")));
        
        assertEquals("META-INF/resources/myapp/app.js", locator.getFullPath("app.js"));
        assertEquals("META-INF/resources/myapp/app.js", locator.getFullPath("myapp/app.js"));
        assertEquals("assets/users/login.css", locator.getFullPath("login.css"));
        assertEquals("assets/users/login.css", locator.getFullPath("users/login.css"));
    }
    
    @Test
    public void should_list_assets() throws Exception {
        WebJarAssetLocator locator = new WebJarAssetLocator(new HashSet<String>(asList("META-INF/resources/myapp/app.js", "assets/users/login.css", "META-INF/resources/webjars/third_party/1.5.2/file.js")));
        
        assertThat(locator.listAssets("META-INF"), contains("META-INF/resources/myapp/app.js", "META-INF/resources/webjars/third_party/1.5.2/file.js"));
        assertThat(locator.listAssets("assets"), contains("assets/users/login.css"));
        assertThat(locator.listAssets("third_party"), contains("META-INF/resources/webjars/third_party/1.5.2/file.js"));
        assertThat(locator.listAssets(), contains("assets/users/login.css", "META-INF/resources/myapp/app.js", "META-INF/resources/webjars/third_party/1.5.2/file.js"));
    }
    
    @Test
    public void should_find_webjars() throws Exception {
        WebJarAssetLocator locator = new WebJarAssetLocator(new HashSet<String>(asList("META-INF/resources/myapp/app.js", "assets/users/login.css", "META-INF/resources/webjars/third_party/1.5.2/file.js")));
        
        Map<String, String> webJars = locator.getWebJars();
        
        assertThat(webJars.keySet(), contains("third_party"));
        assertThat(webJars.values(), contains("1.5.2"));
    }
}

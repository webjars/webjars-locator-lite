package org.webjars.urlprotocols;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.webjars.CloseQuietly;
import org.webjars.WebJarAssetLocator;

/**
 * Testing behavior of {@link JarUrlProtocolHandler}.
 */
@RunWith(Parameterized.class)
public class JarUrlProtocolHandlerTest {

    @Parameters(name = "test JAR = {0}")
    public static Collection<String[]> testFilePaths() {
        return Arrays.asList(
            new String[]{"normal/path/to.jar"},
            new String[]{"strange/!path/to.jar"},
            new String[]{"misplaced!/" + WebJarAssetLocator.WEBJARS_PATH_PREFIX + "/path/to.jar"});
    }

    /** Temporary directory for the files created during test method executions. */
    @Rule
    public final TemporaryFolder tmpDir = new TemporaryFolder();

    /**
     * Creates JAR file with JS files in it. The file created in
     * a temporary directory and will be removed automatically after every
     * test method execution.
     * @param path
     *        the path of the JAR file to create, relative to {@link #tmpDir} location
     * @param jsLibPaths
     *        the paths of JS files to create inside JAR file. This paths must be relative to
     *        {@link WebJarAssetLocator#WEBJARS_PATH_PREFIX} and must not contain leading slash
     * @return newly created file handler
     * @throws MalformedURLException
     *         if the path specified contains charactes illegal for URL
     */
    private File createJarFile(String path, String... jsLibPaths) throws MalformedURLException {
        // Creating new file
        File jarFile = new File(tmpDir.getRoot(), path);
        File jarDir = jarFile.getParentFile();
        if (!jarDir.exists()) {
            if (!jarDir.mkdirs()) {
                throw new IllegalStateException("Unable to create directories for path " + path);
            }
        }
        // Writing file content
        ZipOutputStream zip = null;
        try {
            zip = new ZipOutputStream(new FileOutputStream(jarFile));
            for (String jsLibPath : jsLibPaths) {
                zip.putNextEntry(new ZipEntry(WebJarAssetLocator.WEBJARS_PATH_PREFIX + "/" + jsLibPath));
                zip.write("var test = true;".getBytes("UTF-8"));
                zip.closeEntry();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create test JAR file", e);
        } finally {
            CloseQuietly.closeQuietly(zip);
        }
        return jarFile;
    }

    private static URL findWebjarsResource(File jarFile, String resourceName) throws IOException {
        URLClassLoader classLoader = null;
        URL resourceUrl = null;
        try {
            classLoader = URLClassLoader.newInstance(new URL[]{jarFile.toURI().toURL()});
            resourceUrl = classLoader.findResource(
                WebJarAssetLocator.WEBJARS_PATH_PREFIX + "/" + resourceName);
        } finally {
            if (classLoader != null) {
                classLoader.close();
            }
        }
        if (resourceUrl != null) {
            return resourceUrl;
        } else {
            throw new IOException("Unable to find resource");
        }
    }

    /** Test file path. */
    private final String testFilePath;

    public JarUrlProtocolHandlerTest(String testFilePath) throws MalformedURLException {
        this.testFilePath = testFilePath;
    }

    @Test
    public void getAssetPaths_finds_existing_webjars_assets_in_JAR() throws IOException {
        File jarFile = createJarFile(testFilePath, "foo/1.0.0/foo.js", "bar/2.3/bar.js");
        URL urlToHandle = findWebjarsResource(jarFile, "bar/2.3/bar.js");
        JarUrlProtocolHandler handler = new JarUrlProtocolHandler();
        Set<String> assets = handler.getAssetPaths(urlToHandle, Pattern.compile(".*foo.*"));
        Assert.assertEquals(1, assets.size());
        Assert.assertEquals(WebJarAssetLocator.WEBJARS_PATH_PREFIX + "/foo/1.0.0/foo.js",
            assets.iterator().next());
    }

}

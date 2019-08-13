package org.webjars;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

public class WebJarExtractorTestUtils {
    public static URLClassLoader createClassLoader() throws Exception {
        // find all webjar urls on the classpath

        WebJarAssetLocator webJarAssetLocator = new WebJarAssetLocator();
        List<URL> webJarUrls = new ArrayList<>();

        for (WebJarAssetLocator.WebJarInfo webJarInfo : webJarAssetLocator.allWebJars.values()) {
            webJarUrls.add(webJarInfo.uri.toURL());
        }

        return new URLClassLoader(webJarUrls.toArray(new URL[0]), null);
    }

    public static File createTmpDir() throws IOException {
        File tmpDir = File.createTempFile("webjarextractortest-", "");
        tmpDir.delete();
        tmpDir.mkdir();
        return tmpDir;
    }

    public static void deleteDir(File dir) {
        if (dir != null) {
            if (dir.isDirectory()) {
                for (File file : dir.listFiles()) {
                    deleteDir(file);
                }
            }
            dir.delete();
        }
    }



}

package org.webjars;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Locates WebJar assets
 * 
 * @deprecated Use @WebJarAssetLocator given its improved performance.
 * 
 */
@Deprecated
public class AssetLocator {

    public static final String[] WEBJARS_PATH_PREFIX = { "META-INF",
            "resources", "webjars" };

    public static String getFullPath(String partialPath) {
        return new WebJarAssetLocator(WebJarAssetLocator.getFullPathIndex(
                Pattern.compile(".*"), AssetLocator.class.getClassLoader()))
                .getFullPath(partialPath);
    }

    public static String getWebJarPath(String partialPath) {
        String fullPath = getFullPath(partialPath);

        if (fullPath == null) {
            return null;
        } else {
            String prefix = WEBJARS_PATH_PREFIX[0] + "/"
                    + WEBJARS_PATH_PREFIX[1] + "/";
            return fullPath.substring(prefix.length());
        }
    }

    public static Set<String> listAssets(String folderPath) {
        if (!folderPath.startsWith("/")) {
            folderPath = "/" + folderPath;
        }

        return new WebJarAssetLocator(WebJarAssetLocator.getFullPathIndex(
                Pattern.compile(".*"), AssetLocator.class.getClassLoader()))
                .listAssets(folderPath);
    }
}

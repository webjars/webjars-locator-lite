package org.webjars;

import java.util.Set;

/**
 * Locates WebJar assets
 * 
 * @deprecated Use @WebJarAssetLocator given its improved performance.
 * 
 */
@Deprecated
public class AssetLocator {

    public static final String META_INF_RESOUCE_PATH = "META-INF/resources/";

    public static String getFullPath(String partialPath) {
        return new WebJarAssetLocator().getFullPath(partialPath);
    }

    public static String getWebJarPath(String partialPath) {
        String fullPath = getFullPath(partialPath);

        if (fullPath == null) {
            return null;
        } else {
            return fullPath.substring(META_INF_RESOUCE_PATH.length());
        }
    }

    public static Set<String> listAssets(String folderPath) {
        return new WebJarAssetLocator().listAssets(folderPath);
    }
}

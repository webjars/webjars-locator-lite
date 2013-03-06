package org.webjars;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

/**
 * Locates WebJar assets
 * 
 * @deprecated Use @AssetLocator given its superior performance.
 * 
 */
@Deprecated
public class AssetLocator {

    public static final String[] WEBJARS_PATH_PREFIX = { "META-INF",
            "resources", "webjars" };

    public static String getFullPath(String partialPath) {
        return WebJarAssetLocator.getFullPath(WebJarAssetLocator
                .getFullPathIndex(Pattern.compile(".*"),
                        AssetLocator.class.getClassLoader()), partialPath);
    }

    public static String getWebJarPath(String partialPath) {
        String fullPath = getFullPath(partialPath);

        if (fullPath != null) {
            String prefix = WEBJARS_PATH_PREFIX[0] + "/"
                    + WEBJARS_PATH_PREFIX[1] + "/";
            return fullPath.substring(prefix.length());
        }

        return null;
    }

    public static Set<String> listAssets(String folderPath) {
        if (!folderPath.startsWith("/")) {
            folderPath = "/" + folderPath;
        }

        final Set<String> allAssets = WebJarAssetLocator.getAssetPaths(
                Pattern.compile(".*"), AssetLocator.class.getClassLoader());

        final Set<String> assets = new HashSet<String>(allAssets.size());
        for (final String asset : allAssets) {
            if (asset.startsWith(StringUtils.join(WEBJARS_PATH_PREFIX, "/")
                    + folderPath)) {
                assets.add(asset);
            }
        }

        return assets;
    }
}

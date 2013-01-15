package org.webjars;

import com.google.common.collect.Multimap;

import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

public class AssetLocator {

    public static final String[] WEBJARS_PATH_PREFIX = {"META-INF", "resources", "webjars"};

    public static String getFullPath(String partialPath) {

        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder()
                .addUrls(ClasspathHelper.forPackage(StringUtils.join(WEBJARS_PATH_PREFIX, "."), AssetLocator.class.getClassLoader()))
                .setScanners(new ResourcesScanner());

        Reflections reflections = new Reflections(configurationBuilder);

        // the map in the reflection store is just the file name so if the file being located doesn't contain a "/" then
        // a shortcut can be taken.  Otherwise the collection of multimap's values need to be searched.
        // Either way the first match is returned (if there is a match)
        if (partialPath.contains("/")) {
            String foundPath = null;
            for (Multimap<String, String> paths : reflections.getStore().getStoreMap().values()) {
                for (String path : paths.values()) {
                    if (path.endsWith(partialPath)) {
                        if (foundPath != null) {
                            throw new IllegalArgumentException("Multiple matches found for " + partialPath + ". Please provide a more specific path, for example by including a version number.");
                        }
                        foundPath = path;
                    }
                }
            }

            if (foundPath != null) {
                return foundPath;
            }
        }
        else {
            Set<String> paths = reflections.getStore().getResources(partialPath);
            if (paths.size() > 1) {
                throw new IllegalArgumentException("Multiple matches found for " + partialPath + ". Please provide a more specific path, for example by including a version number.");
            }
            if (paths.size() > 0) {
                return paths.iterator().next(); // pick the first one
            }
        }

        throw new IllegalArgumentException(partialPath + " could not be found. Make sure you've added the corresponding WebJar and please check for typos.");
    }

    public static String getWebJarPath(String partialPath) {
        String fullPath = getFullPath(partialPath);

        if (fullPath != null) {
            String prefix = WEBJARS_PATH_PREFIX[0] + "/" + WEBJARS_PATH_PREFIX[1] + "/";
            return fullPath.substring(prefix.length());
        }

        return null;
    }

}

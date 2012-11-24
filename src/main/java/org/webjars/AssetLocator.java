package org.webjars;

import com.google.common.collect.Multimap;
import org.apache.commons.lang.StringUtils;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.util.Collection;
import java.util.Set;

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
            for (Multimap<String, String> paths : reflections.getStore().getStoreMap().values()) {
                for (String path : paths.values()) {
                    if (path.endsWith(partialPath)) {
                        return path;
                    }
                }
            }
        }
        else {
            Set<String> paths = reflections.getStore().getResources(partialPath);
            if (paths.size() > 0) {
                return paths.iterator().next(); // pick the first one
            }
        }

        return null;
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

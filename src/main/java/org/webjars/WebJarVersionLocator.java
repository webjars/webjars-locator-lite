package org.webjars;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;


/**
 * Locate WebJar version. The class is thread safe.
 */
@NullMarked
public class WebJarVersionLocator {

    /**
     * The path to where webjar resources live.
     */
    public static final String WEBJARS_PATH_PREFIX = "META-INF/resources/webjars";

    private static final String PROPERTIES_ROOT = "META-INF/maven/";
    private static final String NPM = "org.webjars.npm/";
    private static final String PLAIN = "org.webjars/";
    private static final String POM_PROPERTIES = "/pom.properties";

    private static final ClassLoader LOADER = WebJarVersionLocator.class.getClassLoader();


    @Nullable
    public static String fullPath(final String webJarName, final String exactPath) {
        String version = webJarVersion(webJarName);
        String fullPath = String.format("%s/%s/%s", WEBJARS_PATH_PREFIX, webJarName, exactPath);
        if (!isEmpty(version)) {
            if (!exactPath.startsWith(version)) {
                fullPath = String.format("%s/%s/%s/%s", WEBJARS_PATH_PREFIX, webJarName, version, exactPath);
            }
        }

        if (LOADER.getResource(fullPath) != null) {
            return fullPath;
        }

        return null;
    }

    @Nullable
    public static String webJarVersion(final String webJarName) {
        InputStream resource = LOADER.getResourceAsStream(PROPERTIES_ROOT + NPM + webJarName + POM_PROPERTIES);
        if (resource == null) {
            resource = LOADER.getResourceAsStream(PROPERTIES_ROOT + PLAIN + webJarName + POM_PROPERTIES);
        }

        // Webjars also uses org.webjars.bower as a group id, but the resource paths are not as standard (and not so many people use those)
        if (resource != null) {
            Properties properties = new Properties();
            try {
                properties.load(resource);
            } catch (IOException ignored) {

            }
            String version = properties.getProperty("version");
            // Sometimes a webjar version is not the same as the Maven artifact version
            if (version != null) {
                if (hasResourcePath(webJarName, version)) {
                    return version;
                }
                if (version.contains("-")) {
                    version = version.substring(0, version.indexOf("-"));
                    if (hasResourcePath(webJarName, version)) {
                        return version;
                    }
                }
            }
        }
        return null;
    }

    private static boolean hasResourcePath(final String webJarName, final String path) {
        return LOADER.getResource(WEBJARS_PATH_PREFIX + "/" + webJarName + "/" + path) != null;
    }

    private static boolean isEmpty(final String str) {
        return str == null || str.trim().isEmpty();
    }

}

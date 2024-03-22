package org.webjars;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Locate WebJar version. The class is thread safe.
 */
@NullMarked
public class WebJarVersionLocator {

    /**
     * The path to where webjar resources live.
     */
    public final String WEBJARS_PATH_PREFIX = "META-INF/resources/webjars";

    private final String PROPERTIES_ROOT = "META-INF/maven/";
    private final String NPM = "org.webjars.npm/";
    private final String PLAIN = "org.webjars/";
    private final String POM_PROPERTIES = "/pom.properties";

    private final ClassLoader LOADER = WebJarVersionLocator.class.getClassLoader();

    private final WebJarCache cache;

    public WebJarVersionLocator() {
        this.cache = new WebJarCacheDefault(new ConcurrentHashMap<>());
    }

    public WebJarVersionLocator(WebJarCache cache) {
        this.cache = cache;
    }

    public static class DEFAULT {
        private static final WebJarVersionLocator webJarVersionLocator = new WebJarVersionLocator();
        public static final String WEBJARS_PATH_PREFIX = webJarVersionLocator.WEBJARS_PATH_PREFIX;

        @Nullable
        public static String fullPath(final String webJarName, final String exactPath) {
            return webJarVersionLocator.fullPath(webJarName, exactPath);
        }

        @Nullable
        public static String webJarVersion(final String webJarName) {
            return webJarVersionLocator.webJarVersion(webJarName);
        }
    }

    @Nullable
    public String fullPath(final String webJarName, final String exactPath) {
        final String cacheKey = "fullpath-" + webJarName + "-" + exactPath;
        final String maybeCached = cache.get(cacheKey);
        if (maybeCached == null) {
            final String version = webJarVersion(webJarName);
            String fullPath = String.format("%s/%s/%s", WEBJARS_PATH_PREFIX, webJarName, exactPath);
            if (!isEmpty(version)) {
                if (!exactPath.startsWith(version)) {
                    fullPath = String.format("%s/%s/%s/%s", WEBJARS_PATH_PREFIX, webJarName, version, exactPath);
                }
            }

            if (LOADER.getResource(fullPath) != null) {
                cache.put(cacheKey, fullPath);
                return fullPath;
            }

            return null;
        }
        else {
            return maybeCached;
        }
    }

    @Nullable
    public String webJarVersion(final String webJarName) {
        final String cacheKey = "version-" + webJarName;
        final String maybeCached = cache.get(cacheKey);
        if (maybeCached == null) {
            InputStream resource = LOADER.getResourceAsStream(PROPERTIES_ROOT + NPM + webJarName + POM_PROPERTIES);
            if (resource == null) {
                resource = LOADER.getResourceAsStream(PROPERTIES_ROOT + PLAIN + webJarName + POM_PROPERTIES);
            }

            // Webjars also uses org.webjars.bower as a group id, but the resource paths are not as standard (and not so many people use those)
            if (resource != null) {
                final Properties properties = new Properties();
                try {
                    properties.load(resource);
                } catch (IOException ignored) {

                }
                String version = properties.getProperty("version");
                // Sometimes a webjar version is not the same as the Maven artifact version
                if (version != null) {
                    if (hasResourcePath(webJarName, version)) {
                        cache.put(cacheKey, version);
                        return version;
                    }
                    if (version.contains("-")) {
                        version = version.substring(0, version.indexOf("-"));
                        if (hasResourcePath(webJarName, version)) {
                            cache.put(cacheKey, version);
                            return version;
                        }
                    }
                }
            }

            return null;
        }
        else {
            return maybeCached;
        }
    }

    private boolean hasResourcePath(final String webJarName, final String path) {
        return LOADER.getResource(WEBJARS_PATH_PREFIX + "/" + webJarName + "/" + path) != null;
    }

    private boolean isEmpty(final String str) {
        return str == null || str.trim().isEmpty();
    }

}

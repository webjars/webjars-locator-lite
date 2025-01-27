package org.webjars;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Map;
import java.util.Optional;
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
    public static final String WEBJARS_PATH_PREFIX = "META-INF/resources/webjars";

    private static final String PROPERTIES_ROOT = "META-INF/maven/";
    private static final String NPM = "org.webjars.npm/";
    private static final String PLAIN = "org.webjars/";
    private static final String POM_PROPERTIES = "/pom.properties";
    private static final String LOCATOR_PROPERTIES = "META-INF/resources/webjars-locator.properties";

    private static final String CACHE_KEY_PREFIX = "version-";

    private static final ClassLoader LOADER = WebJarVersionLocator.class.getClassLoader();

    private final WebJarCache cache;

    public WebJarVersionLocator() {
        this.cache = new WebJarCacheDefault(new ConcurrentHashMap<>());
        readLocatorProperties();
    }

    WebJarVersionLocator(WebJarCache cache) {
        this.cache = cache;
        readLocatorProperties();
    }

    /**
     * @param webJarName The name of the WebJar, i.e. bootstrap
     * @param exactPath The path to the file within the WebJar, i.e. js/bootstrap.js
     * @return The full path to the file in the classpath including the version, i.e. META-INF/resources/webjars/bootstrap/3.1.1/js/bootstrap.js
     */
    @Nullable
    public String fullPath(final String webJarName, final String exactPath) {
        final String path = path(webJarName, exactPath);

        if (notEmpty(path)) {
            return String.format("%s/%s", WEBJARS_PATH_PREFIX, path);
        }

        return null;
    }

    /**
     *
     * @param webJarName The name of the WebJar, i.e. bootstrap
     * @param exactPath The path to the file within the WebJar, i.e. js/bootstrap.js
     * @return The path to the file in the standard WebJar classpath location, including the version, i.e. bootstrap/3.1.1/js/bootstrap.js
     */
    @Nullable
    public String path(final String webJarName, final String exactPath) {
        final String version = version(webJarName);

        if (notEmpty(version)) {
            if (exactPath.startsWith(version)) {
                return String.format("%s/%s", webJarName, exactPath);
            } else {
                return String.format("%s/%s/%s", webJarName, version, exactPath);
            }
        }

        return null;
    }

    /**
     * @param webJarName The name of the WebJar, i.e. bootstrap
     * @return The version of the WebJar, i.e 3.1.1
     */
    @Nullable
    public String version(final String webJarName) {
        final String cacheKey = CACHE_KEY_PREFIX + webJarName;
        final Optional<String> optionalVersion = cache.computeIfAbsent(cacheKey, (key) -> {
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

                } finally {
                    try {
                        resource.close();
                    } catch (IOException ignored) {

                    }
                }

                String version = properties.getProperty("version");
                // Sometimes a webjar version is not the same as the Maven artifact version
                if (version != null) {
                    if (hasResourcePath(webJarName, version)) {
                        return Optional.of(version);
                    }
                    if (version.contains("-")) {
                        version = version.substring(0, version.indexOf("-"));
                        if (hasResourcePath(webJarName, version)) {
                            return Optional.of(version);
                        }
                    }
                }
            }

            return Optional.empty();
        });

        return optionalVersion.orElse(null);
    }

    private void readLocatorProperties() {
        try {
            Enumeration<URL> resources = LOADER.getResources(LOCATOR_PROPERTIES);
            while (resources.hasMoreElements()) {
                URL resourceUrl = resources.nextElement();
                try (InputStream resource = resourceUrl.openStream()) {
                    Properties properties = new Properties();
                    properties.load(resource);
                    for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                        String webJarName = entry.getKey().toString();
                        if (!webJarName.endsWith(".version")) {
                            // ".version" suffix is required
                            continue;
                        }

                        webJarName = webJarName.substring(0, webJarName.lastIndexOf(".version"));

                        String version = entry.getValue().toString();
                        if (hasResourcePath(webJarName, version)) {
                            // Only add configured versions if their path exists
                            cache.computeIfAbsent(CACHE_KEY_PREFIX + webJarName, x -> Optional.of(version));
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("unable to load locator properties", e);
        }
    }

    private boolean hasResourcePath(final String webJarName, final String path) {
        return LOADER.getResource(WEBJARS_PATH_PREFIX + "/" + webJarName + "/" + path) != null;
    }

    private boolean notEmpty(@Nullable final String str) {
        return str != null && !str.trim().isEmpty();
    }

}

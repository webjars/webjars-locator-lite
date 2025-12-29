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
 * Helper Class to locate WebJar versions.
 *
 * <p>By default, this class only supports looking up official WebJars with the Maven group IDs of {@code org.webjars.npm} and {@code org.webjars}.
 *
 * <p>Custom WenJars can be registered by providing a {@code META-INF/resources/webjars-locator.properties} file.
 *
 * <p><b>Note:</b> It is recommended, to add this file directly to the custom WebJar to ease the usage.
 * But for WebJars not providing the file, you can add a {@code webjars-locator.properties} to your project.
 *
 * <p>Example file (multiple WebJars can be provided, one per line):
 * <pre>{@code
 * mywebjar.version=3.2.1
 * anotherwebjar.version=1.4.3
 * }</pre>
 *
 * <p>As the lookup of all {@code webjars-locator.properties} files happens during the construction of the class
 * and the found versions are directly added to the cache, these property files can and will override versions
 * that otherwise would be looked up by {@link WebJarVersionLocator#version(String)}.
 *
 * <p>When multiple {@code webjars-locator.properties} files contain a version for the same WebJar, the one that has been found first wins.
 *
 * <p>The class is thread safe.
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
    private static final String GROUPID_CACHE_KEY_PREFIX = "groupid-";

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
     * Builds the versioned path for a file of a WebJar within the standard WebJar classpath location (see {@link WebJarVersionLocator#WEBJARS_PATH_PREFIX}).
     *
     * <p>The path is built by prefixing the versioned path built by {@link WebJarVersionLocator#path(String, String)} with the standard WebJars location classpath.
     *
     * <p>See {@link WebJarVersionLocator#path(String, String)} for a detailed explanation of how the versioned file path is built.
     *
     * <p><b>Note:</b> This method does not perform any checks if the resulting path references an existing file.
     *
     * @param webJarName The name of the WebJar, this is the directory in the standard WebJar classpath location, usually the same as the Maven artifact ID
     * @param filePath   The path to the file within the WebJar
     * @return The versioned path to the file in the classpath, if a version has been found, otherwise {@code null}
     * @see WebJarVersionLocator#path(String, String)
     * @see WebJarVersionLocator#WEBJARS_PATH_PREFIX
     */
    @Nullable
    public String fullPath(final String webJarName, final String filePath) {
        final String path = path(webJarName, filePath);

        if (notEmpty(path)) {
            return String.format("%s/%s", WEBJARS_PATH_PREFIX, path);
        }

        return null;
    }

    /**
     * Builds the versioned path for a file of a WebJar relative to the standard WebJar classpath location (see {@link WebJarVersionLocator#WEBJARS_PATH_PREFIX}).
     *
     * <p>The path is built by joining the {@code webJarName}, the known version and the {@code filePath}, if no version (from classpath checking) is known for the WebJar this method returns {@code null}.
     *
     * <p><b>Note:</b> In cases where the {@code filePath} parameter already starts with the known version of the WebJar, the version will not be added again. But it is recommended that you do NOT include a hard-coded version when looking up WebJar file paths.
     *
     * <pre>{@code
     * // returns "bootstrap/3.1.1/css/bootstrap.css"
     * locator.path("bootstrap", "css/bootstrap.css");
     *
     * // returns "bootstrap/3.1.1/css/bootstrap.css" as well
     * locator.path("bootstrap", "3.1.1/css/bootstrap.css");
     *
     * // returns null, assuming there is no "unknown" WebJar
     * locator.path("unknown", "some/file.css");
     * }</pre>
     *
     * <p><b>Note:</b> This method does not perform any checks if the resulting path references an existing file.
     *
     * @param webJarName The name of the WebJar, this is the directory in the standard WebJar classpath location, usually the same as the Maven artifact ID
     * @param filePath   The path to the file within the WebJar
     * @return The versioned path relative to the standard WebJar classpath location, if a version has been found, otherwise {@code null}
     * @see WebJarVersionLocator#fullPath(String, String)
     * @see WebJarVersionLocator#WEBJARS_PATH_PREFIX
     */
    @Nullable
    public String path(final String webJarName, final String filePath) {
        final String version = version(webJarName);

        if (notEmpty(version)) {
            if (filePath.startsWith(version)) {
                return String.format("%s/%s", webJarName, filePath);
            } else {
                return String.format("%s/%s/%s", webJarName, version, filePath);
            }
        }

        return null;
    }

    /**
     * This method tries to determine the available version for a WebJar in the classpath.
     *
     * <p>For official WebJars, the version lookup is performed by checking for a {@code pom.properties} file for either {@link WebJarVersionLocator#NPM}
     * or {@link WebJarVersionLocator#PLAIN} WebJars within {@code META-INF/maven}. The lookup result is cached.
     *
     * <p>Custom WebJars can be registered by using a {@code webjars-locator.properties} file. See {@link WebJarVersionLocator} for details.
     *
     * @param webJarName The name of the WebJar, this is the directory in the standard WebJar classpath location, usually the same as the Maven artifact ID
     * @return The version of the WebJar, if found, otherwise {@code null}
     * @see WebJarVersionLocator
     */
    @Nullable
    public String version(final String webJarName) {
        final String cacheKey = CACHE_KEY_PREFIX + webJarName;
        final Optional<String> optionalVersion = cache.computeIfAbsent(cacheKey, (key) -> {
            final Properties properties = new Properties();

            // Try NPM-style WebJar first
            try (InputStream resource = LOADER.getResourceAsStream(PROPERTIES_ROOT + NPM + webJarName + POM_PROPERTIES)) {
                if (resource != null) {
                    properties.load(resource);
                }
            } catch (IOException ignored) {
                // ignore and try next format
            }

            // If no properties were loaded from the NPM path, try the PLAIN WebJar path
            if (properties.isEmpty()) {
                try (InputStream resource = LOADER.getResourceAsStream(PROPERTIES_ROOT + PLAIN + webJarName + POM_PROPERTIES)) {
                    if (resource != null) {
                        properties.load(resource);
                    }
                } catch (IOException ignored) {
                    // ignore
                }
            }

            if (!properties.isEmpty()) {
                final String version = properties.getProperty("version");
                // Sometimes a webjar version is not the same as the Maven artifact version
                if (version != null) {
                    if (hasResourcePath(webJarName, version)) {
                        return Optional.of(version);
                    }
                    if (version.contains("-")) {
                        String versionBeforeDash = version.substring(0, version.indexOf("-"));
                        // some webjars remove the dash and everything after in the path
                        if (hasResourcePath(webJarName, versionBeforeDash)) {
                            return Optional.of(versionBeforeDash);
                        }
                        // and some webjars remove everything before the dash in the path
                        else if (hasResourcePath(webJarName, version.substring(version.indexOf("-") + 1))) {
                            return Optional.of(version);
                        }
                    }
                }
            }

            return Optional.empty();
        });

        return optionalVersion.orElse(null);
    }

    /**
     * This method tries to determine the groupId for a WebJar in the classpath.
     *
     * <p>For official WebJars, the version lookup is performed by checking for a {@code pom.properties} file for either {@link WebJarVersionLocator#NPM}
     * or {@link WebJarVersionLocator#PLAIN} WebJars within {@code META-INF/maven}. The lookup result is cached.
     *
     * <p>Custom WebJars can be registered by using a {@code webjars-locator.properties} file. See {@link WebJarVersionLocator} for details.
     *
     * @param webJarName The name of the WebJar, this is the directory in the standard WebJar classpath location, usually the same as the Maven artifact ID
     * @return The groupId of the WebJar, if found, otherwise {@code null}
     * @see WebJarVersionLocator
     */
    @Nullable
    public String groupId(final String webJarName) {
        final String cacheKey = GROUPID_CACHE_KEY_PREFIX + webJarName;
        final Optional<String> optionalGroupId = cache.computeIfAbsent(cacheKey, (key) -> {
            // Try NPM-style WebJar first
            try (InputStream resource = LOADER.getResourceAsStream(PROPERTIES_ROOT + NPM + webJarName + POM_PROPERTIES)) {
                if (resource != null) {
                    return Optional.of("org.webjars.npm");
                }
            } catch (IOException ignored) {
                // ignore
            }

            // Try PLAIN WebJar
            try (InputStream resource = LOADER.getResourceAsStream(PROPERTIES_ROOT + PLAIN + webJarName + POM_PROPERTIES)) {
                if (resource != null) {
                    return Optional.of("org.webjars");
                }
            } catch (IOException ignored) {
                // ignore
            }

            return Optional.empty();
        });

        return optionalGroupId.orElse(null);
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

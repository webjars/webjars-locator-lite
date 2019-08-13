package org.webjars;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.Resource;
import io.github.classgraph.ResourceList;
import io.github.classgraph.ScanResult;

import java.net.URI;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Locate WebJar assets. The class is thread safe.
 */
public class WebJarAssetLocator {

    /**
     * The webjar package name.
     */
    public static final String WEBJARS_PACKAGE = "META-INF.resources.webjars";

    /**
     * The path to where webjar resources live.
     */
    public static final String WEBJARS_PATH_PREFIX = "META-INF/resources/webjars";

    private static Pattern WEBJAR_EXTRACTOR_PATTERN = Pattern.compile(WEBJARS_PATH_PREFIX + "/([^/]*)/([^/]*)/(.*)$");

    static class WebJarInfo {
        final String version;
        final URI uri;
        final List<String> contents;

        public WebJarInfo(final String version, final URI uri, final List<String> contents) {
            this.version = version;
            this.uri = uri;
            this.contents = contents;
        }
    }

    protected final Map<String, WebJarInfo> allWebJars;

    protected static ResourceList webJarResources(final String webJarName, final ResourceList resources) {
        return resources.filter(new ResourceList.ResourceFilter() {
            @Override
            public boolean accept(Resource resource) {
                return resource.getPath().startsWith(WEBJARS_PATH_PREFIX + "/" + webJarName + "/");
            }
        });
    }

    protected static String webJarVersion(final String webJarName, final ResourceList resources) {
        if (resources.isEmpty()) {
            return null;
        }
        else {
            final String aPath = resources.get(0).getPath();
            final String prefix = WEBJARS_PATH_PREFIX + "/" + webJarName + "/";
            if (aPath.startsWith(prefix)) {
                final String withoutName = aPath.substring(prefix.length());
                try {
                    final String maybeVersion = withoutName.substring(0, withoutName.indexOf("/"));
                    ResourceList withMaybeVersion = resources.filter(new ResourceList.ResourceFilter() {
                        @Override
                        public boolean accept(Resource resource) {
                            return resource.getPath().startsWith(prefix + maybeVersion + "/");
                        }
                    });

                    if (withMaybeVersion.size() == resources.size()) {
                        return maybeVersion;
                    } else {
                        return null;
                    }
                }
                catch (Exception e) {
                    return null;
                }
            }
            else {
                return null;
            }
        }
    }

    protected static Map<String, WebJarInfo> findWebJars(ScanResult scanResult) {
        Map<String, WebJarInfo> allWebJars = new HashMap<>();

        for (Resource resource : scanResult.getAllResources()) {
            final String noPrefix = resource.getPath().substring(WEBJARS_PATH_PREFIX.length() + 1);
            final String webJarName = noPrefix.substring(0, noPrefix.indexOf("/"));
            WebJarInfo webJarInfo = allWebJars.get(webJarName);
            if (webJarInfo == null) {
                final ResourceList webJarResources = webJarResources(webJarName, scanResult.getAllResources());
                final String maybeWebJarVersion = webJarVersion(webJarName, webJarResources);
                webJarInfo = new WebJarInfo(maybeWebJarVersion, resource.getClasspathElementURI(), webJarResources.getPaths());
                allWebJars.put(webJarName, webJarInfo);
            }
        }

        return allWebJars;
    }

    /**
     * @param path The full WebJar path
     * @return A WebJar tuple (Entry) with key = id and value = version
     */
    public static Entry<String, String> getWebJar(String path) {

        Matcher matcher = WEBJAR_EXTRACTOR_PATTERN.matcher(path);
        if (matcher.find()) {
            String id = matcher.group(1);
            String version = matcher.group(2);
            return new AbstractMap.SimpleEntry<>(id, version);
        } else {
            // not a legal WebJar file format
            return null;
        }
    }

    private Map<String, WebJarInfo> scanForWebJars(ClassGraph classGraph) {
        try(ScanResult scanResult =  classGraph.whitelistPaths(WEBJARS_PATH_PREFIX).scan()) {
            return findWebJars(scanResult);
        }
    }

    public WebJarAssetLocator() {
        allWebJars = scanForWebJars(new ClassGraph());
    }

    public WebJarAssetLocator(final ClassLoader classLoader) {
        allWebJars = scanForWebJars(new ClassGraph().overrideClassLoaders(classLoader));
    }

    public WebJarAssetLocator(final Map<String, WebJarInfo> allWebJars) {
        this.allWebJars = allWebJars;
    }

    private String throwNotFoundException(final String partialPath) {
        throw new NotFoundException(
                partialPath
                        + " could not be found. Make sure you've added the corresponding WebJar and please check for typos."
        );
    }

    private String throwMultipleMatchesException(final String partialPath, final List<String> matches) {
        throw new MultipleMatchesException(
                "Multiple matches found for "
                        + partialPath
                        + ". Please provide a more specific path, for example by including a version number.", matches);
    }

    /**
     * Given a distinct path within the WebJar index passed in return the full
     * path of the resource.
     *
     * @param partialPath the path to return e.g. "jquery.js" or "abc/someother.js".
     *                    This must be a distinct path within the index passed in.
     * @return a fully qualified path to the resource.
     */
    public String getFullPath(final String partialPath) {
        List<String> paths = new ArrayList<>();

        for(String webJarName : allWebJars.keySet()) {
            try {
                paths.add(getFullPath(webJarName, partialPath));
            }
            catch (NotFoundException e) {
                // ignored
            }
        }

        if (paths.size() == 0) {
            throwNotFoundException(partialPath);
        }
        else if (paths.size() > 1) {
            throwMultipleMatchesException(partialPath, paths);
        }

        return paths.get(0);
    }

    /**
     * Returns the full path of an asset within a specific WebJar
     *
     * @param webjar      The id of the WebJar to search
     * @param partialPath The partial path to look for
     * @return a fully qualified path to the resource
     */
    public String getFullPath(final String webjar, final String partialPath) {
        List<String> paths = new ArrayList<>();

        for (String path : allWebJars.get(webjar).contents) {
            if (path.endsWith(partialPath)) {
                paths.add(path);
            }
        }

        if (paths.size() == 0) {
            throwNotFoundException(partialPath);
        }
        else if (paths.size() > 1) {
            throwMultipleMatchesException(partialPath, paths);
        }

        return paths.get(0);
    }

    /**
     * Returns the full path of an asset within a specific WebJar
     *
     * @param webJarName      The id of the WebJar to search
     * @param exactPath   The exact path of the file within the WebJar
     * @return a fully qualified path to the resource
     */
    public String getFullPathExact(final String webJarName, final String exactPath) {
        final String maybeVersion = getWebJars().get(webJarName);

        String fullPath;
        if (maybeVersion != null) {
            fullPath = WEBJARS_PATH_PREFIX + "/" + webJarName + "/" + maybeVersion + "/" + exactPath;
        }
        else {
            fullPath = WEBJARS_PATH_PREFIX + "/" + webJarName + "/" + exactPath;
        }

        if (allWebJars.get(webJarName).contents.contains(fullPath)) {
            return fullPath;
        }

        return null;
    }

    public Set<String> listAssets() {
        return listAssets("");
    }

    /**
     * List assets within a folder.
     *
     * @param folderPath the root path to the folder.
     * @return a set of folder paths that match.
     */
    public Set<String> listAssets(final String folderPath) {
        Set<String> assets = new HashSet<>();

        final String prefix = WEBJARS_PATH_PREFIX + (!folderPath.startsWith("/") ? "/" : "") + folderPath;
        for (final WebJarInfo webJarInfo : allWebJars.values()) {
            for (final String path : webJarInfo.contents) {
                if (path.startsWith(folderPath) || path.startsWith(prefix)) {
                    assets.add(path);
                }
            }
        }

        return assets;
    }

    /**
     * @return A map of the WebJars based on the files in the CLASSPATH where the key is the artifactId and the value is the version
     */
    public Map<String, String> getWebJars() {
        Map<String, String> webJars = new HashMap<>();
        for (String webJarName : allWebJars.keySet()) {
            webJars.put(webJarName, allWebJars.get(webJarName).version);
        }

        return webJars;
    }

}

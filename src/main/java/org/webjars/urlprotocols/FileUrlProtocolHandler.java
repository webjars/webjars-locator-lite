package org.webjars.urlprotocols;

import java.io.File;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.webjars.WebJarAssetLocator;

public class FileUrlProtocolHandler implements UrlProtocolHandler {

    private static final int MAX_DIRECTORY_DEPTH = 32;

    @Override
    public boolean accepts(String protocol) {
        return "file".equals(protocol);
    }

    @Override
    public Set<String> getAssetPaths(URL url, Pattern filterExpr, ClassLoader... classLoaders) {
        final Set<String> assetPaths = new HashSet<String>();
        final File file;
        file = new File(url.getPath());
        final Set<String> paths = listFiles(file, filterExpr);
        assetPaths.addAll(paths);

        return assetPaths;
    }

    /*
     * Recursively search all directories for relative file paths matching `filterExpr`.
     */
    private static Set<String> listFiles(final File file, final Pattern filterExpr) {
        final Set<String> aggregatedChildren = new HashSet<String>();
        aggregateChildren(file, file, aggregatedChildren, filterExpr, 0);
        return aggregatedChildren;
    }

    private static void aggregateChildren(final File rootDirectory, final File file, final Set<String> aggregatedChildren, final Pattern filterExpr, final int level) {
        if (file.isDirectory()) {
            if (level > MAX_DIRECTORY_DEPTH) {
                throw new IllegalStateException("Got deeper than " + MAX_DIRECTORY_DEPTH + " levels while searching " + rootDirectory);
            }

            for (final File child : file.listFiles()) {
                aggregateChildren(rootDirectory, child, aggregatedChildren, filterExpr, level + 1);
            }
        } else {
            aggregateFile(file, aggregatedChildren, filterExpr);
        }
    }

    private static void aggregateFile(final File file, final Set<String> aggregatedChildren, final Pattern filterExpr) {
        final String path = file.getPath().replace('\\', '/');
        final String relativePath = path.substring(path.indexOf(WebJarAssetLocator.WEBJARS_PATH_PREFIX));
        if (filterExpr.matcher(relativePath).matches()) {
            aggregatedChildren.add(relativePath);
        }
    }
}

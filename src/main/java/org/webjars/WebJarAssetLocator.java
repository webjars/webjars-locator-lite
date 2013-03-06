package org.webjars;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.reflections.Configuration;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

/**
 * Locate WebJar assets
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

    /*
     * FIXME: Package protected convenience method to return all of the resource
     * paths filtered given an expression and a list of class loaders. This
     * method should ideally be private but the @AssetLocator is presently using
     * it.
     */
    static Set<String> getAssetPaths(Pattern filterExpr,
            ClassLoader... classLoaders) {
        final Configuration config = new ConfigurationBuilder().addUrls(
                ClasspathHelper.forPackage(WEBJARS_PACKAGE, classLoaders))
                .setScanners(new ResourcesScanner());

        final Reflections reflections = new Reflections(config);

        return reflections.getStore().getResources(filterExpr);
    }

    /**
     * Given a distinct path within the WebJar index passed in return the full
     * path of the resource.
     * 
     * @param partialPath
     *            the path to return e.g. "jquery.js" or "abc/someother.js".
     *            This must be a distinct path within the index passed in.
     * @return a fully qualified path to the resource.
     */
    public static String getFullPath(
            final SortedMap<String, String> fullPathIndex,
            final String partialPath) {

        final String reversePartialPath = reversePath(partialPath);

        final SortedMap<String, String> fullPathTail = fullPathIndex
                .tailMap(reversePartialPath);

        if (fullPathTail.size() == 0) {
            throw new IllegalArgumentException(
                    partialPath
                            + " could not be found. Make sure you've added the corresponding WebJar and please check for typos.");
        }

        final Iterator<Entry<String, String>> fullPathTailIter = fullPathTail
                .entrySet().iterator();
        final String fullPath = fullPathTailIter.next().getValue();

        if (fullPathTailIter.hasNext()
                && fullPathTailIter.next().getKey()
                        .startsWith(reversePartialPath)) {
            throw new IllegalArgumentException(
                    "Multiple matches found for "
                            + partialPath
                            + ". Please provide a more specific path, for example by including a version number.");
        }

        return fullPath;
    }

    /**
     * Return a map that can be used to perform index lookups of partial file
     * paths. This index constitutes a key that is the reverse form of the path
     * it relates to. Thus if a partial lookup needs to be performed from the
     * rightmost path components then the key to access can be expressed easily
     * e.g. the path "a/b" would be the map tuple "b/a" -> "a/b". If we need to
     * look for an asset named "a" without knowing the full path then we can
     * perform a partial lookup on the sorted map.
     * 
     * @param filterExpr
     *            the regular expression to be used to filter resources that
     *            will be included in the index.
     * @param classLoaders
     *            the class loaders to be considered for loading the resources
     *            from.
     * @return the index.
     */
    public static SortedMap<String, String> getFullPathIndex(
            final Pattern filterExpr, final ClassLoader... classLoaders) {

        final Set<String> assetPaths = getAssetPaths(filterExpr, classLoaders);

        final SortedMap<String, String> assetPathIndex = new TreeMap<String, String>();
        for (final String assetPath : assetPaths) {
            assetPathIndex.put(reversePath(assetPath), assetPath);
        }

        return assetPathIndex;
    }

    private static String reversePath(String assetPath) {
        final String[] assetPathComponents = assetPath.split("/");
        final StringBuilder reversedAssetPath = new StringBuilder();
        for (int i = assetPathComponents.length - 1; i >= 0; --i) {
            if (reversedAssetPath.length() > 0) {
                reversedAssetPath.append('/');
            }
            reversedAssetPath.append(assetPathComponents[i]);
        }
        return reversedAssetPath.toString();
    }
}

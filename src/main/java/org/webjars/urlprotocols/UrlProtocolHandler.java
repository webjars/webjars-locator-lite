package org.webjars.urlprotocols;

import java.net.URL;
import java.util.Set;
import java.util.regex.Pattern;

public interface UrlProtocolHandler {

    boolean accepts(String protocol);

    /**
     * Returns a list of all asset paths found in resource
     * @param url url to resource
     * @param filterExpr filter for the assets to find
     * @param classLoaders classloader to locate resources
     * @return Set of String containing found asset paths. In case the resource cannot be handled properly,
     * null is returned
     */
    Set<String> getAssetPaths(URL url, final Pattern filterExpr, final ClassLoader... classLoaders);
}

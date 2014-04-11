package org.webjars.urlprotocols;

import java.net.URL;
import java.util.Set;
import java.util.regex.Pattern;

public interface UrlProtocolHandler {

    boolean accepts(String protocol);
    Set<String> getAssetPaths(URL url, final Pattern filterExpr, final ClassLoader... classLoaders);
}

package org.webjars.urlprotocols;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.regex.Pattern;

public class JarUrlProtocolHandler implements UrlProtocolHandler {

    @Override
    public boolean accepts(String protocol) {
        return "jar".equals(protocol);
    }

    @Override
    public Set<String> getAssetPaths(URL url, Pattern filterExpr, ClassLoader... classLoaders) {
        HashSet<String> assetPaths = new HashSet<>();

        try {
            final JarURLConnection jarUrlConnection = (JarURLConnection) url.openConnection();

            for (JarEntry jarEntry : Collections.list(jarUrlConnection.getJarFile().entries())) {
                String assetPathCandidate = jarEntry.getName();
                if (!jarEntry.isDirectory() && filterExpr.matcher(assetPathCandidate).matches()) {
                    assetPaths.add(assetPathCandidate);
                }
            }

        }
        catch (IOException ignored) { }

        return assetPaths;
    }

}

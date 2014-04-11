package org.webjars.urlprotocols;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

import org.webjars.CloseQuietly;

public class JarUrlProtocolHandler implements UrlProtocolHandler {

    @Override
    public boolean accepts(String protocol) {
        return "jar".equals(protocol);
    }

    @Override
    public Set<String> getAssetPaths(URL url, Pattern filterExpr, ClassLoader... classLoaders) {
        final Set<String> assetPaths = new HashSet<String>();
        final JarFile jarFile;
        try {
            final String path = url.getPath();
            final File file = new File(URI.create(path.substring(0, path.indexOf("!"))));
            jarFile = new JarFile(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            final Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                final JarEntry entry = entries.nextElement();
                final String assetPathCandidate = entry.getName();
                if (!entry.isDirectory() && filterExpr.matcher(assetPathCandidate).matches()) {
                    assetPaths.add(assetPathCandidate);
                }
            }
        } finally {
            // Littering is bad for the environment.
            CloseQuietly.closeQuietly(jarFile);
        }

        return assetPaths;
    }

}

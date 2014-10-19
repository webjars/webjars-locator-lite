package org.webjars.urlprotocols;

import org.webjars.CloseQuietly;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.regex.Pattern;

public class FatJarUrlProtocolHandler implements UrlProtocolHandler {

    @Override
    public boolean accepts(final String protocol) {
        return "jar".equals(protocol);
    }

    @Override
    public Set<String> getAssetPaths(final URL url, final Pattern filterExpr,
            final ClassLoader... classLoaders) {

        final Set<String> assetPaths = new HashSet<String>();
        final String path = url.getPath();
        final String[] partialJarPaths = path.split("!");

        //check if we are acctauly usinf fat jar. if not ignore.
        if (partialJarPaths.length != 3)  { return null; }

        final JarFile jarFile;
        try {
            final File file = new File(URI.create(path.substring(0, path.indexOf("!"))));
            jarFile = new JarFile(file);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
        JarInputStream jarJarStream = null ;
        try {
            jarJarStream = new JarInputStream(jarFile.getInputStream(
                    jarFile.getEntry(partialJarPaths[1].substring(1))));

            JarEntry jarjarEntry = jarJarStream.getNextJarEntry();
            while (jarjarEntry !=null) {
                final String assetPathCandidate = jarjarEntry.getName();
                if (!jarjarEntry.isDirectory() && filterExpr.matcher(assetPathCandidate).matches()) {
                    assetPaths.add(assetPathCandidate);
                }
                jarjarEntry = jarJarStream.getNextJarEntry();
            }


        } catch (final IOException e) {
             throw new RuntimeException(e);
        } finally {
            // Littering is bad for the environment.
            CloseQuietly.closeQuietly(jarJarStream);
            CloseQuietly.closeQuietly(jarFile);
        }

        return assetPaths;
    }

}

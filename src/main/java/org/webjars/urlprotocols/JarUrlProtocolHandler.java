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
import java.util.jar.JarInputStream;
import java.util.regex.Pattern;

import org.webjars.CloseQuietly;
import org.webjars.WebJarAssetLocator;

public class JarUrlProtocolHandler implements UrlProtocolHandler {

    @Override
    public boolean accepts(String protocol) {
        return "jar".equals(protocol);
    }

    @Override
    public Set<String> getAssetPaths(URL url, Pattern filterExpr, ClassLoader... classLoaders) {
        final String[] partialJarPaths = url.getPath().split("!");

        if (partialJarPaths.length == 3)  { 
            return handleFatJar(url, filterExpr, classLoaders);
        } else {
            return handleJar(url, filterExpr, classLoaders);
        }
    }

    private Set<String> handleJar(URL url, Pattern filterExpr, ClassLoader[] classLoaders) {
        Set<String> assetPaths = new HashSet<String>();
        JarFile jarFile = null;
        try {
            final String path = url.getPath();
            final File file = new File(URI.create(path.substring(0,
                path.lastIndexOf("!/" + WebJarAssetLocator.WEBJARS_PATH_PREFIX))));
            jarFile = new JarFile(file);
            final Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                final JarEntry entry = entries.nextElement();
                final String assetPathCandidate = entry.getName();
                if (!entry.isDirectory() && filterExpr.matcher(assetPathCandidate).matches()) {
                    assetPaths.add(assetPathCandidate);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            // Littering is bad for the environment.
            CloseQuietly.closeQuietly(jarFile);
        }
        return assetPaths;
    }
    
    private Set<String> handleFatJar(URL url, Pattern filterExpr, ClassLoader[] classLoaders) {
        Set<String> assetPaths = new HashSet<String>();
        JarFile jarFile = null;
        JarInputStream jarJarStream = null;
        try {
            String path = url.getPath();
            String[] partialJarPaths = path.split("!");
            File file = new File(URI.create(path.substring(0, path.indexOf("!"))));
            jarFile = new JarFile(file);
            jarJarStream = new JarInputStream(jarFile.getInputStream(jarFile.getEntry(partialJarPaths[1].substring(1))));
            JarEntry jarjarEntry = jarJarStream.getNextJarEntry();
            while (jarjarEntry !=null) {
                String assetPathCandidate = jarjarEntry.getName();
                if (!jarjarEntry.isDirectory() && filterExpr.matcher(assetPathCandidate).matches()) {
                    assetPaths.add(assetPathCandidate);
                }
                jarjarEntry = jarJarStream.getNextJarEntry();
            }
            
            return assetPaths;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            // Littering is bad for the environment.
            CloseQuietly.closeQuietly(jarJarStream);
            CloseQuietly.closeQuietly(jarFile);
        }
    }
}

package org.webjars.urlprotocols;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.regex.Pattern;

import org.webjars.CloseQuietly;

public class JarUrlProtocolHandler implements UrlProtocolHandler {

    @Override
    public boolean accepts(String protocol) {
        return "jar".equals(protocol);
    }

    @Override
    public Set<String> getAssetPaths(URL url, Pattern filterExpr, ClassLoader... classLoaders) {
        HashSet<String> assetPaths = new HashSet<String>();
        String[] segments = url.getPath().split(".jar!/");
        JarFile jarFile = null;
        JarInputStream jarInputStream = null;
        
        try {
            for (int i = 0; i < segments.length - 1; i++) {
                String segment = segments[i] + ".jar";
                if (jarFile == null) {
                    File file = new File(URI.create(segment));
                    jarFile = new JarFile(file);
                    if (i == segments.length - 2) {
                        jarInputStream = new JarInputStream(new FileInputStream(file));
                    }
                } else {
                    jarInputStream = new JarInputStream(jarFile.getInputStream(jarFile.getEntry(segment)));
                }
            }
            
            JarEntry jarEntry = jarInputStream.getNextJarEntry();
            while (jarEntry !=null) {
                String assetPathCandidate = jarEntry.getName();
                if (!jarEntry.isDirectory() && filterExpr.matcher(assetPathCandidate).matches()) {
                    assetPaths.add(assetPathCandidate);
                }
                jarEntry = jarInputStream.getNextJarEntry();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            CloseQuietly.closeQuietly(jarFile);
            CloseQuietly.closeQuietly(jarInputStream);
        }
        
        return assetPaths;
    }
}

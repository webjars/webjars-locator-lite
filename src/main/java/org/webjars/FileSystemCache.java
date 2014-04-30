package org.webjars;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

import org.webjars.WebJarExtractor.Cacheable;

import static org.webjars.CloseQuietly.closeQuietly;

/**
 * A cache backed by a file on the filesystem.
 * The cache expects that every single object in the cache will be touched on each use.  This means, when it saves,
 * if one object hasn't been touched, that one won't be saved.
 */
public class FileSystemCache implements WebJarExtractor.Cache {

    private final File cache;

    private Map<String, Cacheable> onFile;
    private Map<String, Cacheable> touched;
    private boolean dirty;

    public FileSystemCache(File cache) throws IOException {
        this.cache = cache;
        reset();
    }

    @Override
    public boolean isUpToDate(String key, Cacheable cacheable) {
        // First check touched
        Cacheable t = touched.get(key);
        if (t != null) {
            return cacheable.equals(t);
        } else {
            Cacheable cached = onFile.get(key);
            if (cached != null) {
                touched.put(key, cached);
            }
            return cacheable.equals(cached);
        }
    }

    @Override
    public void put(String key, Cacheable cacheable) {
        touched.put(key, cacheable);
        dirty = true;
    }

    public void save() throws IOException {
        if (dirty || onFile.size() != touched.size()) {
            Writer writer = new OutputStreamWriter(new FileOutputStream(cache), "UTF-8");
            try {
                for (Map.Entry<String, Cacheable> item : touched.entrySet()) {
                    writer.write(item.getKey() + ":" + item.getValue().getLastModified() + ":" + item.getValue().getPath() + "\n");
                }
                writer.flush();
            } finally {
                closeQuietly(writer);
            }
        }
        onFile = touched;
        touched = new HashMap<String, Cacheable>();
        dirty = false;
    }

    public void reset() throws IOException {
        onFile = new HashMap<String, Cacheable>();
        if (cache.exists()) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(cache), "UTF-8"));
            try {
                String line = reader.readLine();
                while (line != null) {
                    if (!line.isEmpty()) {
                        String[] splitted = line.split(":", 3);
                        if (splitted.length == 3) {
                            String key = splitted[0];
                            String lastModified = splitted[1];
                            String path = splitted[2];
                            try {
                                long lm = Long.parseLong(lastModified);
                                onFile.put(key, new Cacheable(path, lm));
                            } catch (NumberFormatException e) {
                                // Ignore
                            }
                        }
                    }
                    line = reader.readLine();
                }
            } finally {
                closeQuietly(reader);
            }
        }
        touched = new HashMap<String, Cacheable>();
        dirty = false;
    }

}

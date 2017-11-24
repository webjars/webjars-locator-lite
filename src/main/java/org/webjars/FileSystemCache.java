package org.webjars;

import org.webjars.WebJarExtractor.Cacheable;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

    /**
     * Create a file system cache.
     *
     * @param cache The file to load and store the cache to.
     * @throws IOException If an error occurs.
     */
    public FileSystemCache(File cache) throws IOException {
        this.cache = cache;
        reset();
    }

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(String key, Cacheable cacheable) {
        touched.put(key, cacheable);
        dirty = true;
    }

    /**
     * Save the cache.
     *
     * This will check only save entries that have been touched since last reset or saved, that is, either isUpToDate
     * has been called on them, or they have explicitly put in the cache.
     *
     * @throws IOException If an error occurred.
     */
    public void save() throws IOException {
        if (dirty || onFile.size() != touched.size()) {
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(cache), "UTF-8")) {
                for (Map.Entry<String, Cacheable> item : touched.entrySet()) {
                    writer.write(item.getKey() + ":" + item.getValue().getLastModified() + ":" + item.getValue().getPath() + "\n");
                }
                writer.flush();
            }
        }
        onFile = touched;
        touched = new HashMap<String, Cacheable>();
        dirty = false;
    }

    /**
     * Reset the cache.
     *
     * This forgets all touched files, and loads the cache from disk.
     *
     * @throws IOException If an error occurred.
     */
    public void reset() throws IOException {
        onFile = new HashMap<String, Cacheable>();
        if (cache.exists()) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(cache), "UTF-8"))) {
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
            }
        }
        touched = new HashMap<String, Cacheable>();
        dirty = false;
    }

    /**
     * Get all the files that this cache knows about that haven't been touched since the last save, and exist, relative
     * to the given base directory.
     *
     * This exists to allow things that use the file system cache to delete files that weren't extracted from a webjar
     * in a subsequent run.
     *
     * @param baseDir The given base directory.
     * @return The set of files that are untouched.
     * @throws IOException If an error occurred.
     */
    public Set<File> getExistingUntouchedFiles(File baseDir) throws IOException {
        // Make sure to use canonical paths, this ensures case sensitivity changes don't cause problems.
        Set<File> untouchedFiles = new HashSet<>();
        for (String key: onFile.keySet()) {
            File file = new File(baseDir, key);
            if (file.exists()) {
                untouchedFiles.add(file.getCanonicalFile());
            }
        }

        for (String key: touched.keySet()) {
            File file = new File(baseDir, key);
            if (file.exists()) {
                untouchedFiles.remove(file.getCanonicalFile());
            }
        }

        return untouchedFiles;
    }

}

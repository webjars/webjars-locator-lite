package org.webjars;

import org.junit.After;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;
import static org.webjars.WebJarExtractor.Cacheable;

public class FileSystemCacheTest {

    private File tmpFile;

    private Cacheable c1 = new Cacheable("c1", 123);
    private Cacheable c2 = new Cacheable("c2", 456);
    private Cacheable c3 = new Cacheable("c3", 789);

    @Test
    public void cacheShouldPersistNewEntries() throws Exception {
        FileSystemCache cache = new FileSystemCache(createTmpFile());
        cache.put("foo", c1);
        cache.put("bar", c2);
        cache.save();
        cache = new FileSystemCache(createTmpFile());
        assertTrue(cache.isUpToDate("foo", c1));
        assertTrue(cache.isUpToDate("bar", c2));
    }

    @Test
    public void cacheShouldFailOnNonExistentEntries() throws Exception {
        FileSystemCache cache = new FileSystemCache(createTmpFile());
        cache.put("foo", c1);
        cache.save();
        cache = new FileSystemCache(createTmpFile());
        assertFalse(cache.isUpToDate("bar", c2));
    }

    @Test
    public void cacheShouldNotRewriteFileIfAllFilesTouched() throws Exception {
        FileSystemCache cache = new FileSystemCache(createTmpFile());
        cache.put("foo", c1);
        cache.put("bar", c2);
        cache.save();
        cache = new FileSystemCache(createTmpFile());
        tmpFile.delete();
        assertTrue(cache.isUpToDate("foo", c1));
        assertTrue(cache.isUpToDate("bar", c2));
        cache.save();
        assertFalse(tmpFile.exists());
    }

    @Test
    public void cacheShouldDeleteUntouchedEntries() throws Exception {
        FileSystemCache cache = new FileSystemCache(createTmpFile());
        cache.put("foo", c1);
        cache.put("bar", c2);
        cache.save();
        cache = new FileSystemCache(createTmpFile());
        assertTrue(cache.isUpToDate("foo", c1));
        cache.save();
        assertFalse(cache.isUpToDate("bar", c2));
    }

    private File createTmpFile() throws Exception {
        if (tmpFile == null) {
            tmpFile = File.createTempFile("filesystemcache-", ".cache");
        }
        return tmpFile;
    }

    @After
    public void deleteTmpFile() {
        if (tmpFile != null) {
            tmpFile.delete();
        }
    }
}

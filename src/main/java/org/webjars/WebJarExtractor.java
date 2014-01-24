package org.webjars;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import static org.webjars.CloseQuietly.closeQuietly;
import static org.webjars.WebJarAssetLocator.WEBJARS_PATH_PREFIX;

/**
 * Utility for extracting WebJars onto the filesystem. The extractor also recognises the node_modules
 * convention used by WebJars. node_modules are a special place within a WebJar that contain the assets
 * required in an environment conforming to the Node API for require.
 */
public class WebJarExtractor {

    /**
     * The node_modules directory prefix as a convenience.
     */
    public static final String PACKAGE_JSON_NAME = "\"name\"";
    public static final String PACKAGE_JSON = "package.json";

	private static final Logger log = LoggerFactory.getLogger(WebJarExtractor.class);

	private final Cache cache;
	private final ClassLoader classLoader;

	public WebJarExtractor() {
		this(WebJarExtractor.class.getClassLoader());
	}

	public WebJarExtractor(ClassLoader classLoader) {
		this(NO_CACHE, classLoader);
	}

	public WebJarExtractor(Cache cache, ClassLoader classLoader) {
		this.cache = cache;
		this.classLoader = classLoader;
	}

    /**
     * Extract all WebJars.
     *
     * @param to The directory to extract to.
     */
    public void extractAllWebJarsTo(File to) throws IOException {
        extractWebJarsTo(null, false, to);
    }

    /**
     * Extract the given WebJar to the given location.
     *
     * The WebJar will be extracted, without its version in the path, to the given directory.
     *
     * @param name The name of the WebJar to extract.
     * @param to The location to extract it to. All WebJars will be merged into this location.
     */
    public void extractWebJarTo(String name, File to) throws IOException {
        extractWebJarsTo(name, false, to);
    }

    /**
     * Extract the node_modules of all WebJars and merge them into the same folder.
     *
     * @param to The location to extract it to. All WebJars will be merged into this location.
     */
    public void extractAllNodeModulesTo(File to) throws IOException {
        extractWebJarsTo(null, true, to);
    }

    /**
     * A generalised form for extracting WebJars.
     *
     * @param name          If null then all WebJars are extracted, otherwise the name of a single WebJars.
     * @param nodeModules   If true then only WebJars containing a package.json at the root will be extracted.
     * @param to            The location to extract it to. All WebJars will be merged into this location.
     */
    private void extractWebJarsTo(String name, boolean nodeModules, File to) throws IOException {
		String fullPath = WEBJARS_PATH_PREFIX + "/";
        String searchPath;
        if (name != null) {
            searchPath = fullPath + name + "/";
        } else {
            searchPath = fullPath;
        }
        for (URL url: WebJarAssetLocator.listParentURLsWithResource(new ClassLoader[] {classLoader}, searchPath)) {
			if ("jar".equals(url.getProtocol())) {

				String urlPath = url.getPath();
				File file = new File(URI.create(urlPath.substring(0, urlPath.indexOf("!"))));
				log.debug("Loading webjar from {}", file);
				JarFile jarFile = new JarFile(file);

				try {
                    boolean filteredNodeModule = !nodeModules;
                    boolean matched = !nodeModules;
                    File matchedTo = to;
                    Enumeration<JarEntry> entries = jarFile.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        if (!entry.isDirectory() && entry.getName().startsWith(fullPath)) {
                            String webJarPath = entry.getName().substring(fullPath.length());
                            String[] nameVersion = webJarPath.split("/", 3);
                            if (nameVersion.length == 3) {
                                if (!filteredNodeModule) {
                                    String moduleId = getJarNodeModuleIdEntry(
                                            jarFile,
                                            fullPath + nameVersion[0] + "/" + nameVersion[1] + "/" + PACKAGE_JSON
                                            );
                                    if (moduleId != null) {
                                        matchedTo = new File(to, moduleId);
                                        matched = true;
                                    }
                                    filteredNodeModule = true;
                                }
                                if (matched) {
                                    String relativeName = nameVersion[2];
                                    File copyTo = new File(matchedTo, relativeName);
                                    copyJarEntry(jarFile, entry, copyTo, relativeName);
                                }
                            } else {
                                log.debug("Found file entry {} where webjar version directory was expected in {}",
                                        webJarPath, url);
                            }
                        }
                    }
				} finally {
					closeQuietly(jarFile);
				}
			} else if ("file".equals(url.getProtocol())) {
				File file;
				try {
					file = new File(url.toURI());
				} catch (URISyntaxException e) {
					throw new RuntimeException(e);
				}
				log.debug("Found file system webjar: {}", file);
				File[] webjars = file.listFiles();
				if (webjars != null) {
					for (File webjar: webjars) {
						if (webjar.isDirectory()) {
							File[] versions = webjar.listFiles();
							if (versions != null) {
								for (File version: versions) {
									if (version.isDirectory()) {
                                        boolean matched = !nodeModules;
                                        File matchedTo = to;
                                        if (nodeModules) {
                                            String moduleId = getFileNodeModuleIdEntry(new File(version, PACKAGE_JSON));
                                            if (moduleId != null) {
                                                matchedTo = new File(to, moduleId);
                                                matched = true;
                                            }
                                        }
                                        if (matched) {
										    copyDirectory(version, matchedTo, webjar.getName());
                                        }
									} else {
										log.debug("Filesystem webjar version {} is not a directory", version);
									}
								}
							} else {
								log.debug("Filesystem webjar has no versions: {}", webjar);
							}
						} else {
							log.debug("Filesystem webjar {} is not a directory", webjar);
						}
					}
				} else {
					log.debug("Filesystem webjar has no webjars: {}", file);
				}
			}
		}
	}

	private void ensureIsDirectory(File dir) {
		if (dir.exists() && !dir.isDirectory()) {
			log.debug("Destination directory is not a directory, deleting {}", dir);
			// Delete the old file
			boolean isDeleted = dir.delete();
            if (!isDeleted)     {
                log.debug("Destination directory {} wasn't deleted", dir);
            }
		}
        boolean created = dir.mkdirs();
        if (!created) {
            log.debug("Destination directory {} didn't need creation", dir);
        }
	}

	private void copyDirectory(File dir, File to, String key) throws IOException {
		File[] files = dir.listFiles();
		if (files != null) {
			for (File file: files) {
				File copyTo = new File(to, file.getName());

				String relativeName;
				if (key.isEmpty()) {
					relativeName = file.getName();
				} else {
					relativeName = key + "/" + file.getName();
				}

				if (file.isDirectory()) {
					copyDirectory(file, copyTo, relativeName);
				} else {
					Cacheable forCache = new Cacheable(file.getPath(), file.lastModified());
					log.debug("Checking whether {} is up to date at {}", relativeName, copyTo);
					// Check for modification
					if (!copyTo.exists() || !cache.isUpToDate(relativeName, forCache)) {

						log.debug("Up to date check failed, copying {} to {}", relativeName, copyTo);
						ensureIsDirectory(copyTo.getParentFile());
						copyAndClose(new FileInputStream(file), copyTo);
						cache.put(relativeName, forCache);
					}
				}
			}
		}
	}

	private void copyJarEntry(JarFile jarFile, JarEntry entry, File copyTo, String key) throws IOException {
		Cacheable forCache = new Cacheable(entry.getName(), entry.getTime());

		log.debug("Checking whether {} is up to date at {}", entry.getName(), copyTo);

		// Check for modification
		if (!copyTo.exists() || !cache.isUpToDate(key, forCache)) {

			log.debug("Up to date check failed, copying {} to {}", entry.getName(), copyTo);
			ensureIsDirectory(copyTo.getParentFile());
			copyAndClose(jarFile.getInputStream(entry), copyTo);
			cache.put(key, forCache);
		}
	}

    private String getJarNodeModuleIdEntry(JarFile jarFile, String moduleIdPath) throws IOException {
        String moduleId = null;
        ZipEntry entry = jarFile.getEntry(moduleIdPath);
        if (entry != null) {
            String packageJson = copyAndClose(jarFile.getInputStream(entry));
            moduleId = getJsonNodeModuleId(packageJson);
        }
        return moduleId;
    }

    private String getFileNodeModuleIdEntry(File packageJsonFile) throws IOException {
        String moduleId = null;
        if (packageJsonFile.exists()) {
            String packageJson = copyAndClose(new FileInputStream(packageJsonFile));
            moduleId = getJsonNodeModuleId(packageJson);
        }
        return moduleId;
    }

    private String getJsonNodeModuleId(String packageJson) {
        String moduleId = null;
        int namePosn = packageJson.indexOf(PACKAGE_JSON_NAME);
        if (namePosn > -1) {
            int moduleIdPosn = namePosn + PACKAGE_JSON_NAME.length();
            while (moduleIdPosn < packageJson.length() && Character.isWhitespace(packageJson.charAt(moduleIdPosn)))
                ++moduleIdPosn;
            if (moduleIdPosn < packageJson.length() && packageJson.charAt(moduleIdPosn) == ':') {
                ++moduleIdPosn;
                while (moduleIdPosn < packageJson.length() && Character.isWhitespace(packageJson.charAt(moduleIdPosn)))
                    ++moduleIdPosn;
                if (moduleIdPosn < packageJson.length() && packageJson.charAt(moduleIdPosn) == '"') {
                    ++moduleIdPosn;
                    StringBuilder sb = new StringBuilder();
                    while (moduleIdPosn < packageJson.length() && packageJson.charAt(moduleIdPosn) != '"') {
                        sb.append(packageJson.charAt(moduleIdPosn++));
                    }
                    moduleId = sb.toString();
                }
            }
        }
        return moduleId;
    }

    /**
     * A cache for extracting WebJar assets.
     */
	public interface Cache {
		/**
		 * Whether the file is up to date.
		 *
		 * @param key The key to check.
		 * @param cacheable The cacheable to check.
		 * @return Whether the file is up to date.
		 */
		public boolean isUpToDate(String key, Cacheable cacheable);

		/**
		 * Put the given file in the cache.
		 *
		 * @param key The key to put it at.
		 * @param cacheable The cacheable.
		 */
		public void put(String key, Cacheable cacheable);
	}

	private static class NoCache implements Cache {
		public boolean isUpToDate(String key, Cacheable cacheable) {
			return false;
		}
		public void put(String key, Cacheable cacheable) {
		}
	}

	public static Cache NO_CACHE = new NoCache();

	/**
	 * An in memory cache.
	 */
	public static class MemoryCache implements Cache {

		private final Map<String, Cacheable> cache = new HashMap<String, Cacheable>();

		public boolean isUpToDate(String key, Cacheable cacheable) {
			return cacheable.equals(cache.get(key));
		}

		@Override
		public void put(String key, Cacheable cacheable) {
			cache.put(key, cacheable);
		}
	}


	public static final class Cacheable {
		private final String path;
		private final long lastModified;

		public Cacheable(String path, long lastModified) {
			this.path = path;
			this.lastModified = lastModified;
		}

		public String getPath() {
			return path;
		}

		public long getLastModified() {
			return lastModified;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			Cacheable cacheable = (Cacheable) o;

			return (lastModified == cacheable.lastModified) && path.equals(cacheable.path);

        }

		@Override
		public int hashCode() {
			int result = path.hashCode();
			result = 31 * result + (int) (lastModified ^ (lastModified >>> 32));
			return result;
		}
	}

	private static void copyAndClose(InputStream source, File to) throws IOException {
		OutputStream dest = new FileOutputStream(to);
		try {
			byte[] buffer = new byte[8192];
			int read = source.read(buffer);
			while (read > 0) {
				dest.write(buffer, 0, read);
				read = source.read(buffer);
			}
			dest.flush();
		} finally {
			closeQuietly(source);
			closeQuietly(dest);
		}

	}

	private static String copyAndClose(InputStream source) throws IOException {
		StringBuilder sb = new StringBuilder();
	    final Reader is = new InputStreamReader(source, "UTF-8");
		try {
			char[] buffer = new char[8192];
	        int read = is.read(buffer, 0, buffer.length);
            sb.append(buffer, 0, read);
		} finally {
			closeQuietly(source);
			closeQuietly(is);
		}
		return sb.toString();
	}

}

package org.webjars;

import org.apache.commons.lang3.SystemUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;

@RunWith(Parameterized.class)
public class WebJarFilePermissionsTest {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        if (SystemUtils.IS_OS_UNIX) {
            return Arrays.asList(new Object[][]{{true}, {false}});
        } else {
            System.err.println("Skipping permissions tests, since they can't be run on non UNIX OSes");
            return Collections.emptyList();
        }
    }
    private File extractDir;
    private File classpathDir;
    private URLClassLoader loader;
    private final boolean jarFile;
    private final String testName;

    public WebJarFilePermissionsTest(boolean jarFile) {
        this.jarFile = jarFile;
        this.testName = jarFile ? "JAR" : "Filesystem";
    }

    @Test
    public void all() throws Exception {
        assertPermissions("all", PosixFilePermission.values());
    }

    @Test
    public void execute() throws Exception {
        assertPermissions("execute", PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE, PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_EXECUTE,
                PosixFilePermission.OTHERS_READ, PosixFilePermission.OTHERS_EXECUTE);
    }

    @Test
    public void groupReadWrite() throws Exception {
        assertPermissions("groupreadwrite", PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_WRITE);
    }

    @Test
    public void ownerOnlyRead() throws Exception {
        assertPermissions("owneronlyread", PosixFilePermission.OWNER_READ);
    }

    @Test
    public void ownerReadWrite() throws Exception {
        assertPermissions("ownerreadwrite", PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.GROUP_READ, PosixFilePermission.OTHERS_READ);
    }

    private void assertPermissions(String file, PosixFilePermission... exp) throws IOException {
        Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(new File(extractDir, "permissions-jar/bin/" + file).toPath());
        Set<PosixFilePermission> expected = new HashSet<PosixFilePermission>(Arrays.asList(exp));
        for (PosixFilePermission e: expected) {
            if (!permissions.contains(e)) {
                fail("WebJar " + testName + " extraction test file " + file + " did not have expected permission " + e);
            }
        }
        for (PosixFilePermission e: permissions) {
            if (!expected.contains(e)) {
                fail("WebJar " + testName + " extraction test file " + file + " had unexpected permission " + e);
            }
        }
    }

    @Before
    public void extractWebJar() throws Exception {
        extractDir = WebJarExtractorTestUtils.createTmpDir();
        loader = new URLClassLoader(new URL[]{
                this.getClass().getClassLoader().getResource("permissions-jar.jar")
        });
        if (!jarFile) {
            // Extract the webjar to a directory, and then use that directory as the classpath, and then extract from
            // that. Obviously this assumes that the first extraction works
            classpathDir = WebJarExtractorTestUtils.createTmpDir();
            new WebJarExtractor(loader).extractWebJarTo("permissions-jar", classpathDir);
            File webjarDir = new File(classpathDir, "META-INF/resources/webjars/permissions-jar/1.0.0");
            loader.close();

            webjarDir.getParentFile().mkdirs();
            Files.move(new File(classpathDir, "permissions-jar").toPath(), webjarDir.toPath());
            loader = new URLClassLoader(new URL[]{
                    classpathDir.toURI().toURL()
            });
        }
        new WebJarExtractor(loader).extractWebJarTo("permissions-jar", extractDir);
    }

    @After
    public void deleteTmpDirectory() throws Exception {
        WebJarExtractorTestUtils.deleteDir(extractDir);
        WebJarExtractorTestUtils.deleteDir(classpathDir);
    }

    @After
    public void closeLoader() throws Exception {
        if (loader != null) {
            loader.close();
            loader = null;
        }
    }

}

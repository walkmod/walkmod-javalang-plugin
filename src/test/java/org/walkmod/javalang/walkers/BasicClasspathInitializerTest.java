package org.walkmod.javalang.walkers;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.walkmod.conf.entities.Configuration;
import org.walkmod.conf.entities.impl.ConfigurationImpl;

public class BasicClasspathInitializerTest {

    @Test
    public void when_no_package_in_src_dir_then_compiles_successfully() throws Exception {
        File repoDir = new File("src/test/resources/no_package");
        String userDir = System.getProperty("user.dir");
        try {

            if (repoDir.exists()) {
                FileUtils.deleteDirectory(repoDir);
            }
            repoDir.mkdirs();

            File srcDir = new File(repoDir, "src");
            srcDir.mkdir();

            FileUtils.write(new File(srcDir, "Foo.java"), "public class Foo{}");

            System.setProperty("user.dir", "src/test/resources/no_package");

            BasicClasspathInitializer action = new BasicClasspathInitializer(".", new DefaultJavaParser());
            Configuration conf = new ConfigurationImpl();

            action.init(conf);
            action.load();

            Object value = conf.getParameters().get("classLoader");

            Assert.assertNotNull(value);

            URLClassLoader urcl = (URLClassLoader) value;

            URL[] urls = urcl.getURLs();

            Assert.assertEquals(1, urls.length);

            File compilationDir = new File(new File(".walkmod"), "classes").getCanonicalFile();

            Assert.assertEquals(compilationDir.toURI().toURL().toString(), urls[0].toString());

            File[] contents = compilationDir.listFiles();

            Assert.assertEquals(1, contents.length);

            Assert.assertTrue(contents[0].getPath().endsWith("Foo.class"));

        } finally {
            FileUtils.deleteDirectory(repoDir);
            System.setProperty("user.dir", userDir);
        }

    }

    @Test
    public void when_package_in_src_dir_then_compiles_successfully() throws Exception {
        File repoDir = new File("src/test/resources/no_package");
        String userDir = System.getProperty("user.dir");
        try {

            if (repoDir.exists()) {
                FileUtils.deleteDirectory(repoDir);
            }
            repoDir.mkdirs();

            File srcDir = new File(repoDir, "src");
            srcDir.mkdir();

            File pkgDir = new File(srcDir, "bar");
            pkgDir.mkdir();

            FileUtils.write(new File(pkgDir, "Foo.java"), "package bar; public class Foo{}");

            System.setProperty("user.dir", "src/test/resources/no_package");

            BasicClasspathInitializer action = new BasicClasspathInitializer(".", new DefaultJavaParser());
            Configuration conf = new ConfigurationImpl();

            action.init(conf);
            action.load();

            Object value = conf.getParameters().get("classLoader");

            Assert.assertNotNull(value);

            URLClassLoader urcl = (URLClassLoader) value;

            URL[] urls = urcl.getURLs();

            Assert.assertEquals(1, urls.length);

            File compilationDir = new File(new File(".walkmod"), "classes").getCanonicalFile();

            Assert.assertEquals(compilationDir.toURI().toURL().toString(), urls[0].toString());

            File[] contents = compilationDir.listFiles();

            Assert.assertEquals(1, contents.length);

            Assert.assertTrue(contents[0].listFiles()[0].getPath().endsWith("Foo.class"));

        } finally {
            FileUtils.deleteDirectory(repoDir);
            System.setProperty("user.dir", userDir);
        }

    }
}

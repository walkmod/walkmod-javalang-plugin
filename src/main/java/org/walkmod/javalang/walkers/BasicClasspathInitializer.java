package org.walkmod.javalang.walkers;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.walkmod.conf.ConfigurationException;
import org.walkmod.conf.ConfigurationProvider;
import org.walkmod.conf.entities.Configuration;
import org.walkmod.javalang.ast.CompilationUnit;
import org.walkmod.javalang.compiler.Compiler;
import org.walkmod.walkers.Parser;

public class BasicClasspathInitializer implements ConfigurationProvider {

    private Configuration configuration;

    private String path;

    private Parser<CompilationUnit> parser;

    public BasicClasspathInitializer(String path, Parser<CompilationUnit> parser) {

        this.path = path;

        this.parser = parser;
    }

    @Override
    public void init(Configuration configuration) {
        this.configuration = configuration;
    }

    protected File[] getSourceFiles() throws Exception {
        File rootDir = new File(path).getCanonicalFile();
        Collection<File> files = FileUtils.listFiles(rootDir, new String[] { "java" }, true);
        
        File[] finalResult = new File[files.size()];
        files.toArray(finalResult);
        return finalResult;
    }

    @Override
    public void load() throws ConfigurationException {
        Compiler compiler = new Compiler();
        try {
            File compilationDir = new File(new File(".walkmod"), "classes").getCanonicalFile();
            if (compilationDir.exists()) {
                FileUtils.deleteDirectory(compilationDir);
            }
            compilationDir.mkdirs();

            compiler.compile(compilationDir, getSourceFiles());

            configuration.getParameters().put("classLoader",
                    new URLClassLoader(new URL[] { compilationDir.toURI().toURL() }));
        } catch (Exception e) {
            throw new ConfigurationException(
                    "Error compiling sources. Please configure the walkmod-rawclasspath-plugin to define the external libraries of this project.",
                    e);
        }

    }

}

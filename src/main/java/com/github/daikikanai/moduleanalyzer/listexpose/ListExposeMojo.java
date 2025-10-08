package com.github.daikikanai.moduleanalyzer.listexpose;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Mojo(name = "list-expose")
public class ListExposeMojo extends AbstractMojo {

    @Parameter(property = "rootDir", required = true)
    private String rootDir;

    @Parameter(property = "showDependency", defaultValue = "false")
    private boolean showDependency;

    public void execute() throws MojoExecutionException {
        try {
            Path root = Paths.get(rootDir);

            if (!Files.exists(root) || !Files.isDirectory(root)) {
                throw new MojoExecutionException("Root directory does not exist: " + rootDir);
            }

            ModuleDataSource dataSource = new ModuleDataSource(root);

            Map<String, List<String>> moduleExposeClasses = dataSource.scanModuleExposeClasses();
            Set<String> allModules = dataSource.scanAllModules();

            Set<String> allExposeClasses = new HashSet<>();
            for (List<String> classes : moduleExposeClasses.values()) {
                allExposeClasses.addAll(classes);
            }

            Map<String, Map<String, Set<String>>> classDependenciesTo = new HashMap<>();
            Map<String, Map<String, Set<String>>> classDependenciesFrom = new HashMap<>();
            Map<String, Set<String>> moduleCallerClasses = new HashMap<>();

            if (showDependency) {
                classDependenciesTo = dataSource.buildClassDependenciesTo(allExposeClasses, allModules);
                classDependenciesFrom = dataSource.buildClassDependenciesFrom(allExposeClasses, allModules);
                moduleCallerClasses = dataSource.buildModuleCallerClasses(allExposeClasses, allModules);
            }

            ModuleView view = new ModuleView(getLog());
            view.displayModules(moduleExposeClasses, moduleCallerClasses,
                               classDependenciesTo, classDependenciesFrom, showDependency);

        } catch (IOException e) {
            throw new MojoExecutionException("Error scanning modules", e);
        }
    }
}

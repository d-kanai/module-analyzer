package com.github.daikikanai.moduleanalyzer;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Mojo(name = "list-expose")
public class ListExposeMojo extends AbstractMojo {

    @Parameter(property = "rootDir", required = true)
    private String rootDir;

    public void execute() throws MojoExecutionException {
        try {
            Path root = Paths.get(rootDir);

            if (!Files.exists(root) || !Files.isDirectory(root)) {
                throw new MojoExecutionException("Root directory does not exist: " + rootDir);
            }

            Map<String, List<String>> moduleExposeClasses = scanModules(root);

            if (moduleExposeClasses.isEmpty()) {
                getLog().info("No expose classes found.");
                return;
            }

            // Sort modules by name
            List<String> sortedModules = new ArrayList<>(moduleExposeClasses.keySet());
            Collections.sort(sortedModules);

            for (String module : sortedModules) {
                getLog().info("");
                getLog().info("[Module: " + module + "]");

                List<String> classes = moduleExposeClasses.get(module);
                Collections.sort(classes);

                for (String className : classes) {
                    getLog().info("  - " + className);
                }
            }
            getLog().info("");

        } catch (IOException e) {
            throw new MojoExecutionException("Error scanning modules", e);
        }
    }

    private Map<String, List<String>> scanModules(Path root) throws IOException {
        Map<String, List<String>> result = new HashMap<>();

        try (Stream<Path> paths = Files.walk(root)) {
            paths.filter(Files::isDirectory)
                 .filter(path -> path.getFileName().toString().equals("expose"))
                 .forEach(exposePath -> {
                     try {
                         String moduleName = extractModuleName(root, exposePath);
                         List<String> classes = scanExposeDirectory(exposePath);
                         if (!classes.isEmpty()) {
                             result.put(moduleName, classes);
                         }
                     } catch (IOException e) {
                         getLog().warn("Error scanning expose directory: " + exposePath, e);
                     }
                 });
        }

        return result;
    }

    private String extractModuleName(Path root, Path exposePath) {
        Path relativePath = root.relativize(exposePath);
        if (relativePath.getNameCount() > 0) {
            return relativePath.getName(0).toString();
        }
        return "unknown";
    }

    private List<String> scanExposeDirectory(Path exposePath) throws IOException {
        List<String> classes = new ArrayList<>();

        try (Stream<Path> files = Files.walk(exposePath)) {
            files.filter(Files::isRegularFile)
                 .filter(path -> path.toString().endsWith(".java"))
                 .forEach(javaFile -> {
                     String className = extractClassName(exposePath, javaFile);
                     if (className != null) {
                         classes.add(className);
                     }
                 });
        }

        return classes;
    }

    private String extractClassName(Path exposePath, Path javaFile) {
        try {
            // Read the file to find package declaration
            String content = new String(Files.readAllBytes(javaFile));
            String packageName = extractPackageName(content);

            if (packageName == null) {
                return null;
            }

            // Get relative path from expose directory
            Path relativePath = exposePath.relativize(javaFile);
            String fileName = relativePath.toString()
                .replace(File.separator, ".")
                .replace(".java", "");

            return packageName + "." + fileName;
        } catch (IOException e) {
            getLog().warn("Error reading file: " + javaFile, e);
            return null;
        }
    }

    private String extractPackageName(String content) {
        // Simple regex to extract package name
        String[] lines = content.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("package ")) {
                return line.substring(8, line.indexOf(';')).trim();
            }
        }
        return null;
    }
}

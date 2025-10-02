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

    @Parameter(property = "showDependency", defaultValue = "false")
    private boolean showDependency;

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

            // Collect all expose classes
            Set<String> allExposeClasses = new HashSet<>();
            for (List<String> classes : moduleExposeClasses.values()) {
                allExposeClasses.addAll(classes);
            }

            // Build module dependencies with class details
            Map<String, Map<String, Set<String>>> moduleDependencies = new HashMap<>();
            if (showDependency) {
                moduleDependencies = buildModuleDependencies(root, allExposeClasses, moduleExposeClasses.keySet());
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

                if (showDependency) {
                    // Show dependencies to (modules this module depends on)
                    if (moduleDependencies.containsKey(module)) {
                        Map<String, Set<String>> deps = moduleDependencies.get(module);
                        if (!deps.isEmpty()) {
                            getLog().info("  Dependencies to:");
                            List<String> sortedModules2 = new ArrayList<>(deps.keySet());
                            Collections.sort(sortedModules2);
                            for (String targetModule : sortedModules2) {
                                Set<String> classList = deps.get(targetModule);
                                List<String> sortedClasses = new ArrayList<>(classList);
                                Collections.sort(sortedClasses);
                                String classNames = sortedClasses.stream()
                                    .map(c -> c.substring(c.lastIndexOf('.') + 1))
                                    .collect(Collectors.joining(", "));
                                getLog().info("    - " + targetModule + " (" + classNames + ")");
                            }
                        }
                    }

                    // Show dependencies from (modules that depend on this module)
                    Map<String, Set<String>> fromModules = new HashMap<>();
                    for (Map.Entry<String, Map<String, Set<String>>> entry : moduleDependencies.entrySet()) {
                        String callerModule = entry.getKey();
                        Map<String, Set<String>> targets = entry.getValue();
                        if (targets.containsKey(module)) {
                            fromModules.put(callerModule, targets.get(module));
                        }
                    }
                    if (!fromModules.isEmpty()) {
                        getLog().info("  Depended by:");
                        List<String> sortedFrom = new ArrayList<>(fromModules.keySet());
                        Collections.sort(sortedFrom);
                        for (String from : sortedFrom) {
                            Set<String> classList = fromModules.get(from);
                            List<String> sortedClasses = new ArrayList<>(classList);
                            Collections.sort(sortedClasses);
                            String classNames = sortedClasses.stream()
                                .map(c -> c.substring(c.lastIndexOf('.') + 1))
                                .collect(Collectors.joining(", "));
                            getLog().info("    - " + from + " (" + classNames + ")");
                        }
                    }
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
                 .filter(path -> !path.toString().endsWith("Dto.java"))  // Exclude Dto classes
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

    private Map<String, Map<String, Set<String>>> buildModuleDependencies(Path root, Set<String> exposeClasses, Set<String> allModules) throws IOException {
        // Map: callerModule -> targetModule -> Set of expose classes
        Map<String, Map<String, Set<String>>> dependencies = new HashMap<>();

        // Initialize empty maps for all modules
        for (String module : allModules) {
            dependencies.put(module, new HashMap<>());
        }

        // Scan all Java files
        try (Stream<Path> paths = Files.walk(root)) {
            paths.filter(Files::isRegularFile)
                 .filter(path -> path.toString().endsWith(".java"))
                 .filter(path -> !path.toString().contains("/expose/")) // Exclude expose files themselves
                 .forEach(javaFile -> {
                     try {
                         String content = new String(Files.readAllBytes(javaFile));
                         String callerModule = extractModuleName(root, javaFile);

                         if (callerModule != null && !callerModule.equals("unknown")) {
                             for (String exposeClass : exposeClasses) {
                                 String exposeModule = extractModuleFromClassName(exposeClass);

                                 // Only add if from different module
                                 if (!callerModule.equals(exposeModule) && isCallingClass(content, exposeClass)) {
                                     Map<String, Set<String>> moduleDeps = dependencies.get(callerModule);
                                     moduleDeps.computeIfAbsent(exposeModule, k -> new HashSet<>()).add(exposeClass);
                                 }
                             }
                         }
                     } catch (IOException e) {
                         getLog().warn("Error reading file: " + javaFile, e);
                     }
                 });
        }

        return dependencies;
    }

    private String extractModuleFromClassName(String className) {
        // Extract module name from package (e.g., com.example.order.expose.OrderApi -> order)
        String[] parts = className.split("\\.");
        if (parts.length >= 3) {
            return parts[2]; // Assuming com.example.{module}.{package}.{class}
        }
        return "unknown";
    }

    private String extractFullClassName(Path root, Path javaFile, String content) {
        String packageName = extractPackageName(content);
        if (packageName == null) {
            return null;
        }

        String fileName = javaFile.getFileName().toString().replace(".java", "");
        return packageName + "." + fileName;
    }

    private boolean isCallingClass(String content, String exposeClass) {
        // Check for import statement
        if (content.contains("import " + exposeClass + ";")) {
            return true;
        }

        // Check for simple class name usage (after last dot)
        String simpleClassName = exposeClass.substring(exposeClass.lastIndexOf('.') + 1);

        // Look for class usage patterns
        String[] patterns = {
            simpleClassName + " ",
            simpleClassName + ".",
            simpleClassName + "(",
            simpleClassName + "<",
            simpleClassName + ">"
        };

        for (String pattern : patterns) {
            if (content.contains(pattern)) {
                return true;
            }
        }

        return false;
    }
}

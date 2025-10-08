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
            Set<String> allModules = scanAllModules(root);

            // Collect all expose classes
            Set<String> allExposeClasses = new HashSet<>();
            for (List<String> classes : moduleExposeClasses.values()) {
                allExposeClasses.addAll(classes);
            }

            // Build class-level dependencies
            Map<String, Map<String, Set<String>>> classDependenciesTo = new HashMap<>();
            Map<String, Map<String, Set<String>>> classDependenciesFrom = new HashMap<>();
            Map<String, Set<String>> moduleCallerClasses = new HashMap<>();
            if (showDependency) {
                classDependenciesTo = buildClassDependenciesTo(root, allExposeClasses, allModules);
                classDependenciesFrom = buildClassDependenciesFrom(root, allExposeClasses, allModules);
                moduleCallerClasses = buildModuleCallerClasses(root, allExposeClasses, allModules);
            }

            // Collect modules to display: modules with expose classes OR modules with caller classes
            Set<String> modulesToDisplay = new HashSet<>(moduleExposeClasses.keySet());
            if (showDependency) {
                modulesToDisplay.addAll(moduleCallerClasses.keySet());
            }

            if (modulesToDisplay.isEmpty()) {
                getLog().info("No modules to display.");
                return;
            }

            // Sort modules by name
            List<String> sortedModules = new ArrayList<>(modulesToDisplay);
            Collections.sort(sortedModules);

            for (String module : sortedModules) {
                getLog().info("");

                // Calculate module-level dependency counts
                Set<String> dependenciesToModules = new HashSet<>();
                Set<String> dependedByModules = new HashSet<>();

                if (showDependency) {
                    // Count Dependencies to (from application classes)
                    if (moduleCallerClasses.containsKey(module)) {
                        for (String callerClass : moduleCallerClasses.get(module)) {
                            if (classDependenciesTo.containsKey(callerClass)) {
                                dependenciesToModules.addAll(classDependenciesTo.get(callerClass).keySet());
                            }
                        }
                    }

                    // Count Depended by (to expose classes)
                    List<String> exposeClassList = moduleExposeClasses.get(module);
                    if (exposeClassList != null) {
                        for (String exposeClass : exposeClassList) {
                            if (classDependenciesFrom.containsKey(exposeClass)) {
                                dependedByModules.addAll(classDependenciesFrom.get(exposeClass).keySet());
                            }
                        }
                    }
                }

                // Display module header with counts
                String moduleHeader = "[Module: " + module + "]";
                if (showDependency && (!dependenciesToModules.isEmpty() || !dependedByModules.isEmpty())) {
                    moduleHeader += " (Dependencies to: " + dependenciesToModules.size() +
                                   ", Depended by: " + dependedByModules.size() + ")";
                }
                getLog().info(moduleHeader);

                // Show expose classes and their dependencies from
                List<String> exposeClasses = moduleExposeClasses.get(module);
                if (exposeClasses != null) {
                    Collections.sort(exposeClasses);
                    for (String className : exposeClasses) {
                        getLog().info("  - " + className);

                        // Show who depends on this expose class
                        if (showDependency && classDependenciesFrom.containsKey(className)) {
                            Map<String, Set<String>> fromModules = classDependenciesFrom.get(className);
                            if (!fromModules.isEmpty()) {
                                getLog().info("    Depended by:");
                                List<String> sortedFrom = new ArrayList<>(fromModules.keySet());
                                Collections.sort(sortedFrom);
                                for (String fromModule : sortedFrom) {
                                    Set<String> callerClasses = fromModules.get(fromModule);
                                    List<String> sortedCallers = new ArrayList<>(callerClasses);
                                    Collections.sort(sortedCallers);
                                    String callerNames = sortedCallers.stream()
                                        .map(c -> c.substring(c.lastIndexOf('.') + 1))
                                        .collect(Collectors.joining(", "));
                                    getLog().info("      - " + fromModule + ": " + callerNames);
                                }
                            }
                        }
                    }
                }

                // Show application/caller classes and their dependencies to
                if (showDependency && moduleCallerClasses.containsKey(module)) {
                    Set<String> callerClasses = moduleCallerClasses.get(module);
                    List<String> sortedCallers = new ArrayList<>(callerClasses);
                    Collections.sort(sortedCallers);
                    for (String callerClass : sortedCallers) {
                        getLog().info("  - " + callerClass);

                        // Show what this class depends on
                        if (classDependenciesTo.containsKey(callerClass)) {
                            Map<String, Set<String>> toModules = classDependenciesTo.get(callerClass);
                            if (!toModules.isEmpty()) {
                                getLog().info("    Dependencies to:");
                                List<String> sortedTo = new ArrayList<>(toModules.keySet());
                                Collections.sort(sortedTo);
                                for (String toModule : sortedTo) {
                                    Set<String> exposeClassNames = toModules.get(toModule);
                                    List<String> sortedExposes = new ArrayList<>(exposeClassNames);
                                    Collections.sort(sortedExposes);
                                    String exposeNames = sortedExposes.stream()
                                        .map(c -> c.substring(c.lastIndexOf('.') + 1))
                                        .collect(Collectors.joining(", "));
                                    getLog().info("      - " + toModule + ": " + exposeNames);
                                }
                            }
                        }
                    }
                }
            }
            getLog().info("");

        } catch (IOException e) {
            throw new MojoExecutionException("Error scanning modules", e);
        }
    }

    private Set<String> scanAllModules(Path root) throws IOException {
        Set<String> modules = new HashSet<>();

        try (Stream<Path> paths = Files.list(root)) {
            paths.filter(Files::isDirectory)
                 .forEach(modulePath -> {
                     String moduleName = modulePath.getFileName().toString();
                     modules.add(moduleName);
                 });
        }

        return modules;
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

    private Map<String, Map<String, Set<String>>> buildClassDependenciesTo(Path root, Set<String> exposeClasses, Set<String> allModules) throws IOException {
        // Map: callerClass -> targetModule -> Set of expose classes
        Map<String, Map<String, Set<String>>> dependencies = new HashMap<>();

        // Scan all Java files
        try (Stream<Path> paths = Files.walk(root)) {
            paths.filter(Files::isRegularFile)
                 .filter(path -> path.toString().endsWith(".java"))
                 .filter(path -> !path.toString().contains("/expose/")) // Exclude expose files themselves
                 .forEach(javaFile -> {
                     try {
                         String content = new String(Files.readAllBytes(javaFile));
                         String callerModule = extractModuleName(root, javaFile);
                         String callerClass = extractFullClassName(root, javaFile, content);

                         if (callerModule != null && !callerModule.equals("unknown") && callerClass != null) {
                             for (String exposeClass : exposeClasses) {
                                 String exposeModule = extractModuleFromClassName(exposeClass);

                                 // Only add if from different module
                                 if (!callerModule.equals(exposeModule) && isCallingClass(content, exposeClass)) {
                                     Map<String, Set<String>> targetDeps = dependencies.computeIfAbsent(callerClass, k -> new HashMap<>());
                                     targetDeps.computeIfAbsent(exposeModule, k -> new HashSet<>()).add(exposeClass);
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

    private Map<String, Map<String, Set<String>>> buildClassDependenciesFrom(Path root, Set<String> exposeClasses, Set<String> allModules) throws IOException {
        // Map: exposeClass -> callerModule -> Set of caller classes
        Map<String, Map<String, Set<String>>> dependenciesFrom = new HashMap<>();

        // Scan all Java files
        try (Stream<Path> paths = Files.walk(root)) {
            paths.filter(Files::isRegularFile)
                 .filter(path -> path.toString().endsWith(".java"))
                 .filter(path -> !path.toString().contains("/expose/")) // Exclude expose files themselves
                 .forEach(javaFile -> {
                     try {
                         String content = new String(Files.readAllBytes(javaFile));
                         String callerModule = extractModuleName(root, javaFile);
                         String callerClass = extractFullClassName(root, javaFile, content);

                         if (callerModule != null && !callerModule.equals("unknown") && callerClass != null) {
                             for (String exposeClass : exposeClasses) {
                                 String exposeModule = extractModuleFromClassName(exposeClass);

                                 // Only add if from different module
                                 if (!callerModule.equals(exposeModule) && isCallingClass(content, exposeClass)) {
                                     Map<String, Set<String>> targetDeps = dependenciesFrom.computeIfAbsent(exposeClass, k -> new HashMap<>());
                                     targetDeps.computeIfAbsent(callerModule, k -> new HashSet<>()).add(callerClass);
                                 }
                             }
                         }
                     } catch (IOException e) {
                         getLog().warn("Error reading file: " + javaFile, e);
                     }
                 });
        }

        return dependenciesFrom;
    }

    private Map<String, Set<String>> buildModuleCallerClasses(Path root, Set<String> exposeClasses, Set<String> allModules) throws IOException {
        // Map: callerModule -> Set of caller classes
        Map<String, Set<String>> callerClasses = new HashMap<>();

        // Initialize empty sets for all modules
        for (String module : allModules) {
            callerClasses.put(module, new HashSet<>());
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
                         String callerClass = extractFullClassName(root, javaFile, content);

                         if (callerModule != null && !callerModule.equals("unknown") && callerClass != null) {
                             // Check if this class calls any expose class from other modules
                             boolean callsOtherModule = false;
                             for (String exposeClass : exposeClasses) {
                                 String exposeModule = extractModuleFromClassName(exposeClass);
                                 if (!callerModule.equals(exposeModule) && isCallingClass(content, exposeClass)) {
                                     callsOtherModule = true;
                                     break;
                                 }
                             }
                             if (callsOtherModule) {
                                 callerClasses.get(callerModule).add(callerClass);
                             }
                         }
                     } catch (IOException e) {
                         getLog().warn("Error reading file: " + javaFile, e);
                     }
                 });
        }

        return callerClasses;
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

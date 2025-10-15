package com.github.daikikanai.moduleanalyzer.listexpose;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class ModuleDataSource {
    private final Path root;

    public ModuleDataSource(Path root) {
        this.root = root;
    }

    public Set<String> scanAllModules() throws IOException {
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

    public Map<String, List<String>> scanModuleExposeClasses() throws IOException {
        Map<String, List<String>> result = new HashMap<>();

        try (Stream<Path> paths = Files.walk(root)) {
            paths.filter(Files::isDirectory)
                 .filter(path -> path.getFileName().toString().equals("expose"))
                 .forEach(exposePath -> {
                     try {
                         String moduleName = extractModuleName(exposePath);
                         List<String> classes = scanExposeDirectory(exposePath);
                         if (!classes.isEmpty()) {
                             result.put(moduleName, classes);
                         }
                     } catch (IOException e) {
                         throw new RuntimeException("Error scanning expose directory: " + exposePath, e);
                     }
                 });
        }

        return result;
    }

    public Map<String, Map<String, Set<String>>> buildClassDependenciesTo(Set<String> exposeClasses, Set<String> allModules) throws IOException {
        Map<String, Map<String, Set<String>>> dependencies = new HashMap<>();

        try (Stream<Path> paths = Files.walk(root)) {
            paths.filter(Files::isRegularFile)
                 .filter(path -> path.toString().endsWith(".java"))
                 .filter(path -> !path.toString().contains("/expose/"))
                 .forEach(javaFile -> {
                     try {
                         String content = new String(Files.readAllBytes(javaFile));
                         String callerModule = extractModuleName(javaFile);
                         String callerClass = extractFullClassName(javaFile, content);

                         if (callerModule != null && !callerModule.equals("unknown") && callerClass != null) {
                             for (String exposeClass : exposeClasses) {
                                 String exposeModule = extractModuleFromClassName(exposeClass);

                                 if (!callerModule.equals(exposeModule) && isCallingClass(content, exposeClass)) {
                                     Map<String, Set<String>> targetDeps = dependencies.computeIfAbsent(callerClass, k -> new HashMap<>());
                                     targetDeps.computeIfAbsent(exposeModule, k -> new HashSet<>()).add(exposeClass);
                                 }
                             }
                         }
                     } catch (IOException e) {
                         throw new RuntimeException("Error reading file: " + javaFile, e);
                     }
                 });
        }

        return dependencies;
    }

    public Map<String, Map<String, Set<String>>> buildClassDependenciesFrom(Set<String> exposeClasses, Set<String> allModules) throws IOException {
        Map<String, Map<String, Set<String>>> dependenciesFrom = new HashMap<>();

        try (Stream<Path> paths = Files.walk(root)) {
            paths.filter(Files::isRegularFile)
                 .filter(path -> path.toString().endsWith(".java"))
                 .filter(path -> !path.toString().contains("/expose/"))
                 .forEach(javaFile -> {
                     try {
                         String content = new String(Files.readAllBytes(javaFile));
                         String callerModule = extractModuleName(javaFile);
                         String callerClass = extractFullClassName(javaFile, content);

                         if (callerModule != null && !callerModule.equals("unknown") && callerClass != null) {
                             for (String exposeClass : exposeClasses) {
                                 String exposeModule = extractModuleFromClassName(exposeClass);

                                 if (!callerModule.equals(exposeModule) && isCallingClass(content, exposeClass)) {
                                     Map<String, Set<String>> targetDeps = dependenciesFrom.computeIfAbsent(exposeClass, k -> new HashMap<>());
                                     targetDeps.computeIfAbsent(callerModule, k -> new HashSet<>()).add(callerClass);
                                 }
                             }
                         }
                     } catch (IOException e) {
                         throw new RuntimeException("Error reading file: " + javaFile, e);
                     }
                 });
        }

        return dependenciesFrom;
    }

    public Map<String, Set<String>> buildModuleCallerClasses(Set<String> exposeClasses, Set<String> allModules) throws IOException {
        Map<String, Set<String>> callerClasses = new HashMap<>();

        for (String module : allModules) {
            callerClasses.put(module, new HashSet<>());
        }

        try (Stream<Path> paths = Files.walk(root)) {
            paths.filter(Files::isRegularFile)
                 .filter(path -> path.toString().endsWith(".java"))
                 .filter(path -> !path.toString().contains("/expose/"))
                 .forEach(javaFile -> {
                     try {
                         String content = new String(Files.readAllBytes(javaFile));
                         String callerModule = extractModuleName(javaFile);
                         String callerClass = extractFullClassName(javaFile, content);

                         if (callerModule != null && !callerModule.equals("unknown") && callerClass != null) {
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
                         throw new RuntimeException("Error reading file: " + javaFile, e);
                     }
                 });
        }

        return callerClasses;
    }

    private String extractModuleName(Path filePath) {
        Path relativePath = root.relativize(filePath);
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
                 .filter(path -> !path.toString().endsWith("Dto.java"))
                 .filter(path -> !path.toString().endsWith("Input.java"))
                 .filter(path -> !path.toString().endsWith("Output.java"))
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
            String content = new String(Files.readAllBytes(javaFile));
            String packageName = extractPackageName(content);

            if (packageName == null) {
                return null;
            }

            Path relativePath = exposePath.relativize(javaFile);
            String fileName = relativePath.toString()
                .replace(File.separator, ".")
                .replace(".java", "");

            return packageName + "." + fileName;
        } catch (IOException e) {
            throw new RuntimeException("Error reading file: " + javaFile, e);
        }
    }

    private String extractPackageName(String content) {
        String[] lines = content.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("package ")) {
                return line.substring(8, line.indexOf(';')).trim();
            }
        }
        return null;
    }

    private String extractModuleFromClassName(String className) {
        String[] parts = className.split("\\.");
        if (parts.length >= 3) {
            return parts[2];
        }
        return "unknown";
    }

    private String extractFullClassName(Path javaFile, String content) {
        String packageName = extractPackageName(content);
        if (packageName == null) {
            return null;
        }

        String fileName = javaFile.getFileName().toString().replace(".java", "");
        return packageName + "." + fileName;
    }

    private boolean isCallingClass(String content, String exposeClass) {
        if (content.contains("import " + exposeClass + ";")) {
            return true;
        }

        String simpleClassName = exposeClass.substring(exposeClass.lastIndexOf('.') + 1);

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

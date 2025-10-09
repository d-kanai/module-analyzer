package com.github.daikikanai.moduleanalyzer.listhttprequest;

import org.apache.maven.plugin.logging.Log;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class Tracer {
    private final Path root;
    private final Log log;
    private final Map<String, Path> classFileMap;

    public Tracer(Path root, Log log) {
        this.root = root;
        this.log = log;
        this.classFileMap = new HashMap<>();
    }

    public Result trace(String startClassName, String searchPattern) throws IOException {
        // Build class file map first
        buildClassFileMap();

        Result result = new Result();
        Set<String> visited = new HashSet<>();
        List<String> currentChain = new ArrayList<>();

        traceRecursive(startClassName, searchPattern, currentChain, visited, result);

        return result;
    }

    private void buildClassFileMap() throws IOException {
        try (Stream<Path> paths = Files.walk(root)) {
            paths.filter(Files::isRegularFile)
                 .filter(path -> path.toString().endsWith(".java"))
                 .forEach(javaFile -> {
                     try {
                         String content = new String(Files.readAllBytes(javaFile));
                         String fullClassName = extractFullClassName(javaFile, content);
                         if (fullClassName != null) {
                             classFileMap.put(fullClassName, javaFile);
                         }
                     } catch (IOException e) {
                         log.warn("Failed to read file: " + javaFile);
                     }
                 });
        }
    }

    private void traceRecursive(String className, String searchPattern,
                                List<String> currentChain, Set<String> visited,
                                Result result) throws IOException {

        if (visited.contains(className)) {
            return;
        }
        visited.add(className);
        currentChain.add(className);

        Path classFile = classFileMap.get(className);
        if (classFile == null) {
            currentChain.remove(currentChain.size() - 1);
            return;
        }

        String content = new String(Files.readAllBytes(classFile));

        // Check if this class contains the search pattern (case-insensitive)
        if (content.toLowerCase().contains(searchPattern.toLowerCase())) {
            int lineNumber = findLineNumber(content, searchPattern);
            String lineContent = extractLine(content, lineNumber);
            String methodName = extractMethodNameAtLine(content, lineNumber);
            result.addPath(new Result.TracePath(currentChain, className, lineNumber, lineContent, methodName));
        }

        // Find all classes this class depends on
        Set<String> dependencies = extractDependencies(content);

        for (String dependency : dependencies) {
            if (classFileMap.containsKey(dependency)) {
                traceRecursive(dependency, searchPattern, currentChain, visited, result);
            }
        }

        currentChain.remove(currentChain.size() - 1);
    }

    private Set<String> extractDependencies(String content) {
        Set<String> dependencies = new HashSet<>();

        // Extract from import statements
        Pattern importPattern = Pattern.compile("import\\s+([a-zA-Z0-9_.]+);");
        Matcher importMatcher = importPattern.matcher(content);
        while (importMatcher.find()) {
            String importedClass = importMatcher.group(1);
            if (classFileMap.containsKey(importedClass)) {
                dependencies.add(importedClass);
            }
        }

        // Extract from field declarations and method calls
        for (String className : classFileMap.keySet()) {
            String simpleClassName = className.substring(className.lastIndexOf('.') + 1);

            // Check if the simple class name is used in the content
            Pattern usagePattern = Pattern.compile("\\b" + simpleClassName + "\\b");
            Matcher usageMatcher = usagePattern.matcher(content);

            if (usageMatcher.find()) {
                // Verify it's imported or in the same package
                if (content.contains("import " + className + ";") ||
                    isSamePackage(content, className)) {
                    dependencies.add(className);
                }
            }
        }

        return dependencies;
    }

    private boolean isSamePackage(String content, String className) {
        String packageName = extractPackageName(content);
        if (packageName == null) {
            return false;
        }

        String classPackage = className.substring(0, className.lastIndexOf('.'));
        return packageName.equals(classPackage);
    }

    private String extractFullClassName(Path javaFile, String content) {
        String packageName = extractPackageName(content);
        if (packageName == null) {
            return null;
        }

        String fileName = javaFile.getFileName().toString().replace(".java", "");
        return packageName + "." + fileName;
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

    private int findLineNumber(String content, String searchPattern) {
        String[] lines = content.split("\n");
        String lowerPattern = searchPattern.toLowerCase();
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].toLowerCase().contains(lowerPattern)) {
                return i + 1;
            }
        }
        return -1;
    }

    private String extractLine(String content, int lineNumber) {
        if (lineNumber < 1) {
            return "";
        }
        String[] lines = content.split("\n");
        if (lineNumber <= lines.length) {
            return lines[lineNumber - 1].trim();
        }
        return "";
    }

    private String extractMethodNameAtLine(String content, int targetLineNumber) {
        String[] lines = content.split("\n");

        // Search backwards from the target line to find the method declaration
        for (int i = targetLineNumber - 1; i >= 0; i--) {
            String line = lines[i].trim();

            // Look for method pattern: (visibility) (modifiers) returnType methodName(
            // Example: public void createOrder(
            // Example: private static String getName(
            Pattern methodPattern = Pattern.compile("(?:public|private|protected)\\s+(?:static\\s+)?(?:void|\\w+(?:<[^>]+>)?)\\s+(\\w+)\\s*\\(");
            Matcher matcher = methodPattern.matcher(line);

            if (matcher.find()) {
                return matcher.group(1);
            }

            // Also try simpler pattern for methods without visibility modifier
            Pattern simplePattern = Pattern.compile("^\\s*(?:void|\\w+)\\s+(\\w+)\\s*\\(");
            Matcher simpleMatcher = simplePattern.matcher(line);

            if (simpleMatcher.find()) {
                String potentialMethod = simpleMatcher.group(1);
                // Filter out keywords
                if (!potentialMethod.equals("if") &&
                    !potentialMethod.equals("while") &&
                    !potentialMethod.equals("for") &&
                    !potentialMethod.equals("switch") &&
                    !potentialMethod.equals("catch")) {
                    return potentialMethod;
                }
            }
        }

        return "unknown";
    }
}

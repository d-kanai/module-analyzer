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

        traceRecursive(startClassName, searchPattern, currentChain, visited, result, "unknown");

        return result;
    }

    public Result traceDirectory(Path targetDir, String searchPattern) throws IOException {
        // Build class file map first
        buildClassFileMap();

        Result result = new Result();

        // Find all classes in target directory
        List<String> targetClasses = findClassesInDirectory(targetDir);

        // Trace each class independently
        for (String className : targetClasses) {
            Set<String> visited = new HashSet<>();
            List<String> currentChain = new ArrayList<>();
            traceRecursive(className, searchPattern, currentChain, visited, result, "unknown");
        }

        return result;
    }

    public Result traceModulesSubDir(Path root, String targetSubDir, String searchPattern) throws IOException {
        // Build class file map first
        buildClassFileMap();

        Result result = new Result();

        // Find all subdirectories matching targetSubDir in all modules
        List<Path> targetDirs = findSubDirectories(root, targetSubDir);

        // For each matching directory, find all classes and trace them
        for (Path targetDir : targetDirs) {
            // Extract module name from path (parent of targetSubDir)
            String moduleName = extractModuleName(targetDir, root);

            List<String> targetClasses = findClassesInDirectory(targetDir);

            // Trace each class independently
            for (String className : targetClasses) {
                Set<String> visited = new HashSet<>();
                List<String> currentChain = new ArrayList<>();
                traceRecursive(className, searchPattern, currentChain, visited, result, moduleName);
            }
        }

        return result;
    }

    private String extractModuleName(Path targetDir, Path root) {
        // Get relative path from root
        Path relativePath = root.relativize(targetDir);

        // The first component is the module name
        if (relativePath.getNameCount() > 0) {
            return relativePath.getName(0).toString();
        }

        return "unknown";
    }

    private List<Path> findSubDirectories(Path root, String targetSubDir) throws IOException {
        List<Path> matchingDirs = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(root, 3)) {
            paths.filter(Files::isDirectory)
                 .filter(path -> path.getFileName().toString().equals(targetSubDir))
                 .forEach(matchingDirs::add);
        }

        return matchingDirs;
    }

    private List<String> findClassesInDirectory(Path targetDir) throws IOException {
        List<String> classes = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(targetDir)) {
            paths.filter(Files::isRegularFile)
                 .filter(path -> path.toString().endsWith(".java"))
                 .forEach(javaFile -> {
                     try {
                         String content = new String(Files.readAllBytes(javaFile));
                         String fullClassName = extractFullClassName(javaFile, content);
                         if (fullClassName != null) {
                             classes.add(fullClassName);
                         }
                     } catch (IOException e) {
                         log.warn("Failed to read file: " + javaFile);
                     }
                 });
        }

        return classes;
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
                                Result result, String moduleName) throws IOException {

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

        // Find all occurrences of the search pattern in this class
        List<Integer> matchingLines = findAllLineNumbers(content, searchPattern);
        for (int lineNumber : matchingLines) {
            String lineContent = extractLine(content, lineNumber);
            String methodName = extractMethodNameAtLine(content, lineNumber);
            String urlExpression = extractFirstArgument(lineContent, searchPattern);
            String url = resolveUrlExpression(urlExpression, content);
            result.addPath(new Result.TracePath(currentChain, className, lineNumber, lineContent, methodName, moduleName, url));
        }

        // Find all classes this class depends on
        Set<String> dependencies = extractDependencies(content);

        for (String dependency : dependencies) {
            if (classFileMap.containsKey(dependency)) {
                traceRecursive(dependency, searchPattern, currentChain, visited, result, moduleName);
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

    private List<Integer> findAllLineNumbers(String content, String searchPattern) {
        List<Integer> lineNumbers = new ArrayList<>();
        String[] lines = content.split("\n");
        String lowerPattern = searchPattern.toLowerCase();
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].toLowerCase().contains(lowerPattern)) {
                lineNumbers.add(i + 1);
            }
        }
        return lineNumbers;
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

    private String extractFirstArgument(String lineContent, String searchPattern) {
        // Find the position of the search pattern (e.g., "client.post")
        int patternIndex = lineContent.toLowerCase().indexOf(searchPattern.toLowerCase());
        if (patternIndex == -1) {
            return "";
        }

        // Find the opening parenthesis after the pattern
        int openParenIndex = lineContent.indexOf('(', patternIndex);
        if (openParenIndex == -1) {
            return "";
        }

        // Extract the first argument (URL)
        int startIndex = openParenIndex + 1;
        String afterParen = lineContent.substring(startIndex).trim();

        // Find the end of the first argument
        int commaIndex = afterParen.indexOf(',');
        int closeParenIndex = afterParen.indexOf(')');

        int endIndex;
        if (commaIndex != -1 && (closeParenIndex == -1 || commaIndex < closeParenIndex)) {
            endIndex = commaIndex;
        } else if (closeParenIndex != -1) {
            endIndex = closeParenIndex;
        } else {
            return "";
        }

        String firstArg = afterParen.substring(0, endIndex).trim();
        return firstArg;
    }

    private String resolveUrlExpression(String expression, String fileContent) {
        expression = expression.trim();

        // If it's a simple string literal, return it
        Pattern literalPattern = Pattern.compile("^[\"']([^\"']*)[\"']$");
        Matcher literalMatcher = literalPattern.matcher(expression);
        if (literalMatcher.matches()) {
            return literalMatcher.group(1);
        }

        // Handle concatenation (e.g., HOST + "/api/orders" + SUFFIX)
        if (expression.contains("+")) {
            StringBuilder result = new StringBuilder();
            String[] parts = expression.split("\\+");

            for (String part : parts) {
                part = part.trim();

                // Check if it's a string literal
                Matcher partLiteralMatcher = literalPattern.matcher(part);
                if (partLiteralMatcher.matches()) {
                    result.append(partLiteralMatcher.group(1));
                } else {
                    // It's a variable or constant - try to resolve it
                    String value = resolveVariable(part, fileContent);
                    if (value != null) {
                        result.append(value);
                    } else {
                        result.append("{").append(part).append("}");
                    }
                }
            }

            return result.toString();
        }

        // Single variable/constant
        String value = resolveVariable(expression, fileContent);
        if (value != null) {
            return value;
        }

        return "{" + expression + "}";
    }

    private String resolveVariable(String varName, String fileContent) {
        varName = varName.trim();

        // Look for constant/field declarations
        // Pattern: private/public static final String VARNAME = "value";
        Pattern constantPattern = Pattern.compile(
            "(?:private|public|protected)?\\s*(?:static)?\\s*(?:final)?\\s*String\\s+" +
            Pattern.quote(varName) + "\\s*=\\s*[\"']([^\"']*)[\"']"
        );
        Matcher matcher = constantPattern.matcher(fileContent);
        if (matcher.find()) {
            return matcher.group(1);
        }

        // Look for simple variable assignments
        // Pattern: String varName = "value";
        Pattern varPattern = Pattern.compile(
            "String\\s+" + Pattern.quote(varName) + "\\s*=\\s*[\"']([^\"']*)[\"']"
        );
        Matcher varMatcher = varPattern.matcher(fileContent);
        if (varMatcher.find()) {
            return varMatcher.group(1);
        }

        return null;
    }
}

package com.github.daikikanai.moduleanalyzer.listtable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class TableDataSource {
    private final Path root;

    public TableDataSource(Path root) {
        this.root = root;
    }

    public Map<String, List<TableInfo>> scanModuleTables() throws IOException {
        Map<String, List<TableInfo>> result = new HashMap<>();

        try (Stream<Path> paths = Files.walk(root)) {
            paths.filter(Files::isDirectory)
                 .filter(path -> path.getFileName().toString().equals("infra"))
                 .forEach(infraPath -> {
                     try {
                         String moduleName = extractModuleName(infraPath);
                         List<TableInfo> tables = scanInfraDirectory(infraPath);
                         if (!tables.isEmpty()) {
                             result.put(moduleName, tables);
                         }
                     } catch (IOException e) {
                         throw new RuntimeException("Error scanning infra directory: " + infraPath, e);
                     }
                 });
        }

        return result;
    }

    private String extractModuleName(Path infraPath) {
        Path relativePath = root.relativize(infraPath);
        if (relativePath.getNameCount() > 0) {
            return relativePath.getName(0).toString();
        }
        return "unknown";
    }

    private List<TableInfo> scanInfraDirectory(Path infraPath) throws IOException {
        List<TableInfo> tables = new ArrayList<>();
        Pattern repositoryPattern = Pattern.compile("(.+)Repository\\.java$");

        try (Stream<Path> files = Files.walk(infraPath)) {
            files.filter(Files::isRegularFile)
                 .filter(path -> path.toString().endsWith("Repository.java"))
                 .forEach(repoFile -> {
                     String fileName = repoFile.getFileName().toString();
                     Matcher matcher = repositoryPattern.matcher(fileName);

                     if (matcher.matches()) {
                         String entityName = matcher.group(1);
                         String tableName = toSnakeCase(entityName);
                         String repositoryName = fileName.replace(".java", "");

                         tables.add(new TableInfo(tableName, repositoryName));
                     }
                 });
        }

        return tables;
    }

    private String toSnakeCase(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
}

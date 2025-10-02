package com.github.daikikanai.moduleanalyzer;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Mojo(name = "list-table")
public class ListTableMojo extends AbstractMojo {

    @Parameter(property = "rootDir", required = true)
    private String rootDir;

    public void execute() throws MojoExecutionException {
        try {
            Path root = Paths.get(rootDir);

            if (!Files.exists(root) || !Files.isDirectory(root)) {
                throw new MojoExecutionException("Root directory does not exist: " + rootDir);
            }

            Map<String, List<TableInfo>> moduleTables = scanModules(root);

            if (moduleTables.isEmpty()) {
                getLog().info("No repository classes found.");
                return;
            }

            // Sort modules by name
            List<String> sortedModules = new ArrayList<>(moduleTables.keySet());
            Collections.sort(sortedModules);

            for (String module : sortedModules) {
                getLog().info("");
                getLog().info("[Module: " + module + "]");

                List<TableInfo> tables = moduleTables.get(module);
                Collections.sort(tables, Comparator.comparing(t -> t.tableName));

                for (TableInfo table : tables) {
                    getLog().info("  - " + table.tableName + " (" + table.repositoryName + ")");
                }
            }
            getLog().info("");

        } catch (IOException e) {
            throw new MojoExecutionException("Error scanning modules", e);
        }
    }

    private Map<String, List<TableInfo>> scanModules(Path root) throws IOException {
        Map<String, List<TableInfo>> result = new HashMap<>();

        try (Stream<Path> paths = Files.walk(root)) {
            paths.filter(Files::isDirectory)
                 .filter(path -> path.getFileName().toString().equals("infra"))
                 .forEach(infraPath -> {
                     try {
                         String moduleName = extractModuleName(root, infraPath);
                         List<TableInfo> tables = scanInfraDirectory(infraPath);
                         if (!tables.isEmpty()) {
                             result.put(moduleName, tables);
                         }
                     } catch (IOException e) {
                         getLog().warn("Error scanning infra directory: " + infraPath, e);
                     }
                 });
        }

        return result;
    }

    private String extractModuleName(Path root, Path infraPath) {
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

    private static class TableInfo {
        final String tableName;
        final String repositoryName;

        TableInfo(String tableName, String repositoryName) {
            this.tableName = tableName;
            this.repositoryName = repositoryName;
        }
    }
}

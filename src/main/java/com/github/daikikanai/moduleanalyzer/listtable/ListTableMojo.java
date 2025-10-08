package com.github.daikikanai.moduleanalyzer.listtable;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

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

            TableDataSource dataSource = new TableDataSource(root);
            TableView view = new TableView(getLog());

            Map<String, List<TableInfo>> moduleTables = dataSource.scanModuleTables();
            view.displayTables(moduleTables);

        } catch (IOException e) {
            throw new MojoExecutionException("Error scanning modules", e);
        }
    }
}

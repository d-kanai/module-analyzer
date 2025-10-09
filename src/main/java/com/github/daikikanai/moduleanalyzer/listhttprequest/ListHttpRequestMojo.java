package com.github.daikikanai.moduleanalyzer.listhttprequest;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Mojo(name = "list-http-request")
public class ListHttpRequestMojo extends AbstractMojo {

    @Parameter(property = "rootDir", required = true)
    private String rootDir;

    @Parameter(property = "searchPatterns", defaultValue = "client.post,client.get")
    private String searchPatterns;

    public void execute() throws MojoExecutionException {
        try {
            Path root = Paths.get(rootDir);

            if (!Files.exists(root) || !Files.isDirectory(root)) {
                throw new MojoExecutionException("Root directory does not exist: " + rootDir);
            }

            // Parse comma-separated patterns
            String[] patterns = searchPatterns.split(",");
            List<String> patternList = new ArrayList<>();
            for (String pattern : patterns) {
                String trimmed = pattern.trim();
                if (!trimmed.isEmpty()) {
                    patternList.add(trimmed);
                }
            }

            getLog().info("Scanning modules in: " + rootDir);
            getLog().info("Target subdirectory: application");
            getLog().info("Searching for patterns: " + String.join(", ", patternList));
            getLog().info("");

            Tracer tracer = new Tracer(root, getLog());
            Result result = tracer.traceModulesSubDir(root, "application", patternList);

            View view = new View(getLog());
            view.displayResult(result);

        } catch (IOException e) {
            throw new MojoExecutionException("Error tracing modules", e);
        }
    }
}

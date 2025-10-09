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

    @Parameter(property = "searchPattern", defaultValue = "client.post")
    private String searchPattern;

    public void execute() throws MojoExecutionException {
        try {
            Path root = Paths.get(rootDir);

            if (!Files.exists(root) || !Files.isDirectory(root)) {
                throw new MojoExecutionException("Root directory does not exist: " + rootDir);
            }

            getLog().info("Scanning modules in: " + rootDir);
            getLog().info("Target subdirectory: application");
            getLog().info("Searching for pattern: " + searchPattern);
            getLog().info("");

            Tracer tracer = new Tracer(root, getLog());
            Result result = tracer.traceModulesSubDir(root, "application", searchPattern);

            View view = new View(getLog());
            view.displayResult(result, searchPattern);

        } catch (IOException e) {
            throw new MojoExecutionException("Error tracing modules", e);
        }
    }
}

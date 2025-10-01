package com.github.daikikanai.moduleanalyzer;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "hi")
public class HiMojo extends AbstractMojo {

    public void execute() throws MojoExecutionException {
        getLog().info("hi");
    }
}

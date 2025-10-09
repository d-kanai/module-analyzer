package com.github.daikikanai.moduleanalyzer.listhttprequest;

import org.apache.maven.plugin.logging.Log;

import java.util.List;

public class View {
    private final Log log;

    public View(Log log) {
        this.log = log;
    }

    public void displayResult(Result result, String startClassName, String searchPattern) {
        if (!result.hasMatches()) {
            log.info("No matches found for pattern: " + searchPattern);
            return;
        }

        log.info("");
        for (Result.TracePath path : result.getPaths()) {
            displayPath(path);
        }
    }

    private void displayPath(Result.TracePath path) {
        String className = path.getMatchedClass();
        String simpleClassName = className.substring(className.lastIndexOf('.') + 1);
        String methodName = path.getMethodName();

        log.info(simpleClassName + "." + methodName);
    }
}

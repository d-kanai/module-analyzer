package com.github.daikikanai.moduleanalyzer.listhttprequest;

import org.apache.maven.plugin.logging.Log;

import java.util.*;

public class View {
    private final Log log;

    public View(Log log) {
        this.log = log;
    }

    public void displayResult(Result result) {
        if (!result.hasMatches()) {
            log.info("No matches found");
            return;
        }

        // Group paths by module name
        Map<String, List<Result.TracePath>> pathsByModule = new LinkedHashMap<>();
        for (Result.TracePath path : result.getPaths()) {
            String moduleName = path.getModuleName();
            pathsByModule.computeIfAbsent(moduleName, k -> new ArrayList<>()).add(path);
        }

        // Sort module names
        List<String> sortedModules = new ArrayList<>(pathsByModule.keySet());
        Collections.sort(sortedModules);

        // Display each module
        log.info("");
        for (String module : sortedModules) {
            displayModule(module, pathsByModule.get(module));
        }
    }

    private void displayModule(String module, List<Result.TracePath> paths) {
        log.info("[Module: " + module + "]");

        for (Result.TracePath path : paths) {
            displayPath(path);
        }
        log.info("");
    }

    private void displayPath(Result.TracePath path) {
        String className = path.getMatchedClass();
        String simpleClassName = className.substring(className.lastIndexOf('.') + 1);
        String methodName = path.getMethodName();
        String url = path.getUrl();

        if (url != null && !url.isEmpty()) {
            log.info("  - " + simpleClassName + "." + methodName + " -> " + url);
        } else {
            log.info("  - " + simpleClassName + "." + methodName);
        }
    }
}

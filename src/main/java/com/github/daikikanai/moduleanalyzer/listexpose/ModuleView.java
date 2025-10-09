package com.github.daikikanai.moduleanalyzer.listexpose;

import org.apache.maven.plugin.logging.Log;

import java.util.*;
import java.util.stream.Collectors;

public class ModuleView {
    private final Log log;

    public ModuleView(Log log) {
        this.log = log;
    }

    public void displayModules(
            Map<String, List<String>> moduleExposeClasses,
            Map<String, Set<String>> moduleCallerClasses,
            Map<String, Map<String, Set<String>>> classDependenciesTo,
            Map<String, Map<String, Set<String>>> classDependenciesFrom,
            boolean showDependency) {

        Set<String> modulesToDisplay = new HashSet<>(moduleExposeClasses.keySet());
        if (showDependency) {
            modulesToDisplay.addAll(moduleCallerClasses.keySet());
        }

        if (modulesToDisplay.isEmpty()) {
            log.info("No modules to display.");
            return;
        }

        List<String> sortedModules = new ArrayList<>(modulesToDisplay);
        Collections.sort(sortedModules);

        for (String module : sortedModules) {
            displayModule(module, moduleExposeClasses, moduleCallerClasses,
                         classDependenciesTo, classDependenciesFrom, showDependency);
        }
        log.info("");
    }

    private void displayModule(
            String module,
            Map<String, List<String>> moduleExposeClasses,
            Map<String, Set<String>> moduleCallerClasses,
            Map<String, Map<String, Set<String>>> classDependenciesTo,
            Map<String, Map<String, Set<String>>> classDependenciesFrom,
            boolean showDependency) {

        log.info("");

        Set<String> dependenciesToModules = new HashSet<>();
        Set<String> dependedByModules = new HashSet<>();

        if (showDependency) {
            dependenciesToModules = calculateDependenciesToModules(module, moduleCallerClasses, classDependenciesTo);
            dependedByModules = calculateDependedByModules(module, moduleExposeClasses, classDependenciesFrom);
        }

        displayModuleHeader(module, dependenciesToModules, dependedByModules, showDependency);
        displayExposeClasses(module, moduleExposeClasses, classDependenciesFrom, showDependency);
        displayCallerClasses(module, moduleCallerClasses, classDependenciesTo, showDependency);
    }

    private Set<String> calculateDependenciesToModules(
            String module,
            Map<String, Set<String>> moduleCallerClasses,
            Map<String, Map<String, Set<String>>> classDependenciesTo) {

        Set<String> dependenciesToModules = new HashSet<>();

        if (moduleCallerClasses.containsKey(module)) {
            for (String callerClass : moduleCallerClasses.get(module)) {
                if (classDependenciesTo.containsKey(callerClass)) {
                    dependenciesToModules.addAll(classDependenciesTo.get(callerClass).keySet());
                }
            }
        }

        return dependenciesToModules;
    }

    private Set<String> calculateDependedByModules(
            String module,
            Map<String, List<String>> moduleExposeClasses,
            Map<String, Map<String, Set<String>>> classDependenciesFrom) {

        Set<String> dependedByModules = new HashSet<>();

        List<String> exposeClassList = moduleExposeClasses.get(module);
        if (exposeClassList != null) {
            for (String exposeClass : exposeClassList) {
                if (classDependenciesFrom.containsKey(exposeClass)) {
                    dependedByModules.addAll(classDependenciesFrom.get(exposeClass).keySet());
                }
            }
        }

        return dependedByModules;
    }

    private void displayModuleHeader(
            String module,
            Set<String> dependenciesToModules,
            Set<String> dependedByModules,
            boolean showDependency) {

        String moduleHeader = "[Module: " + module + "]";
        if (showDependency) {
            moduleHeader += " (Dependencies to: " + dependenciesToModules.size() +
                           ", Depended by: " + dependedByModules.size() + ")";
        }
        log.info(moduleHeader);
    }

    private void displayExposeClasses(
            String module,
            Map<String, List<String>> moduleExposeClasses,
            Map<String, Map<String, Set<String>>> classDependenciesFrom,
            boolean showDependency) {

        List<String> exposeClasses = moduleExposeClasses.get(module);
        if (exposeClasses == null) {
            return;
        }

        Collections.sort(exposeClasses);
        for (String className : exposeClasses) {
            log.info("  - " + className);

            if (showDependency && classDependenciesFrom.containsKey(className)) {
                Map<String, Set<String>> fromModules = classDependenciesFrom.get(className);
                if (!fromModules.isEmpty()) {
                    log.info("    Depended by:");
                    displayDependencies(fromModules);
                }
            }
        }
    }

    private void displayCallerClasses(
            String module,
            Map<String, Set<String>> moduleCallerClasses,
            Map<String, Map<String, Set<String>>> classDependenciesTo,
            boolean showDependency) {

        if (!showDependency || !moduleCallerClasses.containsKey(module)) {
            return;
        }

        Set<String> callerClasses = moduleCallerClasses.get(module);
        List<String> sortedCallers = new ArrayList<>(callerClasses);
        Collections.sort(sortedCallers);

        for (String callerClass : sortedCallers) {
            log.info("  - " + callerClass);

            if (classDependenciesTo.containsKey(callerClass)) {
                Map<String, Set<String>> toModules = classDependenciesTo.get(callerClass);
                if (!toModules.isEmpty()) {
                    log.info("    Dependencies to:");
                    displayDependencies(toModules);
                }
            }
        }
    }

    private void displayDependencies(Map<String, Set<String>> dependencies) {
        List<String> sortedModules = new ArrayList<>(dependencies.keySet());
        Collections.sort(sortedModules);

        for (String targetModule : sortedModules) {
            Set<String> classNames = dependencies.get(targetModule);
            List<String> sortedClasses = new ArrayList<>(classNames);
            Collections.sort(sortedClasses);

            String simpleNames = sortedClasses.stream()
                .map(c -> c.substring(c.lastIndexOf('.') + 1))
                .collect(Collectors.joining(", "));

            log.info("      - " + targetModule + ": " + simpleNames);
        }
    }
}

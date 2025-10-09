package com.github.daikikanai.moduleanalyzer.listhttprequest;

import java.util.*;

public class Result {
    private final List<TracePath> paths;
    private final Set<String> seenMatches;

    public Result() {
        this.paths = new ArrayList<>();
        this.seenMatches = new HashSet<>();
    }

    public void addPath(TracePath path) {
        // Create unique key: className.methodName
        String key = path.getMatchedClass() + "." + path.getMethodName();

        // Only add if not already seen
        if (!seenMatches.contains(key)) {
            seenMatches.add(key);
            paths.add(path);
        }
    }

    public List<TracePath> getPaths() {
        return paths;
    }

    public boolean hasMatches() {
        return !paths.isEmpty();
    }

    public static class TracePath {
        private final List<String> classChain;
        private final String matchedClass;
        private final int lineNumber;
        private final String lineContent;
        private final String methodName;
        private final String moduleName;
        private final String url;

        public TracePath(List<String> classChain, String matchedClass, int lineNumber, String lineContent, String methodName, String moduleName, String url) {
            this.classChain = new ArrayList<>(classChain);
            this.matchedClass = matchedClass;
            this.lineNumber = lineNumber;
            this.lineContent = lineContent;
            this.methodName = methodName;
            this.moduleName = moduleName;
            this.url = url;
        }

        public List<String> getClassChain() {
            return classChain;
        }

        public String getMatchedClass() {
            return matchedClass;
        }

        public int getLineNumber() {
            return lineNumber;
        }

        public String getLineContent() {
            return lineContent;
        }

        public String getMethodName() {
            return methodName;
        }

        public String getModuleName() {
            return moduleName;
        }

        public String getUrl() {
            return url;
        }
    }
}

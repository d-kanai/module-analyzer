package com.github.daikikanai.moduleanalyzer.listhttprequest;

import java.util.*;

public class Result {
    private final List<TracePath> paths;

    public Result() {
        this.paths = new ArrayList<>();
    }

    public void addPath(TracePath path) {
        paths.add(path);
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

        public TracePath(List<String> classChain, String matchedClass, int lineNumber, String lineContent, String methodName) {
            this.classChain = new ArrayList<>(classChain);
            this.matchedClass = matchedClass;
            this.lineNumber = lineNumber;
            this.lineContent = lineContent;
            this.methodName = methodName;
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
    }
}

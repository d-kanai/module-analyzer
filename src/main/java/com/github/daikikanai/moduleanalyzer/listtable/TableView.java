package com.github.daikikanai.moduleanalyzer.listtable;

import org.apache.maven.plugin.logging.Log;

import java.util.*;

public class TableView {
    private final Log log;

    public TableView(Log log) {
        this.log = log;
    }

    public void displayTables(Map<String, List<TableInfo>> moduleTables) {
        if (moduleTables.isEmpty()) {
            log.info("No repository classes found.");
            return;
        }

        List<String> sortedModules = new ArrayList<>(moduleTables.keySet());
        Collections.sort(sortedModules);

        for (String module : sortedModules) {
            displayModule(module, moduleTables.get(module));
        }
        log.info("");
    }

    private void displayModule(String module, List<TableInfo> tables) {
        log.info("");
        log.info("[Module: " + module + "]");

        Collections.sort(tables, Comparator.comparing(t -> t.tableName));

        for (TableInfo table : tables) {
            log.info("  - " + table.tableName);
        }
    }
}

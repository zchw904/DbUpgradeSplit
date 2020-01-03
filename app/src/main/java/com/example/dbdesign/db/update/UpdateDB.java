package com.example.dbdesign.db.update;

import java.util.List;

public class UpdateDB {
    private String dbName;
    private List<String> sqlBefores;
    private List<String> sqlAfters;

    public UpdateDB(String dbName, List<String> sqlBefores, List<String> sqlAfters) {
        this.dbName = dbName;
        this.sqlBefores = sqlBefores;
        this.sqlAfters = sqlAfters;
    }

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public List<String> getSqlBefores() {
        return sqlBefores;
    }

    public void setSqlBefores(List<String> sqlBefores) {
        this.sqlBefores = sqlBefores;
    }

    public List<String> getSqlAfters() {
        return sqlAfters;
    }

    public void setSqlAfters(List<String> sqlAfters) {
        this.sqlAfters = sqlAfters;
    }
}

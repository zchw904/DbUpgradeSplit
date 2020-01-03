package com.example.dbdesign.db.update;

import java.util.List;

public class CreateDB {
    private String name;
    private List<String> dbSqlList;

    public CreateDB(String name, List<String> dbSqlList) {
        this.name = name;
        this.dbSqlList = dbSqlList;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getDbSqlList() {
        return dbSqlList;
    }

    public void setDbSqlList(List<String> dbSqlList) {
        this.dbSqlList = dbSqlList;
    }
}

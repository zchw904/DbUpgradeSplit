package com.example.dbdesign.db.update;

import java.util.List;

public class CreateVersion {
    private String version;
    private List<CreateDB> dbList;

    public CreateVersion(String version, List<CreateDB> dbList) {
        this.version = version;
        this.dbList = dbList;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<CreateDB> getDbList() {
        return dbList;
    }

    public void setDbList(List<CreateDB> dbList) {
        this.dbList = dbList;
    }
}

package com.example.dbdesign.db.update;

import java.util.List;

public class UpdateStep {
    private String versionFrom;
    private String versionTo;
    private List<UpdateDB> dbList;

    public UpdateStep(String versionFrom, String versionTo, List<UpdateDB> dbList) {
        this.versionFrom = versionFrom;
        this.versionTo = versionTo;
        this.dbList = dbList;
    }

    public String getVersionFrom() {
        return versionFrom;
    }

    public void setVersionFrom(String versionFrom) {
        this.versionFrom = versionFrom;
    }

    public String getVersionTo() {
        return versionTo;
    }

    public void setVersionTo(String versionTo) {
        this.versionTo = versionTo;
    }

    public List<UpdateDB> getDbList() {
        return dbList;
    }

    public void setDbList(List<UpdateDB> dbList) {
        this.dbList = dbList;
    }
}

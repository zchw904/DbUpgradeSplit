package com.example.dbdesign.db.update;

import java.util.List;

public interface IUpdateSource {
    List<CreateVersion> getCreateVersions();

    List<UpdateStep> getUpdateStep();

    String getLatestVersion();
}

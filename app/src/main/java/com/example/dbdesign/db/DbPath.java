package com.example.dbdesign.db;

import android.os.Environment;

public class DbPath {
    public static final String ROOT_PATH =
            Environment.getExternalStorageDirectory()+"/dbdesign/update";
    public static final String LOCAL_VERSION_FILE_PATH =
            Environment.getExternalStorageDirectory()+"/dbdesign/update/update.txt";
    public static final String LOCAL_UPGARDE_FILE_PATH =
            Environment.getExternalStorageDirectory()+"/dbdesign/update/updateXml.xml";
    public static final String ROOT_DB_NAME = "user";


    public static final String BACKUP_DB_APPEND = "_backup.db";
    public static final String DB_APPEND = ".db";
    public static final String DB_VERSION = "V001";
}

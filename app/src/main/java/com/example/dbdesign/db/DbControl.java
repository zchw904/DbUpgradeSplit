package com.example.dbdesign.db;

import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.IOException;

class DbControl {
    protected ArrayMap<String, SQLiteDatabase> DATA_BASE_HOLDER = new ArrayMap<>();

    protected static DbControl getInstance(){
        return DbControlCreator.singleton;
    }

    protected DbControl() {

    }

    private SQLiteDatabase createDB(String dbPath, String key) {
        final SQLiteDatabase sqLiteDatabase =
                SQLiteDatabase.openOrCreateDatabase(dbPath, null);
        DATA_BASE_HOLDER.put(key, sqLiteDatabase);
        return sqLiteDatabase;
    }

    /**
     * root dir DB_ROOT = Environment.getExternalStorageState()+"/dbdesign/update"
     * .db will append in method
     * @param dbName db file name or "newDir/myDb"
     * @return
     */
    protected SQLiteDatabase getDbShortPathNoSuffix(String dbName) {
        if (TextUtils.isEmpty(dbName)){
            return null;
        }
        StringBuilder key = new StringBuilder(dbName);
        key.append(".db");
        StringBuilder dbPath = new StringBuilder(DbPath.ROOT_PATH);
        dbPath.append("/");
        dbPath.append(key.toString());
        return getDb(dbPath.toString(), key.toString());
    }

    protected SQLiteDatabase getDbShortPath(String dbName) {
        if (TextUtils.isEmpty(dbName)){
            return null;
        }
        StringBuilder dbPath = new StringBuilder(DbPath.ROOT_PATH);
        dbPath.append("/");
        dbPath.append(dbName);
        return getDb(dbPath.toString(), dbName);
    }

    protected SQLiteDatabase getDbFullPath(String fullPath){
        if (TextUtils.isEmpty(fullPath))
            return null;
        String key = getKeyFromFull(fullPath);
        return getDb(fullPath, key);
    }

    private SQLiteDatabase getDb(@NonNull String fullPath, @NonNull String key) {
        if (!DATA_BASE_HOLDER.containsKey(key)) {
            synchronized (DbControl.class){
                try {
                    File file = new File(fullPath);
                    if (!file.exists()) {
                        File parent = file.getParentFile();
                        if (!parent.exists()) {
                            parent.mkdirs();
                        }
                        file.createNewFile();
                    }
                    if (!DATA_BASE_HOLDER.containsKey(key)){
                        createDB(fullPath, key);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return DATA_BASE_HOLDER.get(key);
    }

    public void clearCache() {
        DATA_BASE_HOLDER.clear();
    }

    private String getKeyFromFull(@NonNull String fullPath){
        if (fullPath.startsWith(DbPath.ROOT_PATH))
            return fullPath.substring(DbPath.ROOT_PATH.length() - 1, fullPath.length() - 1);
        return fullPath;
    }

    private static class DbControlCreator{
        private static final DbControl singleton = new DbControl();
    }

}

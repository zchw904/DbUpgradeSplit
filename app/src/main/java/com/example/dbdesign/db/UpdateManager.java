package com.example.dbdesign.db;

import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.dbdesign.db.bean.User;
import com.example.dbdesign.db.update.CreateDB;
import com.example.dbdesign.db.update.CreateVersion;
import com.example.dbdesign.db.update.IUpdateSource;
import com.example.dbdesign.db.update.UpdateDB;
import com.example.dbdesign.db.update.UpdateSourceXML;
import com.example.dbdesign.db.update.UpdateStep;
import com.example.dbdesign.utils.FileUtil;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 升级逻辑：1.createVersion负责新建表如果数据库不存在也会新建
 * 2. updateStep负责将原表修改成备份表，然后插入数据到新表，删除备份表---
 * 3. updateStep的新建表语句要到createVersion中拿
 * 放在createVersion好处：如果是新app就不需要到updateStep中拿建表语句
 * 4. 分步升级,不同版本CreateTableSQL不支持放在同一个版本中(即最新版包含所有建表SQL),考虑到不同版本,相同业务表的名称不一样;
 * 如果需要找不到的SQL去上一版本或者最新版本找,自行修改
 * 跨版本：1. 从updateStep逐一升级，2.直接createVersion覆盖；当然跨大版本会出现推荐用户直接升级APP
 * XML: 1. updateStep的新建表语句要到createVersion.
 * 2. updateStep的versionFrom支持V1,V2,V3,且不能为空
 * 3. updateStep的versionTo只支持一个版本,且不能为空
 * 4. 不同版本CreateTableSQL不要一起放入最新版本(除非多个版本使用一套类似:6),需要按版本提供升级所需CreateTableSQL
 * 5. createVersion最新版本需要最新版本所有表的CreateTableSQL,保证覆盖升级或者初次使用APP时通过xml建表
 * 6. createVersion version支持多版本使用同一套CreateTableSQL V1,V2,V3
 */
public class UpdateManager {

    //方法传递可以不给，不给去DbControl拿
    private IUpdateSource upgradeSource = null;
    private List<User> userList;
    private String currentVersion;
    private String olderVersion;
    //    private int
    static final int ERROR = -1;
    static final int SUCCESS = 0;
    static final int IGNORE = 1;

    public static final int MODEL_CREATE = 1;
    public static final int MODEL_UPGRADE= 2;
    public static final int MODEL_CREATE_UPGRADE = 3;

    @Documented
    @IntDef(flag = true, value = {MODEL_UPGRADE, MODEL_CREATE, MODEL_CREATE_UPGRADE})
    @Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
    @Retention(RetentionPolicy.CLASS)
    public @interface Model {

    }

    public int executeUpdateDb(@Model int updateModel) {
        int flag = IGNORE;

        String currentVersion = getCurrentVersion();
        if (!TextUtils.isEmpty(currentVersion)) {
            upgradeSource = new UpdateSourceXML(getUpgradeFilePath());
            //无最新版本代表不升级
            String latestVersion = getLatestVersion();
            if (TextUtils.isEmpty(latestVersion) || latestVersion.equals(currentVersion)) {
                return IGNORE;
            }

            String rootDbBackupPath = null;
            String rootDbPath = getRootDbPath();
            String rootDbName = getRootDbName();
            //因为当前是分库设计
            if (TextUtils.isEmpty(getRootDbName()) || TextUtils.isEmpty(getRootDbPath()))
                return IGNORE;

            List<String> userDbPathList = new ArrayList<>();
            List<String> userDbBackupPathList = new ArrayList<>();

            //为所有用户升级,不存在当前用户问题
            BaseDao<User> userDao = DaoSplitFactory.getInstance().getSubDao(UserDao.class, User.class);
            userList = userDao.query(new User());

            CreateVersion currentCreateVersion = analyseCreateVersion(latestVersion);
            List<CreateDB> list = currentCreateVersion.getDbList();
            Map<String, CreateDB> dbMap = new HashMap<>();
            for (CreateDB createDB : list) {
                //没有提供多个DB，name
                dbMap.put(createDB.getName(), createDB);
            }

            if (updateModel == MODEL_CREATE || updateModel == MODEL_CREATE_UPGRADE) {
                //1.升级部分表
                //1.1 获取多个升级步骤
                List<UpdateStep> updateStepList = analyseUpdateStep(upgradeSource, currentVersion, latestVersion);
                //1.2 循环UpdateStep的集合
                if (updateStepList != null && updateStepList.size() > 0) {
                    for (UpdateStep updateStep : updateStepList) {
                        CreateVersion createVersion = analyseCreateVersion(updateStep.getVersionTo());
                        //1.3 循环UpdateDB的集合
                        for (UpdateDB updateDB : updateStep.getDbList()) {
                            if (updateDB == null || TextUtils.isEmpty(updateDB.getDbName()))
                                continue;
                            //1.4 获取升级ROOT DB
                            if (updateDB.getDbName().equals(rootDbName)) {
                                rootDbBackupPath = getRootDbBackupPath();
                                File file = new File(rootDbPath);
                                if (file.exists()) {
                                    boolean fileControl = FileUtil.copySingleFile(rootDbPath, rootDbBackupPath);
                                    if (!fileControl) {
                                        flag = ERROR;
                                        rootDbBackupPath = "";
                                        break;
                                    }

                                    boolean successFlag = executeUpdate(rootDbPath, updateDB, createVersion);
                                    if (successFlag) {
                                        flag = SUCCESS;
                                        if (createVersion.getVersion().equals(currentVersion) &&
                                                dbMap.containsKey(updateDB.getDbName())) {
                                            dbMap.remove(updateDB.getDbName());
                                        }
                                    } else {
                                        flag = ERROR;
                                        break;
                                    }
                                }
                            }
                            //1.5 获取升级USER DB
                            else {
                                if (userList == null || userList.size() == 0) {
                                    continue;
                                }

                                for (User user : userList) {
                                    if (user == null || user.getId() < 0) {
                                        continue;
                                    }

                                    String userDbPath = getUserDbPath(user, updateDB.getDbName());
                                    String userDbBackupPath = getUserDbBackupPath(user, updateDB.getDbName());
                                    File file = new File(userDbPath);
                                    if (file.exists()) {
                                        //备份每个账号下的数据裤文件
                                        boolean fileControl = FileUtil.copySingleFile(userDbPath, userDbBackupPath);
                                        if (fileControl) {
                                            userDbPathList.add(userDbPath);
                                            userDbBackupPathList.add(userDbBackupPath);
                                        } else {
                                            flag = ERROR;
                                            break;
                                        }

                                        boolean successFlag = executeUpdate(userDbPath, updateDB, createVersion);
                                        if (successFlag) {
                                            flag = SUCCESS;
                                            if (createVersion.getVersion().equals(currentVersion) &&
                                                    dbMap.containsKey(updateDB.getDbName())) {
                                                dbMap.remove(updateDB.getDbName());
                                            }
                                        } else {
                                            flag = ERROR;
                                            break;
                                        }
                                    }
                                }
                            }


                            if (flag == ERROR) {
                                break;
                            }
                        }
                    }
                }
            }

            List<String> newFileList = new ArrayList();
            //2.建表
            if (updateModel == MODEL_CREATE || updateModel == MODEL_CREATE_UPGRADE) {
                if (flag != ERROR && dbMap.size() > 0) {
                    for (String key : dbMap.keySet()) {
                        CreateDB createDB = dbMap.get(key);
                        if (createDB == null) {
                            continue;
                        }

                        if (createDB.getName().equals(rootDbName)) {
                            rootDbBackupPath = getRootDbBackupPath();
                            File file = new File(rootDbBackupPath);
                            if (file.exists()) {
                                boolean fileControl = FileUtil.copySingleFile(rootDbPath, rootDbBackupPath);
                                if (!fileControl) {
                                    flag = ERROR;
                                    rootDbBackupPath = "";
                                    break;
                                }
                            } else {
                                File parentFile = file.getParentFile();
                                parentFile.mkdirs();
                                try {
                                    file.createNewFile();
                                    newFileList.add(rootDbPath);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    flag = ERROR;
                                    break;
                                }
                            }

                            int executeFlag = executeSql(rootDbPath, createDB.getDbSqlList());
                            if (executeFlag == ERROR) {
                                flag = ERROR;
                                break;
                            } else {
                                flag = SUCCESS;
                            }
                        } else {
                            for (User user : userList) {
                                if (user == null || user.getId() < 0) {
                                    continue;
                                }

                                String userDbPath = getUserDbPath(user, createDB.getName());
                                String userDbBackupPath = getUserDbBackupPath(user, createDB.getName());
                                if (TextUtils.isEmpty(userDbPath) && TextUtils.isEmpty(userDbBackupPath)) {
                                    continue;
                                }

                                File file = new File(userDbPath);
                                if (file.exists()) {
                                    //备份每个账号下的数据裤文件
                                    boolean userFileControl = FileUtil.copySingleFile(userDbPath, userDbBackupPath);
                                    if (userFileControl) {
                                        flag = SUCCESS;
                                        userDbPathList.add(userDbPath);
                                        userDbBackupPathList.add(userDbBackupPath);
                                    } else {
                                        flag = ERROR;
                                        break;
                                    }
                                } else {
                                    File parentFile = file.getParentFile();
                                    parentFile.mkdirs();
                                    try {
                                        file.createNewFile();
                                        newFileList.add(userDbPath);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                        flag = ERROR;
                                        break;
                                    }
                                }

                                int executeFlag = executeSql(userDbPath, createDB.getDbSqlList());
                                if (executeFlag == ERROR) {
                                    flag = ERROR;
                                    break;
                                } else {
                                    flag = SUCCESS;
                                }
                            }
                        }

                        if (flag == ERROR) {
                            break;
                        }
                    }
                }
            }


            if (flag == SUCCESS) {
                //升级成功删除备份的数据裤文件
                for (String backupPath : userDbBackupPathList) {
                    FileUtil.deleteFile(backupPath);
                }
                if (!TextUtils.isEmpty(rootDbBackupPath)) {
                    FileUtil.deleteFile(rootDbBackupPath);
                }
                saveCurrentVersion(latestVersion);
                DbControl.getInstance().clearCache();
                DaoSplitFactory.getInstance().clearCache();
            } else if (flag == ERROR) {
                //升级失败从备份文件恢复
                for (String path : userDbPathList) {
                    FileUtil.deleteFile(path);
                }
                for (String backupPath : userDbBackupPathList) {
                    FileUtil.copySingleFile(backupPath, userDbPathList.get(userDbBackupPathList.indexOf(backupPath)));
                    FileUtil.deleteFile(backupPath);
                }
                if (!TextUtils.isEmpty(rootDbBackupPath)) {
                    FileUtil.deleteFile(rootDbPath);
                    FileUtil.copySingleFile(rootDbBackupPath, rootDbPath);
                    FileUtil.deleteFile(rootDbBackupPath);
                }
                DbControl.getInstance().clearCache();
                DaoSplitFactory.getInstance().clearCache();
            }

        }

        return flag;
    }

    private CreateVersion analyseCreateVersion(String versionTo) {
        if (upgradeSource == null) {
            return null;
        }

        for (CreateVersion createVersion : upgradeSource.getCreateVersions()) {
            String[] versions = createVersion.getVersion().trim().split(",");
            for (int i = 0; i < versions.length; i++) {
                if (versions[i].trim().equalsIgnoreCase(versionTo)) {
                    return createVersion;
                }
            }
        }
        return null;
    }


    /**
     * @param source
     * @param currentVersion
     * @param latestVersion
     * @return
     */
    private List<UpdateStep> analyseUpdateStep(IUpdateSource source, String currentVersion,
                                               String latestVersion) {
        if (TextUtils.isEmpty(currentVersion) || TextUtils.isEmpty(latestVersion)) {
            return null;
        }
        //如果完美直接过
        boolean perfectFlag = false;
        List<UpdateStep> list = new ArrayList();
        //如果给的XML有Tag有序这里还能优化,不能保证有序只能不断全循环处理
        UpdateStep loopUpdateStep = getUpdateStepTarget(source.getUpdateStep(), latestVersion, currentVersion);
        while (loopUpdateStep != null) {
            list.add(loopUpdateStep);
            //完美升级推出循环
            if (loopUpdateStep.getVersionTo().equals(latestVersion)) {
                perfectFlag = true;
                break;
            }

            loopUpdateStep = getUpdateStepTarget(source.getUpdateStep(), loopUpdateStep.getVersionTo(), latestVersion);
        }

        if (perfectFlag) {
            return list;
        }
        return null;
    }

    private UpdateStep getUpdateStepTarget(List<UpdateStep> updateStepList, String latestVersion, String currentVersion) {
        UpdateStep appropriateUpdateStep = null;
        UpdateStep enableUpdateStep = null;

        for (UpdateStep updateStep : updateStepList) {
            //防版本号空,带来的异常,根据自家不同逻辑可以修改
            if (TextUtils.isEmpty(updateStep.getVersionFrom())
                    || TextUtils.isEmpty(updateStep.getVersionTo())) {
                continue;
            }
            //一步升级,就不用找了
            if (updateStep.getVersionFrom().toUpperCase().contains(currentVersion)
                    && updateStep.getVersionTo().equals(latestVersion)) {
                appropriateUpdateStep = updateStep;
                break;
            }
            //分步升级
            if (updateStep.getVersionFrom().toUpperCase().contains(currentVersion)) {
                if (enableUpdateStep == null) {
                    enableUpdateStep = updateStep;
                } else {
                    //针对同一版本多个升级方法取最大
                    int compareResult = updateStep.getVersionTo().toUpperCase()
                            .compareTo(enableUpdateStep.getVersionTo().toUpperCase());
                    if (compareResult > 0) {
                        enableUpdateStep = updateStep;
                    }
                }
                continue;
            }
        }

        if (appropriateUpdateStep != null) {
            return appropriateUpdateStep;
        }

        if (enableUpdateStep != null) {
            return enableUpdateStep;
        }

        return null;
    }


    private boolean executeUpdate(@NonNull String dbPath, @NonNull UpdateDB updateDB, @NonNull CreateVersion createVersion) {
        File file = new File(dbPath);
        if (file.exists()) {
            int alterFlag = executeSql(dbPath, updateDB.getSqlBefores());
            if (alterFlag == ERROR) {
                return false;
            }

            int createFlag = executeCreateVersion(dbPath, updateDB.getDbName(), createVersion);
            if (createFlag == ERROR) {
                return false;
            }

            int insertFlag = executeSql(dbPath, updateDB.getSqlAfters());
            if (insertFlag == ERROR) {
                return false;
            }
        }

        return true;
    }

    private int executeCreateVersion(@NonNull String fullPath, @NonNull String dbName,
                                     @NonNull CreateVersion createVersion) {
        int flag = IGNORE;
        if (createVersion != null && !TextUtils.isEmpty(dbName) && !TextUtils.isEmpty(fullPath)) {
            for (CreateDB createDB : createVersion.getDbList()) {
                if (createDB == null || createDB.getDbSqlList() == null || createDB.getDbSqlList().size() == 0)
                    return flag;
                if (createDB.getName().equals(dbName)) {
                    flag = executeSql(fullPath, createDB.getDbSqlList());
                    return flag;
                }
            }
        }

        return flag;
    }

    private int executeSql(@NonNull String dbFullPath, @NonNull List<String> sqlList) {
        int flag = IGNORE;
        if (sqlList != null && sqlList.size() > 0) {
            if (TextUtils.isEmpty(dbFullPath)) {
                return flag;
            }

            SQLiteDatabase db = getDb(null, dbFullPath);
            if (db != null) {
                db.beginTransaction();
                try {
                    for (String sql : sqlList) {
                        if (TextUtils.isEmpty(sql)) {
                            continue;
                        }
                        String newSql = sql.replace("\r\n", "")
                                .replace("\n", "")
                                .trim();
                        if (!TextUtils.isEmpty(newSql)) {
                            db.execSQL(newSql);
                        }
                    }
                    db.setTransactionSuccessful();
                    flag = SUCCESS;
                } catch (SQLException exception) {
                    flag = ERROR;
                } finally {
                    db.endTransaction();
                }
            } else
                flag = ERROR;

        }

        return flag;
    }


    /**
     * 也可以从网络获取
     *
     * @return
     */
    public String getLatestVersion() {
        if (upgradeSource == null)
            return null;
        return upgradeSource.getLatestVersion();
    }

    private String getUpgradeFilePath() {
        return DbPath.LOCAL_UPGARDE_FILE_PATH;
    }

    /**
     * 视具体业务逻辑修改
     */
    public void saveCurrentVersion(String currentVersion) {
        if (TextUtils.isEmpty(currentVersion) || TextUtils.isEmpty(this.currentVersion)) {
            return;
        }
        if (currentVersion.equals(this.currentVersion)) {
            return;
        }
        StringBuilder strBuilder = new StringBuilder(currentVersion);
        strBuilder.append("/");
        strBuilder.append(olderVersion);
        this.currentVersion = currentVersion;
        FileUtil.writeFile(strBuilder.toString(), DbPath.LOCAL_VERSION_FILE_PATH, false);
    }

    /**
     * 视具体业务逻辑修改
     *
     * @return 当前版本
     */
    @Nullable
    private String getCurrentVersion() {
        if (TextUtils.isEmpty(currentVersion)) {
            olderVersion = FileUtil.readFile(DbPath.LOCAL_VERSION_FILE_PATH);

            if (TextUtils.isEmpty(olderVersion)) {
                olderVersion = DbPath.DB_VERSION;
                currentVersion = olderVersion;
            } else {
                currentVersion = olderVersion.split("/")[0];
            }
        }
        return currentVersion;
    }

    private SQLiteDatabase getDb(String dbNameNoSuffix, String dbFullPath) {
        if (!TextUtils.isEmpty(dbNameNoSuffix))
            return DbControl.getInstance().getDbShortPathNoSuffix(dbNameNoSuffix);
        if (!TextUtils.isEmpty(dbFullPath)) {
            return DbControl.getInstance().getDbFullPath(dbFullPath);
        }
        return null;
    }

    private String getRootDbName() {
        return DbPath.ROOT_DB_NAME;
    }


    private String getRootDbPath() {
        StringBuilder sb = new StringBuilder(DbPath.ROOT_PATH);
        sb.append("/");
        sb.append(DbPath.ROOT_DB_NAME);
        sb.append(DbPath.DB_APPEND);
        return sb.toString();
    }

    private String getRootDbBackupPath() {
        StringBuilder sb = new StringBuilder(DbPath.ROOT_PATH);
        sb.append("/");
        sb.append(DbPath.ROOT_DB_NAME);
        sb.append(DbPath.BACKUP_DB_APPEND);
        return sb.toString();
    }

    private String getUserDbPath(User user, String dbName) {
        StringBuilder sb = new StringBuilder(DbPath.ROOT_PATH);
        sb.append("/");
        sb.append(user.getId());
        sb.append("/");
        sb.append(dbName);
        sb.append(DbPath.DB_APPEND);
        return sb.toString();
    }

    private String getUserDbBackupPath(User user, String dbName) {
        StringBuilder sb = new StringBuilder(DbPath.ROOT_PATH);
        sb.append("/");
        sb.append(user.getId());
        sb.append("/");
        sb.append(dbName);
        sb.append(DbPath.BACKUP_DB_APPEND);
        return sb.toString();
    }
}

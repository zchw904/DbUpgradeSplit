package com.example.dbdesign.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.ArrayMap;

import com.example.dbdesign.db.annotatoin.DBField;
import com.example.dbdesign.db.annotatoin.DBTable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BaseDao<T> implements IBaseDao<T>{
    //需要查询的数据库持有
    protected SQLiteDatabase sqLiteDatabase;
    //表对应的实体字段map
    protected ArrayMap<String, Field> mapFields = new ArrayMap<>();

    protected Class<T> entity;
    protected String sTableName = null;

    protected BaseDao(Class<T> cSource, SQLiteDatabase sqd){
        entity = cSource;
        sqLiteDatabase = sqd;
        initFields(entity);
        createTable();
    }

    private void createTable(){
        StringBuilder sbTaSql = new StringBuilder("create table if not exists ");
        DBTable dbTable = entity.getAnnotation(DBTable.class);
        if (dbTable != null) {
            sTableName = dbTable.value();
        } else {
            sTableName = entity.getSimpleName();
        }
        sbTaSql.append(sTableName);
        sbTaSql.append("(");
        Set<Map.Entry<String, Field>> set = mapFields.entrySet();
        String typeName;
        for (Map.Entry<String, Field> entry: set) {
            typeName = getTypeField(entry.getValue().getType());
            if (typeName == null) {
                continue;
            }
            sbTaSql.append(entry.getKey());
            sbTaSql.append(" ");
            sbTaSql.append(typeName);
        }
        if (set.size() > 0){
            int length = sbTaSql.length();
            sbTaSql.delete(length-1, length);
        }
        sbTaSql.append(")");
        sqLiteDatabase.execSQL(sbTaSql.toString());
    }

    @Override
    public List<T> query(T where) {
        return query(where, null, -1, -1);
    }

    @Override
    public List<T> query(T where, String orderBy, int startIndex, int limit) {
        return query(false, where, null, null, orderBy, startIndex, limit );
    }

    @Override
    public List<T> query(boolean distinct,T where, String groupBy, String having, String orderBy, int startIndex, int limit) {
        final BaseWhere queryWhere = new BaseWhere(initValues(where));
        StringBuilder sbLimit = null;
        if (startIndex>=0 && limit>0){
            sbLimit = new StringBuilder();
            sbLimit.append(startIndex);
            sbLimit.append(" , ");
            sbLimit.append(limit);
        }
        Cursor sqLiteCursor = sqLiteDatabase.query(distinct ,sTableName, null, queryWhere.whereClause,
                queryWhere.whereArgs, groupBy, having, orderBy, sbLimit == null ?null :sbLimit.toString());
        return getEntityList(sqLiteCursor, where);
    }


    @Override
    public int delete(T where) {
        BaseWhere baseWhere = new BaseWhere(initValues(where));
        return sqLiteDatabase.delete(sTableName, baseWhere.whereClause, baseWhere.whereArgs);
    }

    @Override
    public long insertList(List<T> listEntity) {
        sqLiteDatabase.beginTransaction();
        long l = 0;
        try {
            ContentValues contentValues;
            for (T entity: listEntity) {
                l++;
                contentValues = getContentValues(initValues(entity));
                sqLiteDatabase.insertWithOnConflict(sTableName, null,
                        contentValues, SQLiteDatabase.CONFLICT_ROLLBACK);
            }
            sqLiteDatabase.setTransactionSuccessful();
        } catch (Exception e) {
            l = -1;
            e.printStackTrace();
        } finally {
            sqLiteDatabase.endTransaction();
        }
        return l;
    }

    @Override
    public long insetEntity(T entity) {
        final ContentValues contentValues = getContentValues(initValues(entity));
        return sqLiteDatabase.insertWithOnConflict(sTableName, null,
                contentValues, SQLiteDatabase.CONFLICT_ROLLBACK);
    }


    @Override
    public int updateEntity(T newEntity, T olderEntity) {
        final BaseWhere baseWhere = new BaseWhere(initValues(olderEntity));
        final ContentValues contentValues = getContentValues(initValues(newEntity));
        return sqLiteDatabase.updateWithOnConflict(sTableName, contentValues,
                baseWhere.whereClause, baseWhere.whereArgs, SQLiteDatabase.CONFLICT_ROLLBACK);
    }

    /**
     * 获取bean类的所有字段
     * @param sourceClass bean类，对应数据库table
     */
    private void initFields(Class sourceClass){
        Field[] fields = sourceClass.getDeclaredFields();
        for (Field field: fields) {
            field.setAccessible(true);
            DBField dbField = field.getAnnotation(DBField.class);
            mapFields.put(dbField != null ? dbField.value(): field.getName(), field);
        }
    }

    /**
     * 获取实体值
     * @param entity T的实例
     * @return
     */
    private Map<String, String> initValues(Object entity){
        Map<String, String> map = new ArrayMap();
        Field field;
        Object obj = null;
        for (Map.Entry<String, Field> entry : mapFields.entrySet()) {
            field = entry.getValue();
            if (field == null) {
                continue;
            }
            try {
                obj = field.get(entity);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            if (obj != null) {
                map.put(entry.getKey(), obj.toString());
            }
        }
        return map;
    }

    private String getTypeField(Class cType){
        if (cType == String.class) {
            return "TEXT,";
        } else if (cType == Integer.class || cType==int.class) {
            return "INTEGER,";
        } else if (cType == Long.class || cType==long.class) {
            return "BIGINT,";
        }else if (cType == Double.class || cType==double.class) {
            return "DOUBLE,";
        }else if (cType == byte[].class) {
            return "BLOB,";
        }
        return null;
    }

    private List<T> getEntityList(Cursor sqLiteCursor, T where){
        ArrayList list = new ArrayList();
        Object obj = null;
        int columnIndex;
        try {
            while (sqLiteCursor.moveToNext()) {
                obj = where.getClass().newInstance();
                for (Map.Entry<String, Field> entry : mapFields.entrySet()) {
                    Field field = entry.getValue();
                    columnIndex = sqLiteCursor.getColumnIndex(entry.getKey());
                    if (columnIndex >= 0) {
                        if (field.getType() == int.class || field.getType() == Integer.class) {
                            field.set(obj, sqLiteCursor.getInt(columnIndex));
                        } else if (field.getType() == long.class || field.getType() == Long.class) {
                            field.set(obj, sqLiteCursor.getLong(columnIndex));
                        }else if (field.getType() == double.class || field.getType() == Double.class) {
                            field.set(obj, sqLiteCursor.getDouble(columnIndex));

                        }else if (field.getType() == String.class) {
                            field.set(obj, sqLiteCursor.getString(columnIndex));
                        }else if (field.getType() == byte[].class) {
                            field.set(obj, sqLiteCursor.getBlob(columnIndex));
                        }
                    }
                }
                list.add(obj);
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }
        return list.size()==0 ? null:list;
    }

    private ContentValues getContentValues(Map<String, String> map){
        final ContentValues contentValues = new ContentValues();
        for (Map.Entry<String,String> entry : map.entrySet()) {
            if (entry.getValue()!=null && entry.getKey()!=null)
            contentValues.put(entry.getKey(), entry.getValue());
        }
        return contentValues;
    }

    protected static class BaseWhere{
        protected String whereClause;
        protected String[] whereArgs;

        protected BaseWhere(Map<String, String> values){
            StringBuilder sb = new StringBuilder("1 = 1");
            String key,value;
            int index = 0;
            if (values.size() == 0) {
                whereArgs = null;
                whereClause = null;
                return;
            } else
                whereArgs = new String[values.size()];

            for (Map.Entry<String,String> entry: values.entrySet()) {
                key = entry.getKey();
                value = entry.getValue();
                if ( key!= null && value!= null) {
                    sb.append(" and ");
                    sb.append(key);
                    sb.append("=?");
                    whereArgs[index] = value;
                    index++;
                }
            }

            whereClause = sb.toString();
        }
    }

    public void destory(){
        sqLiteDatabase = null;
        mapFields.clear();
        mapFields = null;
        entity = null;
        sTableName = null;
    }
}

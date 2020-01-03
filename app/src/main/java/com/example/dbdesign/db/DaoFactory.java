package com.example.dbdesign.db;

import android.database.sqlite.SQLiteDatabase;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DaoFactory {
    private Map<Class, BaseDao> baseDaoMap = new ConcurrentHashMap<>();

    public static DaoFactory getInstance(){
        return DaoFactory.DaoFactoryCreator.singleton;
    }

    protected DaoFactory(){
    }

    //可以优化拿创建相同的数据库才阻塞
    public <T> BaseDao<T> getBaseDao(Class<T> typeClass){
        if (!baseDaoMap.containsKey(typeClass)) {
            synchronized (DaoFactory.class){
                if (!baseDaoMap.containsKey(typeClass)){
                    BaseDao baseDao =
                            createDao(BaseDao.class, typeClass, getDbName());
                    if (baseDao != null)
                        baseDaoMap.put(typeClass, baseDao);
                }
            }
        }
        return baseDaoMap.get(typeClass);
    }

    public <T extends BaseDao<S>, S> T getSubDao(Class<T> daoClass, Class<S> typeClass){
        if (!baseDaoMap.containsKey(typeClass)) {
            synchronized (DaoFactory.class){
                if (!baseDaoMap.containsKey(typeClass)){
                    T baseDao = createDao(daoClass, typeClass, getDbName());
                    if (baseDao != null)
                        baseDaoMap.put(typeClass, baseDao);
                }
            }
        }
        return (T)baseDaoMap.get(typeClass);
    }

    protected <T extends BaseDao<S>, S> T createDao(Class<T> daoClass,
                                        Class<S> typClass, String dbName) {
        T baseDao = null;
        try {
            final Constructor<T> constructor =
                    daoClass.getDeclaredConstructor(Class.class, SQLiteDatabase.class);
            final SQLiteDatabase sqLiteDatabase =
                    DbControl.getInstance().getDbShortPathNoSuffix(dbName);
            baseDao = constructor.newInstance(typClass, sqLiteDatabase);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        return baseDao;
    }

    private String getDbName(){
        return DbPath.ROOT_DB_NAME;
    }

    private static class DaoFactoryCreator{
        private static final DaoFactory singleton = new DaoFactory();
    }

}

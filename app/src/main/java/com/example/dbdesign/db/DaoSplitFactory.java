package com.example.dbdesign.db;

import com.example.dbdesign.db.bean.User;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DaoSplitFactory extends DaoFactory {
    private Map<String, BaseDao> baseDaoMap = new ConcurrentHashMap<>();
    private User currentUser = null;

    public static DaoSplitFactory getInstance() {
        return DaoSplitFactory.DDaoSplitFactoryCreator.singleton;
    }

    protected DaoSplitFactory() {
        baseDaoMap.put(getKey(User.class), createDao(UserDao.class, User.class, getDbName(User.class)));
    }

    @Override
    public <T> BaseDao<T> getBaseDao(Class<T> typeClass) {
        currentUser = getUser();
        String key = getKey(typeClass);
        if (!baseDaoMap.containsKey(key)) {
            synchronized (DaoFactory.class){
                if (!baseDaoMap.containsKey(key)){
                    BaseDao baseDao = createDao(BaseDao.class,  typeClass, getDbName(typeClass));
                    if (baseDao != null)
                        baseDaoMap.put(key, baseDao);
                }
            }
        }
        return baseDaoMap.get(key);
    }

    @Override
    public <T extends BaseDao<S>, S> T getSubDao(Class<T> daoClass, Class<S> typeClass) {
        currentUser = getUser();
        String name = getDbName(typeClass);
        if (!baseDaoMap.containsKey(name)) {
            synchronized (DaoFactory.class){
                if (!baseDaoMap.containsKey(name)){
                    T baseDao = createDao(daoClass, typeClass, getDbName(typeClass));
                    if (baseDao != null)
                        baseDaoMap.put(name, baseDao);
                }
            }
        }
        return (T) baseDaoMap.get(name);
    }

    /**
     * 可以换成其他方式获取
     * @param typeClass
     * @return
     */
    private String getDbName(Class typeClass){
        if (typeClass == User.class) {
            return DbPath.ROOT_DB_NAME;
        } else {
            if (currentUser == null) {
                return null;
            }
            StringBuilder sb = new StringBuilder();
            sb.append(currentUser.getId());
            sb.append("/");
            sb.append("login");
            return sb.toString();
        }
    }

    private String getKey(Class typeClass){
        if (typeClass == User.class) {
            return User.class.getSimpleName();
        } else {
            if (currentUser == null) {
                return null;
            }
            StringBuilder sb = new StringBuilder();
            sb.append(currentUser.getId());
            sb.append("/");
            sb.append(typeClass.getSimpleName());
            return sb.toString();
        }
    }

    private User getUser(){
        if (!baseDaoMap.containsKey(getKey(User.class)))
            baseDaoMap.put(getKey(User.class), createDao(UserDao.class, User.class, getDbName(User.class)));
        UserDao userDao = (UserDao) baseDaoMap.get(getKey(User.class));
        User user = userDao.getCurrentUser();
        return user;
    }

    public void clearCache() {
        baseDaoMap.clear();
    }

    private static class DDaoSplitFactoryCreator {
        private static final DaoSplitFactory singleton = new DaoSplitFactory();
    }
}

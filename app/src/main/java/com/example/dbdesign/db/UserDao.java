package com.example.dbdesign.db;

import android.database.sqlite.SQLiteDatabase;

import com.example.dbdesign.db.bean.User;

import java.util.List;

public class UserDao extends BaseDao<User> {

    protected UserDao(Class cSource, SQLiteDatabase sqd) {
        super(cSource, sqd);
    }

    @Override
    public long insetEntity(User entity) {
        User userState = new User();
        userState.setStatus(0);
        updateEntity(userState, new User());
        entity.setStatus(1);
        return super.insetEntity(entity);
    }

    public User getCurrentUser(){
        User user = new User();
        user.setStatus(1);
        List<User> list = query(user);
        if(list != null && list.size() > 0){
            return list.get(0);
        }
        return null;
    }
}

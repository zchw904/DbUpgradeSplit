package com.example.dbdesign.db.bean;

import com.example.dbdesign.db.annotatoin.DBField;
import com.example.dbdesign.db.annotatoin.DBTable;

@DBTable("tb_user")
public class User {
    @DBField("user_id")
    private Integer id;
    private String name;
    private String password;
    private Integer status;

    public User(Integer id, String name, String password, Integer status) {
        this.id = id;
        this.name = name;
        this.password = password;
        this.status = status;
    }

    public User() {

    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}

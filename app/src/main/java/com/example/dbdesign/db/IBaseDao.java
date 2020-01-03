package com.example.dbdesign.db;

import java.util.List;

public interface IBaseDao<T> {
    List<T> query(T where);
    List<T> query(T Where, String orderBy, int startIndex, int limit);

    List<T> query(boolean distinct, T where, String groupBy, String having, String orderBy, int startIndex, int limit);

    long insetEntity(T entity);
    int updateEntity(T newEntity, T olderEntity);
    int delete(T where);
    long insertList(List<T> listEntity);
}

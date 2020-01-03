package com.example.dbdesign.db.bean;

public class Address {
    private int id;
    private String country;
    private String street;
    private String userId;

    public Address(int id, String country, String street, String userId) {
        this.id = id;
        this.country = country;
        this.street = street;
        this.userId = userId;
    }

    public Address() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}

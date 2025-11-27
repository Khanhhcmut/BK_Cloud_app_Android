package com.example.bkcloud;

public class UserItem {
    public String username;
    public String project;
    public String token;
    public String storageUrl;
    public String password;


    public UserItem(String username, String project, String token, String storageUrl, String password) {
        this.username = username;
        this.project = project;
        this.token = token;
        this.storageUrl = storageUrl;
        this.password = password;
    }

}

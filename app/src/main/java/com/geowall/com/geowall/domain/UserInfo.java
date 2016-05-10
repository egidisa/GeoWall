package com.geowall.com.geowall.domain;

/**
 * Created by Alessio on 09/05/2016.
 */
public class UserInfo {
    String uid;
    String email;
    String nickname;

    public UserInfo() {
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}

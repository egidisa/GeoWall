package com.geowall.com.geowall.domain;

import java.util.List;

/**
 * Created by Alessio on 09/05/2016.
 */
public class Wall {

    String name;
    String geoFireKey;
    List<Message> messages;

    public Wall() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGeoFireKey() {
        return geoFireKey;
    }

    public void setGeoFireKey(String geoFireKey) {
        this.geoFireKey = geoFireKey;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }
}

package com.geowall.domain;

/**
 * Created by Alessio on 09/05/2016.
 */
public class Message {
    String id;
    String uid;
    String timestamp;
    String text;

    public Message() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getText() {
        return text;
    }

    public void setContent(String content) {
        this.text = content;
    }
}

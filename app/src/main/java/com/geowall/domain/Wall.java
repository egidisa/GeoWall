package com.geowall.domain;

import java.util.List;
import java.util.Map;

/**
 * Created by Alessio on 09/05/2016.
 */
public class Wall {

    String id;
    String name;
    Double lat;
    Double lon;
    Long msgCount;
    Map<String,Message> messages;

    public Wall() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLon() {
        return lon;
    }

    public void setLon(Double lon) {
        this.lon = lon;
    }

    public Long getMsgCount() {
        return msgCount;
    }

    public void setMsgCount(Long msgCount) {
        this.msgCount = msgCount;
    }

    @Override
    public String toString() {
        return "Wall{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", lat=" + lat +
                ", lon=" + lon +
                ", msgCount=" + msgCount +
                '}';
    }
    public Map<String,Message> getMessages() {return messages;    }

    public void setMessages(Map<String,Message> messages) {this.messages = messages;    }
}

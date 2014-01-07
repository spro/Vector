package com.sprobertson.vector;

public class Peer {
    private String uuid;
    private int port;

    public Peer(String uuid, int port) {
        this.uuid = uuid;
        this.port = port;
    }

    public String getUUID() {
        return uuid;
    }

    public int getPort() {
        return port;
    }

    public String toString() {
        return uuid + "<:" + Integer.toString(port) + ">";
    }
}


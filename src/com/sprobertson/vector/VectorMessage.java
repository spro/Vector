package com.sprobertson.vector;

import java.text.SimpleDateFormat;
import java.util.Date;

public class VectorMessage {
    public String sender;
    public String body;
    public String time;

    public VectorMessage(String sender, String body) {
        this.sender = sender;
        this.body = body;
        this.time = getTime();
    }

    private String getTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        return sdf.format(new Date());
    }
}



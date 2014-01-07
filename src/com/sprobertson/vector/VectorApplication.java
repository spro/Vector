package com.sprobertson.vector;

import android.app.Application;
import android.util.Log;

import java.util.Random;
import java.util.UUID;

public class VectorApplication extends Application {
    public final UUID uuid;
    public final String uuid_str;
    public String username;
    public short port;

    public VectorApplication() {
        uuid = UUID.randomUUID();
        uuid_str = uuid.toString().replaceAll("-", "");
        port = (short) (new Random().nextInt(5000) + 5000); // Random port 5k - 10k
        Log.e("VectorApplication init", "Initializing with UUID " + uuid_str);
    }
}

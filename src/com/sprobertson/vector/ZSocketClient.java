package com.sprobertson.vector;

import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import java.lang.Runnable;
import java.lang.Thread;

import org.zeromq.ZMQ;

public class ZSocketClient {
    private String serverMessage;
    public static final String IP = "192.168.42.80";
    public static final int PORT = 4444;
    private OnMessageReceived messageListener = null;
    private boolean run = false;

    private ZMQ.Socket zsocket;

    public ZSocketClient(OnMessageReceived listener) {
        messageListener = listener;
    }

    public void stopClient() {
        run = false;
    }

    // AsyncTask for sending a message over the outgoing socket
    private class ZSocketSendTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... message) {
            Log.e("ZSocket", "Sending " + message[0]);
            zsocket.send(message[0].getBytes(ZMQ.CHARSET), 0);
            return null;
        }
    }

    // Public interface for running above task
    public void sendMessage(String message) {
        new ZSocketSendTask().execute(message);
    }

    public void run() {
        run = true;

        try {
            // Create an incoming ROUTER socket
            ZMQ.Context context = ZMQ.context(1);

            Log.e("ZSocket", "Connecting to server...");

            zsocket = context.socket(ZMQ.ROUTER);
            zsocket.bind("tcp://*:5595");

            while (run) {
                String sender = zsocket.recvStr();
                String message = zsocket.recvStr();
                Log.e("ZSocket", "Received '" + message + "' from " + sender);
                messageListener.messageReceived(sender, message);
            }
        } catch (Exception e) {
            Log.e("ZSocket", "Error in run()", e);
        }
    }

    public interface OnMessageReceived {
        public void messageReceived(String sender, String message);
    }
}


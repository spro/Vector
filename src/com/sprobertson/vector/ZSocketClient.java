package com.sprobertson.vector;

import android.os.AsyncTask;
import android.util.Log;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.lang.Thread;
import java.util.UUID;

import org.zeromq.ZMQ;

public class ZSocketClient {

    private class ZSocketSendTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... message) {
            Log.e("ZSocket", "Sending " + message[0]);
            zsocket.send(message[0].getBytes(ZMQ.CHARSET), 0);
            return null;
        }
    }

    private String serverMessage;
    public static final String IP = "192.168.42.80";
    public static final int PORT = 4444;
    private OnMessageReceived messageListener = null;
    private boolean run = false;

    private ZMQ.Socket zsocket;
    private DatagramSocket bsocket;

    public ZSocketClient(OnMessageReceived listener) {
        messageListener = listener;
    }

    public void sendMessage(String message) {
        new ZSocketSendTask().execute(message);
    }

    public void stopClient() {
        run = false;
    }

    public byte[] build_beacon() {
        byte[] buffer = new byte[22];
        ByteBuffer bb = ByteBuffer.wrap(buffer);
        bb.put("ZRE".getBytes());
        bb.putInt(1);
        UUID uuid = UUID.randomUUID();
        Log.e("UUID", uuid.toString());
        bb.putLong(4, uuid.getMostSignificantBits());
        bb.putLong(12, uuid.getLeastSignificantBits());
        return bb.array();
    }

    public void announceSelf() {
        try {
            InetAddress address = InetAddress.getByName("255.255.255.255");
            byte[] beacon = build_beacon();
            DatagramPacket packet = new DatagramPacket(beacon, beacon.length, address, 5670);
            //bsocket.connect(address, 5670);
            bsocket.send(packet);
        } catch (Exception e) {
            Log.e("Announce", "Error", e);
        }   
    }

    public void run() {
        run = true;

        try {
            InetAddress serverAddr = InetAddress.getByName(IP);

            // Set up UDP broadcast
            bsocket = new DatagramSocket(5670);
            bsocket.setBroadcast(true);
            announceSelf();

            // Try connecting to ZMQ
            ZMQ.Context context = ZMQ.context(1);

            Log.e("ZSocket", "Connecting to server...");

            zsocket = context.socket(ZMQ.DEALER);
            zsocket.connect("tcp://192.168.42.77:5555");

            while (run) {
                byte[] reply = zsocket.recv(0);
                String response = new String(reply, ZMQ.CHARSET);
                Log.e("ZSocket", "Received " + response);
                messageListener.messageReceived(response);
            }
        } catch (Exception e) {
            Log.e("ZSocket", "C: Error", e);
        }
    }

    public interface OnMessageReceived {
        public void messageReceived(String message);
    }
}


package com.sprobertson.vector;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import java.lang.Runnable;
import java.lang.Thread;
import java.util.HashMap;

import org.zeromq.ZMQ;

import org.json.JSONObject;

public class ZSocketService extends IntentService {
    /*
     * Constants for Intent-based Service-to-Activity communication
     */

    public static final String MESSAGE_SEND
     = "com.sprobertson.vector.MESSAGE_SEND";
    public static final String MESSAGE_RECEIVED
     = "com.sprobertson.vector.MESSAGE_RECEIVED";
    public static final String MESSAGE_SENDER
     = "com.sprobertson.vector.MESSAGE_SENDER";
    public static final String MESSAGE_RECIPIENT
     = "com.sprobertson.vector.MESSAGE_RECIPIENT";
    public static final String MESSAGE_BODY
     = "com.sprobertson.vector.MESSAGE_BODY";

    public static final String HELLO_SEND
     = "com.sprobertson.vector.HELLO_SEND";
    public static final String HELLO_RECEIVED
     = "com.sprobertson.vector.HELLO_RECEIVED";
    public static final String HELLO_SENDER
     = "com.sprobertson.vector.HELLO_SENDER";
    public static final String HELLO_USERNAME
     = "com.sprobertson.vector.HELLO_USERNAME";

    private boolean run = false;

    // Maps for referencing peer UUIDs to sockets and usernames
    private HashMap<String, ZMQ.Socket> peer_sockets;
    private HashMap<String, String> peer_usernames;

    private ZMQ.Socket zsocket;

    public ZSocketService() {
        super("ZSocketService");
    }

    public void stopService() {
        run = false;
    }

    // Sending a message over the outgoing socket
    public void sendMessage(String peer_uuid, String message) {
        if (peer_uuid.equals("*")) {
            Log.e("Vector.ZSocketService.sendMessage", "Sending " + message + " to *");
            for (String _peer_uuid : peer_sockets.keySet()) {
                Log.e("Vector ZSocket", "Sending " + message + " to " + _peer_uuid);
                ZMQ.Socket psocket = peer_sockets.get(_peer_uuid);
                try {
                    JSONObject message_object = new JSONObject();
                    message_object.put("type", "message");
                    message_object.put("body", message);
                    psocket.send(message_object.toString().getBytes(ZMQ.CHARSET), 0);
                } catch (Exception e) {
                    Log.e("Vector.ZSocketService.sendMessage", "Error sending message", e);
                }
            }
        } else {
            Log.e("Vector.ZSocketService.sendMessage", "Sending " + message + " to " + peer_uuid);
            ZMQ.Socket psocket = peer_sockets.get(peer_uuid);
            psocket.send(message.getBytes(ZMQ.CHARSET), 0);
        }
    }

    private void dispatchSendMessage(final String peer_uuid, final String message) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.e("Vector.ZSocketService.dispatchSendMessage", "sendMessage");
                sendMessage(peer_uuid, message);
            }
        }).start();
    }

    // Sending a hello over the outgoing socket
    public void sendHello(String peer_uuid) {
        String username = getVector().username;
        Log.e("Vector.ZSocketService.sendHello", "Sending hello to " + peer_uuid);
        ZMQ.Socket psocket = peer_sockets.get(peer_uuid);
        try {
            JSONObject message_object = new JSONObject();
            message_object.put("type", "hello");
            message_object.put("username", username);
            psocket.send(message_object.toString().getBytes(ZMQ.CHARSET), 0);
        } catch (Exception e) {
            Log.e("Vector.ZSocketService.sendHello", "Error sending hello", e);
        }
    }
    public void sendHellos() {
        String username = getVector().username;
        Log.e("Vector.ZSocketService.sendHellos", "Sending hellos as " + username);
        for (String peer_uuid : peer_sockets.keySet()) {
            sendHello(peer_uuid);
        }
    }

    private void dispatchSendHellos() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.e("Vector.ZSocketService.dispatchSendHellos", "sendHellos");
                sendHellos();
            }
        }).start();
    }

    // Called when an IntentService is started
    // Happens outside the UI thread by default, no dispatch necessary here
    @Override
    protected void onHandleIntent(Intent intent) {
        peer_sockets = new HashMap<String, ZMQ.Socket>();
        peer_usernames = new HashMap<String, String>();
        run = true;

        // Listen for Intents to send a message or hello, or of new peers
        registerReceiver(sendReceiver, new IntentFilter(ZSocketService.MESSAGE_SEND));
        registerReceiver(helloReceiver, new IntentFilter(ZSocketService.HELLO_SEND));
        registerReceiver(joinReceiver, new IntentFilter(UDPBroadcastService.PEER_JOIN));
        Log.e("Vector.ZSocketService", "Filters set");

        try {
            // Create an incoming ROUTER socket
            ZMQ.Context zcontext = ZMQ.context(1);

            Log.e("Vector.ZSocketService", "Connecting to server...");

            zsocket = zcontext.socket(ZMQ.ROUTER);
            zsocket.bind("tcp://*:" + Integer.toString(getVector().port));

            while (run) {
                String sender = zsocket.recvStr();
                String message_json = zsocket.recvStr();
                Log.e("Vector.ZSocketService", "Received '" + message_json + "' from " + sender);
                messageReceived(sender, message_json);
            }
        } catch (Exception e) {
            Log.e("Vector.ZSocketService", "Error in run()", e);
        }
    }

    // Receive a message to send from MainActivity
    private BroadcastReceiver sendReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                String peer_uuid = bundle.getString(ZSocketService.MESSAGE_RECIPIENT);
                String message = bundle.getString(ZSocketService.MESSAGE_BODY);
                Log.e("Vector.ZSocketService.sendReceiver", "dispatching send...");
                dispatchSendMessage(peer_uuid, message);
            }
        }
    };

    // Receive a hello to send from MainActivity
    private BroadcastReceiver helloReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e("Vector.ZSocketService.helloReceiver", "dispatching hellos...");
            dispatchSendHellos();
        }
    };

    // Receive a peer that has joined from UDPBroadcastService
    // Create a socket connecting to the peer's port and add it to peer_sockets
    private BroadcastReceiver joinReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                String peer_uuid = bundle.getString(UDPBroadcastService.PEER_JOIN_UUID);
                String peer_address = bundle.getString(UDPBroadcastService.PEER_JOIN_ADDRESS);
                
                if (!peer_uuid.equals(getVector().uuid_str)) {
                    dispatchPeerJoined(peer_uuid, peer_address);
                    Log.e("Vector.ZSocketService.joinReceiver", "dispatched peerJoined");
                } else {
                    Log.e("Vector.ZSocketService.joinReceiver", "Strange, same uuid");
                }
            }
        }
    };

    private void peerJoined(String peer_uuid, String peer_address) {
        ZMQ.Context zcontext = ZMQ.context(1);
        ZMQ.Socket peer_socket = zcontext.socket(ZMQ.DEALER);
        peer_socket.setIdentity(getVector().uuid_str.getBytes(ZMQ.CHARSET));
        peer_socket.connect("tcp://" + peer_address);
        peer_sockets.put(peer_uuid, peer_socket);
        sendHello(peer_uuid);
    }

    private void dispatchPeerJoined(final String peer_uuid, final String peer_address) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.e("Vector.ZSocketService.Dispatch", "peerJoined");
                peerJoined(peer_uuid, peer_address);
            }
        }).start();
    }

    // Notify other Services & Activities that a new message was received
    // TODO: Serialize the message object itself to allow more flexibility
    private void notifyMessageReceived(String sender, String body) {
        Intent intent = new Intent(MESSAGE_RECEIVED);
        String sender_username = peer_usernames.get(sender);
        if (sender_username != null) {
            intent.putExtra(MESSAGE_SENDER, sender_username);
        } else {
            intent.putExtra(MESSAGE_SENDER, sender);
        }
        intent.putExtra(MESSAGE_BODY, body);
        sendBroadcast(intent);
    }

    // Notify other Services & Activities that a new hello was received
    // TODO: Serialize the hello object itself to allow more flexibility
    private void notifyHelloReceived(String sender, String username) {
        Intent intent = new Intent(HELLO_RECEIVED);
        String sender_username = peer_usernames.get(sender);
        if (sender_username != null) {
            intent.putExtra(HELLO_SENDER, sender_username);
        } else {
            intent.putExtra(HELLO_SENDER, sender);
        }
        intent.putExtra(HELLO_USERNAME, username);
        sendBroadcast(intent);
    }

    // Handle received message
    protected void messageReceived(String sender, String message_json) {
        try {
            // Parse the JSON encoded message
            JSONObject message_object = new JSONObject(message_json);
            String type = message_object.getString("type");
            if (type.equals("message")) {
                // Notify others of this message
                notifyMessageReceived(sender, message_object.getString("body"));
            } else if (type.equals("hello")) {
                // Keep track of uuid -> username connection
                peer_usernames.put(sender, message_object.getString("username"));
                // Notify others of this hello
                notifyHelloReceived(sender, message_object.getString("username"));
            }
        } catch (Exception e) {
            Log.e("Vector.ZSocketService.messageReceived", "Error receiving message", e);
        }
    }

    // Slight shortcut for getting at global app variables
    private VectorApplication getVector() {
        return (VectorApplication)getApplication();
    }

}


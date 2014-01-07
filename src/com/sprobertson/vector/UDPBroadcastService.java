package com.sprobertson.vector;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.UUID;
import java.lang.Runnable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class UDPBroadcastService extends Service {
    public static final String PEER_JOIN = "com.sprobertson.vector.PEER_JOIN";
    public static final String PEER_JOIN_UUID = "com.sprobertson.vector.PEER_JOIN_UUID";
    public static final String PEER_JOIN_ADDRESS = "com.sprobertson.vector.PEER_JOIN_ADDRESS";

    public static final long BROADCAST_INTERVAL = 1000; // ms
    public static final short BROADCAST_PORT = 5670;
    public static InetAddress BROADCAST_ADDRESS;

    private DatagramSocket bsocket;
    private ArrayList<String> seen_uuids = new ArrayList<String>();

    private Handler loopHandler = new Handler();
    private Timer loopTimer = null;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        try {
            // Create UDP socket
            BROADCAST_ADDRESS = InetAddress.getByName("255.255.255.255");
            bsocket = new DatagramSocket(5670);
            bsocket.setBroadcast(true);
            Log.i("Vector.UDPBroadcastService.onCreate", "Set stuff up");
        } catch (Exception e) {
            Log.e("Vector.UDPBroadcastService.onCreate", "Error creating UDP socket", e);
        }

        // Create broadcast task
        if (loopTimer != null) {
            loopTimer.cancel();
        } else {
            loopTimer = new Timer();
        }
        loopTimer.scheduleAtFixedRate(new UDPBroadcastTask(), 0, BROADCAST_INTERVAL);
        // TODO: Create receiver task
        new Thread(udpListener).start();
    }

    Runnable udpListener = new Runnable () {
        @Override
        public void run() {
            Log.i("Vector.UDPBroadcastService.udpListener", "UDP should be listening");
            try {
                while (true) {
                    byte[] recv_buf = new byte[255];
                    DatagramPacket packet = new DatagramPacket(recv_buf, recv_buf.length);
                    bsocket.receive(packet);
                    parseBeacon(packet.getAddress().getHostAddress(), packet.getData());
                    Log.i("Vector.UDPBroadcastService.udpListener", "Receiving...");
                }
            } catch (Exception e) {
                // TODO: Handle socket read error
                Log.e("Vector UDP read error", "errr", e);
            }
        }
    };

    class UDPBroadcastTask extends TimerTask {

        @Override
        public void run() {
            sendBeacon();

            if (true) return;
            loopHandler.post(new Runnable() {

                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), getDateTime(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private String getDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("[yyyy/MM/dd - HH:mm:ss]");
        return sdf.format(new Date());
    }

    // Broadcast a presence beacon
    public void sendBeacon() {
        try {
            byte[] beacon = buildBeacon();
            DatagramPacket packet = new DatagramPacket(beacon, beacon.length, BROADCAST_ADDRESS, 5670);
            bsocket.send(packet);
        } catch (Exception e) {
            Log.e("Vector UDP sendBeacon", "Error", e);
        }   
    }

    // Build a presence beacon for broadcasting
    public byte[] buildBeacon() {
        byte[] buffer = new byte[22];
        ByteBuffer bb = ByteBuffer.wrap(buffer);
        bb.put("ZRE".getBytes());
        bb.putInt(1);
        bb.putLong(4, getVector().uuid.getMostSignificantBits());
        bb.putLong(12, getVector().uuid.getLeastSignificantBits());
        bb.order(ByteOrder.nativeOrder());
        bb.putShort(20, getVector().port);
        return bb.array();
    }

    // Turn a byte array into a hex string representation
    final protected static char[] hexArray = "0123456789abcdef".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j=0; j<bytes.length; j++) {
            int v = bytes[j] & 0xff;
            hexChars[j*2] = hexArray[v >>> 4];
            hexChars[j*2+1] = hexArray[v & 0x0f];
        }
        return new String(hexChars);
    }

    // Parse incoming presence beacons for protocol and identity
    public void parseBeacon(String address, byte[] beacon) {
        ByteBuffer bb = ByteBuffer.wrap(beacon);
        byte[] _header = new byte[3];
        byte[] _version = new byte[1];
        byte[] _uuid = new byte[16];
        bb.get(_header);
        bb.get(_version);
        int version = (int)_version[0];
        bb.get(_uuid);
        String uuid = new String(bytesToHex(_uuid));
        bb.order(ByteOrder.nativeOrder());
        int port = (int)bb.getChar();
        if (true) Log.e("Vector UDP parseBeacon Parsed Beacon",
            new String(_header) + " : " +
            new String(Integer.toString(version)) + " : " +
            uuid + " : " +
            new String(Integer.toString(port))
        );

        if (!seen_uuids.contains(uuid)) {
            seen_uuids.add(uuid);
            Peer new_peer = new Peer(uuid, port);
            Log.e("Vector UDP New Peer", new_peer.toString());
            Intent intent = new Intent(UDPBroadcastService.PEER_JOIN);
            intent.putExtra(UDPBroadcastService.PEER_JOIN_UUID, uuid);
            intent.putExtra(UDPBroadcastService.PEER_JOIN_ADDRESS, address + ":" + Integer.toString(port));
            sendBroadcast(intent);
        }
    }

    // Slight shortcut for getting at global app variables
    private VectorApplication getVector() {
        return (VectorApplication)getApplication();
    }

}

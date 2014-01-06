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
    public static final long BROADCAST_INTERVAL = 1000; // ms

    private DatagramSocket bsocket;
    private ArrayList<String> seen_uuids = new ArrayList<String>();
    private static final UUID uuid = UUID.randomUUID();
    private static final short port = 5595;

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
            bsocket = new DatagramSocket(5670);
            bsocket.setBroadcast(true);
        } catch (Exception e) {
            // TODO: Handle socket creation error
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
            try {
                while (true) {
                    byte[] recv_buf = new byte[255];
                    DatagramPacket packet = new DatagramPacket(recv_buf, recv_buf.length);
                    bsocket.receive(packet);
                    parseBeacon(packet.getData());
                }
            } catch (Exception e) {
                // TODO: Handle socket read error
                Log.e("Reading error", "errr", e);
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
            InetAddress address = InetAddress.getByName("255.255.255.255");
            byte[] beacon = buildBeacon();
            DatagramPacket packet = new DatagramPacket(beacon, beacon.length, address, 5670);
            //bsocket.connect(address, 5670);
            bsocket.send(packet);
        } catch (Exception e) {
            Log.e("Announce", "Error", e);
        }   
    }

    // Build a presence beacon for broadcasting
    public byte[] buildBeacon() {
        byte[] buffer = new byte[22];
        ByteBuffer bb = ByteBuffer.wrap(buffer);
        bb.put("ZRE".getBytes());
        bb.putInt(1);
        bb.putLong(4, uuid.getMostSignificantBits());
        bb.putLong(12, uuid.getLeastSignificantBits());
        bb.order(ByteOrder.nativeOrder());
        bb.putShort(20, port);
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
    public void parseBeacon(byte[] beacon) {
        Log.e("Got beacon", "eh");
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
        Log.e("Header", new String(_header)); 
        Log.e("Version", new String(Integer.toString(version))); 
        Log.e("UUID", uuid); 
        Log.e("Port", new String(Integer.toString(port)));

        if (!seen_uuids.contains(uuid)) {
            seen_uuids.add(uuid);
            Peer newPeer = new Peer(uuid, port);
            Log.e("This is new:", newPeer.toString());
        }
    }

}

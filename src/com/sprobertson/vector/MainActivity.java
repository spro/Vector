package com.sprobertson.vector;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.util.Log;

import java.util.ArrayList;

public class MainActivity extends Activity
{
    public final static String EXTRA_MESSAGE = "com.sprobertson.vector.MESSAGE";

    private ListView list;
    private ArrayList<VectorMessage> arrayList;
    private MessagesAdapter adapter;
    //private ZSocketClient client;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // Create the username dialog
        LayoutInflater li = LayoutInflater.from(this);
        View promptView = li.inflate(R.layout.prompt, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setView(promptView);
        final EditText username_input = (EditText)promptView.findViewById(R.id.username_input);
        alertDialogBuilder
            .setCancelable(false)
            .setPositiveButton("OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            Log.i("Vector MainActivity", "Set username as " + username_input.getText());
                            ((VectorApplication)getApplication()).username = username_input.getText().toString();
                            sendHellos();
                        }
            });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();

        // Set up main sending layout
        final EditText editText = (EditText) findViewById(R.id.editText);
        Button send = (Button)findViewById(R.id.send_button);

        // Set up message list view
        list = (ListView)findViewById(R.id.list);
        arrayList = new ArrayList<VectorMessage>();
        adapter = new MessagesAdapter(this, arrayList);
        list.setAdapter(adapter);

        // Stop services
        stopService(new Intent(this, UDPBroadcastService.class));
        stopService(new Intent(this, ZSocketService.class));

        // Then start them
        startService(new Intent(this, ZSocketService.class));
        startService(new Intent(this, UDPBroadcastService.class));

        Log.i("Vector MainActivity startup", "Services started up");

        // Register for messages received with ZSocketService
        registerReceiver(messageReceiver, new IntentFilter(ZSocketService.MESSAGE_RECEIVED));

        // Set up send button listener
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String message = editText.getText().toString();

                sendMessage(message);
                arrayList.add(new VectorMessage("Me", message));

                adapter.notifyDataSetChanged();
                editText.setText("");
            }
        });

    }

    @Override
    public void onStop() {
        super.onStop();
        // Stop services
        stopService(new Intent(this, UDPBroadcastService.class));
        stopService(new Intent(this, ZSocketService.class));
    }

    // Receive a message from the ZSocketService
    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                String sender = bundle.getString(ZSocketService.MESSAGE_SENDER);
                String body = bundle.getString(ZSocketService.MESSAGE_BODY);
                Log.d("Vector.MainActivity.messageReceiver", "Adding message to list");
                arrayList.add(new VectorMessage(sender, body));
                adapter.notifyDataSetChanged();
            }
        }
    };

    // Send message with ZSocketService
    public void sendMessage(String body) {
        String recipient = "*";
        Intent intent = new Intent(ZSocketService.MESSAGE_SEND);
        intent.putExtra(ZSocketService.MESSAGE_RECIPIENT, recipient);
        intent.putExtra(ZSocketService.MESSAGE_BODY, body);
        sendBroadcast(intent);
        Log.e("Vector MainActivity send message", "broadcasting intent");
    }

    // Send hello with ZSocketService
    public void sendHellos() {
        Intent intent = new Intent(ZSocketService.HELLO_SEND);
        sendBroadcast(intent);
        Log.e("Vector MainActivity sendHellos", "broadcasting intent");
    }
}

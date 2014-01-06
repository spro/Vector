package com.sprobertson.vector;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import java.util.ArrayList;

public class MainActivity extends Activity
{
    public final static String EXTRA_MESSAGE = "com.sprobertson.vector.MESSAGE";

    private ListView list;
    private ArrayList<String> arrayList;
    private MessagesAdapter adapter;
    private ZSocketClient client;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        arrayList = new ArrayList<String>();
        final EditText editText = (EditText) findViewById(R.id.editText);
        Button send = (Button)findViewById(R.id.send_button);

        list = (ListView)findViewById(R.id.list);
        adapter = new MessagesAdapter(this, arrayList);
        list.setAdapter(adapter);

        // Start up broadcast service
        startService(new Intent(this, UDPBroadcastService.class));

        // Start up ZSocket server connection
        new connectTask().execute("");

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String message = editText.getText().toString();

                arrayList.add("Me: " + message);

                if (client != null) {
                    client.sendMessage(message);
                }

                adapter.notifyDataSetChanged();
                editText.setText("");
            }
        });

    }

    public class connectTask extends AsyncTask<String, String, ZSocketClient> {
        @Override
        protected ZSocketClient doInBackground(String... message) {
            client = new ZSocketClient(new ZSocketClient.OnMessageReceived() {
                @Override
                public void messageReceived(String sender, String message) {
                    publishProgress(sender + ": " + message);
                }
            });
            client.run();

            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);

            arrayList.add(values[0]);
            adapter.notifyDataSetChanged();
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_search:
                openSearch();
                return true;
            case R.id.action_settings:
                openSettings();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void openSearch() { }
    public void openSettings() { }

    /** Called when send button is pressed. */
    public void sendMessage(View view) {
        /*
        Intent intent = new Intent(this, DisplayMessageActivity.class);
        EditText editText = (EditText)findViewById(R.id.edit_message);
        String message = editText.getText().toString();
        intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);
        */
    }
}

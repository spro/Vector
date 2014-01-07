package com.sprobertson.vector;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class MessagesAdapter extends BaseAdapter {
    private ArrayList<VectorMessage> messages;
    private LayoutInflater layoutInflater;

    public MessagesAdapter(Context context, ArrayList<VectorMessage> messages) {
        this.messages = messages;
        layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return messages.size();
    }

    @Override
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = layoutInflater.inflate(R.layout.list_item, null);
        }

        TextView message_sender_view = (TextView) view.findViewById(R.id.message_sender);
        TextView message_body_view = (TextView) view.findViewById(R.id.message_body);
        TextView message_time_view = (TextView) view.findViewById(R.id.message_time);

        VectorMessage message = messages.get(position);

        message_sender_view.setText(message.sender);
        message_body_view.setText(message.body);
        message_time_view.setText(message.time);

        return view;
    }
}


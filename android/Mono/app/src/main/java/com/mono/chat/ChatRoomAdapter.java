package com.mono.chat;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mono.R;

import java.util.ArrayList;

/**
 * Created by hduan on 3/8/2016.
 */
public class ChatRoomAdapter extends RecyclerView.Adapter<ChatRoomAdapter.ChatViewHolder> {
    //types determine if the chat message is mine or others'
    public static final int MY_MESSAGE = 456;
    public static final int OTHERS_MESSAGE = 351;

    private ArrayList<ChatMessage> chatMessages;
    private Context mContext;

    public ChatRoomAdapter(ArrayList<ChatMessage> chatMessages, Context context) {
        this.chatMessages = chatMessages;
        mContext = context;
    }

    @Override
    public ChatViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        if (viewType == MY_MESSAGE) {
            view = View.inflate(mContext, R.layout.chat_message_left, null);
        } else {
            view = View.inflate(mContext, R.layout.chat_message_right, null);
        }
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ChatViewHolder holder, int position) {
        holder.chatText.setText(chatMessages.get(position).getMessageText());
    }

    @Override
    public int getItemViewType(int position) {
        if (chatMessages.get(position).isMyMessage()) {
            return MY_MESSAGE;
        } else {
            return OTHERS_MESSAGE;
        }
    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView chatText;
        public ChatViewHolder(View v) {
            super(v);
            chatText = (TextView) v.findViewById(R.id.txtMessage);
        }
    }
}

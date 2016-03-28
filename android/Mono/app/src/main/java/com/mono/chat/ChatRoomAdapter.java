package com.mono.chat;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mono.R;
import com.mono.model.Attendee;
import com.mono.model.Message;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by hduan on 3/8/2016.
 */
public class ChatRoomAdapter extends RecyclerView.Adapter<ChatRoomAdapter.ChatViewHolder> {
    //types determine if the chat message is mine or others'
    public static final int MY_MESSAGE = 456;
    public static final int OTHERS_MESSAGE = 351;

    private String myId;
    private Map<String, Attendee> chatAttendees;
    private List<Message> chatMessages;
    private Context mContext;
    private SimpleDateFormat sdf;

    public ChatRoomAdapter(String myId, ChatAttendeeMap attendeeMap, List<Message> chatMessageList, Context context) {
        this.myId = myId;
        this.chatAttendees = attendeeMap.getChatAttendeeMap();
        this.chatMessages = chatMessageList;
        mContext = context;
        sdf = new SimpleDateFormat(context.getString(R.string.chat_time_format));
    }

    @Override
    public ChatViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        if (viewType == MY_MESSAGE) {
            view = LayoutInflater.from(mContext).inflate(R.layout.chat_message_left, null);
        } else {
            view = LayoutInflater.from(mContext).inflate(R.layout.chat_message_right, null);
        }
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ChatViewHolder holder, int position) {
        Message message = chatMessages.get(position);
        holder.chatText.setText(message.getMessageText());
        String formattedTime = sdf.format(new Date(message.getTimestamp()));
        holder.timeStamp.setText(formattedTime);
        if (holder.getItemViewType() == OTHERS_MESSAGE) {
            Attendee attendee = chatAttendees.get(message.getUserId());
            holder.senderName.setText(attendee.userName);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (chatMessages.get(position).getUserId().equals(myId)) {
            return MY_MESSAGE;
        } else {
            return OTHERS_MESSAGE;
        }
    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }
/*
    public String getFormattedDate(Context context, long smsTimeInMilis) {
        Calendar smsTime = Calendar.getInstance();
        smsTime.setTimeInMillis(smsTimeInMilis);

        Calendar now = Calendar.getInstance();

        final String timeFormatString = "h:mm aa";
        final String dateTimeFormatString = "EEEE, MMMM d, h:mm aa";
        final long HOURS = 60 * 60 * 60;
        if(now.get(Calendar.DATE) == smsTime.get(Calendar.DATE) ){
            return "Today " + DateFormat.format(timeFormatString, smsTime);
        }else if(now.get(Calendar.DATE) - smsTime.get(Calendar.DATE) == 1 ){
            return "Yesterday " + DateFormat.format(timeFormatString, smsTime);
        }else if(now.get(Calendar.YEAR) == smsTime.get(Calendar.YEAR)){
            return DateFormat.format(dateTimeFormatString, smsTime).toString();
        }else
            return DateFormat.format("MMMM dd yyyy, h:mm aa", smsTime).toString();
    }
*/
    class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView senderName;
        TextView timeStamp;
        TextView chatText;
        public ChatViewHolder(View v) {
            super(v);
            senderName = (TextView) v.findViewById(R.id.senderName);
            chatText = (TextView) v.findViewById(R.id.txtMessage);
            timeStamp = (TextView) v.findViewById(R.id.messageTime);
        }
    }
}

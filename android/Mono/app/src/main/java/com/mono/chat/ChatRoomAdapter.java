package com.mono.chat;

import android.content.Context;
import android.graphics.PorterDuff;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.mono.R;
import com.mono.model.Attendee;
import com.mono.model.Message;
import com.mono.util.Colors;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Created by hduan on 3/8/2016.
 */
public class ChatRoomAdapter extends RecyclerView.Adapter<ChatRoomAdapter.ChatViewHolder> {
    //types determine if the chat message is mine or others'
    public static final int MY_MESSAGE = 456;
    public static final int OTHERS_MESSAGE = 351;

    private static final SimpleDateFormat DATE_TIME_FORMAT;
    private static final SimpleDateFormat TIME_FORMAT;

    private Context context;

    private String myId;
    private Map<String, Attendee> chatAttendees;
    private Map<String, Integer> userColors = new HashMap<>();
    private List<Message> chatMessages;

    static {
        DATE_TIME_FORMAT = new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault());
        TIME_FORMAT = new SimpleDateFormat("h:mm a", Locale.getDefault());
    }

    public ChatRoomAdapter(Context context, String myId, ChatAttendeeMap attendeeMap,
            List<Message> chatMessageList) {
        this.context = context;
        this.myId = myId;
        this.chatAttendees = attendeeMap.getAttendeeMap();
        this.chatMessages = chatMessageList;
    }

    @Override
    public ChatViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.chat_message, parent, false);

        ViewGroup bubble = (ViewGroup) view.findViewById(R.id.chat_bubble);
        View arrow = bubble.findViewById(R.id.arrow);

        if (viewType == MY_MESSAGE) {
            arrow.setRotation(-90);
        } else {
            view.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            arrow.setRotation(90);
        }

        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ChatViewHolder holder, int position) {
        Message message = chatMessages.get(position);

        int color;

        if (holder.getItemViewType() == MY_MESSAGE) {
            holder.senderName.setText(R.string.me);
            color = Colors.getColor(context, R.color.blue_1);
        } else {
            String userId = message.getUserId();

            Attendee attendee = chatAttendees.get(userId);
            if (attendee != null) {
                holder.senderName.setText(attendee.userName);
            }

            holder.senderName.setText(message.getUserId());

            if (!userColors.containsKey(userId)) {
                userColors.put(userId, getRandomColor());
            }

            color = userColors.get(userId);
        }

        holder.sendImage.getDrawable().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);

        View arrow = holder.bubble.findViewById(R.id.arrow);
        arrow.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);

        holder.chatText.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        holder.chatText.setText(message.getMessageText());

        int textColor;
        if (Colors.getLuma(color) < 160) {
            textColor = Colors.getColor(context, android.R.color.white);
        } else {
            textColor = Colors.getColor(context, R.color.gray_dark);
        }
        holder.chatText.setTextColor(textColor);

        holder.timeStamp.setText(getDateString(message.getTimestamp()));
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

    public int getRandomColor() {
        int[] colorIds = {
            R.color.blue,
            R.color.blue_1,
            R.color.blue_dark,
            R.color.brown,
            R.color.green,
            R.color.lavender,
            R.color.orange,
            R.color.purple,
            R.color.red_1,
            R.color.yellow_1
        };

        int colorId = colorIds[(int) (Math.random() * colorIds.length) % colorIds.length];
        return Colors.getColor(context, colorId);
    }

    private String getDateString(long time) {
        String dateString;

        Calendar calendar = Calendar.getInstance();
        int currentYear = calendar.get(Calendar.YEAR);
        int currentMonth = calendar.get(Calendar.MONTH);
        int currentDay = calendar.get(Calendar.DAY_OF_MONTH);

        calendar.add(Calendar.DAY_OF_MONTH, -1);
        int yesterYear = calendar.get(Calendar.YEAR);
        int yesterMonth = calendar.get(Calendar.MONTH);
        int yesterDay = calendar.get(Calendar.DAY_OF_MONTH);

        calendar.setTimeInMillis(time);
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        if (year == currentYear && month == currentMonth && day == currentDay) {
            dateString = "Today, " + TIME_FORMAT.format(time);
        } else if (year == yesterYear && month == yesterMonth && day == yesterDay) {
            dateString = "Yesterday, " + TIME_FORMAT.format(time);
        } else {
            dateString = DATE_TIME_FORMAT.format(time);
        }

        return dateString;
    }
    public class ChatViewHolder extends RecyclerView.ViewHolder {

        public TextView senderName;
        public ImageView sendImage;
        public ViewGroup bubble;
        public TextView chatText;
        public TextView timeStamp;

        public ChatViewHolder(View itemView) {
            super(itemView);

            senderName = (TextView) itemView.findViewById(R.id.senderName);
            sendImage = (ImageView) itemView.findViewById(R.id.senderImage);
            bubble = (ViewGroup) itemView.findViewById(R.id.chat_bubble);
            chatText = (TextView) itemView.findViewById(R.id.text);
            timeStamp = (TextView) itemView.findViewById(R.id.messageTime);
        }
    }
}

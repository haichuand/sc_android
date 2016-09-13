package com.mono.chat;

import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.mono.MediaManager;
import com.mono.R;
import com.mono.db.DatabaseHelper;
import com.mono.db.dao.ConversationDataSource;
import com.mono.model.Attendee;
import com.mono.model.Media;
import com.mono.model.Message;
import com.mono.network.HttpServerManager;
import com.mono.network.NetworkManager;
import com.mono.util.BitmapHelper;
import com.mono.util.Colors;
import com.mono.util.Pixels;

import org.json.JSONObject;

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

    private static final int PHOTO_WIDTH_DP = 120;
    private static final int PHOTO_HEIGHT_DP = 90;

    private static final SimpleDateFormat DATE_TIME_FORMAT;
    private static final SimpleDateFormat TIME_FORMAT;

    private Context context;

    private String myId;
    private Map<String, Attendee> chatAttendees;
    private Map<String, Integer> userColors = new HashMap<>();
    private List<Message> chatMessages;
    private static int[] colorIds = {
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
        String userId = message.getSenderId();
        if (message.showMessageSender) {
            holder.senderName.setVisibility(View.VISIBLE);
            if (holder.getItemViewType() == MY_MESSAGE) {
                holder.senderName.setText(R.string.me);
            } else {
                Attendee attendee = chatAttendees.get(userId);
                if (attendee != null) {
                    holder.senderName.setText(attendee.toString());
                } else {
                    holder.senderName.setText(userId);
                }
            }
        } else {
            holder.senderName.setVisibility(View.GONE);
        }

        int color;
        if (holder.getItemViewType() == MY_MESSAGE) {
            color = Colors.getColor(context, R.color.blue_1);
        } else {
            color = getColorByUserId(userId);
        }

        holder.senderImage.getDrawable().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);

        View arrow = holder.bubble.findViewById(R.id.arrow);
        arrow.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);

        handleAttachments(holder, message);

        holder.container.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);

        String msgText = message.getMessageText();
        if (!msgText.isEmpty()) {
            holder.chatText.setText(msgText);
            holder.chatText.setVisibility(View.VISIBLE);

            int textColor;
            if (Colors.getLuma(color) < 160) {
                textColor = Colors.getColor(context, android.R.color.white);
            } else {
                textColor = Colors.getColor(context, R.color.gray_dark);
            }
            holder.chatText.setTextColor(textColor);
        } else {
            holder.chatText.setVisibility(View.GONE);
        }

        if (message.showWarningIcon) {
            holder.warningIcon.setVisibility(View.VISIBLE);
        } else {
            holder.warningIcon.setVisibility(View.INVISIBLE);
        }

        if (message.showMessageTime) {
            holder.groupTime.setVisibility(View.VISIBLE);
            holder.groupTime.setText(getDateString(message.getTimestamp()));
        } else {
            holder.groupTime.setVisibility(View.GONE);
        }

        if (position == chatMessages.size() - 1) {
            float dpCoeff = context.getResources().getDisplayMetrics().density;
            holder.chatLayout.setPadding(0, (int) (10 * dpCoeff), 0, (int) (10 * dpCoeff));
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (chatMessages.get(position).getSenderId().equals(myId)) {
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
        int colorId = colorIds[(int) (Math.random() * colorIds.length) % colorIds.length];
        return Colors.getColor(context, colorId);
    }

    public int getColorByUserId (String userId) {
        int colorId = colorIds[Math.abs(userId.hashCode()) % colorIds.length];
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

    public void handleAttachments(final ChatViewHolder holder, final Message message) {
        holder.attachments.removeAllViews();

        if (message.attachments != null) {
            final int width = Pixels.pxFromDp(context, PHOTO_WIDTH_DP);
            final int height = Pixels.pxFromDp(context, PHOTO_HEIGHT_DP);

            for (final Media media : message.attachments) {
                if (media.size < 0) {
                    TextView textView = new TextView(context);
                    textView.setText(R.string.attachment_error);
                    textView.setTextColor(Colors.getColor(context, R.color.red));
                    textView.setTextSize(14);

                    holder.attachments.addView(textView);
                    continue;
                }

                String path = media.uri.toString();

                final ProgressBar progressBar = new ProgressBar(context, null,
                    android.R.attr.progressBarStyle);
                holder.attachments.addView(progressBar);

                if (media.size == 0) {
                    NetworkManager.getInstance(context).get(HttpServerManager.DOWNLOAD_URL + path,
                        new NetworkManager.ResponseListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                media.size = -1;

                                ConversationDataSource dataSource =
                                    DatabaseHelper.getDataSource(context,
                                        ConversationDataSource.class);
                                dataSource.setMessageAttachments(message.getMessageId(),
                                    message.attachments);

                                notifyItemChanged(holder.getAdapterPosition());
                            }

                            @Override
                            public void onResponse(JSONObject response) {

                            }

                            @Override
                            public void onResponse(Uri uri) {
                                media.uri = uri;
                                media.size = 1;

                                ConversationDataSource dataSource =
                                    DatabaseHelper.getDataSource(context,
                                        ConversationDataSource.class);
                                dataSource.setMessageAttachments(message.getMessageId(),
                                    message.attachments);

                                notifyItemChanged(holder.getAdapterPosition());
                            }
                        }
                    );

                    continue;
                }

                new AsyncTask<Object, Void, byte[]>() {
                    @Override
                    protected byte[] doInBackground(Object... params) {
                        String path = (String) params[0];

                        byte[] thumbnail = null;

                        switch (media.type) {
                            case Media.IMAGE:
                                thumbnail = MediaManager.createThumbnail(path);
                                break;
                            case Media.VIDEO:
                                thumbnail = MediaManager.createVideoThumbnail(path);
                                break;
                        }

                        return thumbnail;
                    }

                    @Override
                    protected void onPostExecute(byte[] result) {
                        holder.attachments.removeView(progressBar);

                        if (result != null) {
                            int overlayResId = 0;

                            switch (media.type) {
                                case Media.VIDEO:
                                    overlayResId = R.drawable.ic_play_circle_outline;
                                    break;
                            }

                            View view = BitmapHelper.createThumbnail(context, null, result,
                                overlayResId, width, height);
                            view.setOnClickListener(new OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    Intent intent = new Intent();
                                    intent.setAction(Intent.ACTION_VIEW);
                                    intent.setDataAndType(Uri.parse("file://" + media.uri), media.type);

                                    context.startActivity(intent);
                                }
                            });

                            holder.attachments.addView(view);
                        }
                    }
                }.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, path);
            }
        }
    }

    public class ChatViewHolder extends RecyclerView.ViewHolder {
        public LinearLayout chatLayout;
        public TextView senderName;
        public ImageView senderImage;
        public ViewGroup bubble;
        public ViewGroup container;
        public ViewGroup attachments;
        public TextView chatText;
//        public TextView timeStamp;
        public ImageView warningIcon;
        public TextView groupTime;

        public ChatViewHolder(View itemView) {
            super(itemView);
            chatLayout = (LinearLayout) itemView.findViewById(R.id.chat_layout);
            senderName = (TextView) itemView.findViewById(R.id.senderName);
            senderImage = (ImageView) itemView.findViewById(R.id.senderImage);
            bubble = (ViewGroup) itemView.findViewById(R.id.chat_bubble);
            container = (ViewGroup) itemView.findViewById(R.id.container);
            attachments = (ViewGroup) itemView.findViewById(R.id.attachments);
            chatText = (TextView) itemView.findViewById(R.id.text);
//            timeStamp = (TextView) itemView.findViewById(R.id.messageTime);
            warningIcon = (ImageView) itemView.findViewById(R.id.warning_icon);
            groupTime = (TextView) itemView.findViewById(R.id.group_time);
        }
    }
}

package com.mono.details;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mono.R;
import com.mono.model.Event;
import com.mono.model.Reminder;
import com.mono.util.Colors;
import com.mono.util.Constants;
import com.mono.util.Pixels;

/**
 * This class is used to handle the reminders section located in Event Details.
 *
 * @author Gary Ng
 */
public class ReminderPanel implements EventDetailsActivity.PanelInterface {

    private static final int DEFAULT_INDEX = 2;
    private static final float WIDTH_DP = 150f;
    private static final float MARGIN_DP = 2f;

    private static final CharSequence[] ITEMS;
    private static final long[] VALUES;

    private EventDetailsActivity activity;
    private ViewGroup reminders;

    private Event event;

    static {
        VALUES = new long[]{
            0,
            5 * Constants.MINUTE_MS,
            10 * Constants.MINUTE_MS,
            15 * Constants.MINUTE_MS,
            30 * Constants.MINUTE_MS,
            Constants.HOUR_MS,
            2 * Constants.HOUR_MS,
            12 * Constants.HOUR_MS,
            Constants.DAY_MS,
        };
        // Create Time Strings from Milliseconds
        ITEMS = new String[VALUES.length];
        for (int i = 0; i < ITEMS.length; i++) {
            ITEMS[i] = getTimeString(VALUES[i]);
        }
    }

    public ReminderPanel(EventDetailsActivity activity) {
        this.activity = activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        reminders = (ViewGroup) activity.findViewById(R.id.reminders);
        // Create Add Reminder Button
        LayoutInflater inflater = LayoutInflater.from(activity);
        View view = inflater.inflate(R.layout.reminder_item, null, false);

        TextView text = (TextView) view.findViewById(R.id.text);
        text.setText(R.string.reminder_add);

        ImageView icon = (ImageView) view.findViewById(R.id.icon);
        icon.setColorFilter(Colors.getColor(activity, R.color.green));
        icon.setImageResource(R.drawable.ic_add_circle_outline);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            Pixels.pxFromDp(activity, WIDTH_DP), ViewGroup.LayoutParams.WRAP_CONTENT
        );

        view.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                showReminderOptions();
            }
        });

        reminders.addView(view, params);
    }

    @Override
    public void setVisible(boolean visible) {
        View view = activity.findViewById(R.id.reminders_layout);
        view.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    @Override
    public void setEnabled(boolean enabled) {

    }

    /**
     * Initialize this panel using the given event.
     *
     * @param event The instance of the event.
     */
    @Override
    public void setEvent(Event event) {
        this.event = event;

        if (event.id == null) {
            Reminder reminder = new Reminder();
            reminder.minutes = (int) (VALUES[DEFAULT_INDEX] / Constants.MINUTE_MS);

            createReminder(reminder);
            event.reminders.add(reminder);
        } else {
            for (Reminder reminder : event.reminders) {
                createReminder(reminder);
            }
        }
    }

    /**
     * Create a reminder item using the reminder information.
     *
     * @param reminder Reminder used to create item.
     */
    public void createReminder(Reminder reminder) {
        LayoutInflater inflater = LayoutInflater.from(activity);
        final View itemView = inflater.inflate(R.layout.reminder_item, null, false);

        TextView text = (TextView) itemView.findViewById(R.id.text);
        text.setText(getTimeString(reminder.minutes * Constants.MINUTE_MS));

        ImageView icon = (ImageView) itemView.findViewById(R.id.icon);
        icon.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                int index = reminders.indexOfChild(itemView);
                event.reminders.remove(index);
                reminders.removeViewAt(index);
            }
        });

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            Pixels.pxFromDp(activity, WIDTH_DP), ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.bottomMargin = Pixels.pxFromDp(activity, MARGIN_DP);

        reminders.addView(itemView, Math.max(reminders.getChildCount() - 1, 0), params);
    }

    /**
     * Handle the action of clicking on the reminder to display time options.
     */
    public void showReminderOptions() {
        AlertDialog.Builder builder =
            new AlertDialog.Builder(activity, R.style.AppTheme_Dialog_Alert);
        builder.setTitle(activity.getString(R.string.reminders));

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            private Reminder reminder;

            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        // Default Reminder Option
                        if (reminder == null) {
                            reminder = new Reminder();
                            reminder.minutes = (int) (VALUES[DEFAULT_INDEX] / Constants.MINUTE_MS);
                        }
                        // Set Reminder to Event
                        if (!event.reminders.contains(reminder)) {
                            createReminder(reminder);
                            event.reminders.add(reminder);
                        }
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        break;
                    default:
                        // Set Reminder Option
                        if (which >= 0) {
                            reminder = new Reminder();
                            reminder.minutes = (int) (VALUES[which] / Constants.MINUTE_MS);
                        }
                        break;
                }
            }
        };

        builder.setSingleChoiceItems(ITEMS, DEFAULT_INDEX, listener);
        builder.setPositiveButton(R.string.okay, listener);
        builder.setNegativeButton(R.string.cancel, listener);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Helper function to convert milliseconds into readable string.
     *
     * @param milliseconds Time to convert to string.
     * @return a time string.
     */
    private static String getTimeString(long milliseconds) {
        String str;

        if (milliseconds == 0) {
            str = "On Time";
        } else if (milliseconds < Constants.MINUTE_MS) {
            milliseconds /= Constants.SECOND_MS;
            str = milliseconds + " Second";
        } else if (milliseconds < Constants.HOUR_MS) {
            milliseconds /= Constants.MINUTE_MS;
            str = milliseconds + " Minute";
        } else if (milliseconds < Constants.DAY_MS) {
            milliseconds /= Constants.HOUR_MS;
            str = milliseconds + " Hour";
        } else {
            milliseconds /= Constants.DAY_MS;
            str = milliseconds + " Day";
        }

        return str + (milliseconds > 1 ? "s" : "");
    }
}

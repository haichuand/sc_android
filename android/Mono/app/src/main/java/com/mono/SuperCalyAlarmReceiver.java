package com.mono;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.mono.parser.KmlDownloadingService;

import java.util.Calendar;

/**
 * Created by xuejing on 3/13/16.
 */
public class SuperCalyAlarmReceiver extends BroadcastReceiver {

    public static final String TAG = "SuperCalyAlarmReceiver";
    public static final String ACTION_ALARM_RECEIVER = "ACTION_ALARM_RECEIVER";
    private Calendar c = Calendar.getInstance();

    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            if (ACTION_ALARM_RECEIVER.equals(intent.getAction())) {
                Log.d(TAG, new Exception().getStackTrace()[0].getMethodName() + " " + c.getTime());
                Intent i = new Intent(context, KmlDownloadingService.class);
                i.putExtra("foo", "bar");
                context.startService(i);
            }
        }
    }
}

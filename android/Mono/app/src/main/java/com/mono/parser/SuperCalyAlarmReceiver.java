package com.mono.parser;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.mono.SuperCalyPreferences;
import com.mono.parser.KmlDownloadingService;

import java.lang.ref.WeakReference;
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
                i.putExtra(KmlDownloadingService.TYPE, KmlDownloadingService.REGULAR);
                context.startService(i);
            }
        }
    }
}

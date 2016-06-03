package com.mono.parser;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import com.mono.SuperCalyPreferences;
import com.mono.parser.KmlDownloadingService;
import com.mono.parser.KmlLocationService;

/**
 * Created by xuejing on 3/14/16.
 */
public class SupercalyDownloadReceiver extends BroadcastReceiver {

    public static final String TAG = "DownloadReceiver";
    private SharedPreferences sharedPreferences;

    public void onReceive(Context context, Intent intent) {

        Log.d(TAG, "downloadReceiver onReceive has been called");
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        long downloadId = intent.getLongExtra(
                DownloadManager.EXTRA_DOWNLOAD_ID, -1L);

        DownloadManager manager =
                (DownloadManager) context.getSystemService(Activity.DOWNLOAD_SERVICE);
        Cursor cursor = manager.query(new DownloadManager.Query().setFilterById(downloadId));
        Uri uri = null;

        if (cursor.moveToNext()) {
            int index = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
            int status = cursor.getInt(index);

            switch (status) {
                case DownloadManager.STATUS_SUCCESSFUL:
                    String fileName = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_TITLE));

                    if(fileName.endsWith(".kml")) {
                        Intent iIntent = new Intent(context, KmlLocationService.class);
                        if(fileName.startsWith("FirstTimeLocation")) {
                            iIntent.putExtra("fileName", fileName);
                            iIntent.putExtra(KmlDownloadingService.TYPE, KmlDownloadingService.FIRST_TIME);
                            context.startService(iIntent);
                        }
                        else if(fileName.startsWith("Today") || fileName.startsWith("Yesterday")) {
                            int curCounter = sharedPreferences.getInt(SuperCalyPreferences.KML_DOWNLOAD_COUNTER, 0);
                            sharedPreferences.edit().putInt(SuperCalyPreferences.KML_DOWNLOAD_COUNTER, curCounter+1).apply();
                            curCounter = sharedPreferences.getInt(SuperCalyPreferences.KML_DOWNLOAD_COUNTER, 0);
                            if(curCounter != 0 && curCounter%2 == 0) {
                                sharedPreferences.edit().putInt(SuperCalyPreferences.KML_DOWNLOAD_COUNTER, 0).apply();
                                iIntent.putExtra(KmlDownloadingService.TYPE, KmlDownloadingService.REGULAR);
                                context.startService(iIntent);
                            }
                        }

                    }
                    break;
                default:
                    Log.d(TAG, "download failed");
                    break;
            }

        }
        cursor.close();

    }
}

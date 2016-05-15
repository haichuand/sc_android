package com.mono;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.mono.parser.KmlDownloadingService;
import com.mono.parser.KmlLocationService;

/**
 * Created by xuejing on 3/14/16.
 */
public class SupercalyDownloadReceiver extends BroadcastReceiver {

    public static final String TAG = "DownloadReceiver";

    public void onReceive(Context context, Intent intent) {

        Log.d(TAG, "downloadReceiver onReceive has been called");
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
                    Intent iIntent = new Intent(context, KmlLocationService.class);
                    iIntent.putExtra("fileName", fileName);
                    context.startService(iIntent);

                    break;
                default:
                    Log.d(TAG, "download failed");
                    break;
            }

        }
        cursor.close();

    }
}

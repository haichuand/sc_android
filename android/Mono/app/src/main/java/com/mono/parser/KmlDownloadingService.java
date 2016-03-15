package com.mono.parser;

import android.app.Activity;
import android.app.DownloadManager;
import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;

import android.util.Log;
import android.webkit.CookieManager;

import com.mono.MainActivity;

import java.io.File;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by xuejing on 3/13/16.
 */
public class KmlDownloadingService extends IntentService {

    private static final String TAG = "Kml Downloading Service";
    private long downloadId;

    public static final String KML_FILENAME = "LocationHistory.kml";

    public static final String COOKIE_URL = "https://www.google.com/maps/timeline";
    public static final String KML_URL = "https://www.google.com/maps/timeline/kml?authuser=0";



    public KmlDownloadingService() {
        super(TAG);
    }

    public void onCreate () {
        super.onCreate();
    }

    protected void onHandleIntent (Intent intent) {
        Log.i(TAG, "Service running");
        if(isSignedIn()) {
            downloadKML(KML_URL + "&pb=" + getPbValue(),KML_FILENAME);
        }
    }

    private void downloadKML (String url, String filename) {
        Log.d(TAG, "downloadKML():" + url + " to " + filename);
        CookieManager cookieManager = CookieManager.getInstance();
        String cookie = cookieManager.getCookie(COOKIE_URL);

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.addRequestHeader("Cookie", cookie);

        String storage = Environment.getExternalStorageDirectory().getPath() + "/";
        File file = new File(storage + MainActivity.APP_DIR + filename);
        if (file.exists()) {
            file.delete();
        }
        request.setDestinationUri(Uri.fromFile(file));

        DownloadManager manager =
                (DownloadManager) getApplicationContext().getSystemService(Activity.DOWNLOAD_SERVICE);
        downloadId = manager.enqueue(request);
    }

    public boolean isSignedIn() {
        CookieManager cookieManager = CookieManager.getInstance();
        String cookie = cookieManager.getCookie(COOKIE_URL);

        return cookie != null && cookie.contains("SID=");
    }

    private String getPbValue() {
        Calendar cal = getDate();
        String year = Integer.toString(cal.get(Calendar.YEAR));
        String month =Integer.toString(cal.get(Calendar.MONTH));
        String date = Integer.toString(cal.get(Calendar.DATE));
        return "!1m8!1m3!1i"+ year + "!2i" +month + "!3i" + date + "!2m3!1i" + year + "!2i" +month + "!3i" + date;
    }

    private Calendar getDate() {
        long timestampLong = System.currentTimeMillis();
        Date d = new Date(timestampLong);
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        return c;
    }
 }

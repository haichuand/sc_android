package com.mono.parser;

import android.app.IntentService;
import android.content.Intent;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.webkit.CookieManager;


import com.mono.SuperCalyPreferences;
import com.mono.db.DatabaseValues;
import com.mono.model.Event;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Created by xuejing on 3/13/16.
 */
public class KmlDownloadingService extends IntentService {

    private KmlParser kmlparser;
    private static final String TAG = "Kml Downloading Service";
    static HashMap<Integer, Integer> endOfMonthMap;
    static {
        endOfMonthMap = new HashMap<>();
        endOfMonthMap.put(0,31);
        endOfMonthMap.put(1,28);
        endOfMonthMap.put(2,31);
        endOfMonthMap.put(3,30);
        endOfMonthMap.put(4,31);
        endOfMonthMap.put(5,30);
        endOfMonthMap.put(6,31);
        endOfMonthMap.put(7,31);
        endOfMonthMap.put(8,30);
        endOfMonthMap.put(9,31);
        endOfMonthMap.put(10,30);
        endOfMonthMap.put(11,31);
    }

    public static final String KML_FILENAME_TODAY = "TodayLocationHistory.kml";
    public static final String REGULAR = "regular";
    public static final String FIRST_TIME = "firstTime";
    public static final String TYPE = "Type";

    public static final String COOKIE_URL = "https://www.google.com/maps/timeline";
    public static final String KML_URL = "https://www.google.com/maps/timeline/kml?authuser=0";

    public KmlDownloadingService() {
        super(TAG);
    }

    public void onCreate () {
        super.onCreate();
    }

    protected void onHandleIntent (Intent intent) {
        String downloadType = intent.getExtras().getString(TYPE);
     //   Log.i(TAG, "downloadType: "+downloadType);
        if(KML.isSignedIn()) {
            if(downloadType.equals(REGULAR)) {
                // Reading from SharedPreferences
                SharedPreferences settings = getSharedPreferences(SuperCalyPreferences.LAST_MODIFIED_KML, MODE_PRIVATE);
                String value = settings.getString("lastModified", "");
                Calendar c = Calendar.getInstance();
                long diff = Math.abs(Long.parseLong(value) - c.getTimeInMillis());
                int diffInDays = (int)(diff / (24 * 60 * 60 * 1000));
                for(int i = 0;i <= diffInDays; i++) {
                    downloadKML(KML_URL + "&pb=" + getPbValue(0), 0);
                }
            }
            else {
                SharedPreferences settings = getSharedPreferences(SuperCalyPreferences.LAST_MODIFIED_KML, MODE_PRIVATE);
                Calendar c = Calendar.getInstance();
                // Writing data to SharedPreferences
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("lastModified",Long.toString(c.getTimeInMillis()));
                editor.commit();
                for(int i = 0; i <= 365; i++) {
                    downloadKML(KML_URL + "&pb=" + getPbValue(i), i);
                }
            }
        }
    }

    private void downloadKML (String url, int day) {

        try {
            // Request + Response
            StringBuilder builder = new StringBuilder();

            CookieManager cookieManager = CookieManager.getInstance();
            String cookie = cookieManager.getCookie(COOKIE_URL);

            URLConnection connection = new URL(url).openConnection();
            connection.addRequestProperty("Cookie", cookie);

            InputStream inputStream = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            String nextLine;
            while ((nextLine = reader.readLine()) != null) {
                builder.append(nextLine + "\n");
            }

            Intent i = new Intent(getApplicationContext(), KmlLocationService.class);
            i.putExtra("dataString", builder.toString());
            i.putExtra("loopNumber", day);
            getApplicationContext().startService(i);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

//    public boolean isSignedIn() {
//        CookieManager cookieManager = CookieManager.getInstance();
//        String cookie = cookieManager.getCookie(COOKIE_URL);
//
//        return cookie != null && cookie.contains("SID=");
//    }

    private String getPbValue(int dayBeforeToday) {
        Calendar cal = getDate(dayBeforeToday);
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int date = cal.get(Calendar.DATE);
        return "!1m8!1m3!1i"+ year + "!2i" +month + "!3i" + date + "!2m3!1i" + year + "!2i" +month + "!3i" + date;
    }

    private Calendar getDate(int dayBeforeToday) {
        long timestampLong = System.currentTimeMillis()- (long)dayBeforeToday*24*60*60*1000;
        Date d = new Date(timestampLong);
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        return c;
    }
 }

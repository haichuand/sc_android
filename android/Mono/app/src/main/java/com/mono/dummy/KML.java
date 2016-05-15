package com.mono.dummy;

import android.app.Activity;
import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.app.DownloadManager.Request;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.webkit.CookieManager;
import android.widget.Toast;

import com.mono.MainActivity;
import com.mono.MainInterface;
import java.io.File;

public class KML {

    public static final String KML_FILENAME = "test.kml";

    public static final String COOKIE_URL = "https://www.google.com/maps/timeline";
    public static final String KML_URL = "https://www.google.com/maps/timeline/kml?authuser=0";
    public static final String TEST_PB_VALUE = "!1m8!1m3!1i2015!2i10!3i24!2m3!1i2015!2i10!3i24";

    private Context kmlContext;
    private MainInterface mainInterface;

    private long downloadId;
    private BroadcastReceiver downloadReceiver;

    private KMLParameters parameters;

    public KML(Context context, MainInterface mainInterface) {
        this.kmlContext = context;
        this.mainInterface = mainInterface;
    }

    public void onResume() {
//        downloadReceiver = new BroadcastReceiver() {
//            @Override
//            public void onReceive(Context context, Intent intent) {
//                Log.d("KML", "downloadReceiver onReceive has been called");
//                DownloadManager manager =
//                    (DownloadManager) context.getSystemService(Activity.DOWNLOAD_SERVICE);
//                Cursor cursor = manager.query(new Query().setFilterById(downloadId));
//
//                if (cursor.moveToNext()) {
//                    int index = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
//                    int status = cursor.getInt(index);
//
//                    Uri uri = null;
//
//                    switch (status) {
//                        case DownloadManager.STATUS_SUCCESSFUL:
//                            index = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);
//                            uri = Uri.parse(cursor.getString(index));
//                            Toast.makeText(context, "Downloaded to " + uri.getPath(),
//                                    Toast.LENGTH_LONG).show();
//                            break;
//                        default:
//                            Toast.makeText(context, "Download Failed", Toast.LENGTH_LONG).show();
//                            break;
//                    }
//
//                    if (uri != null) {
//                        //Intent parsingIntent = new Intent(kmlContext, KmlLocationService.class);
//                        //parsingIntent.putExtra("fileName", KmlDownloadingService.KML_FILENAME);
//                        //kmlContext.startService(parsingIntent);
//                    }
//                }
//
//                cursor.close();
//            }
//        };
//
//        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
//        kmlContext.registerReceiver(downloadReceiver, filter);
    }

    public void onPause() {
//        kmlContext.unregisterReceiver(downloadReceiver);
    }

    public static boolean isSignedIn() {
        CookieManager cookieManager = CookieManager.getInstance();
        String cookie = cookieManager.getCookie(COOKIE_URL);

        return cookie != null && cookie.contains("SID=");
    }

    public static void clearCookies(Context context) {
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookies(null);

        Toast.makeText(context, "Cleared Cookies", Toast.LENGTH_SHORT).show();
    }

    public void getLastKML() {
        if (parameters != null) {
            getKML(parameters);
        }
    }

    public void getKML(Fragment fragment, int requestCode, DownloadListener listener) {
        getKML(new KMLParameters(fragment, requestCode, TEST_PB_VALUE, KML_FILENAME, listener));
    }

    public void getKML(KMLParameters parameters) {
        this.parameters = parameters;

        if (isSignedIn()) {
            downloadKML(KML_URL + "&pb=" + parameters.pbValue, parameters.filename);
        } else {
            mainInterface.showWebActivity(parameters.fragment, parameters.requestCode);
        }
    }

    private void downloadKML(String url, String filename) {
        CookieManager cookieManager = CookieManager.getInstance();
        String cookie = cookieManager.getCookie(COOKIE_URL);

        Request request = new Request(Uri.parse(url));
        request.addRequestHeader("Cookie", cookie);

        String storage = Environment.getExternalStorageDirectory().getPath() + "/";
        File file = new File(storage + MainActivity.APP_DIR + filename);
        if (file.exists()) {
            file.delete();
        }
        request.setDestinationUri(Uri.fromFile(file));

        DownloadManager manager =
            (DownloadManager) kmlContext.getSystemService(Activity.DOWNLOAD_SERVICE);
        downloadId = manager.enqueue(request);

        Toast.makeText(kmlContext, "Downloading KML", Toast.LENGTH_SHORT).show();
    }

    public static class KMLParameters {

        public Fragment fragment;
        public int requestCode;
        public String pbValue;
        public String filename;
        public DownloadListener listener;

        public KMLParameters(Fragment fragment, int requestCode, String pbValue, String filename,
                DownloadListener listener) {
            this.fragment = fragment;
            this.requestCode = requestCode;
            this.pbValue = pbValue;
            this.filename = filename;
            this.listener = listener;
        }
    }

    public interface DownloadListener {

        void onFinish(int status, Uri uri);
    }
}

package com.mono.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class NetworkHelper {

    private static final String PING_CMD = "ping";
    private static final String PING_HOST = "google.com";
    private static final int PING_COUNT = 1;
    private static final int PING_TIMEOUT = 3;
    private static final String PING_KEYWORD = "received";

    private static final long REACHABLE_DELAY = 60 * Constants.SECOND_MS;
    private static final long TIMEOUT_DELAY = 20 * Constants.SECOND_MS;

    public static final int TYPE_NONE = 0;
    public static final int TYPE_MOBILE = 1;
    public static final int TYPE_WIFI = 2;

    public static final int STATUS_NONE = 0;
    public static final int STATUS_AVAILABLE = 1;
    public static final int STATUS_CONNECTED = 2;

    private static long LAST_REACHABLE;
    private static long LAST_TIMEOUT;

    public static int getNetworkType(Context context) {
        int type = TYPE_NONE;

        ConnectivityManager manager =
            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = manager.getActiveNetworkInfo();

        if (info != null) {
            switch (info.getType()) {
                case ConnectivityManager.TYPE_MOBILE:
                    type = TYPE_MOBILE;
                    break;
                case ConnectivityManager.TYPE_WIFI:
                    type = TYPE_WIFI;
                    break;
            }
        }

        return type;
    }

    public static int getNetworkStatus(Context context) {
        int status = STATUS_NONE;

        ConnectivityManager manager =
            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = manager.getActiveNetworkInfo();

        if (info != null) {
            if (info.isConnected()) {
                status = STATUS_CONNECTED;
            } else if (info.isAvailable()) {
                status = STATUS_AVAILABLE;
            }
        }

        return status;
    }

    public static boolean isConnected(Context context) {
        return getNetworkStatus(context) == STATUS_CONNECTED;
    }

    private static void updateLastReachable() {
        LAST_REACHABLE = System.currentTimeMillis();
    }

    private static void updateLastTimeout() {
        LAST_TIMEOUT = System.currentTimeMillis();
    }

    public static boolean isReachable(Context context) {
        boolean status = false;

        if (isConnected(context)) {
            try {
                String[] command = {
                    PING_CMD,
                    "-c",
                    String.valueOf(PING_COUNT),
                    "-w",
                    String.valueOf(PING_TIMEOUT),
                    PING_HOST
                };

                ProcessBuilder builder = new ProcessBuilder(command);
                Process process = builder.start();

                BufferedReader input =
                    new BufferedReader(new InputStreamReader(process.getInputStream()));
                String nextLine;

                while ((nextLine = input.readLine()) != null) {
                    if (nextLine.contains(PING_KEYWORD)) {
                        String[] items = nextLine.split(",");

                        for (String item : items) {
                            item = item.trim();

                            if (item.contains(PING_KEYWORD)) {
                                String value = item.substring(0, item.indexOf(" ")).trim();
                                if (!value.equals("0")) {
                                    status = true;
                                    break;
                                }
                            }
                        }

                        break;
                    }
                }

                input.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (status) {
                updateLastReachable();
            }
        }

        return status;
    }

    public static boolean execute(Context context, NetworkTask request) {
        boolean status = false;

        new AsyncTask<Object, Void, Boolean>() {
            private NetworkTask request;

            @Override
            protected Boolean doInBackground(Object... params) {
                Context context = (Context) params[0];
                request = (NetworkTask) params[1];

                boolean status = false;

                boolean isReachable =
                    System.currentTimeMillis() - LAST_REACHABLE > REACHABLE_DELAY;

                if (isReachable || isReachable(context)) {
                    try {
                        request.onRequest();

                        status = true;
                        updateLastReachable();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (!status) {
                    updateLastTimeout();
                }

                return status;
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (result) {
                    request.onResponse();
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, context, request);

        return status;
    }

    public interface NetworkTask {

        void onRequest() throws IOException;

        void onResponse();
    }
}

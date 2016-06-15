package com.mono.parser;


import android.content.Context;
import android.webkit.CookieManager;
import android.widget.Toast;

public class KML {

    public static final String COOKIE_URL = "https://www.google.com/maps/timeline";


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

}

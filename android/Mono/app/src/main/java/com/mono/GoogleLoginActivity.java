package com.mono;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.mono.parser.KML;
import com.mono.settings.Settings;
import com.mono.util.GestureActivity;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;

/**
 * This activity is used to display the Google login form to enable KML handling after logging in.
 *
 * @author Gary Ng
 */
public class GoogleLoginActivity extends GestureActivity {

    public static final String LOGIN_URL = "https://accounts.google.com/ServiceLogin";
    public static final String EXTRA_EMAIL = "email";
    public static final String EXTRA_NAME = "name";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_google_login);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        WebView webView = (WebView) findViewById(R.id.web_view);
        webView.getSettings().setSaveFormData(false);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (!url.startsWith(LOGIN_URL) && KML.isSignedIn()) {
                    retrieveData(); // Retrieve Profile Information
                    return true;
                }

                return false;
            }
        });

        webView.loadUrl(LOGIN_URL);
    }

    public void setActivityResult(int resultCode, Intent data) {
        if (getParent() == null) {
            setResult(resultCode, data);
        } else {
            getParent().setResult(resultCode, data);
        }
    }

    /**
     * Extract profile information from web page such as email and name after logging in.
     *
     * @return an instance of the task.
     */
    public AsyncTask<Object, Void, String[]> retrieveData() {
        return new AsyncTask<Object, Void, String[]>() {
            @Override
            protected String[] doInBackground(Object[] params) {
                String[] result = null;

                CookieManager cookieManager = CookieManager.getInstance();
                String cookie = cookieManager.getCookie(LOGIN_URL);

                Connection connection = Jsoup.connect(LOGIN_URL);
                connection.header("Cookie", cookie);

                try {
                    String email, name = null;

                    Document document = connection.get();
                    // Email (Required)
                    email = document.getElementById("Email").val().trim();
                    // Name (Optional)
                    Elements profileNames = document.getElementsByClass("profile-name");
                    if (!profileNames.isEmpty()) {
                        name = profileNames.get(0).text().trim();
                    }

                    result = new String[]{email, name};
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return result;
            }

            @Override
            protected void onPostExecute(String[] result) {
                if (result != null) {
                    Intent data = new Intent();
                    data.putExtra(EXTRA_EMAIL, result[0]);
                    data.putExtra(EXTRA_NAME, result[1]);

                    Settings.getInstance(getApplicationContext()).setGoogleHasCookie(true);
                    setActivityResult(RESULT_OK, data);
                }

                finish();
            }
        }.execute(AsyncTask.THREAD_POOL_EXECUTOR);
    }
}

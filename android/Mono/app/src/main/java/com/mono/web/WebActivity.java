package com.mono.web;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.mono.R;
import com.mono.parser.KML;
import com.mono.settings.Settings;
import com.mono.util.GestureActivity;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * This activity is used to display the Google login form to enable KML handling after logging in.
 *
 * @author Gary Ng
 */
public class WebActivity extends GestureActivity {

    public static final String LOGIN_URL = "https://accounts.google.com/ServiceLogin";
    public static final String EXTRA_EMAIL = "email";
    public static final String EXTRA_NAME = "name";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        WebView webView = (WebView) findViewById(R.id.web_view);
        webView.getSettings().setSaveFormData(false);
        webView.setWebViewClient(new WebViewClient() {
            private boolean hasVerified = false;

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                boolean override = false;

                if (!url.startsWith(LOGIN_URL) && KML.isSignedIn()) {
                    override = true;
                    // Reload to Retrieve Name + Email
                    hasVerified = true;
                    view.loadUrl(LOGIN_URL);
                }

                return override;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (hasVerified && url.startsWith(LOGIN_URL)) {
                    WebSettings settings = view.getSettings();
                    settings.setJavaScriptEnabled(true);
                    // Retrieve Name + Email
                    view.evaluateJavascript("(function() { return JSON.parse('{\"" +
                        EXTRA_NAME + "\":\"' + document.getElementsByClassName('profile-name')[0].textContent.trim() + '\",\"" +
                        EXTRA_EMAIL + "\":\"' + document.getElementById('Email').value.trim() + '\"}'); })();",
                        new ValueCallback<String>() {
                            @Override
                            public void onReceiveValue(String value) {
                                try {
                                    JSONObject response = new JSONObject(value);

                                    Intent data = new Intent();
                                    data.putExtra(EXTRA_EMAIL, response.getString(EXTRA_EMAIL));
                                    data.putExtra(EXTRA_NAME, response.getString(EXTRA_NAME));

                                    Settings.getInstance(getApplicationContext()).setGoogleHasCookie(true);
                                    setActivityResult(RESULT_OK, data);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }

                                finish();
                            }
                        }
                    );

                    settings.setJavaScriptEnabled(false);
                }
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
}

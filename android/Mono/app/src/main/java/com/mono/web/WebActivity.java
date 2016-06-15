package com.mono.web;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.mono.R;
import com.mono.parser.KML;
import com.mono.util.GestureActivity;

public class WebActivity extends GestureActivity {

    public static final String LOGIN_URL = "https://accounts.google.com/ServiceLogin";

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        webView = (WebView) findViewById(R.id.web_view);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                boolean override = false;

                if (!url.startsWith(LOGIN_URL) && KML.isSignedIn()) {
                    setActivityResult(Activity.RESULT_OK);
                    finish();

                    override = true;
                }

                return override;
            }
        });

        login();
    }

    public void setActivityResult(int resultCode) {
        if (getParent() == null) {
            setResult(resultCode);
        } else {
            getParent().setResult(resultCode);
        }
    }

    public void login() {
        webView.loadUrl(LOGIN_URL);
    }
}

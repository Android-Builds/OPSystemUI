package com.oneplus.lib.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import com.oneplus.commonctrl.R$id;
import com.oneplus.commonctrl.R$layout;
import com.oneplus.commonctrl.R$string;
import com.oneplus.lib.util.NavigationBarUtils;

public class PrivacyOnLineActivity extends Activity {
    RelativeLayout mLoadingView;
    String mUrl;
    WebView mWebView;
    FrameLayout mWebViewContainer;

    /* access modifiers changed from: protected */
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R$layout.web_browser_activity_layout);
        if (getActionBar() != null) {
            getActionBar().setTitle(getString(R$string.about_privacy_policy));
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
        Intent intent = getIntent();
        if (intent != null) {
            this.mUrl = intent.getStringExtra("url");
        }
        initViews();
    }

    /* access modifiers changed from: protected */
    public void onResume() {
        super.onResume();
        NavigationBarUtils.setNavBarColor(this);
    }

    private void initViews() {
        this.mWebViewContainer = (FrameLayout) findViewById(R$id.web_view_container);
        this.mLoadingView = (RelativeLayout) findViewById(R$id.loading_layout);
        this.mWebView = new WebView(this);
        this.mWebView.setWebViewClient(new WebViewClient());
        this.mWebView.setWebChromeClient(new WebChromeClient() {
            public void onProgressChanged(WebView webView, int i) {
                if (i > 20) {
                    PrivacyOnLineActivity.this.mLoadingView.setVisibility(8);
                } else if (PrivacyOnLineActivity.this.mLoadingView.getVisibility() != 0) {
                    PrivacyOnLineActivity.this.mLoadingView.setVisibility(0);
                }
                super.onProgressChanged(webView, i);
            }
        });
        loadUrl(this.mUrl);
        WebSettings settings = this.mWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setDomStorageEnabled(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(true);
        this.mWebViewContainer.addView(this.mWebView);
    }

    private void loadUrl(String str) {
        if (!TextUtils.isEmpty(str)) {
            this.mWebView.loadUrl(str);
        }
    }

    /* access modifiers changed from: protected */
    public void onDestroy() {
        super.onDestroy();
        this.mWebViewContainer.removeAllViews();
        WebView webView = this.mWebView;
        if (webView != null) {
            webView.clearView();
            this.mWebView.stopLoading();
            this.mWebView.removeAllViews();
            this.mWebView.destroy();
        }
    }

    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == 16908332) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(menuItem);
    }
}

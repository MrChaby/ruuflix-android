package com.ruuflix.app;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.app.AlertDialog;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.net.URI;
import org.json.JSONArray;
import org.json.JSONObject;

/** A lightweight Android client for the RuuFlix website. */
public class MainActivity extends Activity {
    private static final String HOME_URL = "https://ruuflixserver1.apps.bot-hosting.cloud/";
    private static final String FIRST_PARTY_HOST = "ruuflixserver1.apps.bot-hosting.cloud";

    private FrameLayout root;
    private LinearLayout appContent;
    private LinearLayout toolbar;
    private FrameLayout webFrame;
    private WebView webView;
    private ProgressBar progressBar;
    private Button bookmarkButton;
    private LinearLayout errorPanel;
    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private ValueCallback<Uri[]> fileUploadCallback;
    private int savedSystemUiVisibility;

    private final int dark = Color.rgb(9, 9, 15);
    private final int surface = Color.rgb(21, 21, 30);
    private final int white = Color.WHITE;
    private final int muted = Color.rgb(167, 167, 179);
    private final int red = Color.rgb(229, 9, 20);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(dark);
        getWindow().setNavigationBarColor(dark);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        buildInterface();
        configureWebView();
        if (savedInstanceState == null) {
            webView.loadUrl(HOME_URL);
        } else {
            webView.restoreState(savedInstanceState);
        }
    }

    private void buildInterface() {
        root = new FrameLayout(this);
        root.setBackgroundColor(dark);

        appContent = new LinearLayout(this);
        appContent.setOrientation(LinearLayout.VERTICAL);
        appContent.setBackgroundColor(dark);
        root.addView(appContent, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        toolbar = new LinearLayout(this);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setPadding(dp(16), 0, dp(8), 0);
        toolbar.setBackgroundColor(surface);
        appContent.addView(toolbar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(56)));

        TextView brand = new TextView(this);
        brand.setText("RuuFlix");
        brand.setTextColor(white);
        brand.setTextSize(21);
        brand.setTypeface(null, android.graphics.Typeface.BOLD);
        toolbar.addView(brand, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button refresh = toolbarButton("↻", "Refresh page");
        refresh.setOnClickListener(v -> reloadPage());
        toolbar.addView(refresh, new LinearLayout.LayoutParams(dp(44), dp(48)));

        bookmarkButton = toolbarButton("☆", "Save this page to your watchlist");
        bookmarkButton.setOnClickListener(v -> toggleWatchlist());
        toolbar.addView(bookmarkButton, new LinearLayout.LayoutParams(dp(44), dp(48)));

        Button watchlist = toolbarButton("♥", "Open your saved watchlist");
        watchlist.setOnClickListener(v -> showWatchlist());
        toolbar.addView(watchlist, new LinearLayout.LayoutParams(dp(44), dp(48)));

        Button share = toolbarButton("↗", "Share RuuFlix");
        share.setOnClickListener(v -> sharePage());
        toolbar.addView(share, new LinearLayout.LayoutParams(dp(44), dp(48)));

        webFrame = new FrameLayout(this);
        appContent.addView(webFrame, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        webView = new WebView(this);
        webView.setBackgroundColor(dark);
        webFrame.addView(webView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(red));
        progressBar.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(red));
        FrameLayout.LayoutParams progressParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(3), Gravity.TOP);
        webFrame.addView(progressBar, progressParams);

        errorPanel = new LinearLayout(this);
        errorPanel.setOrientation(LinearLayout.VERTICAL);
        errorPanel.setGravity(Gravity.CENTER);
        errorPanel.setPadding(dp(28), dp(28), dp(28), dp(28));
        errorPanel.setBackgroundColor(dark);
        TextView errorTitle = new TextView(this);
        errorTitle.setText("Unable to connect");
        errorTitle.setTextColor(white);
        errorTitle.setTextSize(22);
        errorTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        errorPanel.addView(errorTitle);
        TextView errorMessage = new TextView(this);
        errorMessage.setText("Check your internet connection and try again.");
        errorMessage.setTextColor(muted);
        errorMessage.setTextSize(14);
        errorMessage.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams messageParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        messageParams.topMargin = dp(8);
        errorPanel.addView(errorMessage, messageParams);
        Button retry = toolbarButton("Try again", "Retry loading RuuFlix");
        retry.setTextColor(white);
        retry.setBackgroundTintList(android.content.res.ColorStateList.valueOf(red));
        LinearLayout.LayoutParams retryParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(46));
        retryParams.topMargin = dp(20);
        errorPanel.addView(retry, retryParams);
        retry.setOnClickListener(v -> reloadPage());
        errorPanel.setVisibility(View.GONE);
        webFrame.addView(errorPanel, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // Android 15+ enforces edge-to-edge for apps targeting API 35+. Apply safe-area insets.
        if (Build.VERSION.SDK_INT >= 35) {
            appContent.setOnApplyWindowInsetsListener((view, insets) -> {
                android.graphics.Insets bars = insets.getInsets(
                        android.view.WindowInsets.Type.systemBars());
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                return insets;
            });
        } else {
            appContent.setFitsSystemWindows(true);
        }
        setContentView(root);
    }

    private Button toolbarButton(String label, String description) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(18);
        button.setTextColor(white);
        button.setAllCaps(false);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setPadding(dp(4), 0, dp(4), 0);
        button.setContentDescription(description);
        button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(surface));
        return button;
    }

    private void configureWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(false);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        if (Build.VERSION.SDK_INT >= 26) {
            settings.setSafeBrowsingEnabled(true);
        }

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                progressBar.setVisibility(View.VISIBLE);
                errorPanel.setVisibility(View.GONE);
                updateBookmarkButton();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
                errorPanel.setVisibility(View.GONE);
                updateBookmarkButton();
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return routeUrl(request.getUrl().toString());
            }

            @SuppressWarnings("deprecation")
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return routeUrl(url);
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                // Never bypass certificate errors.
                handler.cancel();
                showConnectionError();
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request,
                                        WebResourceError error) {
                if (request.isForMainFrame()) {
                    showConnectionError();
                }
            }

            @SuppressWarnings("deprecation")
            @Override
            public void onReceivedError(WebView view, int errorCode, String description,
                                        String failingUrl) {
                if (Build.VERSION.SDK_INT < 23) {
                    showConnectionError();
                }
            }

            @Override
            public boolean onRenderProcessGone(WebView view,
                    android.webkit.RenderProcessGoneDetail detail) {
                webFrame.removeView(webView);
                webView.destroy();
                webView = new WebView(MainActivity.this);
                webFrame.addView(webView, 0, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                configureWebView();
                webView.loadUrl(HOME_URL);
                return true;
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
                progressBar.setVisibility(newProgress >= 100 ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (customView != null) {
                    callback.onCustomViewHidden();
                    return;
                }
                customView = view;
                customViewCallback = callback;
                savedSystemUiVisibility = root.getSystemUiVisibility();
                appContent.setVisibility(View.INVISIBLE);
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                root.addView(customView, params);
                root.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            }

            @Override
            public void onHideCustomView() {
                exitFullscreen();
            }

            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback,
                                               FileChooserParams params) {
                if (fileUploadCallback != null) {
                    fileUploadCallback.onReceiveValue(null);
                }
                fileUploadCallback = callback;
                Intent intent;
                try {
                    intent = params.createIntent();
                } catch (Exception e) {
                    intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("*/*");
                }
                try {
                    startActivityForResult(intent, 2201);
                    return true;
                } catch (ActivityNotFoundException e) {
                    fileUploadCallback = null;
                    callback.onReceiveValue(null);
                    Toast.makeText(MainActivity.this,
                            "No file picker is available on this device.", Toast.LENGTH_LONG).show();
                    return false;
                }
            }
        });

        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            } catch (Exception e) {
                Toast.makeText(this, "Unable to open this download.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private boolean routeUrl(String rawUrl) {
        try {
            URI uri = URI.create(rawUrl);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
            if ("https".equals(scheme) || "http".equals(scheme)) {
                String host = uri.getHost();
                if (host != null && (host.equals(FIRST_PARTY_HOST)
                        || host.endsWith("." + FIRST_PARTY_HOST))) {
                    return false;
                }
                // Keep non-website destinations out of the embedded app.
                openExternal(rawUrl);
                return true;
            }
            if ("mailto".equals(scheme) || "tel".equals(scheme)) {
                openExternal(rawUrl);
                return true;
            }
            return true;
        } catch (Exception e) {
            return true;
        }
    }

    private void openExternal(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception ignored) {
            Toast.makeText(this, "No app can open this link.", Toast.LENGTH_SHORT).show();
        }
    }


    private SharedPreferences watchlistPrefs() {
        return getSharedPreferences("ruuflix_watchlist", MODE_PRIVATE);
    }

    private JSONArray readWatchlist() {
        String stored = watchlistPrefs().getString("items", "[]");
        try {
            return new JSONArray(stored);
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    private void toggleWatchlist() {
        if (webView == null || webView.getUrl() == null) return;
        String url = webView.getUrl();
        String title = webView.getTitle();
        if (title == null || title.trim().isEmpty()) title = "RuuFlix item";
        try {
            URI uri = URI.create(url);
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || uri.getHost() == null
                    || !(uri.getHost().equals(FIRST_PARTY_HOST)
                    || uri.getHost().endsWith("." + FIRST_PARTY_HOST))) {
                Toast.makeText(this, "Only RuuFlix pages can be saved.", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (Exception e) {
            return;
        }

        JSONArray current = readWatchlist();
        JSONArray updated = new JSONArray();
        boolean removed = false;
        for (int i = 0; i < current.length(); i++) {
            JSONObject item = current.optJSONObject(i);
            if (item == null) continue;
            if (url.equals(item.optString("url"))) {
                removed = true;
            } else {
                updated.put(item);
            }
        }
        if (!removed) {
            JSONObject item = new JSONObject();
            try {
                item.put("title", title);
                item.put("url", url);
                updated.put(item);
            } catch (Exception ignored) { }
        }
        watchlistPrefs().edit().putString("items", updated.toString()).apply();
        updateBookmarkButton();
        Toast.makeText(this, removed ? "Removed from your watchlist" : "Saved to your watchlist",
                Toast.LENGTH_SHORT).show();
    }

    private boolean isSaved(String url) {
        JSONArray items = readWatchlist();
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.optJSONObject(i);
            if (item != null && url.equals(item.optString("url"))) return true;
        }
        return false;
    }

    private void updateBookmarkButton() {
        if (bookmarkButton == null) return;
        String url = webView == null ? null : webView.getUrl();
        boolean saved = url != null && isSaved(url);
        bookmarkButton.setText(saved ? "★" : "☆");
        bookmarkButton.setContentDescription(saved
                ? "Remove this page from your watchlist"
                : "Save this page to your watchlist");
    }

    private void showWatchlist() {
        JSONArray items = readWatchlist();
        if (items.length() == 0) {
            new AlertDialog.Builder(this)
                    .setTitle("Your watchlist")
                    .setMessage("No saved pages yet. Open a RuuFlix page and tap ☆ to save it here.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }
        String[] titles = new String[items.length()];
        String[] urls = new String[items.length()];
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.optJSONObject(i);
            titles[i] = item == null ? "RuuFlix item" : item.optString("title", "RuuFlix item");
            urls[i] = item == null ? HOME_URL : item.optString("url", HOME_URL);
        }
        new AlertDialog.Builder(this)
                .setTitle("Your watchlist")
                .setItems(titles, (dialog, which) -> webView.loadUrl(urls[which]))
                .setNegativeButton("Close", null)
                .show();
    }

    private void showConnectionError() {
        progressBar.setVisibility(View.GONE);
        errorPanel.setVisibility(View.VISIBLE);
    }

    private void reloadPage() {
        errorPanel.setVisibility(View.GONE);
        if (webView != null) {
            webView.reload();
        }
    }

    private void sharePage() {
        String url = webView == null ? HOME_URL : webView.getUrl();
        if (url == null || url.trim().isEmpty()) url = HOME_URL;
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, "RuuFlix: " + url);
        startActivity(Intent.createChooser(send, "Share RuuFlix"));
    }

    private void exitFullscreen() {
        if (customView == null) return;
        root.removeView(customView);
        customView = null;
        appContent.setVisibility(View.VISIBLE);
        root.setSystemUiVisibility(savedSystemUiVisibility);
        if (customViewCallback != null) {
            customViewCallback.onCustomViewHidden();
            customViewCallback = null;
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 2201 && fileUploadCallback != null) {
            Uri[] results = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
            fileUploadCallback.onReceiveValue(results);
            fileUploadCallback = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (customView != null) {
            exitFullscreen();
        } else if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (webView != null) webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onPause() {
        if (webView != null) webView.onPause();
        CookieManager.getInstance().flush();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) webView.onResume();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.stopLoading();
            webView.setWebChromeClient(null);
            webView.setWebViewClient(null);
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}

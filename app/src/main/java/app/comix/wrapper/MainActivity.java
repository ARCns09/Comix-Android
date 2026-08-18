package app.comix.wrapper;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends Activity {
    private static final String HOME = "https://comix.to/";
    private WebView webView;

    // Only these TOP-LEVEL navigation families are allowed.
    private static final Set<String> COMIX_HOSTS = new HashSet<>(Arrays.asList(
            "comix.to", "comix.ws"
    ));

    private static final Set<String> GOOGLE_HOSTS = new HashSet<>(Arrays.asList(
            "accounts.google.com",
            "google.com"
    ));

    // Conservative tracker/ad hosts. Subresources are blocked, while the app remains
    // functional for Comix assets/CDNs. Add hosts here only when verified as ads/trackers.
    private static final String[] BLOCKED_HOST_SUFFIXES = {
            "doubleclick.net",
            "googlesyndication.com",
            "googleadservices.com",
            "adservice.google.com",
            "googletagmanager.com",
            "google-analytics.com",
            "scorecardresearch.com",
            "quantserve.com"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureSystemBars();

        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(17, 20, 24));
        setContentView(webView);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setLoadWithOverviewMode(false);
        s.setUseWideViewPort(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setSupportZoom(false);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setJavaScriptCanOpenWindowsAutomatically(false);
        s.setSupportMultipleWindows(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);

        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(webView, true);

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();

                // Nothing launches Chrome/Brave/another app.
                if (!isHttp(uri)) return true;

                // Main-frame navigation is locked to Comix + Google auth only.
                if (request.isForMainFrame()) {
                    return !isAllowedNavigationHost(uri.getHost());
                }

                return false;
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String host = normalizeHost(uri.getHost());

                // Block known advertising/tracking subresources.
                if (isBlockedHost(host)) return emptyResponse();

                // Do NOT blanket-block third-party subresources here: Comix may require image,
                // API, CDN, font, captcha, or auth resources hosted on other domains.
                // They cannot become top-level pages because navigation is locked above.
                return null;
            }
        });

        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            Uri uri = Uri.parse(url);
            if (!isAllowedNavigationHost(uri.getHost())) {
                Toast.makeText(this, "Blocked external download", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                DownloadManager.Request req = new DownloadManager.Request(uri);
                String name = URLUtil.guessFileName(url, contentDisposition, mimeType);
                req.setTitle(name);
                req.setMimeType(mimeType);
                req.addRequestHeader("User-Agent", userAgent);
                String cookie = CookieManager.getInstance().getCookie(url);
                if (cookie != null) req.addRequestHeader("Cookie", cookie);
                req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name);
                ((DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE)).enqueue(req);
                Toast.makeText(this, "Download started", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Download failed", Toast.LENGTH_SHORT).show();
            }
        });

        if (savedInstanceState == null) webView.loadUrl(HOME);
        else webView.restoreState(savedInstanceState);
    }

    private boolean isHttp(Uri uri) {
        String scheme = uri.getScheme();
        return "https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme);
    }

    private String normalizeHost(String host) {
        if (host == null) return "";
        host = host.toLowerCase();
        return host.startsWith("www.") ? host.substring(4) : host;
    }

    private boolean hostMatches(String host, String root) {
        host = normalizeHost(host);
        return host.equals(root) || host.endsWith("." + root);
    }

    private boolean isAllowedNavigationHost(String host) {
        host = normalizeHost(host);
        for (String root : COMIX_HOSTS) if (hostMatches(host, root)) return true;
        for (String root : GOOGLE_HOSTS) if (hostMatches(host, root)) return true;
        return false;
    }

    private boolean isBlockedHost(String host) {
        host = normalizeHost(host);
        for (String suffix : BLOCKED_HOST_SUFFIXES) {
            if (host.equals(suffix) || host.endsWith("." + suffix)) return true;
        }
        return false;
    }

    private WebResourceResponse emptyResponse() {
        return new WebResourceResponse(
                "text/plain",
                "utf-8",
                new ByteArrayInputStream(new byte[0])
        );
    }

    private void configureSystemBars() {
        Window window = getWindow();
        window.setStatusBarColor(Color.rgb(17, 20, 24));
        window.setNavigationBarColor(Color.rgb(17, 20, 24));
        // Keep content below system bars. This fixes the header colliding with
        // clock/network/battery icons on edge-to-edge Android builds.
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(true);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.loadUrl("about:blank");
            webView.stopLoading();
            webView.destroy();
        }
        super.onDestroy();
    }
}

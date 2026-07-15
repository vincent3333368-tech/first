package com.howmuch.app;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private static final int FILE_CHOOSER_REQUEST = 10;
    private static final String LOCAL_APP_URL = "file:///android_asset/index.html";
    // 侧滑返回只从屏幕左边缘开始识别，避免和页面内部横向滑动冲突。
    private static final int EDGE_SWIPE_WIDTH_DP = 36;
    private static final int BACK_SWIPE_DISTANCE_DP = 96;
    private static final int BACK_SWIPE_MAX_VERTICAL_DP = 72;

    private WebView webView;
    private ValueCallback<Uri[]> filePathCallback;
    private float gestureStartX;
    private float gestureStartY;
    private boolean trackingBackGesture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showWebView();
    }

    private void showWebView() {
        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);

        webView.addJavascriptInterface(new BackupBridge(), "HowMuchAndroid");
        webView.setWebViewClient(new WebViewClient());
        // WebView 不会自动提供 Android 的全局侧滑返回，这里手动把边缘手势转成应用返回事件。
        webView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                return handleBackSwipe(event);
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (filePathCallback != null) {
                    filePathCallback.onReceiveValue(null);
                }
                filePathCallback = callback;
                Intent intent = params.createIntent();
                try {
                    startActivityForResult(intent, FILE_CHOOSER_REQUEST);
                } catch (Exception e) {
                    filePathCallback = null;
                    Toast.makeText(MainActivity.this, "无法打开文件选择器", Toast.LENGTH_SHORT).show();
                    return false;
                }
                return true;
            }
        });

        webView.loadUrl(LOCAL_APP_URL);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_REQUEST && filePathCallback != null) {
            Uri[] result = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
            filePathCallback.onReceiveValue(result);
            filePathCallback = null;
        }
    }

    @Override
    public void onBackPressed() {
        requestAppBack();
    }

    private boolean handleBackSwipe(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                // 只记录从左边缘开始的触摸，普通页面点击和滚动继续交给 WebView。
                gestureStartX = event.getX();
                gestureStartY = event.getY();
                trackingBackGesture = gestureStartX <= dpToPx(EDGE_SWIPE_WIDTH_DP);
                return false;
            case MotionEvent.ACTION_MOVE:
                if (!trackingBackGesture) {
                    return false;
                }
                float moveX = event.getX() - gestureStartX;
                float moveY = Math.abs(event.getY() - gestureStartY);
                // 横向距离足够且纵向偏移不大时，拦截后续事件，避免页面误触。
                return moveX > 12 && moveY < dpToPx(BACK_SWIPE_MAX_VERTICAL_DP);
            case MotionEvent.ACTION_UP:
                if (trackingBackGesture) {
                    float dx = event.getX() - gestureStartX;
                    float dy = Math.abs(event.getY() - gestureStartY);
                    trackingBackGesture = false;
                    if (dx >= dpToPx(BACK_SWIPE_DISTANCE_DP) && dy <= dpToPx(BACK_SWIPE_MAX_VERTICAL_DP)) {
                        requestAppBack();
                        return true;
                    }
                }
                return false;
            case MotionEvent.ACTION_CANCEL:
                trackingBackGesture = false;
                return false;
            default:
                return false;
        }
    }

    private void requestAppBack() {
        if (webView == null) {
            finish();
            return;
        }

        // 先交给网页处理弹层、未保存提示等业务返回；网页未处理时再走 WebView/Activity 默认返回。
        webView.evaluateJavascript(
                "(async function(){try{return !!(window.HowMuchAppBack && await window.HowMuchAppBack());}catch(e){return false;}})();",
                handled -> {
                    if (!"true".equals(handled)) {
                        fallbackBack();
                    }
                }
        );
    }

    private void fallbackBack() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
            return;
        }
        finish();
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    public class BackupBridge {
        @JavascriptInterface
        public void saveBackup(String content, String fileName) {
            runOnUiThread(() -> {
                try {
                    writeDownloadFile(content, fileName);
                    Toast.makeText(MainActivity.this, "备份已保存到下载目录", Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "导出失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void writeDownloadFile(String content, String fileName) throws Exception {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
            values.put(MediaStore.Downloads.MIME_TYPE, "application/json");
            values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
            ContentResolver resolver = getContentResolver();
            Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) {
                throw new IllegalStateException("无法创建下载文件");
            }
            try (OutputStream output = resolver.openOutputStream(uri)) {
                if (output == null) {
                    throw new IllegalStateException("无法写入下载文件");
                }
                output.write(content.getBytes(StandardCharsets.UTF_8));
            }
            return;
        }

        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException("无法创建下载目录");
        }
        File file = new File(dir, fileName);
        try (OutputStream output = new FileOutputStream(file)) {
            output.write(content.getBytes(StandardCharsets.UTF_8));
        }
    }
}

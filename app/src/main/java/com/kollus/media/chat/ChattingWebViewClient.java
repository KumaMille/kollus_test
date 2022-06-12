package com.kollus.media.chat;

import android.net.Uri;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.kollus.sdk.media.util.Log;

public class ChattingWebViewClient extends WebViewClient {
    private static final String TAG = ChattingWebViewClient.class.getSimpleName();
    private final String KollusScheme = "kollusapp";

    private Listener mListener;

    private final String COMMAND_READY = "ready";
    private final String COMMAND_BYPASS_TAP = "bypassTap";

    public interface Listener {
        public void onReady();
        public void onBypassTap();
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        Log.d(TAG, "shouldOverrideUrlLoading " + url);
        String scheme = Uri.parse(url).getScheme();

        if (scheme.equalsIgnoreCase(KollusScheme)) {
            if (mListener != null) {
                parseCommand(view, url);
            }
            return true;
        }
        else {
            return super.shouldOverrideUrlLoading(view, url);
        }
    }

    private void parseCommand(WebView view, String url) {
        Uri uri = Uri.parse(url);
        String command = uri.getHost();
        Log.d(TAG, "parseCommand:" + command);
        if (command.equals(COMMAND_READY)) {
            mListener.onReady();
        }
        else if (command.equals(COMMAND_BYPASS_TAP)) {
            mListener.onBypassTap();
        }
    }
}

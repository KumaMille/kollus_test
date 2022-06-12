package com.kollus.media.chat;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.kollus.media.MoviePlayer;
import com.kollus.sdk.media.content.KollusContent.ChattingInfo;
import com.kollus.sdk.media.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Set;
import java.util.Vector;

public class ChattingView extends WebView {
    private static final String TAG = ChattingView.class.getSimpleName();

//    private final String mChatUrl = "http://file.kollus.com/chat/kollus_mobile.html";
//    private final String mChatUrl = "http://sample.videoclouds.net/kollusChatting/kollus_mobile.html";

    private Context mContext;
    private MoviePlayer mMoviePlayer;
    private ChattingWebChromeClient mWebChromeClient;
    ChattingWebViewClient mWebViewClient;
    private WebSettings mWebSetting;
    private boolean mSendInit;
    private Object mLocker = new Object();
    private Vector<String> mPandingMsgList = new Vector<String>();
    private ChattingInfo mChattingInfo;

    public ChattingView(Context context) {
        this(context, null);
    }

    public ChattingView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mContext = context;
        mWebSetting = this.getSettings();
//        mWebSetting.setTextZoom(100);
        mWebSetting.setDomStorageEnabled(true);
        mWebSetting.setJavaScriptEnabled(true);
        mWebSetting.setCacheMode(WebSettings.LOAD_NO_CACHE);
        mWebSetting.setAppCacheEnabled(false);
//        mWebSetting.setBuiltInZoomControls(true);
//        mWebSetting.setAllowFileAccess(true);
//        mWebSetting.setAllowContentAccess(true);

//		clearCache(true);
        mWebChromeClient = new ChattingWebChromeClient(mContext);
        setWebChromeClient(mWebChromeClient);

        mWebViewClient = new ChattingWebViewClient();
        setWebViewClient(mWebViewClient);

        setHorizontalScrollBarEnabled(true);
        setVerticalScrollBarEnabled(true);
        setScrollbarFadingEnabled(true);
        setBackgroundColor(Color.TRANSPARENT);
    }

    public void setMoviePlayer(MoviePlayer player) {
        mMoviePlayer = player;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(mMoviePlayer.isControlsShowing())
            return false;
        return super.onTouchEvent(event);
    }

    public void setListener(ChattingInfo info, ChattingWebViewClient.Listener listener) {
        mWebViewClient.setListener(listener);
        mChattingInfo = info;
        String mainUrl = mChattingInfo.mainUrl;
        if(Log.isDebug()) {
            Set<String> paramters = Uri.parse(mainUrl).getQueryParameterNames();
            if(paramters != null && !paramters.isEmpty())
                mainUrl += "&";
            else
                mainUrl += "?";
            mainUrl += "chat_debug_mode";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                setWebContentsDebuggingEnabled(true);
        }
//        mainUrl = "http://www.daum.net";
        loadUrl(mainUrl);
    }

    public void onInit() {
        synchronized (mLocker) {
            JSONObject json = new JSONObject();
            try {
                json.put("admin", mChattingInfo.isAdmin);
                json.put("anonymous", mChattingInfo.isAnonymous);
                json.put("roomId", mChattingInfo.roomId);
                json.put("chattingUrl", mChattingInfo.chatServer);
                json.put("userId", mChattingInfo.userId);
                json.put("nickName", mChattingInfo.userName);
                json.put("photoUrl", mChattingInfo.photoUrl);
                String actionData = String.format("javascript:KOLLUS_CHATTING.KollusExternalDevice.onInit('%s')", json.toString());
                loadUrl(actionData);
                mSendInit = true;
                while (!mPandingMsgList.isEmpty()) {
                    loadUrl(mPandingMsgList.get(0));
                    mPandingMsgList.remove(0);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void onChangedControlVisibility(boolean visible) {
        synchronized (mLocker) {
            String actionData = String.format("javascript:KOLLUS_CHATTING.KollusExternalDevice.onControlUIVisibleChanged(%b)", visible);
            if (!mSendInit) {
                mPandingMsgList.add(actionData);
                return;
            }
            loadUrl(actionData);
        }
    }

    public void onVisibleHeightChanged(float height) {
        synchronized (mLocker) {
            String actionData = String.format("javascript:KOLLUS_CHATTING.KollusExternalDevice.onVisibleHeightChanged(%f)", height);
            if (!mSendInit) {
                mPandingMsgList.add(actionData);
                return;
            }
            loadUrl(actionData);
        }
    }

    public void onVisibleHeightChanged(int bottomMargin) {
        Log.d(TAG, String.format("onVisibleHeightChanged(%d)", bottomMargin));
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)getLayoutParams();
        params.bottomMargin = bottomMargin;
        setLayoutParams(params);
    }

    public void onResume() {
        synchronized (mLocker) {
            String actionData = "javascript:KOLLUS_CHATTING.KollusExternalDevice.onResume()";
            if (!mSendInit) {
                mPandingMsgList.add(actionData);
                return;
            }
            loadUrl(actionData);
        }
    }

    public void onChatVisibleChanged(boolean visible) {
        synchronized (mLocker) {
            String actionData = String.format("javascript:KOLLUS_CHATTING.KollusExternalDevice.onChatVisibleChanged(%b)", visible);
            if (!mSendInit) {
                mPandingMsgList.add(actionData);
                return;
            }
            loadUrl(actionData);
        }
    }

    public void onClose() {
        synchronized (mLocker) {
            String actionData = "javascript:KOLLUS_CHATTING.KollusExternalDevice.onClose()";
            if (!mSendInit) {
                mPandingMsgList.add(actionData);
                return;
            }
            loadUrl(actionData);
        }
    }

    public void loadUrl(String url) {
        Log.d(TAG, url);
        super.loadUrl(url);
    }
}

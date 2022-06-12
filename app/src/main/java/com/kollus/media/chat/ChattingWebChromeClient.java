package com.kollus.media.chat;

import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import androidx.appcompat.app.AlertDialog;

import com.kollus.media.view.KollusAlertDialog;
import com.kollus.sdk.media.util.Log;

/**
 * Created by Song on 2017-03-28.
 */

public class ChattingWebChromeClient extends WebChromeClient {
    private static final String TAG = ChattingWebChromeClient.class.getSimpleName();

    private static final String TYPE_IMAGE = "image/*";
    public static final int INPUT_FILE_REQUEST_CODE = 1;

    private ValueCallback<Uri> mUploadMessage;
    private ValueCallback<Uri[]> mFilePathCallback;
    private String mCameraPhotoPath;

    private Context mContext;

    public ChattingWebChromeClient(Context context) {
        mContext = context;
    }

    @Override
    public boolean onJsAlert(WebView view, String url, String message, final JsResult result) {
        Log.d(TAG, "onJsAlert:"+message);
        new KollusAlertDialog(mContext)
                .setTitle("AlertDialog")
                .setMessage(message)
                .setPositiveButton(android.R.string.ok,
                        new AlertDialog.OnClickListener()
                        {
                            public void onClick(DialogInterface dialog, int which)
                            {
                                result.confirm();
                            }
                        })
                .setCancelable(false)
                .show();

        return true;
    }

    @Override
    public boolean onJsConfirm(WebView view, String url, String message, final JsResult result) {
        Log.d(TAG, "onJsConfirm:"+message);
        new KollusAlertDialog(mContext)
                .setTitle("AlertDialog")
                .setMessage(message)
                .setPositiveButton(android.R.string.ok,
                        new AlertDialog.OnClickListener()
                        {
                            public void onClick(DialogInterface dialog, int which)
                            {
                                result.confirm();
                            }
                        })
                .setNegativeButton(android.R.string.cancel,
                        new AlertDialog.OnClickListener()
                        {
                            public void onClick(DialogInterface dialog, int which)
                            {
                                result.cancel();
                            }
                        })
                .show();
        return true;
    }

    @Override
    public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, JsPromptResult result) {
        Log.d(TAG, "onJsPrompt:"+message);
        return super.onJsPrompt(view, url, message, defaultValue, result);
    }


}

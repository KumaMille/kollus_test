package com.kollus.media;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Window;

import androidx.appcompat.app.AppCompatActivity;

import com.kollus.media.preference.KollusConstants;
import com.kollus.media.contents.MultiKollusStorage;
import com.kollus.media.util.DiskUtil;
import com.kollus.media.view.KollusAlertDialog;
import com.kollus.sdk.media.util.ErrorCodes;
import com.kollus.sdk.media.util.Log;
import com.kollus.sdk.media.util.Utils;

import java.util.Vector;

public class BaseActivity extends AppCompatActivity {
    private final String TAG = BaseActivity.class.getSimpleName();
    protected MultiKollusStorage mMultiStorage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);
//        requestWindowFeature(Window.FEATURE_NO_TITLE);
//        getSupportActionBar().hide();

        ConnectivityManager manager =(ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        if(networkInfo != null && networkInfo.isConnected()) {
            KollusConstants.NETWORK_TIMEOUT_SEC = KollusConstants.BASE_NETWORK_TIMEOUT_SEC;
            KollusConstants.NETWORK_RETRY_COUNT = KollusConstants.BASE_NETWORK_RETRY_COUNT;
        }
        else {
            KollusConstants.NETWORK_TIMEOUT_SEC = 1;
            KollusConstants.NETWORK_RETRY_COUNT = 1;
        }

        if(Utils.getDeviceType(BaseActivity.this) == Utils.DEVICE_TV)
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        initStorage();
    }

    protected void initStorage() {
        mMultiStorage = MultiKollusStorage.getInstance(getApplicationContext());
        mMultiStorage.setCertification(
                KollusConstants.KEY,
                KollusConstants.EXPIRE_DATE,
                Utils.getPlayerId(BaseActivity.this),
                Utils.getPlayerIdMd5(BaseActivity.this),
                Utils.getDeviceType(BaseActivity.this) == Utils.DEVICE_TABLET);

        int nRet = mMultiStorage.getErrorCode();
        if (nRet != ErrorCodes.ERROR_OK) {
            String msg = "App Key Invalid.";
            if (nRet == ErrorCodes.ERROR_EXPIRED_KEY)
                msg = "App Key Expired.";
            new KollusAlertDialog(this)
                    .setTitle(R.string.error_title)
                    .setMessage(msg)
                    .setPositiveButton(R.string.confirm,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    /* If we get here, there is no onError listener, so
                                     * at least inform them that the video is over.
                                     */
                                    finish();
                                }
                            })
                    .setCancelable(false)
                    .show();
            return;
        }
        mMultiStorage.setNetworkTimeout(KollusConstants.NETWORK_TIMEOUT_SEC, KollusConstants.NETWORK_RETRY_COUNT);
    }
}

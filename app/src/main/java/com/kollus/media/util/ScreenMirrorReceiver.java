package com.kollus.media.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class ScreenMirrorReceiver extends BroadcastReceiver {
    Context mContext;

    @Override
    public void onReceive(Context context, Intent intent) {
        mContext = context;
    }

    private boolean checkUsingWifiP2P()
    {
        ConnectivityManager connectManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectManager.getActiveNetworkInfo();

        if(networkInfo != null)
        {
            if(networkInfo.getType() == ConnectivityManager.TYPE_WIFI)
            {
                for(NetworkInfo info : connectManager.getAllNetworkInfo())
                {
                    if(info.getType() == 13 && info.isConnected())
                    {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}

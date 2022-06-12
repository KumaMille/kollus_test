package com.kollus.media.util;

import com.kollus.media.R;
import com.kollus.sdk.media.util.Log;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class ApplicationReceiver extends BroadcastReceiver {
	private static final String TAG = ApplicationReceiver.class.getSimpleName();
	@Override
    public void onReceive(Context context, Intent intent) 
    {
        String action  = intent.getAction();
        if(action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            ConnectivityManager manager =(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        	NetworkInfo info = manager.getActiveNetworkInfo();
        	Resources r = context.getResources();
        	String msg;
        	if(info == null) {
        		msg = r.getString(R.string.network_not_connected);
        	}
        	else {
        		msg = String.format(r.getString(R.string.network_connected), info.getTypeName());
        	}
        	
        	Log.d(TAG, "onReceive >>> "+msg);
        }
    }
}

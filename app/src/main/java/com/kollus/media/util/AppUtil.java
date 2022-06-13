package com.kollus.media.util;

import android.os.Build;

public class AppUtil {

    public  static boolean isPipAbleSDK(){
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
    }
}

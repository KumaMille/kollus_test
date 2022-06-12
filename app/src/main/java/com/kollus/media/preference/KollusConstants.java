package com.kollus.media.preference;

import android.content.Context;

import com.kollus.sdk.media.util.Utils;

public class KollusConstants {
    public static final String KEY = "1c7776237d5e5459aa9334be724b3321f6444910";
    public static final String EXPIRE_DATE = "2022/12/31";
    public static final int SERVER_PORT = 7430;
    public static String PLAYER_ID = null;
//    public static final boolean SUPPORT_VR      = false;
    public static final boolean SUPPORT_SCREEN_CAPTURE  = false;

    public static final boolean SUPPORT_BACKGROUND_PLAYBACK = true;

    public static final boolean SUPPORT_REMOTE_MEDIA_ROUTE = false;

    public static final boolean SUPPORT_MULTI_STORAGE = true;

    public static final int BASE_NETWORK_TIMEOUT_SEC     = 10;
    public static final int BASE_NETWORK_RETRY_COUNT     = 3;

    public static int NETWORK_TIMEOUT_SEC     = BASE_NETWORK_TIMEOUT_SEC;
    public static int NETWORK_RETRY_COUNT     = BASE_NETWORK_RETRY_COUNT;
    public static final boolean SECURE_MODE          = true;

    public static final float MIN_PLAYING_RATE = 0.5f;
    public static final float MAX_PLAYING_RATE = 2.0f;
    public static final float MAX_PLAYING_RATE_DEBUG = 4.0f;

    public static final int MAX_SW_VOLUME = 15;

    public static final int SERVICE_DOWNLOAD = 1;
    public static final int SERVICE_PLAY = 2;

    public static String getPlayerId(Context context) {
        String playerId;
//        if(Log.isDebug())
//            playerId = "febb933cbcf041c9c996a9d6e38b24adc0c2adb6";
//        else
        playerId = Utils.getPlayerId(context);

        return playerId;
    }

    public static String getPlayerIdWithMD5(Context context) {
        String playerIdWidthMd5;
//        if(Log.isDebug())
//            playerIdWidthMd5 = "57589c1e8f046812e4a4f518d57ff929";
//        else
        playerIdWidthMd5 = Utils.getPlayerIdMd5(context);
        return playerIdWidthMd5;
    }
}

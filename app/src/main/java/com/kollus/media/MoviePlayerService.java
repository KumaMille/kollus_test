package com.kollus.media;

import android.app.Activity;
import android.app.AppOpsManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.kollus.media.contents.MultiKollusContent;
import com.kollus.media.preference.KollusConstants;
import com.kollus.sdk.media.util.Log;

public class MoviePlayerService extends Service {
    private final String TAG = MoviePlayerService.class.getSimpleName();

    public static final int ADD_HANDLER     = 10;
    public static final int PLAY_STREAM    = 11;
    public static final int PLAY_DOWNLOAD  = 12;
    public static final int RESUME          = 13;
    public static final int PAUSE           = 14;
    public static final int RESUME_PAUSE   = 15;
    public static final int STOP 		    = 16;
    public static final int COMPLETE	    = 17;

    public static final int APP_FORGROUND	= 18;
    public static final int APP_BACKGROUND	= 19;
    public static final int APP_PIP	= 20;

    private Messenger mMessenger = new Messenger(new LocalHandler());
    private Messenger mClientMessenger;
    private static Activity mActivity;
    private static View mRootView; //movie_view
    private static MoviePlayer mPlayer;
    private PowerManager.WakeLock mWakeLock;
    private WifiManager.WifiLock mWifiLock;
    private boolean mBackgroundPlay; //설정값에서 백그라운드 재생 유무

    private String mChannelId = "com.kollus.media.play";
    private String mChannelName = "Kollus Player Channel";
    private Notification mNotification;

    @Override
    public void onCreate() {
        super.onCreate();
        PowerManager powerManager = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
//        mWakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, ":PlayerWakeLock");
        mWakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, ":PlayerWakeLock");

        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
//        mWifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF,"PlayerWifiLock");
        mWifiLock = wifiManager.createWifiLock("PlayerWifiLock");
        mWifiLock.setReferenceCounted(true);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        Resources resources = getResources();
        mBackgroundPlay = preferences.getBoolean(resources.getString(R.string.preference_background_play_key),
                resources.getBoolean(R.bool.default_background_play));


        if (mBackgroundPlay && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent notificationIntent = new Intent(this, MovieActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
            NotificationChannel channel = new NotificationChannel(
                    mChannelId, mChannelName,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);

            mNotification = new NotificationCompat.Builder(this, mChannelId)
                    .setContentIntent(pendingIntent)
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setPriority(NotificationCompat.PRIORITY_MIN)
                    .build();
        }

    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "PlayerService Bind");
        return mMessenger.getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // TODO Auto-generated method stub
        Log.d(TAG, "PlayerService Unbind");
        return super.onUnbind(intent);
    }

    private void wakeLock() {
        if(!mWakeLock.isHeld()) {
            mWakeLock.acquire();
            Log.d(TAG, "Wake Lock");
        }
        if(!mWifiLock.isHeld()) {
            mWifiLock.acquire();
            Log.d(TAG, "Wifi Lock");
        }
    }

    private void wakeUnlock() {
        if(mWakeLock.isHeld()) {
            mWakeLock.release();
            Log.d(TAG, "Wake Unlock");
        }
        if(mWifiLock.isHeld()) {
            mWifiLock.release();
            Log.d(TAG, "Wifi Unlock");
        }
    }

    public static boolean isPlayerNull() {
        return mPlayer == null;
    }

    public static boolean isInPlaybackState() {
        if(mPlayer != null)
            return mPlayer.isInPlaybackState();
        return false;
    }

    public static boolean onKeyDown(int keyCode, KeyEvent event) {
        if(mPlayer == null)
            return false;

        return mPlayer.onKeyDown(keyCode, event);
    }

    public static boolean onKeyUp(int keyCode, KeyEvent event) {
        if(mPlayer == null)
            return false;

        return mPlayer.onKeyUp(keyCode, event);
    }

    public static void setOrientation(int orientation) {
        if(mPlayer == null)
            return;

        mPlayer.setOrientation(orientation);
    }

    public static void setVolumeLabel(int level, int maxLevel) {
        if(mPlayer == null)
            return;
        mPlayer.setVolumeLabel(level, maxLevel);
    }

    public static void setBrightnessLabel(int level) {
        if(mPlayer == null)
            return;
        mPlayer.setBrightnessLabel(level);
    }

    public static void screenSizeScaleBegin(ScaleGestureDetector detector) {
        if(mPlayer == null)
            return;
        mPlayer.screenSizeScaleBegin(detector);
    }

    public static void screenSizeScale(ScaleGestureDetector detector) {
        if(mPlayer == null)
            return;
        mPlayer.screenSizeScale(detector);
    }

    public static void screenSizeScaleEnd(ScaleGestureDetector detector) {
        if(mPlayer == null)
            return;
        mPlayer.screenSizeScaleEnd(detector);
    }

    public static void toggleMediaControlsVisibility() {
        if(mPlayer == null)
            return;
        mPlayer.toggleMediaControlsVisibility();
    }

    public static void setSeekLabel(int maxX, int maxY, int x, int y, int mountMs, boolean bShow) {
        if(mPlayer == null)
            return;
        mPlayer.setSeekLabel(maxX, maxY, x, y, mountMs, bShow);
    }

    public static boolean isVRMode() {
        if(mPlayer == null)
            return false;
        return mPlayer.isVRMode();
    }

    public static boolean canMoveVideoScreen() {
        if(mPlayer == null)
            return false;
        return mPlayer.canMoveVideoScreen();
    }

    public static void moveVideoFrame(float x, float y) {
        if(mPlayer == null)
            return;

        mPlayer.moveVideoFrame(x, y);
    }

    public static void toggleVideoSize() {
        if(mPlayer == null)
            return;
        mPlayer.toggleVideoSize();
    }

    public static void setBluetoothConnectChanged(boolean connect) {
        if(mPlayer == null)
            return;
        mPlayer.setBluetoothConnectChanged(connect);
    }

    protected static void setActivity(Activity activity) { //movieActivity 바인딩
        mActivity = activity;
        mRootView = mActivity.findViewById(R.id.movie_view_root);
    }

    private class LocalHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "handleMessage what : " + msg.what);
            switch (msg.what) {
                case ADD_HANDLER:
                    mClientMessenger = new Messenger((Handler) msg.obj);
                    try {
                        mClientMessenger.send(Message.obtain(null, ADD_HANDLER, "Registed messanger"));
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    break;
                case PLAY_STREAM:
                    if(mPlayer != null)
                        mPlayer.onDestroy();

                    mPlayer = new MoviePlayer(mRootView, mActivity, Uri.parse((String) msg.obj), msg.arg1==1?true:false) {
                        @Override
                        public void onCompletion() {
                            try {
                                mClientMessenger.send(Message.obtain(null, COMPLETE, "Playback Complete"));
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        }
                    };
                    break;
                case PLAY_DOWNLOAD:
                    if(mPlayer != null)
                        mPlayer.onDestroy();

                    mPlayer = new MoviePlayer(mRootView, mActivity, (MultiKollusContent) msg.obj, msg.arg1==1?true:false) {
                        @Override
                        public void onCompletion() {
                            try {
                                mClientMessenger.send(Message.obtain(null, COMPLETE, "Playback Complete"));
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        }
                    };
                    break;
                case RESUME:
                    if(mPlayer != null)
                        mPlayer.onResume();
                    break;
                case PAUSE:
                    if(mPlayer != null)
                        mPlayer.onPause();
                    break;
                case RESUME_PAUSE:
                    if(mPlayer != null)
                        mPlayer.onPlayPause();
                    break;
                case STOP:
                    if(mPlayer != null) {
                        mPlayer.onDestroy();

//                        try {
//                            mClientMessenger.send(Message.obtain(null, COMPLETE, "Playback Complete"));
//                        } catch (RemoteException e) {
//                            e.printStackTrace();
//                        }
                    }
                    mPlayer = null;
                    break;
                case APP_FORGROUND:
                    if(mBackgroundPlay) {
                        stopForeground(true);
                        wakeUnlock();
                    }
                    break;
                case APP_BACKGROUND:
                    if(mBackgroundPlay) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                            startForeground(KollusConstants.SERVICE_PLAY, mNotification);
                        else
                            startForeground(KollusConstants.SERVICE_PLAY, new Notification());
                        wakeLock();
                    }
                    break;
                case APP_PIP:
                    Log.d(TAG, "APP_PIP");
                    break;
            }
        }
    }
}

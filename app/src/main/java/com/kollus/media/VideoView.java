/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kollus.media;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.asha.vrlib.MD360Director;
import com.asha.vrlib.MD360DirectorFactory;
import com.asha.vrlib.MDVRLibrary;
import com.asha.vrlib.common.MDDirection;
import com.asha.vrlib.model.BarrelDistortionConfig;
import com.asha.vrlib.model.MDPinchConfig;
import com.asha.vrlib.strategy.projection.AbsProjectionStrategy;
import com.asha.vrlib.strategy.projection.IMDProjectionFactory;
import com.asha.vrlib.strategy.projection.MultiFishEyeProjection;
import com.kollus.media.contents.MultiKollusContent;
import com.kollus.media.contents.MultiKollusStorage;
import com.kollus.media.listener.MultiKollusPlayerDRMListener;
import com.kollus.media.preference.KollusConstants;
import com.kollus.media.preference.ValuePreference;
import com.kollus.media.util.DiskUtil;
import com.kollus.media.view.KollusAlertDialog;
import com.kollus.sdk.media.CaptureDetectLister;
import com.kollus.sdk.media.EmulatorCheckerListener;
import com.kollus.sdk.media.ForensicWatermarkView;
import com.kollus.sdk.media.KollusPlayerBookmarkListener;
import com.kollus.sdk.media.KollusPlayerCallbackListener;
import com.kollus.sdk.media.KollusPlayerContentMode;
import com.kollus.sdk.media.KollusPlayerLMSListener;
import com.kollus.sdk.media.KollusPlayerThumbnailListener;
import com.kollus.sdk.media.KollusStorage;
import com.kollus.sdk.media.MediaPlayer;
import com.kollus.sdk.media.MediaPlayer.OnBufferingUpdateListener;
import com.kollus.sdk.media.MediaPlayer.OnCompletionListener;
import com.kollus.sdk.media.MediaPlayer.OnErrorListener;
import com.kollus.sdk.media.MediaPlayer.OnExternalDisplayDetectListener;
import com.kollus.sdk.media.MediaPlayer.OnInfoListener;
import com.kollus.sdk.media.MediaPlayer.OnPreparedListener;
import com.kollus.sdk.media.MediaPlayer.OnSeekCompleteListener;
import com.kollus.sdk.media.MediaPlayer.OnTimedTextDetectListener;
import com.kollus.sdk.media.MediaPlayer.OnTimedTextListener;
import com.kollus.sdk.media.MediaPlayer.OnVideoSizeChangedListener;
import com.kollus.sdk.media.MediaPlayerBase;
import com.kollus.sdk.media.MediaPlayerBase.TrackInfo;
import com.kollus.sdk.media.content.BandwidthItem;
import com.kollus.sdk.media.content.KollusContent;
import com.kollus.sdk.media.util.ErrorCodes;
import com.kollus.sdk.media.util.Log;
import com.kollus.sdk.media.util.Utils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Displays a video file.  The VideoView class
 * can load images from various sources (such as resources or content
 * providers), takes care of computing its measurement from the video so that
 * it can be used in any layout manager, and provides various display options
 * such as scaling and tinting.
 */
public class VideoView extends RelativeLayout {
    private static final String TAG = VideoView.class.getSimpleName();
    private static final String RENDER_THREAD_NAME = "360RenderThread";

    // settable by the client
    private Uri                 mUri;
    private MultiKollusContent  mKollusContent;
    private Map<String, String> mHeaders;

    // all possible internal states
    private static final int STATE_ERROR              = -1;
    private static final int STATE_IDLE               = 0;
    private static final int STATE_PREPARING          = 1;
    private static final int STATE_PREPARED           = 2;
    private static final int STATE_PLAYING            = 3;
    private static final int STATE_PAUSED             = 4;
    private static final int STATE_PLAYBACK_COMPLETED = 5;
    
    private static final float MIN_SCALE_FACTOR = 0.5f;
    private static final float MAX_SCALE_FACTOR = 3.0f;
    
    /** Feature state: Wifi display is not available on this device. */
    public static final int FEATURE_STATE_UNAVAILABLE = 0;
    /** Feature state: Wifi display is disabled, probably because Wifi is disabled. */
    public static final int FEATURE_STATE_DISABLED = 1;
    /** Feature state: Wifi display is turned off in settings. */
    public static final int FEATURE_STATE_OFF = 2;
    /** Feature state: Wifi display is turned on in settings. */
    public static final int FEATURE_STATE_ON = 3;

    /** Scan state: Not currently scanning. */
    public static final int SCAN_STATE_NOT_SCANNING = 0;
    /** Scan state: Currently scanning. */
    public static final int SCAN_STATE_SCANNING = 1;

    // mCurrentState is a VideoView object's current state.
    // mTargetState is the state that a method caller intends to reach.
    // For instance, regardless the VideoView object's current state,
    // calling pause() intends to bring the object to a target state
    // of STATE_PAUSED.
    private int mCurrentState = STATE_IDLE;
    private int mTargetState  = STATE_IDLE;

    // All the stuff we need for playing and showing a video
    private View mSurfaceView;

    private TextView mVideoWatermarkView;
    private int mVideoWatermarkX;
    private int mVideoWatermarkY;
    private Rect mLastControlRect = new Rect();
    private int mVideoWatermarkWidth;
    private int mVideoWatermarkHeight;
    private Random mRandom = new Random();

    private MediaPlayer mMediaPlayer = null;
    private MDVRLibrary mVRLibrary;
    private int         mVideoWidth;
    private int         mVideoHeight;
    private int         mSurfaceWidth;
    private int         mSurfaceHeight;
    private ControllerOverlay mMediaController;
    private OnCompletionListener mOnCompletionListener;
    private OnPreparedListener mOnPreparedListener;
    private KollusPlayerBookmarkListener mKollusPlayerBookmarkListener;
    private KollusPlayerLMSListener mKollusPlayerLMSListener;
    private MultiKollusPlayerDRMListener mMultiKollusPlayerDRMListener;
    private KollusPlayerCallbackListener mKollusPlayerCallbackListener;
    private KollusPlayerThumbnailListener mKollusPlayerThumbnailListener;

    private int         mCurrentBufferPercentage;
    private OnErrorListener mOnErrorListener;
    private OnTimedTextDetectListener  mOnTimedTextDetectListener;
    private OnTimedTextListener mOnTimedTextListener;
    private OnExternalDisplayDetectListener mOnExternalDisplayDetectListener;
    private int         mSeekWhenPrepared;  // recording the seek position while preparing
    private boolean     mCanPause;
    private boolean     mCanSeekBack;
    private boolean     mCanSeekForward;
    private Context		mContext;
    private @ValuePreference.SeekType int mSeekType;
    private ForensicWatermarkView mForensicWatermarkView;

    private MultiKollusStorage mMultiStorage;
    private AudioManager mAudioManager;
    private boolean		mVideoSurfaceReady = false;
    
    private Rect		mDisplayRect;
    private Rect		mVideoRect;
    private Rect		mSurfaceViewRect;
    private int			mScreenSizeMode;
    
    private float		mScalefactor = 1.0f;
    private KollusAlertDialog mAlertMessage;
    private boolean mLive;
    private boolean mVRMode;

    private int mCurrentPosition = -1;
    private int mSeekPosition = -1;
    private SharedPreferences mSharedPreferences;
    private final String INIT_BANDWIDTH_KEY = "init_bandwidth";

    public VideoView(Context context, ForensicWatermarkView view) {
        super(context);
        mContext = context;
        mForensicWatermarkView = view;
        mDisplayRect = new Rect();
        mVideoRect = new Rect();
        mSurfaceViewRect = new Rect();
        mAlertMessage = new KollusAlertDialog(mContext);
        mMultiStorage = MultiKollusStorage.getInstance(mContext);
        mAudioManager = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);

        mVideoWidth = 0;
        mVideoHeight = 0;

        mCurrentState = STATE_IDLE;
        mTargetState  = STATE_IDLE;
    }

    @Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		// TODO Auto-generated method stub
		super.onLayout(changed, l, t, r, b);
		Log.d(TAG, String.format("onLayout %d,%d - %d,%d", l, t, r, b));
		mDisplayRect.set(l, t, r, b);
        if(mScreenSizeMode == KollusPlayerContentMode.ScaleAspectFillStretch)
            mVideoRect.set(mDisplayRect);

        toggleVideoSize(mScreenSizeMode);
	}

    public View getSurfaceView() {
        return mSurfaceView;
    }

    public void setVideoWaterMark(TextView waterMark) {
        mVideoWatermarkView = waterMark;
        mVideoWatermarkView.measure(0, 0);
        mVideoWatermarkWidth = mVideoWatermarkView.getMeasuredWidth();
        mVideoWatermarkHeight = mVideoWatermarkView.getMeasuredHeight();
    }

    public void showVideoWaterMark() {
        Log.d(TAG, "showVideoWaterMark");
        if(mVideoWatermarkView != null) {
            randomVideoWaterMarkPosition();
            mVideoWatermarkView.setVisibility(View.VISIBLE);
        }
    }

    public void hideVideoWaterMark() {
        Log.d(TAG, "hideVideoWaterMark");
        if(mVideoWatermarkView != null) {
            mVideoWatermarkView.setVisibility(View.GONE);
        }
    }

    private void randomVideoWaterMarkPosition() {
        if(mVideoWatermarkView == null)
            return;

        int xMarginMax = mSurfaceViewRect.width()<mDisplayRect.width()?mSurfaceViewRect.width():mDisplayRect.width();
        int yMarginMax = mSurfaceViewRect.height()<mDisplayRect.height()?mSurfaceViewRect.height():mDisplayRect.height();
        int xMargin = 0, yMargin = 0;
        int l=0, t=0;

        xMarginMax -= mVideoWatermarkWidth;
        yMarginMax -= mVideoWatermarkHeight;

        if(xMarginMax > 0)
            xMargin = mRandom.nextInt(xMarginMax);
        if(yMarginMax > 0)
            yMargin = mRandom.nextInt(yMarginMax);

        if(mSurfaceViewRect.left > 0)
            l = mSurfaceViewRect.left;
        if(mSurfaceViewRect.top > 0)
            t = mSurfaceViewRect.top;

        l += xMargin;
        t += yMargin;

        mVideoWatermarkX = l;
        mVideoWatermarkY = t;

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)mVideoWatermarkView.getLayoutParams();
        params.setMargins(l, t, 0, 0);
        mVideoWatermarkView.setLayoutParams(params);

        if(mMediaController.isShowing())
            adjustVideoWaterMarkPosition(mLastControlRect.left, mLastControlRect.top, mLastControlRect.right, mLastControlRect.bottom);
    }

    public void adjustVideoWaterMarkPosition(int left, int top, int right, int bottom) {
        mLastControlRect.set(left, top, right, bottom);

        if(mVideoWatermarkView == null)
            return;

        int showRightMax = right-left;
        int showBottomMax = bottom-top;
        int l=mVideoWatermarkX, t=mVideoWatermarkY;

        if(l+mVideoWatermarkWidth > showRightMax) {
            l = showRightMax - mVideoWatermarkWidth;
        }
        if(l+mVideoWatermarkWidth > mSurfaceViewRect.right) {
            l = mSurfaceViewRect.right - mVideoWatermarkWidth;
        }
        if(l < mSurfaceViewRect.left) {
            l = mSurfaceViewRect.left;
        }

        if(t+mVideoWatermarkHeight > showBottomMax) {
            t = showBottomMax - mVideoWatermarkHeight;
        }
        if(t+mVideoWatermarkHeight > mSurfaceViewRect.bottom) {
            t = mSurfaceViewRect.bottom - mVideoWatermarkHeight;
        }
        if(t < mSurfaceViewRect.top) {
            t = mSurfaceViewRect.top;
        }

        mVideoWatermarkX = l;
        mVideoWatermarkY = t;

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)mVideoWatermarkView.getLayoutParams();
        params.setMargins(l, t, 0, 0);
        mVideoWatermarkView.setLayoutParams(params);
    }

    public void playVideoStream(Uri uri) {
        playVideoStream(uri, null);
    }
    
    public void playVideoDownload(MultiKollusContent content) {
        mKollusContent = content;
        mSeekWhenPrepared = 0;
        initSurfaceView();
        requestLayout();
        invalidate();
    }

    /**
     * @hide
     */
    public void playVideoStream(Uri uri, Map<String, String> headers) {
        Log.d(TAG, "setVideoURI "+uri);
        mUri = uri;
        mHeaders = headers;
        mSeekWhenPrepared = 0;
        initSurfaceView();
        requestLayout();
        invalidate();
    }

    private void initSurfaceView() {
        SurfaceView surfaceView = new SurfaceView(mContext);
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                mVideoSurfaceReady = true;
                if (mCurrentState == STATE_IDLE) {
                    openVideo();
                } else {
                    if(mMediaPlayer != null) {
                        if(surfaceHolder.getSurface().isValid()) {
                            mMediaPlayer.setDisplay(surfaceHolder);
                        }

                        if(mCurrentState == STATE_PAUSED || mMediaPlayer.isPlaying())
                            start();
                    }
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                mVideoSurfaceReady = false;
                if(mMediaPlayer != null)
                    mMediaPlayer.destroyDisplay();
            }
        });

        if(KollusConstants.SECURE_MODE && !Log.isDebug() &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
            surfaceView.setSecure(true);

        mSurfaceView = surfaceView;
        super.addView(mSurfaceView, new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT));

        if(mForensicWatermarkView != null)
            super.addView(mForensicWatermarkView, new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.MATCH_PARENT));
    }

    public void stopPlayback() {
        if (mMediaPlayer != null) {
            Log.d(TAG, "MediaPlayer Stop Call");
            mMediaPlayer.stop();
            Log.d(TAG, "MediaPlayer Release Call");
            mMediaPlayer.release();
            mMediaPlayer = null;
            
            mCurrentState = STATE_IDLE;
            mTargetState  = STATE_IDLE;
        }

        if(mSurfaceView != null)
            super.removeView(mSurfaceView);

        mSurfaceView = null;
        mMediaController.dettachController();

        if(mAlertMessage.isShowing())
            mAlertMessage.dismiss();

        mMultiStorage.unregisterKollusPlayerDRMListener(mMultiKollusPlayerDRMListener);

        if(mKollusContent != null) {
            KollusStorage storage = mKollusContent.getKollusStorage();
            if(storage != null) {
                storage.unregisterKollusPlayerCallbackListener(mKollusPlayerCallbackListener);
                storage.unregisterKollusPlayerThumbnailListener(mKollusPlayerThumbnailListener);
            }
        }
        Log.d(TAG, "stopPlayback Complete");
    }
    
    public void addTimedTextSource(Uri uri) throws IllegalArgumentException, IllegalStateException, IOException {
    	if(mMediaPlayer != null) {
    		mMediaPlayer.addTimedTextSource(mContext, uri);
    	}    	
    }
    
    public void selectTrack(int index) {
    	if(mMediaPlayer != null)
    		mMediaPlayer.selectTrack(index);
    }
    
    public void deselectTrack(int index) {
    	if(mMediaPlayer != null)
    		mMediaPlayer.deselectTrack(index);
    }
    
    public TrackInfo[] getTrackInfo() {
    	if(mMediaPlayer != null)
    		return mMediaPlayer.getTrackInfo();
    	else
    		return null;
    }
    
//    public void checkBookmarkInfo() throws IllegalStateException {
//    	if(mMediaPlayer != null)
//    		mMediaPlayer.checkBookmarkInfo();
//    }
    
    public void updateKollusBookmark(int position, String title) throws IllegalStateException {
    	if(mMediaPlayer != null) {
    	    mMediaPlayer.updateKollusBookmark(position, title);
        }
    }
    
    public void deleteKollusBookmark(int position) throws IllegalStateException {
    	if(mMediaPlayer != null)
    		mMediaPlayer.deleteKollusBookmark(position);
    }
    
//    public boolean isBookmarkable() throws IllegalStateException {
//    	if(mMediaPlayer != null)
//    		return mMediaPlayer.isBookmarkable();
//    	else
//    		return false;
//    }

//    protected int addSubDataSource(String role, String path, SurfaceView surfaceView) {
//        int nRet = mMediaPlayer.addSubDataSource(role, path, surfaceView);
//        Log.d(TAG, "addSubDataSource nRet = "+nRet);
//        return nRet;
//    }

//    protected String swapDisplay(String role) {
//        return mMediaPlayer.swapDisplay(role);
//    }
    
    private void openVideo() {
        if (mUri == null && mKollusContent == null) {
            // not ready for playback just yet, will try again later
            Log.e(TAG, "Not Ready for playback yet.");
            return;
        }
        // Tell the music playback service to pause
        // TODO: these constants need to be published somewhere in the framework.
        Intent i = new Intent("com.android.music.musicservicecommand");
        i.putExtra("command", "pause");
        mContext.sendBroadcast(i);

        // we shouldn't clear the target state, because somebody might have
        // called start() previously
        release(false);

        try {
            KollusStorage storage;
            if(mUri != null)
                storage = mMultiStorage.getStorage(DiskUtil.getDiskIndex(mContext));
            else
                storage = mKollusContent.getKollusStorage();
            mMediaPlayer = new MediaPlayer(mContext, storage, KollusConstants.SERVER_PORT);
            mMediaPlayer.setForensicWatermarkView(mForensicWatermarkView);
            mMediaPlayer.setOnPreparedListener(mPreparedListener);
            mMediaPlayer.setOnVideoSizeChangedListener(mSizeChangedListener);
            mMediaPlayer.setOnCompletionListener(mCompletionListener);
            mMediaPlayer.setOnSeekCompleteListener(mSeekCompleteListener);
            mMediaPlayer.setOnErrorListener(mErrorListener);
            mMediaPlayer.setOnBufferingUpdateListener(mBufferingUpdateListener);
            mMediaPlayer.setOnInfoListener(mInfoListener);
            mMediaPlayer.setKollusPlayerBookmarkListener(mKollusPlayerBookmarkListener);
            mMediaPlayer.setKollusPlayerLMSListener(mKollusPlayerLMSListener);

            mMultiStorage.registerKollusPlayerDRMListener(mMultiKollusPlayerDRMListener);
            storage.registerKollusPlayerCallbackListener(mKollusPlayerCallbackListener);
            storage.registerKollusPlayerThumbnailListener(mKollusPlayerThumbnailListener);

            mMediaPlayer.setOnTimedTextDetectListener(mOnTimedTextDetectListener);
            mMediaPlayer.setOnTimedTextListener(mOnTimedTextListener);
            mMediaPlayer.setOnExternalDisplayDetectListener(mOnExternalDisplayDetectListener);
            mMediaPlayer.setCaptureDetectLister(mCaptureDetectLister);
            mMediaPlayer.setEmulatorCheckerListener(mEmulatorCheckListener);
            mMediaPlayer.setNetworkTimeout(KollusConstants.NETWORK_TIMEOUT_SEC*KollusConstants.NETWORK_RETRY_COUNT);

            mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
            Resources resources = mContext.getResources();
            mSeekType = mSharedPreferences.getInt(resources.getString(R.string.preference_seek_type_key), ValuePreference.QUICK);
            mMediaPlayer.setInitialBandwidth(mSharedPreferences.getInt(INIT_BANDWIDTH_KEY, 0));
            mMediaPlayer.setMinDurationForQualityIncreaseMs(1000);

            mCurrentBufferPercentage = 0;
            if(mSurfaceView != null) {
                if(mSurfaceView instanceof TextureView) {
                    TextureView surface_texture = (TextureView) mSurfaceView;
                    Surface surface = new Surface(surface_texture.getSurfaceTexture());
                    mMediaPlayer.setSurface(surface);
                }
                else if(mSurfaceView instanceof SurfaceView)
                    mMediaPlayer.setDisplay(((SurfaceView) mSurfaceView).getHolder());
            }

            mMediaPlayer.setScreenOnWhilePlaying(true);
            String extraDrmParam = null;
            if(mUri != null)
                mMediaPlayer.setDataSourceByUrl(mUri.toString(), extraDrmParam);
            else if(mKollusContent != null)
                mMediaPlayer.setDataSourceByKey(mKollusContent.getKollusContent().getMediaContentKey(), extraDrmParam);
//            mMediaPlayer.setDataSourceByUrl("http://v.kr.kollus.com/si?jwt=eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJjdWlkIjpudWxsLCJleHB0Ijo0NjY2OTE1MTcxLCJtYyI6W3sibWNrZXkiOiJNSnRRcEhXVCIsImxpdmUiOnsidXJsIjoiaHR0cDpcL1wvcGlwLWxpdmVkcm0tc2t5bGlmZS1jaGNndi5jamVubS5za2Nkbi5jb21cL2xpdmVcL2NoY2d2XC9zN1wvbWFuaWZlc3QubXBkIn19XX0.z-C00Z8jhQWfMqVbqadvo4n2JDZ_8Rlq7lAWbktzOn0&custom_key=4a7afb5bf12768b106c91cfcea161c587e0a2340331aa1259e0f89704f8be6fd");
//            Map<String, String> requesetHeader = new HashMap<String, String>();
//            requesetHeader.put("AcquireLicenseAssertion",
//                    "9b7o9sPkzUSNpO4PIWwW+bVsbgrwFU23Rk/ELcKsaV0jT7TtK9kBwtznlMFDNG/+s5outu/jst6Ye+WTxxtyjPNu8yUVOrFJ/+erqRN4qP5SeTVuj6vbXPzCC/dTUVYZBKUncaOFP2AIhLPbkTTQDaL4uHlEQri8q+mVivEZenPyEQkhs4b8kNgiI1Kjweqlq+L6qgUllfEu9cVpj1GR6mIm4DoLDQRIbrsbyTo78kzDTMGEb5dDMyZg+bRV+SQeI8D2hm9pcQyjOu9AH+gu+kFVWojWLIR3Sh2Xs5Ob39lLE4Xg0zJHN4476fOQsxnCZhRxUoYPi0KOWrk76rvGMAHYc6zXh943kp0Y4SAEBBD9wYTCZNM4sGaRUOSowrbFtjWTwM3A5cKAGDbMjpE+JQyDdc1cIBZ9XlZbFATwfSpETkmPqfgYozp+yqHnnu+Uoec+mhJ+TNoaVSJeSENj1eLBvSk1UUuOFzBNGgQD2NwMmQaFMzZkWCJFOmjIeUXJTHah9MV9INDpK7upvPq0MwiBzuhL+2zdA5bZL9sSVtQFqdyYNWk8ukkCpKNsb5j52Bc+E9NxEniDDJgiyCTqYzYiNU2gMzReYAAxz2iQlFNANylRu9GnKwKYVv4ri3tY");
//            mMediaPlayer.setCencProxyPath("http://skylife.drmkeyserver.com/widevine_license", requesetHeader);
            mMediaPlayer.prepareAsync();
            // we don't set the target state here either, but preserve the
            // target state that was there before.
            mCurrentState = STATE_PREPARING;
            attachMediaController();
        } catch (IllegalArgumentException ex) {
            Log.w(TAG, "Unable to open content: " + mUri+":"+ex);
            mErrorListener.onError(mMediaPlayer, MediaPlayerBase.MEDIA_ERROR_UNKNOWN, 0);
        }
    }

    protected boolean supportPlaybackrateControl() {
        if(mMediaPlayer != null)
            return mMediaPlayer.supportPlaybackrateControl();

        return false;
    }

    protected void setLive(boolean bLive) {
        mLive = bLive;
    }

    protected boolean isLive() {
        return mLive;
    }

    protected boolean isVRMode() {
        return mVRMode;
    }
    
    public void setMediaController(ControllerOverlay controller) {
        if (mMediaController != null) {
            mMediaController.hide();
        }
        mMediaController = controller;
        attachMediaController();
    }

    private void attachMediaController() {
        if (mMediaPlayer != null && mMediaController != null) {
//            mMediaController.setMediaPlayer(this);
//            mMediaController.setEnabled(isInPlaybackState());
        }
    }

    private static final int FADEOUT_INTERVAL = 500;
    private Handler mHandler = new Handler();
//    private Runnable mSoundFadeOut = new Runnable() {
//        @Override
//        public void run() {
//            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mCurrentVolumeLevel, 0);
//        }
//    };

    private class CustomProjectionFactory implements IMDProjectionFactory {

        public static final int CUSTOM_PROJECTION_FISH_EYE_RADIUS_VERTICAL = 9611;

        @Override
        public AbsProjectionStrategy createStrategy(int mode) {
            switch (mode){
                case CUSTOM_PROJECTION_FISH_EYE_RADIUS_VERTICAL:
                    return new MultiFishEyeProjection(0.745f, MDDirection.VERTICAL);
                default:return null;
            }
        }
    }

    private int ALIGN(int x, int y) {
    	return (x + y - 1) & ~(y - 1);
    }
    
    OnVideoSizeChangedListener mSizeChangedListener =
        new OnVideoSizeChangedListener() {
            public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
                boolean bFirst = (mVideoWidth == 0) || (mVideoHeight == 0);
                mVideoWidth = ALIGN(mp.getVideoWidth(), 4);
                mVideoHeight = ALIGN(mp.getVideoHeight(), 4);
                Log.d(TAG, String.format("onVideoSizeChanged (%d %d) dimension(%d %d)",
            			width, height, mVideoWidth, mVideoHeight));
                if (mVideoWidth != 0 && mVideoHeight != 0) {
                    if(mSurfaceView != null && mSurfaceView instanceof SurfaceView)
                        ((SurfaceView)mSurfaceView).getHolder().setFixedSize(mVideoWidth, mVideoHeight);
                    if(bFirst)
                        requestLayout();
                    else
                        toggleVideoSize(mScreenSizeMode);
                }
                
                if (mMediaController != null) {
                	mMediaController.setResolutionText(mVideoWidth, mVideoHeight);
                }
            }
    };

    OnPreparedListener mPreparedListener = new OnPreparedListener() {
        public void onPrepared(MediaPlayer mp) {
            Log.d(TAG, "onPrepared");
        	if(mCurrentState == STATE_ERROR)
        		return;

            mTargetState  = STATE_IDLE;
        	mCurrentState = STATE_PREPARED;

            // Get the capabilities of the player for this stream
            mCanPause = mCanSeekBack = mCanSeekForward = true;

            if (mMediaController != null) {
//                mMediaController.setEnabled(true);
            	mMediaController.setPlayerTypeText(mMediaPlayer.getPlayerName());
                mMediaController.setCodecText(mMediaPlayer.getVideoCodecName());
            }

            mVideoWidth = ALIGN(mp.getVideoWidth(), 4);
            mVideoHeight = ALIGN(mp.getVideoHeight(), 4);

            if (mVideoWidth != 0 && mVideoHeight != 0) {
                if(mSurfaceView != null && mSurfaceView instanceof SurfaceView)
                    ((SurfaceView)mSurfaceView).getHolder().setFixedSize(mVideoWidth, mVideoHeight);
            }

            Resources resources = mContext.getResources();
            boolean lmsSendDownloadContent = mSharedPreferences.getBoolean(resources.getString(R.string.preference_lms_send_download_content_key), true);
            mMediaPlayer.setLmsOffDownloadContent(!lmsSendDownloadContent);
            mMediaPlayer.setNotifyLastReport(false);

            KollusContent content = new KollusContent();
            mMediaPlayer.getKollusContent(content);
            if(mKollusContent == null) {
                mKollusContent = new MultiKollusContent(mMultiStorage.getStorage(DiskUtil.getDiskIndex(mContext)), content);
            }
//            content.setVr(true);
            if(content.isVr()) {
                VideoView.super.removeView(mSurfaceView);

                supportVR360(true);
                GLSurfaceView glSurfaceView = new GLSurfaceView(mContext);
                if(KollusConstants.SECURE_MODE && !Log.isDebug() &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
                    glSurfaceView.setSecure(true);

                mSurfaceView = glSurfaceView;

                mVRLibrary = MDVRLibrary.with(mContext)
                        .displayMode(MDVRLibrary.DISPLAY_MODE_NORMAL)
                        .interactiveMode(MDVRLibrary.INTERACTIVE_MODE_CARDBORAD_MOTION_WITH_TOUCH)
                        .asVideo(new MDVRLibrary.IOnSurfaceReadyCallback() {
                            @Override
                            public void onSurfaceReady(Surface surface) {
                                mMediaPlayer.setSurface(surface);
                            }
                        })
                        .listenGesture(new MDVRLibrary.IGestureListener() {
                            @Override
                            public void onClick(MotionEvent e) {
                                toggleMediaControlsVisibility();
                            }
                        })
                        .ifNotSupport(new MDVRLibrary.INotSupportCallback() {
                            @Override
                            public void onNotSupport(int mode) {
                                String tip = mode == MDVRLibrary.INTERACTIVE_MODE_MOTION
                                        ? "onNotSupport:MOTION" : "onNotSupport:" + String.valueOf(mode);
                                Toast.makeText(mContext, tip, Toast.LENGTH_SHORT).show();
                            }
                        })
                        .pinchConfig(new MDPinchConfig().setMin(1.0f).setMax(8.0f).setDefaultValue(0.1f))
                        .pinchEnabled(true)
                        .directorFactory(new MD360DirectorFactory() {
                            @Override
                            public MD360Director createDirector(int index) {
                                return MD360Director.builder().setPitch(90).build();
                            }
                        })
                        .projectionFactory(new CustomProjectionFactory())
                        .barrelDistortionConfig(new BarrelDistortionConfig().setDefaultEnabled(false).setScale(0.95f))
                        .build(mSurfaceView);

                toggleVideoSize(KollusPlayerContentMode.ScaleAspectFill);
                ((Activity)mContext).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

                VideoView.super.addView(mSurfaceView, new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.MATCH_PARENT,
                        RelativeLayout.LayoutParams.MATCH_PARENT));
                mVRLibrary.onResume(mContext);

                mVRMode = true;
            }

            if(mOnPreparedListener != null)
                mOnPreparedListener.onPrepared(mMediaPlayer);
        }
    };

    private OnSeekCompleteListener mSeekCompleteListener =
        new OnSeekCompleteListener() {
            @Override
            public void onSeekComplete(MediaPlayer mp) {
//                mHandler.postDelayed(mSoundFadeOut, FADEOUT_INTERVAL);
                if (mSeekPosition != mCurrentPosition) {
                    Log.d(TAG, "Executing queued seekTo : "+mCurrentPosition);
                    mSeekPosition = -1;
                    seekTo(mCurrentPosition);
                }
                else {
                    Log.d(TAG, "All seekTo complete - return to regularly scheduled program");
                    mCurrentPosition = mSeekPosition = -1;
                }
            }
        };

    private OnCompletionListener mCompletionListener =
        new OnCompletionListener() {
        public void onCompletion(MediaPlayer mp) {
            mCurrentState = STATE_PLAYBACK_COMPLETED;
            mTargetState = STATE_PLAYBACK_COMPLETED;
            if (mMediaController != null) {
                mMediaController.hide();
            }
            if (mOnCompletionListener != null) {
                mOnCompletionListener.onCompletion(mMediaPlayer);
            }
        }
    };

    private OnErrorListener mErrorListener =
        new OnErrorListener() {
        public boolean onError(MediaPlayer mp, int framework_err, int impl_err) {
        	Log.d(TAG, "Error: " + framework_err + "," + impl_err);

            if(mCurrentState == STATE_ERROR) {
//            	if (mOnCompletionListener != null) {
//                    mOnCompletionListener.onCompletion(mMediaPlayer);
//                }
            	
            	return true;
            }
            if(mMediaPlayer != null)
                mMediaPlayer.pause();
            
            if (mMediaController != null) {
                mMediaController.hide();
            }

            /* If an error handler has been supplied, use it and finish. */
            if (mOnErrorListener != null) {
                if (mOnErrorListener.onError(mMediaPlayer, framework_err, impl_err)) {
                    return true;
                }
            }
            
            /* Otherwise, pop up an error dialog so the user knows that
             * something bad has happened. Only try and pop up the dialog
             * if we're attached to a window. When we're going away and no
             * longer have a window, don't bother showing the user an error.
             */
            if (getWindowToken() != null) {
                Resources r = mContext.getResources();
                String title;
                String message = null;
                
                title = r.getString(R.string.error_title);
                if(framework_err != ErrorCodes.ERROR_EXPIRATION_DATE &&
                		framework_err != ErrorCodes.ERROR_EXPIRATION_COUNT &&
                		framework_err != ErrorCodes.ERROR_EXPIRATION_PLAY_TIME)
                	title = r.getString(R.string.ERROR_CODE) + " : "+impl_err;

                if (framework_err == MediaPlayerBase.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK) {
                    message = r.getString(R.string.VideoView_error_text_invalid_progressive_playback);
                } else if(framework_err == ErrorCodes.ERROR_EXPIRATION_DATE){
                	message = String.format(ErrorCodes.getInstance(mContext).getErrorString(framework_err), 
                			mMediaPlayer.getErrorString(impl_err));
                } else if (framework_err == ErrorCodes.ERROR_EXPIRATION_COUNT) {
                	message = String.format(ErrorCodes.getInstance(mContext).getErrorString(framework_err), impl_err);
                } else if (framework_err == ErrorCodes.ERROR_EXPIRATION_PLAY_TIME) {
                	String dayString = r.getString(R.string.day);
    				String hourString = r.getString(R.string.hours);
    				String minString = r.getString(R.string.min);
    				String secString = r.getString(R.string.sec);
                	message = String.format(ErrorCodes.getInstance(mContext).getErrorString(framework_err), 
                			Utils.stringForTime(dayString, hourString, minString, secString, impl_err*1000));
                } else {
                    if(mMediaPlayer != null) {
                        message = mMediaPlayer.getErrorString(impl_err);
                    }

                    if(message == null) {
                        message = ErrorCodes.getInstance(mContext).getErrorString(impl_err);
                    }
                }

                if(!mAlertMessage.isShowing()) {
	                mAlertMessage
	                        .setTitle(title)
	                        .setMessage(message)
	                        .setPositiveButton(R.string.VideoView_error_button,
	                                new DialogInterface.OnClickListener() {
	                                    public void onClick(DialogInterface dialog, int whichButton) {
	                                        /* If we get here, there is no onError listener, so
	                                         * at least inform them that the video is over.
	                                         */
	                                    	if (mOnCompletionListener != null) {
	                                            mOnCompletionListener.onCompletion(mMediaPlayer);
	                                        }
	                                    }
	                                })
	                        .setCancelable(false)
	                        .show();
                }
            }

            mCurrentState = STATE_ERROR;
            mTargetState = STATE_ERROR;

            return true;
        }
    };

    private OnBufferingUpdateListener mBufferingUpdateListener =
        new OnBufferingUpdateListener() {
        public void onBufferingUpdate(MediaPlayer mp, int percent) {
            mCurrentBufferPercentage = percent;
        }       
    };
    
    private OnInfoListener mInfoListener =
    	new OnInfoListener() {

			@Override
			public boolean onInfo(MediaPlayer mp, int what, int extra) {
				// TODO Auto-generated method stub
				if (mMediaController != null && what == MediaPlayerBase.MEDIA_INFO_FRAME_RATE) {
					int frameRate = (extra>>16)&0x0000FFFF;
					int rejectRate = extra&0x0000FFFF;
	        	    mMediaController.setFrameRateText(frameRate, rejectRate);
	            }
				return false;
			}

			@Override
	        public void onBufferingStart(MediaPlayer mp) {
				// TODO Auto-generated method stub
	        	if (mMediaController != null) {
//	        	    mMediaController.showBuffering();
                    mMediaController.setState(ControllerOverlay.State.BUFFERING);
	            }
	        }

			@Override
	        public void onBufferingEnd(MediaPlayer mp) {
				// TODO Auto-generated method stub
	        	if (mMediaController != null) {
//                    mMediaController.hideBuffering(mCurrentState == STATE_PLAYING);
                    if(mCurrentState == STATE_PLAYING)
                        mMediaController.setState(ControllerOverlay.State.PLAYING);
                    else
                        mMediaController.setState(ControllerOverlay.State.PAUSED);
	            }
	        }

            @Override
            public void onFrameDrop(MediaPlayer mp) {
                Log.e(TAG, "Frame Drop ~~~~~~~~");
            }

            @Override
            public void onDownloadRate(MediaPlayer mp, int downloadRate) {
			    Log.d(TAG, "onDownloadRate "+downloadRate);
                mSharedPreferences.edit().putInt(INIT_BANDWIDTH_KEY, downloadRate).commit();
            }

            @Override
            public void onDetectBandwidthList(MediaPlayer mp, List<BandwidthItem> list) {
			    Log.d(TAG, "onDetectBandwidthList");
			    mMediaController.setBandwidthItemList(list);
            }

            @Override
            public void onChangedBandwidth(MediaPlayer mp, BandwidthItem item) {
                Log.d(TAG, "onChangedBandwidth "+item.getBandwidthName());
                mMediaController.setCurrentBandwidth(item.getBandwidthName());
            }

            @Override
            public void onCodecInitFail(MediaPlayer mp, String componentName) {
                Log.d(TAG, "onCodecInitFail "+componentName);
            }
    };

    /**
     * Register a callback to be invoked when the media file
     * is loaded and ready to go.
     *
     * @param l The callback that will be run
     */
    public void setOnPreparedListener(OnPreparedListener l)
    {
        mOnPreparedListener = l;
    }
    
    public void setKollusPlayerBookmarkListener(KollusPlayerBookmarkListener l) {
    	mKollusPlayerBookmarkListener = l;
    }
    
    public void setKollusPlayerLMSListener(KollusPlayerLMSListener l) {
    	mKollusPlayerLMSListener = l;
    }
    
    public void setKollusPlayerDRMListener(MultiKollusPlayerDRMListener l) {
        mMultiKollusPlayerDRMListener = l;
    }

    public void setKollusPlayerCallbackListener(KollusPlayerCallbackListener l) {
        mKollusPlayerCallbackListener = l;
    }

    public void setKollusPlayerThumbnailListener(KollusPlayerThumbnailListener l) {
        mKollusPlayerThumbnailListener = l;
    }
    
    /**
     * Register a callback to be invoked when the end of a media file
     * has been reached during playback.
     *
     * @param l The callback that will be run
     */
    public void setOnCompletionListener(OnCompletionListener l)
    {
        mOnCompletionListener = l;
    }

    /**
     * Register a callback to be invoked when an error occurs
     * during playback or setup.  If no listener is specified,
     * or if the listener returned false, VideoView will inform
     * the user of any errors.
     *
     * @param l The callback that will be run
     */
    public void setOnErrorListener(OnErrorListener l)
    {
        mOnErrorListener = l;
    }

    /**
     * Register a callback to be invoked when an informational event
     * occurs during playback or setup.
     *
     * @param l The callback that will be run
     */
    public void setOnTimedTextDetectListener(OnTimedTextDetectListener l) {
        mOnTimedTextDetectListener = l;
    }
    
    public void setOnTimedTextListener(OnTimedTextListener l) {
    	mOnTimedTextListener = l;
    }
    
    public void setOnExternalDisplayDetectListener(OnExternalDisplayDetectListener l) {
    	mOnExternalDisplayDetectListener = l;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private TextureView.SurfaceTextureListener getSurfaceListener21(){
        return new TextureView.SurfaceTextureListener() {

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                // TODO Auto-generated method stub
                Log.w(TAG, "onSurfaceTextureSizeChanged");
                mSurfaceWidth = width;
                mSurfaceHeight = height;
            }

            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                Log.w(TAG, "onSurfaceTextureAvailable");
                mVideoSurfaceReady = true;

                if (mCurrentState == STATE_PREPARED) {
                    if(mPreparedListener != null)
                        mPreparedListener.onPrepared(mMediaPlayer);
                }
                else {
                    //etlim 20170831 STATE_ERROR ==>  mMediaPlayer release Issue.
                    //  if((mCurrentState != STATE_ERROR) && (mMediaPlayer != null)) {
                    if(mMediaPlayer != null) {
                        mMediaPlayer.setSurface(new Surface(surface));
                    }

                    if(mCurrentState == STATE_PAUSED || mMediaPlayer.isPlaying())
                        start();
                }
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                // TODO Auto-generated method stub
                Log.w(TAG, "onSurfaceTextureDestroyed");
                mVideoSurfaceReady = false;

                if (mMediaPlayer != null)
                    mMediaPlayer.destroyDisplay();

                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            }
        };
    };
    
    private CaptureDetectLister mCaptureDetectLister =
            new CaptureDetectLister() {
            public void onCaptureDetected(String appName, String packageName) {
            	Log.d(TAG, String.format("Capture Detected (%s, %s)", appName, packageName));
            	new KollusAlertDialog(mContext)
			        .setTitle(R.string.menu_info_str)
			        .setMessage(R.string.stop_for_capture_tool)
			        .setPositiveButton(R.string.confirm,
			                new DialogInterface.OnClickListener() {
			                    public void onClick(DialogInterface dialog, int whichButton) {
			                    	if (mOnCompletionListener != null) {
                                        mOnCompletionListener.onCompletion(mMediaPlayer);
                                    };
			                    }
			                })
			        .show();
            	mMediaPlayer.release();
            	mMediaPlayer = null;
            	mCurrentState = STATE_IDLE;
            }       
        };

    private EmulatorCheckerListener mEmulatorCheckListener = new EmulatorCheckerListener() {
        @Override
        public void onDetectRooting() {
            Log.d(TAG, "onDetectRooting");
            new KollusAlertDialog(mContext)
                    .setTitle(R.string.error_title)
                    .setMessage(R.string.error_rooting)
                    .setPositiveButton(R.string.confirm,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    if (mOnCompletionListener != null) {
                                        mOnCompletionListener.onCompletion(mMediaPlayer);
                                    };
                                }
                            })
                    .show();
            mMediaPlayer.release();
            mMediaPlayer = null;
            mCurrentState = STATE_IDLE;
        }

        @Override
        public void onRunningEmulator() {
            Log.d(TAG, "onRunningEmulator");
            if(mKollusContent.getKollusContent().isVmCheck() && KollusConstants.SECURE_MODE) {
                new KollusAlertDialog(mContext)
                        .setTitle(R.string.error_title)
                        .setMessage(R.string.error_vm)
                        .setPositiveButton(R.string.confirm,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        if (mOnCompletionListener != null) {
                                            mOnCompletionListener.onCompletion(mMediaPlayer);
                                        };
                                    }
                                })
                        .show();
                mMediaPlayer.release();
                mMediaPlayer = null;
                mCurrentState = STATE_IDLE;
            }
        }
    };

    /*
     * release the media player in any state
     */
    private void release(boolean cleartargetstate) {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
            mCurrentState = STATE_IDLE;
            if (cleartargetstate) {
                mTargetState  = STATE_IDLE;
            }
        }
    }

    private void layoutVideoView() {
        if(mSurfaceView != null) {
            if(mSurfaceViewRect.left == mDisplayRect.left && mSurfaceViewRect.top == mDisplayRect.top &&
                mSurfaceViewRect.right == mDisplayRect.right && mSurfaceViewRect.bottom == mDisplayRect.bottom) {
                mSurfaceViewRect.left = mDisplayRect.left+1;
                mSurfaceViewRect.top = mDisplayRect.top+1;
                mSurfaceViewRect.right = mDisplayRect.right-1;
                mSurfaceViewRect.bottom = mDisplayRect.bottom-1;
            }

            mSurfaceView.layout(mSurfaceViewRect.left, mSurfaceViewRect.top, mSurfaceViewRect.right, mSurfaceViewRect.bottom);
        }
    }
    
//    @Override
//    public boolean onTouchEvent(MotionEvent ev) {
//    	if (isInPlaybackState() && mMediaController != null) {
//            toggleMediaControlsVisiblity();
//        }
//        return false;
//    }
    
    @Override
    public boolean onTrackballEvent(MotionEvent ev) {
        if (isInPlaybackState() && mMediaController != null) {
        	toggleMediaControlsVisibility();
        }
        return false;
    }

    public void toggleMediaControlsVisibility() {
        if (mMediaController.isShowing()) {
            mMediaController.hide();
        } else {
            mMediaController.show();
        }
    }
	
	public boolean isControlsShowing() {
		return mMediaController.isShowing();
	}

    public void toggleVideoSize(int screenSizeMode) {
        if (mVideoWidth > 0 && mVideoHeight > 0) {
        	int l = mDisplayRect.left;
        	int t = mDisplayRect.top;
        	int r = mDisplayRect.right;
        	int b = mDisplayRect.bottom;
        	int displayWidth = mDisplayRect.width();
            int displayHeight = mDisplayRect.height();
        	
            mScreenSizeMode = screenSizeMode;
        	if(mScreenSizeMode == KollusPlayerContentMode.ScaleAspectFill) {
        		if ( mVideoWidth * displayHeight  > displayWidth * mVideoHeight ) {
        			displayWidth = displayHeight * mVideoWidth / mVideoHeight;
        		} else if ( mVideoWidth * displayHeight  < displayWidth * mVideoHeight ) {
        			displayHeight = displayWidth * mVideoHeight / mVideoWidth;
        		}
        	}
        	else if(mScreenSizeMode == KollusPlayerContentMode.ScaleAspectFit) {
                if ( mVideoWidth * displayHeight  > displayWidth * mVideoHeight ) {
                    //Log.i("@@@", "image too tall, correcting");
                	displayHeight = displayWidth * mVideoHeight / mVideoWidth;
                } else if ( mVideoWidth * displayHeight  < displayWidth * mVideoHeight ) {
                    //Log.i("@@@", "image too wide, correcting");
                	displayWidth = displayHeight * mVideoWidth / mVideoHeight;
                } else {
                    //Log.i("@@@", "aspect ratio is correct: " +
                            //width+"/"+height+"="+
                            //mVideoWidth+"/"+mVideoHeight);
                }
        	}
        	else if(mScreenSizeMode == KollusPlayerContentMode.ScaleCenter) {
        		displayWidth = mVideoWidth;
        		displayHeight = mVideoHeight;
        	}

            if(mScreenSizeMode == KollusPlayerContentMode.ScaleZoom) {
                float scalefactor = mScalefactor;
                int width = mVideoRect.width();
                int height = mVideoRect.height();
                int scaleWidth = (int)(width*scalefactor);
                int scaleHeight = (int)(height*scalefactor);

                l = (displayWidth-scaleWidth)/2;
                r = l+scaleWidth;
                t = (displayHeight-scaleHeight)/2;
                b = t+scaleHeight;

                mSurfaceViewRect.set(l, t, r, b);
            }
            else {
                mScalefactor = 1.0f;
                l = (r - l - displayWidth) / 2;
                r = l + displayWidth;
                t = (b - t - displayHeight) / 2;
                b = t + displayHeight;

                mVideoRect.set(l, t, r, b);
                mSurfaceViewRect.set(mVideoRect);
            }

            layoutVideoView();
        }
    }
    
    public void screenSizeScaleBegin(ScaleGestureDetector detector) {
        if(mScreenSizeMode == KollusPlayerContentMode.ScaleAspectFillStretch)
            return;

        mMediaController.screenSizeScaleBegin();
        mScreenSizeMode = KollusPlayerContentMode.ScaleZoom;
    }

    public void screenSizeScale(ScaleGestureDetector detector) {
        if(mScreenSizeMode == KollusPlayerContentMode.ScaleAspectFillStretch)
            return;

        float scalefactor = detector.getScaleFactor()+mScalefactor-1.0f;
    	if(scalefactor < MIN_SCALE_FACTOR) {
    		scalefactor = MIN_SCALE_FACTOR;
    	}
    	else if(scalefactor > MAX_SCALE_FACTOR) {
    		scalefactor = MAX_SCALE_FACTOR;
    	}
    	
    	int scaleWidth = (int)(mVideoRect.width()*scalefactor);
    	int scaleHeight = (int)(mVideoRect.height()*scalefactor);
    	
    	int resizeX = (mVideoRect.width() - scaleWidth)/2;
    	int resizeY = (mVideoRect.height() - scaleHeight)/2;
    	
    	int l = mVideoRect.left + resizeX;
    	int t = mVideoRect.top + resizeY;
    	int r = l + scaleWidth;
    	int b = t + scaleHeight;
    	
    	if(scaleWidth < mDisplayRect.width()) {
    		l = (mDisplayRect.width()-scaleWidth)/2;
    		r = l + scaleWidth;
    	}
    	else {
    		if(l > 0) {
    			l = 0;
    			r = scaleWidth;
    		}
    		else if(r < mDisplayRect.width()) {
    			r = mDisplayRect.width();
    			l = r - scaleWidth;
    		}    			
    	}
    	
    	if(scaleHeight < mDisplayRect.height()) {
    		t = (mDisplayRect.height()-scaleHeight)/2;
    		b = t + scaleHeight;
    	}
    	else {
    		if(t > 0) {
    			t = 0;
    			b = scaleHeight;
    		}
    		else if(b < mDisplayRect.height()) {
    			b = mDisplayRect.height();
    			t = b - scaleHeight;
    		} 
    	}

        mSurfaceViewRect.set(l, t, r, b);
        layoutVideoView();
    }
    
    public void screenSizeScaleEnd(ScaleGestureDetector detector) {
        mScalefactor += (detector.getScaleFactor()-1.0f);
    	if(mScalefactor < MIN_SCALE_FACTOR) {
    		mScalefactor = MIN_SCALE_FACTOR;
    	}
    	else if(mScalefactor > MAX_SCALE_FACTOR) {
    		mScalefactor = MAX_SCALE_FACTOR;
    	}

        adjustVideoWaterMarkPosition(mLastControlRect.left, mLastControlRect.top, mLastControlRect.right, mLastControlRect.bottom);
    }
    
    public boolean canMoveVideoScreen() {
    	if( mSurfaceViewRect.width() > mDisplayRect.width() ||
    			mSurfaceViewRect.height() > mDisplayRect.height() )
    		return true;

    	return  false;
    }
    
    public void moveVideoFrame(float velocityX, float velocityY) {
    	if(mSurfaceViewRect.width() > mDisplayRect.width() || mSurfaceViewRect.height() > mDisplayRect.height()) {
	    	int l = mSurfaceViewRect.left;
	    	int t = mSurfaceViewRect.top;
	    	int r = mSurfaceViewRect.right;
	    	int b = mSurfaceViewRect.bottom;
	    	int width = mSurfaceViewRect.width();
	    	int height = mSurfaceViewRect.height();
	    	int displayWidth = mDisplayRect.width();
	    	int displayHeight = mDisplayRect.height();
	    	
	    	int moveX = 0;
			int moveY = 0;
			
	    	if(width > displayWidth) {
	    		if(velocityX >= 0) {
	    			if((l+(int)velocityX) > 0)
	    				moveX = -l;
	    			else
	    				moveX = (int)velocityX;
	    		}
	    		else {
	    			if((r+(int)velocityX) < mDisplayRect.right)
	    				moveX = mDisplayRect.right - r;
	    			else
	    				moveX = (int)velocityX;
	    		}
	    	}
	    	else {
	    		moveX = 0;
	    		l = (displayWidth-width)/2;
	    		r = l+width;
	    	}
	    	
	    	if(height > displayHeight) {
	    		if(velocityY > 0) {
	    			if((t+(int)velocityY) >= 0)
	    				moveY = -t;
	    			else
	    				moveY = (int)velocityY;
	    		}
	    		else {
	    			if((b+(int)velocityY) < mDisplayRect.bottom)
	    				moveY = mDisplayRect.bottom - b;
	    			else
	    				moveY = (int)velocityY;
	    		}
	    	}
	    	else {
	    		moveY = 0;
	    		t = (displayHeight-height)/2;
	    		b = t+height;
	    	}
	    	
	    	mVideoRect.set(mVideoRect.left+moveX, mVideoRect.top+moveY, mVideoRect.right+moveX, mVideoRect.bottom+moveY);
	    	mSurfaceViewRect.set(l+moveX, t+moveY, r+moveX, b+moveY);
            layoutVideoView();
    	}
    }

    public void start() {
        if (isInPlaybackState()) {
            if(!mMediaPlayer.isPlaying()) {
                mMediaPlayer.start();
            }
            mCurrentState = STATE_PLAYING;
        }
        mTargetState = STATE_PLAYING;
        mMediaController.setState(ControllerOverlay.State.PLAYING);
    }

    public void pause() {
        if (isInPlaybackState()) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
            }
            mCurrentState = STATE_PAUSED;
        }
        mTargetState = STATE_PAUSED;
        mMediaController.setState(ControllerOverlay.State.PAUSED);
    }

    public void suspend() {
        release(false);
    }

    public void setLooping(boolean looping) {
    	if (isInPlaybackState())
    		mMediaPlayer.setLooping(looping);
    }

    public void setAudioDelay(int timeMs) {
        if (isInPlaybackState())
            mMediaPlayer.setAudioDelay(timeMs);
    }
    
    public void setVolumeLevel(int level) {
        if (isInPlaybackState())
    		mMediaPlayer.setVolumeLevel(level);
    }
    
    public void setMute(boolean mute) {
    	if (isInPlaybackState()) {
//            mMediaPlayer.setMute(mute);
            if(mute) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                            AudioManager.ADJUST_MUTE, AudioManager.RINGER_MODE_SILENT);
                }
                else {
                    mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, true);
                }
            }
            else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                            AudioManager.ADJUST_UNMUTE, AudioManager.RINGER_MODE_SILENT);
                } else {
                    mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
                }
            }
        }
    }

    public int getDuration() {
        if (isInPlaybackState()) {
            return mMediaPlayer.getDuration();
        }

        return -1;
    }
    
    public int getCurrentPosition() {
        if (isInPlaybackState()) {
            return mMediaPlayer.getCurrentPosition();
        }
        return 0;
    }

    public void seekTo(int msec) {
        if (isInPlaybackState()) {
//            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
            mCurrentPosition = msec;
            if (mSeekPosition < 0) {
                mSeekPosition = msec;
                if(mSeekType == ValuePreference.QUICK)
                    mMediaPlayer.seekTo(msec);
                else
                    mMediaPlayer.seekToExact(msec);
            }
            else {
                Log.d(TAG, "Seek in progress - queue up seekTo : "+msec);
            }

            mSeekWhenPrepared = 0;
        } else {
            mSeekWhenPrepared = msec;
        }
    }

    public void seekToExact(int msec) {
        if (isInPlaybackState()) {
//            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
            mCurrentPosition = msec;
            if (mSeekPosition < 0) {
                mSeekPosition = msec;
                mMediaPlayer.seekToExact(msec);
            }
            else {
                Log.d(TAG, "Seek in progress - queue up seekToExact : "+msec);
            }

            mSeekWhenPrepared = 0;
        } else {
            mSeekWhenPrepared = msec;
        }
    }
    
    public boolean isPlaying() {
        return isInPlaybackState() && mMediaPlayer.isPlaying();
    }

    public int getBufferPercentage() {
        if (mMediaPlayer != null) {
            return mCurrentBufferPercentage;
        }
        return 0;
    }

    public boolean isInPlaybackState() {
        Log.d(TAG, String.format("mCurrentState %d in isInPlaybackState", mCurrentState));
        return (mMediaPlayer != null &&
                mCurrentState != STATE_ERROR &&
                mCurrentState != STATE_IDLE &&
                mCurrentState != STATE_PREPARING);
    }

    public boolean canPause() {
        return mCanPause;
    }

    public boolean canSeekBackward() {
        return mCanSeekBack;
    }

    public boolean canSeekForward() {
        return mCanSeekForward;
    }
    
    public boolean setPlayingRate(float playing_rate) {
    	boolean success = false;
    	if (isInPlaybackState()) {
    		success = mMediaPlayer.setPlayingRate(playing_rate);
    		//success = mMediaPlayer.setParameter(1300, (int)(playing_rate*1000));
    	}
    	
    	return success;
    }
    
    public boolean getKollusContent(KollusContent content) {
    	if (isInPlaybackState())
    		return mMediaPlayer.getKollusContent(content);
    	
    	return false;
    }
    
    public int getVideoWidth() {
    	if(mMediaPlayer == null)
    		return 0;
    	return mVideoWidth;
    }
    
    public int getVideoHeight() {
    	if(mMediaPlayer == null)
    		return 0;
    	return mVideoHeight;
    }
    
    public int getPlayAt() {
    	return mMediaPlayer.getPlayAt();
    }
    
    public int getCachedDuration() {
        if (isInPlaybackState()) {
            return mMediaPlayer.getCachedDuration();
        }

        return -1;
    }
    
    public void skip() {
    }

    private void supportVR360(boolean support) {
        if(mMediaController != null)
            mMediaController.supportVR360(support);
    }

    public int getPlayerType() {
        if(mMediaPlayer != null)
            return mMediaPlayer.getPlayerType();

        return Utils.PLAYER_TYPE_NONE;
    }

    public void onVRCardView(boolean enable) {
        if(!mVRMode)
            return;

        if(enable) {
            mVRLibrary.switchDisplayMode(mContext, MDVRLibrary.DISPLAY_MODE_GLASS);
        }
        else {
            mVRLibrary.switchDisplayMode(mContext, MDVRLibrary.DISPLAY_MODE_NORMAL);
        }
    }

    public void onPause() {
        if(mVRLibrary != null)
            mVRLibrary.onPause(mContext);
    }

    public void onResume() {
        if(mVRLibrary != null)
            mVRLibrary.onResume(mContext);
    }

    public void setBandwidth(int bandwidth) {
        Log.d(TAG, "setBandwidth to "+bandwidth);
        if (mMediaPlayer != null) {
            mMediaPlayer.setBandwidth(bandwidth);
        }
    }
}

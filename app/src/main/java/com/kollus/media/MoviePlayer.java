/*
 * Copyright (C) 2009 The Android Open Source Project
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
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothHeadset;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Color;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Html;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.util.Util;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaLoadRequestData;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.CastState;
import com.google.android.gms.cast.framework.CastStateListener;
import com.google.android.gms.cast.framework.SessionManagerListener;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.google.android.gms.common.images.WebImage;
import com.kollus.media.chat.ChattingView;
import com.kollus.media.chat.ChattingWebViewClient;
import com.kollus.media.contents.BookmarkAdapter;
import com.kollus.media.contents.BookmarkAdapter.onBookmarkRemoveListener;
import com.kollus.media.contents.MultiKollusContent;
import com.kollus.media.contents.MultiKollusStorage;
import com.kollus.media.listener.MultiKollusPlayerDRMListener;
import com.kollus.media.preference.KollusConstants;
import com.kollus.media.preference.ValuePreference;
import com.kollus.media.util.DiskUtil;
import com.kollus.media.util.DisplayUtil;
import com.kollus.media.view.KollusAlertDialog;
import com.kollus.sdk.media.ForensicWatermarkView;
import com.kollus.sdk.media.KollusPlayerBookmarkListener;
import com.kollus.sdk.media.KollusPlayerCallbackListener;
import com.kollus.sdk.media.KollusPlayerContentMode;
import com.kollus.sdk.media.KollusPlayerLMSListener;
import com.kollus.sdk.media.KollusPlayerThumbnailListener;
import com.kollus.sdk.media.KollusStorage;
import com.kollus.sdk.media.MediaPlayer;
import com.kollus.sdk.media.MediaPlayer.OnCompletionListener;
import com.kollus.sdk.media.MediaPlayer.OnErrorListener;
import com.kollus.sdk.media.MediaPlayer.OnExternalDisplayDetectListener;
import com.kollus.sdk.media.MediaPlayer.OnPreparedListener;
import com.kollus.sdk.media.MediaPlayer.OnTimedTextDetectListener;
import com.kollus.sdk.media.MediaPlayer.OnTimedTextListener;
import com.kollus.sdk.media.MediaPlayerBase;
import com.kollus.sdk.media.MediaPlayerBase.TrackInfo;
import com.kollus.sdk.media.content.KollusBookmark;
import com.kollus.sdk.media.content.KollusContent;
import com.kollus.sdk.media.content.KollusContent.SubtitleInfo;
import com.kollus.sdk.media.util.ErrorCodes;
import com.kollus.sdk.media.util.Log;
import com.kollus.sdk.media.util.SkinManager;
import com.kollus.sdk.media.util.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.Vector;

import static android.content.Context.ACCESSIBILITY_SERVICE;

@TargetApi(Build.VERSION_CODES.GINGERBREAD_MR1)
public class MoviePlayer implements
        OnErrorListener, OnCompletionListener,
        ControllerOverlay.Listener, OnPreparedListener, 
        OnTimedTextListener, OnTimedTextDetectListener, onBookmarkRemoveListener,
        OnExternalDisplayDetectListener {
    @SuppressWarnings("unused")
    private static final String TAG = "MoviePlayer";

    // Copied from MediaPlaybackService in the Music Player app.
    private static final String SERVICECMD = "com.android.music.musicservicecommand";
    private static final String CMDNAME = "command";
    private static final String CMDPAUSE = "pause";

    private static final long BLACK_TIMEOUT = 500;


    private Activity mContext;
    private ViewGroup mRootView;
    private ChattingView mChattingView;
    private ViewTreeObserver.OnGlobalLayoutListener mKeyboardLayoutListener;

    private ViewGroup mControlView;
    private VideoView mVideoView;
	private ImageView mCaptionImageView;
	private ImageView mSoundOnlyImageView;
	private Bitmap    mSoundBitmap;
    private Bitmap    mCurrentThumbnail;
    private Bitmap    mDefaultThumbnail;
//	private boolean   mSoundOnly;
	private TextView mCaptionStringView;
	
	private int mCaptionColor = 0xffffffff;
    private int mCaptionSize = 12;
    private int mStrokeColor = 0xff000000;
    private float mStrokeWidth = 5;//0.5f;

    private Uri mUri;
    private KollusStorage mKollusStorage;
    private MultiKollusContent mMultiKollusContent;
    private KollusContent mKollusContent;
    private Uri mCaptionUri;
    private final Handler mHandler = new Handler();
    private MovieControllerOverlay mController;

    // If the time bar is being dragged.
    private boolean mDragging;

    // If the time bar is visible.
    private boolean mShowing;
    
    private float mPlayingRate = 1.0f;
    
    private BookmarkAdapter mBookmarkAdapter;
    private ArrayList<KollusBookmark> mBookmarkList;
    private BitmapRegionDecoder mScreenShot = null;
    private BitmapFactory.Options mScreenShotOption;
    private int mScreenShotWidth;
    private int mScreenShotHeight;
    private int mScreenShotCount;
    private float mScreenShotInterval;
    private Toast mToast;
    private int mRepeatAMs = -1;
    private int mRepeatBMs = -1;
    private boolean mVolumeMute;
    private Vector<SubtitleInfo> mSubtitles;
    private boolean mVideoWaterMarkShow;

    private boolean mExternalDisplayPlugged;
    private int mExternalDisplayType;
    
    private int mSeekScreenShotTimeMs = -1;
    private boolean mSeekLocked = false;

    private boolean mScreenLocked;

    private KollusAlertDialog mAlertDialog;
    private boolean mStarted;
    private int mSeekableEnd = -1;

    private boolean mTalkbackEnabled;
    private int mSeekInterval;

    private boolean mBluetoothConnect;
    private int mAudioDelayMs;
    private boolean mReceivedThumbnail;
    private boolean mPopupModeChanged;
    private boolean mIsAppForground = true;

    private CastContext mCastContext;
    private CastSession mCastSession;
    private SessionManagerListener<CastSession> mSessionManagerListener;
    private CastStateListener mCastStateListener;
    private Object mLocker = new Object();
    private boolean mReceivedMediaInfo;

    private final Runnable mPlayingChecker = new Runnable() {
        @Override
        public void run() {
            if (mVideoView.isPlaying()) {
                mController.showPlaying();
            } else {
                mHandler.postDelayed(mPlayingChecker, 250);
            }
        }
    };

    private final Runnable mProgressChecker = new Runnable() {
        @Override
        public void run() {
            if(mSeekLocked && mKollusContent.getSkipSec()*1000 < mVideoView.getCurrentPosition())
                mSeekLocked = false;

            if(mRepeatAMs >= 0 && mRepeatAMs < mRepeatBMs && mRepeatBMs <= mVideoView.getCurrentPosition()) {
                Log.d(TAG, String.format("Repeat Seek (%d~%d)", mRepeatAMs, mRepeatBMs));
        		mVideoView.seekToExact(mRepeatAMs);
        	}
            int pos = setProgress();
            mHandler.postDelayed(mProgressChecker, 1000 - (pos % 1000));
        }
    };

    private final Runnable mVideoWaterMarkRunnable = new Runnable() {
        @Override
        public void run() {
            mVideoWaterMarkShow = !mVideoWaterMarkShow;
            if(mVideoWaterMarkShow) {
                mVideoView.showVideoWaterMark();
                mHandler.postDelayed(mVideoWaterMarkRunnable, mKollusContent.getVideoWaterMarkShowTime()*1000);
            }
            else {
                mVideoView.hideVideoWaterMark();
                mHandler.postDelayed(mVideoWaterMarkRunnable, mKollusContent.getVideoWaterMarkHideTime()*1000);
            }
        }
    };

    public MoviePlayer(View rootView, Activity activity, Uri videoUri, boolean bPopupChanged) {
    	Log.d(TAG, "MoviePlayer Creator");
    	mUri = videoUri;
        mKollusContent = new KollusContent();
        MultiKollusStorage multiKollusStorage = MultiKollusStorage.getInstance(activity);
        mKollusStorage = multiKollusStorage.getStorage(DiskUtil.getDiskIndex(activity));
        mMultiKollusContent = new MultiKollusContent(mKollusStorage, mKollusContent);
    	init(rootView, activity, bPopupChanged);
    }
    
    public MoviePlayer(View rootView, Activity activity, MultiKollusContent content, boolean bPopupChanged) {
    	Log.d(TAG, "MoviePlayer Creator");
        mMultiKollusContent = content;
        mKollusStorage = content.getKollusStorage();
        mKollusContent = content.getKollusContent();

    	init(rootView, activity, bPopupChanged);
    }
    
    private void init(View rootView, Activity activity, boolean bPopupModeChanged) {
    	mContext = activity;
    	mRootView = (ViewGroup)rootView;
        mPopupModeChanged = bPopupModeChanged;

        SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(mContext);
		Resources resources = mContext.getResources();

        mChattingView = (ChattingView)mRootView.findViewById(R.id.chatting_view);
        mChattingView.setMoviePlayer(this);

        mControlView = (ViewGroup)mRootView.findViewById(R.id.control_view_layer);
		mController = new MovieControllerOverlay(mContext, mControlView);
        mController.setListener(this);
        mController.setCanReplay(false);
        
        RelativeLayout videoView = (RelativeLayout)mRootView.findViewById(R.id.surface_view_layer);
//        ForensicWatermarkView watermarkView = new ForensicWatermarkView(mContext, mKollusStorage, KollusConstants.KEY, KollusConstants.EXPIRE_DATE, mUri == null);
        mVideoView = new VideoView(activity, null/*watermarkView*/);
        mVideoView.setMediaController(mController);
        mVideoView.setOnExternalDisplayDetectListener(this);
        
        mBookmarkAdapter = new BookmarkAdapter(mContext);
        mBookmarkAdapter.setOnBookmarkRemoveListener(this);
        mController.setBookmarkAdapter(mBookmarkAdapter);
        
        videoView.addView(mVideoView,
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT);

        mVideoView.setOnErrorListener(this);
        mVideoView.setOnCompletionListener(this);
        mVideoView.setOnPreparedListener(this);
        mVideoView.setKollusPlayerBookmarkListener(mKollusPlayerBookmarkListener);
        mVideoView.setKollusPlayerLMSListener(mKollusPlayerLMSListener);
        mVideoView.setKollusPlayerDRMListener(mKollusPlayerDRMListener);
        mVideoView.setKollusPlayerCallbackListener(mKollusPlayerCallbackListener);
        mVideoView.setKollusPlayerThumbnailListener(mKollusPlayerThumbnailListener);
        mVideoView.setOnTimedTextDetectListener(this);
        mVideoView.setOnTimedTextListener(this);
        if(mUri != null)
        	mVideoView.playVideoStream(mUri);
        else
        	mVideoView.playVideoDownload(mMultiKollusContent);
//        mVideoView.setOnTouchListener(mTouchListener);
        
        mCaptionColor = preference.getInt(
        		resources.getString(R.string.preference_caption_color_key), 
        		resources.getColor(R.color.default_caption_color));
        mCaptionSize = preference.getInt(
        		resources.getString(R.string.preference_caption_size_key), 
        		resources.getInteger(R.integer.default_caption_size));
        
        mStrokeColor = preference.getInt(resources.getString(R.string.preference_stroke_color_key), 
        		resources.getColor(R.color.default_stroke_color));
		boolean stroke = preference.getBoolean(resources.getString(R.string.preference_stroke_key), 
				resources.getBoolean(R.bool.default_stroke));
		if(!stroke)
			mStrokeWidth = 0;

        mCaptionImageView = (ImageView)mRootView.findViewById(R.id.captionImage);
		mCaptionImageView.setBackgroundColor(Color.TRANSPARENT);
        mCaptionImageView.setImageBitmap(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888));
//		mCaptionImageView.setOnTouchListener(mTouchListener);
		
		mCaptionStringView = (TextView)mRootView.findViewById(R.id.captionString);
//		mCaptionSize = (int) TypedValue.applyDimension(
//							TypedValue.COMPLEX_UNIT_PX, mCaptionSize, resources.getDisplayMetrics());
		mCaptionStringView.setTextColor(mCaptionColor);
		mCaptionStringView.setTextSize(mCaptionSize);
		//mCaptionStringView.setShadowLayer(mStrokeWidth, 1, 1, mStrokeColor);
		mCaptionStringView.setShadowLayer(mStrokeWidth, 0, 0, mStrokeColor);
		
		mSoundOnlyImageView = (ImageView)mRootView.findViewById(R.id.sound_only);

        mSessionManagerListener = new SessionManagerListener<CastSession>() {
            @Override
            public void onSessionEnded(CastSession session, int error) {
                Log.d("PlayCastSession", "onSessionEnded");
                if (session == mCastSession) {
                    mCastSession = null;
                }
                mController.setStateMediaRoute(ControllerOverlay.MediaRouteState.END);
                onApplicationDisconnected();
            }

            @Override
            public void onSessionResumed(CastSession session, boolean wasSuspended) {
                Log.d("PlayCastSession", "onSessionResumed");
                mCastSession = session;
                mController.setStateMediaRoute(ControllerOverlay.MediaRouteState.RESUME);
                onApplicationConnected(session);
            }

            @Override
            public void onSessionResumeFailed(CastSession session, int error) {
                Log.d("PlayCastSession", "onSessionResumeFailed");
                onApplicationDisconnected();
            }

            @Override
            public void onSessionStarted(CastSession session, String sessionId) {
                Log.d("PlayCastSession", "onSessionStarted");
                mCastSession = session;
                mController.setStateMediaRoute(ControllerOverlay.MediaRouteState.STARTED);
                onApplicationConnected(session);
            }

            @Override
            public void onSessionStarting(CastSession session) {
                Log.d("PlayCastSession", "onSessionStarting");
                mController.setStateMediaRoute(ControllerOverlay.MediaRouteState.STARTING);
            }

            @Override
            public void onSessionStartFailed(CastSession session, int error) {
                Log.d("PlayCastSession", "onSessionStartFailed");
                onApplicationDisconnected();
            }

            @Override
            public void onSessionEnding(CastSession session) {
                Log.d("PlayCastSession", "onSessionEnding");
            }

            @Override
            public void onSessionResuming(CastSession session, String sessionId) {
                Log.d("PlayCastSession", "onSessionResuming");
            }

            @Override
            public void onSessionSuspended(CastSession session, int reason) {
                Log.d("PlayCastSession", "onSessionSuspended");
            }

            private void onApplicationConnected(CastSession castSession) {
                mCastSession = castSession;
                if(mVideoView.isPlaying()) {
                    mVideoView.pause();
                    loadRemoteMedia(mVideoView.getCurrentPosition(), true);
                }
            }

            private void onApplicationDisconnected() {
                mVideoView.start();
            }
        };

        mCastStateListener = new CastStateListener() {
            @Override
            public void onCastStateChanged(int newState) {
                Log.d("PlayCastSession", "onCastStateChanged "+newState);
                if (newState == CastState.NO_DEVICES_AVAILABLE) {
                    mController.setAvailableMediaRoute(false);
                }
                else {
                    mController.setAvailableMediaRoute(true);
                    if(mCastSession != null && mCastSession.isConnected()) {
                        mController.setStateMediaRoute(ControllerOverlay.MediaRouteState.STARTED);
                    }
                }
            }
        };

        try {
            mCastContext = CastContext.getSharedInstance(mContext);
            if (KollusConstants.SUPPORT_REMOTE_MEDIA_ROUTE && mCastContext != null) {
                mCastContext.addCastStateListener(mCastStateListener);
                mCastContext.getSessionManager().addSessionManagerListener(
                        mSessionManagerListener, CastSession.class);
                mCastContext.getSessionManager().startSession(mContext.getIntent());
                if (mCastSession == null) {
                    mCastSession = CastContext.getSharedInstance(mContext).getSessionManager()
                            .getCurrentCastSession();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
//        mRootView.setOnTouchListener(mTouchListener);

        // The SurfaceView is transparent before drawing the first frame.
        // This makes the UI flashing when open a video. (black -> old screen
        // -> video) However, we have no way to know the timing of the first
        // frame. So, we hide the VideoView for a while to make sure the
        // video has been drawn on it.
        mVideoView.postDelayed(new Runnable() {
            @Override
            public void run() {
                mVideoView.setVisibility(View.VISIBLE);
            }
        }, BLACK_TIMEOUT);

        Intent intent = new Intent(SERVICECMD);
        intent.putExtra(CMDNAME, CMDPAUSE);
        activity.sendBroadcast(intent);
        
        mController.setMoviePlayer(this);
        mController.setState(ControllerOverlay.State.LOADING);
        mSeekInterval = preference.getInt(resources.getString(R.string.preference_seek_interval_key),
                10)*1000;

        mAudioDelayMs = preference.getInt(resources.getString(R.string.preference_audio_delay_key), 0);
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter != null && bluetoothAdapter.isEnabled()
                && bluetoothAdapter.getProfileConnectionState(BluetoothHeadset.HEADSET) == BluetoothHeadset.STATE_CONNECTED) {
            mController.setBluetoothConnectChanged(true);
            mBluetoothConnect = true;
        }

//        WebView chat = new WebView(mContext);
////        chat.loadUrl("https://chatroll.com/embed/chat/v43r?id=vq8IfnmqguE&platform=html");
//        chat.loadUrl("https://chatroll.com/v43r");
//        mRootView.addView(chat, new ViewGroup.LayoutParams(800, ViewGroup.LayoutParams.FILL_PARENT));
        unlockScreenOrientation();
    }

    private void loadRemoteMedia(int position, boolean autoPlay) {
        if (mCastSession == null) {
            Log.e(TAG, "CastSession is Null.");
            return;
        }

        final RemoteMediaClient remoteMediaClient = mCastSession.getRemoteMediaClient();
        if (remoteMediaClient == null) {
            Log.e(TAG, "RemoteMediaClient is Null.");
            return;
        }

        MediaMetadata movieMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);
        movieMetadata.putString(MediaMetadata.KEY_TITLE, mKollusContent.getSubCourse());
        movieMetadata.addImage(new WebImage(Uri.parse(mKollusContent.getThumbnailPath())));

        JSONObject jsonObj = null;
        if(mKollusContent.getLicenseUrl() != null && mKollusContent.getLicenseUrl().length() > 0) {
            try {
                jsonObj = new JSONObject();
                jsonObj.put("licenseUrl",
                        mKollusContent.getLicenseUrl()+
                                "?"+mKollusContent.getLicenseKey()+"="+
                                mKollusContent.getLicenseToken());
            } catch (JSONException e) {
                android.util.Log.e(TAG, "Failed to add description to the json object", e);
            }
        }

        int type = Util.inferContentType(Uri.parse(mKollusContent.getMediaUrl()).getLastPathSegment());
        MediaInfo.Builder builder = new MediaInfo.Builder(mKollusContent.getMediaUrl())
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setMetadata(movieMetadata)
                .setMediaTracks(null)
                .setStreamDuration(mVideoView.getDuration())
                .setCustomData(jsonObj);
        switch (type) {
            case C.TYPE_SS:
                builder.setContentType("application/vnd.ms-sstr+xml");
                break;
            case C.TYPE_DASH:
                builder.setContentType("application/dash+xml");
                break;
            case C.TYPE_HLS:
                builder.setContentType("application/x-mpegurl");
                break;
            case C.TYPE_OTHER:
                builder.setContentType("videos/mp4");
                break;
            default: {
                throw new IllegalStateException("Unsupported type: " + type);
            }
        }
        MediaInfo mediaInfo = builder.build();
        remoteMediaClient.load(new MediaLoadRequestData.Builder()
                .setMediaInfo(mediaInfo)
                .setAutoplay(autoPlay)
                .setCurrentTime(position).build());
    }
    
    private String formatDuration(final Context context, int duration) {
        int h = duration / 3600;
        int m = (duration - h * 3600) / 60;
        int s = duration - (h * 3600 + m * 60);
        String durationValue;
        if (h == 0) {
            durationValue = String.format(context.getString(R.string.details_ms), m, s);
        } else {
            durationValue = String.format(context.getString(R.string.details_hms), h, m, s);
        }
        return durationValue;
    }

    private void showResumeDialog(Context context, final int playAt) {
        mAlertDialog = new KollusAlertDialog(context).
        	setTitle(R.string.resume_playing_title).
        	setMessage(String.format(
                context.getString(R.string.resume_playing_message),
                formatDuration(context, playAt / 1000))).
            setOnCancelListener(new OnCancelListener() {
		            @Override
		            public void onCancel(DialogInterface dialog) {
		                onCompletion();
		            }
		        }).
		    setPositiveButton(
		                R.string.resume_playing_resume, new OnClickListener() {
		            @Override
		            public void onClick(DialogInterface dialog, int which) {
		                mVideoView.seekTo(playAt);
                        startVideo();
		            }
		        }).
        	setNegativeButton(
		                R.string.resume_playing_restart, new OnClickListener() {
		            @Override
		            public void onClick(DialogInterface dialog, int which) {
		                startVideo();
		            }
		        }).
	        show();
    }
    
    public void addTimedTextSource(Uri uri) {
    	mCaptionUri = uri;    	
    }

    public void onPause() {
        Log.d(TAG, "onPause");
        mIsAppForground = false;

        mVideoView.onPause();
        pauseVideo();
    }

    public void onResume() {
        Log.d(TAG, "onResume");
        mIsAppForground = true;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            AccessibilityManager am = (AccessibilityManager) mContext.getSystemService(ACCESSIBILITY_SERVICE);
            mTalkbackEnabled = am.isEnabled() && am.isTouchExplorationEnabled();
        }
        mController.setTalkbackEnabled(mTalkbackEnabled);

        mVideoView.onResume();

        if(mChattingView != null) {
            mChattingView.onResume();
        }

        if(mAlertDialog != null && mAlertDialog.isShowing())
            return;

        if(mStarted)
            playVideo();
        else
            startVideo();
    }

    private void recycleResource() {
        mSoundOnlyImageView.setVisibility(View.GONE);

        if(mScreenShot != null) {
            mScreenShot.recycle();
        }
        mScreenShot= null;

        if(mBookmarkList != null) {
            for (KollusBookmark bookmark : mBookmarkList) {
                Bitmap bitmap = bookmark.getThumbnail();
                if (bitmap != null && bitmap != mDefaultThumbnail)
                    bitmap.recycle();
            }
        }
        mBookmarkList = null;

        if(mDefaultThumbnail != null && mDefaultThumbnail != mSoundBitmap)
            mDefaultThumbnail.recycle();
        mDefaultThumbnail = null;
    }

    public void onDestroy() {
    	Log.d(TAG, "onDestroy");
        mVideoView.stopPlayback();
        recycleResource();

        if(mChattingView != null) {
            mChattingView.onClose();
            mChattingView.getViewTreeObserver().removeGlobalOnLayoutListener(mKeyboardLayoutListener);
        }
        mChattingView = null;

        if(mCastContext != null) {
            mCastContext.removeCastStateListener(mCastStateListener);
            mCastContext.getSessionManager().removeSessionManagerListener(
                    mSessionManagerListener, CastSession.class);
        }
        mCastContext = null;

        if(mAlertDialog != null) {
            mAlertDialog.dismiss();
        }

        try {
            mHandler.removeCallbacks(mProgressChecker);
        } catch (Exception e) {}

         try {
            mHandler.removeCallbacks(mVideoWaterMarkRunnable);
        } catch (Exception e) {}
    }

    // This updates the time bar display (if necessary). It is called every
    // second by mProgressChecker and also from places where the time bar needs
    // to be updated immediately.
    private int setProgress() {
        if (mDragging || !mShowing) {
            return 0;
        }
        int position = mVideoView.getCurrentPosition();
        if(mCastSession != null && mCastSession.isConnected()) {
            final RemoteMediaClient remoteMediaClient = mCastSession.getRemoteMediaClient();
            position = (int)remoteMediaClient.getApproximateStreamPosition();
        }
        int duration = mVideoView.getDuration();
        if(mSeekableEnd >= 0 && mSeekableEnd < position)
            mSeekableEnd = position;
        mController.setTimes(position, mSeekableEnd, duration, 0, 0);
        return position;
    }

    private void startVideo() {
        // For streams that we expect to be slow to start up, show a
        // progress spinner until playback starts.
        Log.d(TAG, "startVideo");
		if(finishByDetectExternalDisplay()) {
			return;
		}

		if(!checkAutoTime()) {
    		return;
		}

    	if(mUri != null) {
	        String scheme = mUri.getScheme();
	        if ("http".equalsIgnoreCase(scheme) || "rtsp".equalsIgnoreCase(scheme)) {
                mController.setState(ControllerOverlay.State.LOADING);
	            mHandler.removeCallbacks(mPlayingChecker);
	            mHandler.postDelayed(mPlayingChecker, 250);
	        }
    	}

        mRootView.setKeepScreenOn(true);
//		float playing_rate = 2.0f;
//        if(mVideoView.setPlayingRate(playing_rate)) {
//            mController.setPlayingRateText(playing_rate);
//            mPlayingRate = playing_rate;
//        }
        mVideoView.start();
        if(!mPopupModeChanged)
            mController.show();
        mStarted = true;

        setProgress();
        if(KollusConstants.SECURE_MODE && !Log.isDebug())
            mContext.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);

    }

    private void playVideo() {
        if(!mStarted) {
            Log.w(TAG, "cannot playVideo, why not started");
            return;
        }

		if(finishByDetectExternalDisplay()) {
			return;
		}

		if(!checkAutoTime()) {
    		return;
		}

        mRootView.setKeepScreenOn(true);
        if(mCastSession != null && mCastSession.isConnected()) {
            final RemoteMediaClient remoteMediaClient = mCastSession.getRemoteMediaClient();
            remoteMediaClient.play();
        }
        else {
            mVideoView.start();
        }
        setProgress();
    }

    private void pauseVideo() {
        if(!mStarted) {
            Log.w(TAG, "cannot pauseVideo, why not started");
            return;
        }

        mRootView.setKeepScreenOn(false);
        if(mCastSession != null && mCastSession.isConnected()) {
            mCastSession.getRemoteMediaClient().pause();
        }
        else {
            mVideoView.pause();
        }
    }

    private void seekVideo(int time) {
        if(mSeekLocked) {
            return;
        }

        if(mSeekableEnd >= 0 && time > mSeekableEnd)
            time = mSeekableEnd;

        resetRepeatAB(time);
        mRootView.setKeepScreenOn(true);  //etlim fixed. 20170829 Screen On.
        if(mCastSession != null && mCastSession.isConnected()) {
            final RemoteMediaClient remoteMediaClient = mCastSession.getRemoteMediaClient();
            remoteMediaClient.seek(time);
            remoteMediaClient.play();
        }
        else {
            mVideoView.seekTo(time);
            if (mStarted && mIsAppForground)
                mVideoView.start();
        }
        setProgress();
    }
    
    private boolean finishByDetectExternalDisplay() {
        if(mKollusContent == null) {
            Log.d(TAG, "Kollus Content is null in finishByDetectExternalDisplay");
            return false;
        }

    	boolean detected = false;

        Log.d(TAG, "plugged "+mExternalDisplayPlugged+
                " disable tvout "+mKollusContent.getDisableTvOut()+
                " has tv feature "+mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEVISION));
    	if(mExternalDisplayPlugged && mKollusContent.getDisableTvOut() && !mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEVISION)) {
//			int message = R.string.restart_after_remove_hdmi_cable;
//			if(mExternalDisplayType == MediaPlayerBase.EXTERNAL_HDMI_DISPLAY)
//				message = R.string.restart_after_not_allowed;
//			else if(mExternalDisplayType == MediaPlayerBase.EXTERNAL_WIFI_DISPLAY)
//				message = R.string.restart_after_off_wifi_display;

            if(mCastSession != null && mCastSession.isConnected()) {
                mCastSession.getRemoteMediaClient().pause();
            }
            else {
                mVideoView.pause();
            }

            mAlertDialog = new KollusAlertDialog(mContext).
            	setTitle(R.string.error_title).
            	setMessage(R.string.restart_after_not_allowed).
            	setPositiveButton(R.string.VideoView_error_button,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            /* If we get here, there is no onError listener, so
                             * at least inform them that the video is over.
                             */
                        	onCompletion();
                        }
                    }).
            setCancelable(false).
            show();
			
			detected = true;
		}
    	
    	return detected;
    }
    
    private boolean checkAutoTime() {
//    	boolean auto = false;
//    	try {
//			auto = (Settings.System.getInt(mContext.getContentResolver(), Settings.System.AUTO_TIME) == 1);
//			if(!auto) {
//				new KollusAlertDialog(mContext)
//	            .setTitle(R.string.error_title)
//	            .setMessage(R.string.notify_auto_time)
//	            .setPositiveButton(R.string.VideoView_error_button,
//	                    new DialogInterface.OnClickListener() {
//	                        public void onClick(DialogInterface dialog, int whichButton) {
//	                            /* If we get here, there is no onError listener, so
//	                             * at least inform them that the video is over.
//	                             */
//	                        	onCompletion(null);
//	                        }
//	                    })
//	            .setCancelable(false)
//	            .show();
//			}
//		} catch (SettingNotFoundException e) {
//			// TODO Auto-generated catch block
//		}
//    	
//    	return auto;
    	return true;
    }
    
    @Override
	public void onExternalDisplayDetect(int type, boolean plugged) {
		// TODO Auto-generated method stub
        Log.d(TAG, String.format("onExternalDisplayDetect type %d plugged %b", type, plugged));
        mExternalDisplayType = type;
        mExternalDisplayPlugged = plugged;

        finishByDetectExternalDisplay();
	}
    
    // Below are notifications from VideoView
    @Override
    public boolean onError(MediaPlayer player, int framework_err, int impl_err) {
        mHandler.removeCallbacksAndMessages(null);
        // VideoView will show an error dialog if we return false, so no need
        // to show more message.
        return false;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        mController.showEnded();
        onCompletion();
    }

	@Override
	public void onPrepared(MediaPlayer mp) {
		// TODO Auto-generated method stub
        Log.d(TAG, "onPrepared.");
//		try {
//			TrackInfo[] tracks = mVideoView.getTrackInfo();
//			mSoundOnly = true;
//			if(tracks != null) {
//                for (TrackInfo info : tracks) {
//                    if (info.getTrackType() == TrackInfo.MEDIA_TRACK_TYPE_VIDEO) {
//                        mSoundOnly = false;
//                        break;
//                    }
//                }
//            }
//		} catch(Exception e) {
//			e.printStackTrace();
//		}
        recycleResource();
        initUI();
        mVideoView.setPlayingRate(mPlayingRate);

        if(Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
            DisplayManager dm = (DisplayManager) mContext.getSystemService(Context.DISPLAY_SERVICE);
//            DisplayManager's getDisplays parts
//            if (category == null) {
//                addAllDisplaysLocked(mTempDisplays, displayIds);
//            } else if (category.equals(DISPLAY_CATEGORY_PRESENTATION)) {
//                addPresentationDisplaysLocked(mTempDisplays, displayIds, Display.TYPE_WIFI);
//                addPresentationDisplaysLocked(mTempDisplays, displayIds, Display.TYPE_HDMI);
//                addPresentationDisplaysLocked(mTempDisplays, displayIds, Display.TYPE_OVERLAY);
//                addPresentationDisplaysLocked(mTempDisplays, displayIds, Display.TYPE_VIRTUAL);
//            }
            Display[] displays = dm.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION);
            int externalDisplayCount = 0;
            Log.d(TAG, "display connected count "+displays.length);
            for(int i=0; i<displays.length; i++) {
                if(displays[i].getDisplayId() != Display.DEFAULT_DISPLAY)
                    externalDisplayCount++;
                Log.d(TAG, i+"th display ==> "+displays[i]);
                Log.d(TAG, i+"th display name ==> "+displays[i].getName());
            }

            if(externalDisplayCount > 0) {
                mExternalDisplayType = MediaPlayerBase.EXTERNAL_WIFI_DISPLAY;
                mExternalDisplayPlugged = true;
                Log.d(TAG, "External Display Connected");
            }
        }

        if(finishByDetectExternalDisplay()) {
            return;
        }

        if(mBluetoothConnect)
            mVideoView.setAudioDelay(mAudioDelayMs);

        int playAt = mVideoView.getPlayAt();
        if (playAt > 0) {
            SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(mContext);
            Resources resources = mContext.getResources();
            boolean resumePlaying = preference.getBoolean(resources.getString(R.string.preference_resume_play_key),
                    resources.getBoolean(R.bool.default_force_nscreen));

            if(!mKollusContent.getForceNScreen() && !resumePlaying && !mPopupModeChanged) {
                showResumeDialog(mContext, playAt);
            }
            else {
                mVideoView.seekTo(playAt);
                if(mIsAppForground)
                    startVideo();
            }
        } else {
            if(mIsAppForground)
                startVideo();
        }

        this.setOrientation(mContext.getResources().getConfiguration().orientation);
        mHandler.post(mProgressChecker);
	}

	@Override
	public void onTimedTextDetect(MediaPlayer mp, int trackIndex) {
		// TODO Auto-generated method stub
		mVideoView.selectTrack(trackIndex);
	}

	@Override
	public void onTimedText(MediaPlayer mp, String text) {
		// TODO Auto-generated method stub
		if(text != null) {
		    mCaptionStringView.setText(Html.fromHtml(text));
        }
        else {
            mCaptionStringView.setText("");
        }
	}

	@Override
	public void onTimedImage(MediaPlayer mp, byte[] image, int width, int height) {
		// TODO Auto-generated method stub
		Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		int idx=0, color;
		for(int j=0; j<height; j++) {
			for(int i=0; i<width; i++) {
				color = ((image[idx++] << 24)&0xff000000);
				color |= ((image[idx++] << 16)&0x00ff0000);
				color |= ((image[idx++] << 8)&0x0000ff00);
				color |= ((image[idx++] << 0)&0x000000ff);
				bitmap.setPixel(i, j, color);
			}
		}
//		Log.d(TAG, String.format("onDrawCaption width %d height %d data len %d", width, height, idx));
		mCaptionImageView.setImageBitmap(bitmap);
	}
	
	private KollusPlayerBookmarkListener mKollusPlayerBookmarkListener = 
			new KollusPlayerBookmarkListener() {

				@Override
				public void onBookmark(List<KollusBookmark> bookmark, boolean bWritable) {
					// TODO Auto-generated method stub
                    mBookmarkList = new ArrayList<KollusBookmark>();
                    mBookmarkList.addAll(bookmark);
                    mBookmarkAdapter.setArrayList(mBookmarkList, bWritable);
                    mController.setBookmarkLableList(mBookmarkAdapter.getBookmarkLableList());
                    mController.setBookmarkList(mBookmarkList);

                    if (!mKollusContent.isIntro())
                        mController.setBookmarkable(true);
                    mController.setBookmarkWritable(bWritable);
                    onBookmarkKind(0);
                    mBookmarkAdapter.notifyDataSetChanged();
                    Log.d(TAG, "onBookmarkInfoDetected " + mBookmarkList.size());
				}

				@Override
				public void onGetBookmarkError(int nErrorCode) {
					// TODO Auto-generated method stub
					Log.d(TAG, "onGetBookmarkError "+nErrorCode);
				}

				@Override
				public void onBookmarkDeleted(int position, boolean bDeleted) {
					// TODO Auto-generated method stub
                    Log.d(TAG, "onBookmarkDeleted position --> "+position+", bDeleted --> "+bDeleted);
				}

				@Override
				public void onBookmarkUpdated(int position, boolean bUpdated) {
					// TODO Auto-generated method stub
                    Log.d(TAG, "onBookmarkUpdated position --> "+position+", bUpdated --> "+bUpdated);
				}
		
	};
	
	private KollusPlayerLMSListener mKollusPlayerLMSListener = 
			new KollusPlayerLMSListener() {

				@Override
				public void onLMS(String request, String response) {
					// TODO Auto-generated method stub
					Log.i(TAG, String.format("onLMS request '%s' response '%s'", request, response));
				}
		
	};
	
	private MultiKollusPlayerDRMListener mKollusPlayerDRMListener = new MultiKollusPlayerDRMListener() {

		@Override
		public void onDRM(KollusStorage storage, String request, String response) {
			// TODO Auto-generated method stub
			Log.i(TAG, String.format("onDRM request '%s' response '%s'", request, response));
		}
		
		@Override
		public void onDRMInfo(KollusStorage storage, KollusContent content, int nInfoCode) {
			Log.i(TAG, String.format("onDRMInfo index %d nInfoCode %d message %s", content.getUriIndex(), nInfoCode, content.getServiceProviderMessage()));
		}
		
	};

    private KollusPlayerCallbackListener mKollusPlayerCallbackListener = new KollusPlayerCallbackListener() {

        @Override
        public void onCallbackMessage(String request, String response) {
            // TODO Auto-generated method stub
            Log.i(TAG, String.format("onPlaycallback request '%s' response '%s'", request, response));
        }

    };

    private KollusPlayerThumbnailListener mKollusPlayerThumbnailListener =
            new KollusPlayerThumbnailListener() {
                @Override
                public void onCached(int index, int nErrorCode, String thumbPath) {
                    Log.d(TAG, String.format("onCachedThumbnail index %d nErrorCode %d path '%s'", index, nErrorCode, thumbPath));
                    synchronized (mLocker) {
                        mReceivedThumbnail = true;
                        if(mReceivedMediaInfo) {
                            if (mKollusContent != null && thumbPath.equals(mKollusContent.getScreenShotPath())) {
                                initScreenShot();
                                mBookmarkAdapter.setThumbnailInfo(mScreenShot, mKollusContent.isAudioFile() ? mSoundBitmap : mDefaultThumbnail,
                                        mMultiKollusContent.getKollusContent().getDuration(),
                                        mScreenShotWidth, mScreenShotHeight, mScreenShotCount, mScreenShotInterval);
                            }
                        }
                    }
                }
            };
	
	public TrackInfo[] getTrackInfo() {
		return mVideoView.getTrackInfo();
	}

    public void onCompletion() {
    }

    // Below are notifications from ControllerOverlay
    @Override
    public void onPlayPause() {
    	if (mVideoView.isPlaying()) {
            pauseVideo();
        } else {
            playVideo();
        }
    }

    @Override
    public void onRew() {
        int time = mVideoView.getCurrentPosition()-mSeekInterval;
        if(time < 0)
            time = 0;
        seekVideo(time);
    }

    @Override
    public void onFf() {
        int time = mVideoView.getCurrentPosition()+mSeekInterval;
        if(time > mVideoView.getDuration())
            time = mVideoView.getDuration();
        seekVideo(time);
    }

    private Bitmap getScreenShotBitmap(int time) {
        if(mKollusContent == null)
            return null;

    	Bitmap bm = null;
        int playSectionStart = 0;
        if(mKollusContent.getPlaySectionEnd() > 0) {
            playSectionStart = mKollusContent.getPlaySectionStart();
        }

        try {
            int x=0, y=0, row=0, column=0;
            if(mScreenShot != null) {
                int index = Math.round((time+playSectionStart)/1000/mScreenShotInterval)+3;
                if(index >= mScreenShotCount)
                    index = mScreenShotCount-1;
//	        Log.d(TAG, String.format("getScreenShotBitmap index %d time %d", index, time));
                column = index%10;
	    		row = index/10;
	    		x = mScreenShotWidth*column;
	    		y = mScreenShotHeight*row;
	    		Rect rect = new Rect(x, y, x+mScreenShotWidth, y+mScreenShotHeight);
	    		bm = mScreenShot.decodeRegion(rect, mScreenShotOption);
	    	}
	    	else if(/*mSoundOnly*/mKollusContent.isAudioFile())
	    		bm = mSoundBitmap;
    	} catch(Exception e) {
    		e.printStackTrace();
    	}

        if(mCurrentThumbnail != null && mCurrentThumbnail != mSoundBitmap && mCurrentThumbnail != mDefaultThumbnail) {
            mCurrentThumbnail.recycle();
        }

        mCurrentThumbnail = bm;
        return bm;
    }

    @Override
    public void onSeekStart() {
        if(mSeekLocked)
            return;

//        mDragging = true;
        if(mCastSession != null && mCastSession.isConnected()) {
            mCastSession.getRemoteMediaClient().pause();
        }
        else {
            mVideoView.pause();
        }
    }

    @Override
    public void onSeekMove(int time) {
        if(mSeekLocked)
            return;

        if(mSeekableEnd >= 0 && time > mSeekableEnd)
            time = mSeekableEnd;

        if(mScreenShot != null) {
            mController.setScreenShot(getScreenShotBitmap(time), time, mVideoView.getDuration());
            mController.showSeekingTime(time, time - mVideoView.getCurrentPosition(), mVideoView.getDuration());
        }

        mController.setTimes(time, mSeekableEnd, mVideoView.getDuration(), 0, 0);
    }

    @Override
    public void onSeekEnd(int time, int start, int end) {
	    if(mKollusContent.isLive() && mKollusContent.getSeekable())
	        time += mVideoView.getDuration();

	    seekVideo(time);
    }

    @Override
    public void onVRCardView(boolean enable) {
        mVideoView.onVRCardView(enable);
    }

    @Override
    public void onScreenCapture() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        String extDir = android.os.Environment.getExternalStorageDirectory() + "/Pictures";
        String saveFileName = String.format("%s/ScreenShot_%s.png", extDir, df.format(new Date()));

        if(mVideoView.getSurfaceView() instanceof TextureView) {
            DisplayUtil.saveScreenShot(((TextureView)mVideoView.getSurfaceView()).getBitmap(), saveFileName);
        }
        else {
            Window window = ((Activity) mContext).getWindow();
            //        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
            mController.hide();
            DisplayUtil.saveScreenShot(mVideoView.getSurfaceView(), 1280, 720, saveFileName);
            mController.show();
        }
        Toast.makeText(mContext,saveFileName+" Captured.", Toast.LENGTH_LONG).show();
//        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE);
}
    
    @Override
    public void onBookmarkAdd() {
    	int time = mVideoView.getCurrentPosition();
    	int myMarking = 0;
    	
//    	if(mScreenShot == null)
//    		return;
    	
    	for(KollusBookmark iter : mBookmarkList) {
    		if(iter.getLevel() == KollusBookmark.USER_LEVEL)
    			myMarking++;
    	}
    	
    	if(myMarking >= KollusBookmark.MAX_BOOKMARK) {
    		Resources resources = mContext.getResources();
    		String msg = String.format(resources.getString(R.string.ERROR_MAX_BOOKMARK), KollusBookmark.MAX_BOOKMARK);
    		showMessage(msg);
    		return;
    	}    	
    	
    	try {
    		int index = mBookmarkAdapter.add(time);
	    	mController.setBookmarkSelected(index);
	    	mController.setBookmarkCount(mBookmarkAdapter.getBookmarkKind(), mBookmarkAdapter.getBookmarkCount());
	    	
	    	mVideoView.updateKollusBookmark(time, "");
    	} catch(IllegalStateException e) {
    		e.printStackTrace();
    	}
    }
    
    @Override
    public void onBookmarkRemoveView() {
    	mBookmarkAdapter.updateBookmarkRemoveView();
    }

	@Override
	public void onBookmarkRemove(int index) {
		// TODO Auto-generated method stub
		try {
			int time = mBookmarkAdapter.getBookmarkTime(index);
			mBookmarkAdapter.remove(index);
			mController.setBookmarkCount(mBookmarkAdapter.getBookmarkKind(), mBookmarkAdapter.getBookmarkCount());
	    	if(mBookmarkAdapter.getBookmarkCount() == 0) {
	    		mController.setDeleteButtonEnable(false);
	    	}
	    	
			mVideoView.deleteKollusBookmark(time);
		} catch(IllegalStateException e) {
    		e.printStackTrace();
    	}
	}
  
    @Override
    public void onBookmarkSeek(int index) {
    	int time = mBookmarkAdapter.getBookmarkTime(index);
    	if(time >= 0) {
    	    if(Log.isDebug())
                mVideoView.updateKollusBookmark(time, String.format("#%d - %d;", (time/1000), (System.currentTimeMillis()/1000)%10));
    		resetRepeatAB(time);
    		mVideoView.seekTo(time);
    	}
    	Log.d(TAG, String.format("onBookmarkSeek index %d time %d", index, time));
    }
    
    @Override
    public void onBookmarkKind(int kind) {
    	mBookmarkAdapter.setBookmarkKind(kind);
    	mController.setBookmarkCount(kind, mBookmarkAdapter.getBookmarkCount());
    }
    
    @Override
    public void onCaptionSelected(int position) {
    	try {
    		mCaptionImageView.setVisibility(View.VISIBLE);
    		mCaptionStringView.setVisibility(View.VISIBLE);
    		
	    	SubtitleInfo subtitle = mSubtitles.get(position);
			mCaptionUri = Uri.parse(subtitle.url);
			if(subtitle.url != null/*&& subtitle.url.startsWith("http://")*/) {
				mVideoView.addTimedTextSource(mCaptionUri);
			}
	    }
    	catch(Exception e) {
    		
    	}
    }
    
    @Override
    public void onCaptionHide() {
		mCaptionImageView.setVisibility(View.GONE);
		mCaptionStringView.setVisibility(View.GONE);
    }
    
    @Override
    public void onShown() {
        mShowing = true;
        setProgress();

    	RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)mCaptionStringView.getLayoutParams();
    	params.bottomMargin = mController.getProgressbarHeight()+5;
    	mCaptionStringView.setLayoutParams(params);
//        showSystemUi(true);
    }

    @Override
    public void onHidden() {
        mShowing = false;
//        showSystemUi(false);
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)mCaptionStringView.getLayoutParams();
    	params.bottomMargin = 5;
    	mCaptionStringView.setLayoutParams(params);
    }

    @Override
    public void onReplay() {
        startVideo();
    }

    @Override
    public boolean isPlaying() {
        return mVideoView.isPlaying();
    }

    @Override
	public void onPlayingRate(@ValuePreference.PLAYING_RATE_MODE int mode) {
		// TODO Auto-generated method stub
		float playing_rate = mPlayingRate;
        float rate_unit = 0.1f;
        if(mode == ValuePreference.PLAYING_RATE_UP && mPlayingRate >= 2)
            rate_unit = 0.5f;
        else if(mode == ValuePreference.PLAYING_RATE_DOWN && mPlayingRate > 2)
            rate_unit = 0.5f;
		switch(mode) {
		case ValuePreference.PLAYING_RATE_DOWN:
			playing_rate -= rate_unit;
			break;
		case ValuePreference.PLAYING_RATE_UP:
			playing_rate += rate_unit;
			break;
		default:
			playing_rate = 1.0f;
			break;
		}

        playing_rate = Math.round(playing_rate*10f)/10f;
		float minPlaybackRate = KollusConstants.MIN_PLAYING_RATE;
		float maxPlaybackRate = KollusConstants.MAX_PLAYING_RATE;
		if(Log.isDebug())
            maxPlaybackRate = KollusConstants.MAX_PLAYING_RATE_DEBUG;
        if(playing_rate < minPlaybackRate)
            playing_rate = minPlaybackRate;
        else if(playing_rate > maxPlaybackRate)
            playing_rate = maxPlaybackRate;

		if(mVideoView.setPlayingRate(playing_rate)) {
			mController.setPlayingRateText(playing_rate);
			mPlayingRate = playing_rate;
		}
	}
	
	@Override
	public void onBookmarkHidden() {
		mBookmarkAdapter.hideBookmarkRemoveView();
	}
	
	@Override
	public void onSkip() {
		if(mVideoView != null)
			mVideoView.skip();
	}

    public boolean isInPlaybackState() {
        if(mVideoView != null)
            return mVideoView.isInPlaybackState();

        return false;
    }

    public void toggleMediaControlsVisibility() {
		if(mVideoView != null)
			mVideoView.toggleMediaControlsVisibility();
	}
	
	public boolean isControlsShowing() {
		if(mVideoView != null)
			return mVideoView.isControlsShowing();
		
		return false;
	}
	
	public void screenSizeScaleBegin(ScaleGestureDetector detector) {
        if(mVideoView != null && !mVideoView.isVRMode())
			mVideoView.screenSizeScaleBegin(detector);
	}
	
	public void screenSizeScale(ScaleGestureDetector detector) {
        if(mVideoView != null && !mVideoView.isVRMode())
			mVideoView.screenSizeScale(detector);
	}
	
	public void screenSizeScaleEnd(ScaleGestureDetector detector) {
        if(mVideoView != null && !mVideoView.isVRMode()) {
            mVideoView.screenSizeScaleEnd(detector);
        }
	}
	
	public void toggleVideoSize() {
        if(mVideoView.isVRMode())
            return;

        mController.toggleScreenSizeMode();
	}
	
	public boolean canMoveVideoScreen() {
        if(mVideoView != null && !mVideoView.isVRMode())
			return mVideoView.canMoveVideoScreen();
		
		return false;
	}

    public void moveVideoFrame(float x, float y) {
		if(mVideoView != null && !mVideoView.isVRMode())
			mVideoView.moveVideoFrame(x, y);
	}
	
	public void setOrientation(int orientation) {
        mController.setOrientation(orientation);
	}

	public void setVolumeLabel(int level, int maxLevel) {
        setMute(false);

        mController.setVolumeLabel(level);

		if(mVideoView != null) {
            if(level > maxLevel)
    			mVideoView.setVolumeLevel(level-maxLevel);
            else
                mVideoView.setVolumeLevel(0);
		}
	}

	private void setMute(boolean mute) {
        mVolumeMute = mute;
        mController.setMute(mVolumeMute);
        if(mVideoView != null)
            mVideoView.setMute(mVolumeMute);
    }

    public void hideUi(){
        mController.hide();
    }
	
	public void setBrightnessLabel(int level) {
		mController.setBrightnessLabel(level);
	}
	
	public void setSeekLabel(int maxX, int maxY, int x, int y, int mountMs, boolean bShow) {
        if(mKollusContent == null || (!mKollusContent.getSeekable() && mSeekableEnd < 0) || mKollusContent.isLive())
            return;

        if(mSeekLocked && mKollusContent.getSkipSec()*1000 < mVideoView.getCurrentPosition())
            mSeekLocked = false;

        if(mSeekLocked)
            return;

        if(mVideoView != null) {
			int seekTimeMs = 0;
			if(bShow) {
                if(mCastSession != null && mCastSession.isConnected()) {
                    mCastSession.getRemoteMediaClient().pause();
                }
                else {
                    mVideoView.pause();
                }
            }
			if(mSeekScreenShotTimeMs < 0)
				mSeekScreenShotTimeMs = mVideoView.getCurrentPosition();
			seekTimeMs = mSeekScreenShotTimeMs+mountMs;

            if(mSeekableEnd >= 0 && seekTimeMs > mSeekableEnd)
                seekTimeMs = mSeekableEnd;
			else if(seekTimeMs < 0)
				seekTimeMs = 0;
            else if(seekTimeMs > mVideoView.getDuration())
                seekTimeMs = mVideoView.getDuration();

            if(!bShow) {
				resetRepeatAB(seekTimeMs);
                if(mCastSession != null && mCastSession.isConnected()) {
                    mCastSession.getRemoteMediaClient().seek(seekTimeMs);
                }
                else {
                    mVideoView.seekTo(seekTimeMs);
                }
				mSeekScreenShotTimeMs = -1;
			}

            mController.showSeekingTime(seekTimeMs, seekTimeMs - mVideoView.getCurrentPosition(), mVideoView.getDuration());
            mController.setSeekLabel(maxX, maxY, x, y, getScreenShotBitmap(seekTimeMs), seekTimeMs, mVideoView.getDuration(), bShow);

            if(!bShow) {
                mRootView.setKeepScreenOn(true);  //etlim fixed. 20170829 Screen On.
                if(mCastSession != null && mCastSession.isConnected()) {
                    mCastSession.getRemoteMediaClient().play();
                }
                else {
                    mVideoView.start();
                }

                if(mShowing) {
                    mController.showPlaying();
                }
                else {
                    mController.hide();
                }
            }
		}
	}

	public boolean isVRMode() {
	    return mVideoView.isVRMode();
    }

    public int getPlayerType() {
        if(mVideoView != null)
            return mVideoView.getPlayerType();

        return Utils.PLAYER_TYPE_NONE;
    }

    protected boolean supportPlaybackrateControl() {
        if(mVideoView != null && mKollusContent != null)
            return mVideoView.supportPlaybackrateControl() && !mKollusContent.getDisablePlayRate();
        return false;
    }

	private void resetRepeatAB(int seekTimeMs) {
		if(mRepeatAMs < 0 || mRepeatBMs < 0)
			return;
	
		if(mRepeatAMs <= seekTimeMs && seekTimeMs <= mRepeatBMs)
			return;
		
		mController.resetRepeatABImage();
	}
	
	// Below are key events passed from MovieActivity.
    private long mPlayToggleTime;
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Some headsets will fire off 7-10 events on a single click
        Log.d(TAG, String.format("onKeyDown keyCode %d repeat count %d", keyCode, event.getRepeatCount()));
        if (event.getRepeatCount() > 0) {
            return isMediaKey(keyCode);
        }

        switch (keyCode) {
            case KeyEvent.KEYCODE_HEADSETHOOK:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_SPACE:
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
            case KeyEvent.KEYCODE_MEDIA_PLAY: {
                long receiveTime = System.currentTimeMillis();
                if((receiveTime - mPlayToggleTime) > 500) {
                    if (mVideoView.isPlaying()) {
                        pauseVideo();
                    } else {
                        playVideo();
                    }
                }
                mPlayToggleTime = receiveTime;
            }
                return true;
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                onRew();
                return true;
            case KeyEvent.KEYCODE_MEDIA_NEXT:
                onFf();
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                onRew();
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                onFf();
                return true;
            case KeyEvent.KEYCODE_M:
                onToggleMute();
                return true;
            case KeyEvent.KEYCODE_Z:
                onPlayingRate(ValuePreference.PLAYING_RATE_1);
                return true;
            case KeyEvent.KEYCODE_X:
                onPlayingRate(ValuePreference.PLAYING_RATE_DOWN);
                return true;
            case KeyEvent.KEYCODE_C:
                onPlayingRate(ValuePreference.PLAYING_RATE_UP);
                return true;
            case KeyEvent.KEYCODE_LEFT_BRACKET:
                onRepeatAB(ControllerOverlay.REPEAT_MODE_A);
                mController.setRepeatABImage(ControllerOverlay.REPEAT_MODE_A);
                return true;
            case KeyEvent.KEYCODE_RIGHT_BRACKET:
                onRepeatAB(ControllerOverlay.REPEAT_MODE_B);
                mController.setRepeatABImage(ControllerOverlay.REPEAT_MODE_B);
                return true;
            case KeyEvent.KEYCODE_BACKSLASH:
            case KeyEvent.KEYCODE_P:
                onRepeatAB(ControllerOverlay.REPEAT_MODE_DISABLE);
                mController.setRepeatABImage(ControllerOverlay.REPEAT_MODE_DISABLE);
                return true;
        }
        return false;
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return isMediaKey(keyCode);
    }

    public void setBluetoothConnectChanged(boolean connect) {
        mBluetoothConnect = connect;
        mController.setBluetoothConnectChanged(mBluetoothConnect);
        if(mVideoView != null) {
            if(mBluetoothConnect)
                mVideoView.setAudioDelay(mAudioDelayMs);
            else
                mVideoView.setAudioDelay(0);
        }
    }

    private static boolean isMediaKey(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_HEADSETHOOK
                || keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS
                || keyCode == KeyEvent.KEYCODE_MEDIA_NEXT
                || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY
                || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE;
    }

    private void initUI() {
        synchronized (mLocker) {
            mController.hide();
            if (mVideoView.getKollusContent(mKollusContent)) {
                mReceivedMediaInfo = true;
                Log.i(TAG, "ContentInfo ==> " + mKollusContent);
                SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(mContext);
                Resources resources = mContext.getResources();
                boolean caption_bg = preference.getBoolean(resources.getString(R.string.preference_caption_bg_color_key),
                        false);
                if ("bg".equalsIgnoreCase(mKollusContent.getCaptionStyle()))
                    caption_bg = true;
                if (caption_bg)
                    mCaptionStringView.setBackgroundColor(resources.getColor(R.color.default_caption_bg_color));
                else
                    mCaptionStringView.setBackgroundColor(Color.TRANSPARENT);
                String cource = mKollusContent.getCourse();
                String subcource = mKollusContent.getSubCourse();
                String title;
                if (cource != null && cource.length() > 0) {
                    if (subcource != null && subcource.length() > 0)
                        title = cource + "(" + subcource + ")";
                    else
                        title = cource;
                } else
                    title = subcource;
                mController.setTitleText(title);

                mVolumeMute = mKollusContent.getMute();
                if (!mKollusContent.getSeekable())
                    mSeekableEnd = mKollusContent.getSeekableEnd();

                mController.setMute(mVolumeMute);
                mController.setSkinManager(new SkinManager(mKollusContent.getSkinString()));
                if (mKollusContent.hasAudioWaterMark()) {
                    mController.showWaterMark();
                } else {
                    mController.hideWaterMark();
                }

                if (mKollusContent.isIntro() && mKollusContent.getSkipSec() >= 0) {
                    mController.showSkip(mKollusContent.getSkipSec());
                    mSeekLocked = false;
                } else {
                    mController.hideSkip();
                }

                try {
                    mSubtitles = mKollusContent.getSubtitleInfo();
                    if (mSubtitles.size() > 0) {
                        SubtitleInfo subtitle = mSubtitles.get(0);
                        mCaptionUri = Uri.parse(subtitle.url);
                        if (subtitle.url != null && subtitle.url.startsWith("http://"))
                            mVideoView.addTimedTextSource(mCaptionUri);
                    }

                    mController.setCaptionList(mSubtitles);
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    mController.setCaptionList(null);
                    e.printStackTrace();
                }

                mController.setBookmarkable(false);
                mController.setSeekable(mKollusContent.getSeekable() || mSeekableEnd >= 0);
                mController.setScreenShotEnabled(mScreenShot != null);
//                mPlayingRate = 1.0f;
                mController.setPlayingRateText(mPlayingRate);
                mController.resetRepeatABImage();
                mController.setLive(mKollusContent.isLive(), mKollusContent.isLive() && mKollusContent.getSeekable());
                mController.hide();
                mCaptionStringView.setText("");

                if (/*mSoundOnly*/mKollusContent.isAudioFile()) {
                    String thumb = mKollusContent.getThumbnailPath();
                    if (thumb != null && !thumb.startsWith("http")) {
                        mSoundBitmap = BitmapFactory.decodeFile(thumb);
                        mSoundOnlyImageView.setImageBitmap(mSoundBitmap);//setImageURI(Uri.parse(thumb));
                    } else {
                        mSoundBitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.sound_only);
                    }

                    mSoundOnlyImageView.setVisibility(View.VISIBLE);
                } else {
                    mSoundOnlyImageView.setVisibility(View.GONE);
                }

                Log.d(TAG, String.format("isThumbnailEnable %b isAudioFile %b isThumbnailDownloadSync %b ScreenShotPath '%s'",
                        mKollusContent.isThumbnailEnable(),
                        mKollusContent.isAudioFile(),
                        mKollusContent.isThumbnailDownloadSync(),
                        mKollusContent.getScreenShotPath()));
                if (mKollusContent.isThumbnailEnable()) {
                    if ((mKollusContent.isThumbnailDownloadSync() ||
                            mReceivedThumbnail ||
                            mKollusContent.getContentType() == KollusContent.EXT_DRM_CONTENT) &&
                        mScreenShot == null) {
                        initScreenShot();
                        mBookmarkAdapter.setThumbnailInfo(mScreenShot, /*mSoundOnly*/mKollusContent.isAudioFile() ? mSoundBitmap : mDefaultThumbnail,
                                mMultiKollusContent.getKollusContent().getDuration(),
                                mScreenShotWidth, mScreenShotHeight, mScreenShotCount, mScreenShotInterval);
                    }
                } else {
                    makeDefaultThumbnail(192, 108);
                    mBookmarkAdapter.setThumbnailInfo(null, /*mSoundOnly*/mKollusContent.isAudioFile() ? mSoundBitmap : mDefaultThumbnail,
                            mMultiKollusContent.getKollusContent().getDuration(),
                            0, 0, 0, 0);
                }

                if (!mKollusContent.getVideoWaterMarkCode().isEmpty()) {
                    Log.d(TAG, String.format("VideoWaterMark code '%s' size %d color #%X alpha %d",
                            mKollusContent.getVideoWaterMarkCode(),
                            mKollusContent.getVideoWaterMarkFontSize(),
                            mKollusContent.getVideoWaterMarkFontColor(),
                            mKollusContent.getVideoWaterMarkAlpha()));
                    TextView watermark = (TextView) mRootView.findViewById(R.id.video_water_mark_view);
                    watermark.setText(mKollusContent.getVideoWaterMarkCode());
                    watermark.setTextSize(mKollusContent.getVideoWaterMarkFontSize());
                    watermark.setTextColor(mKollusContent.getVideoWaterMarkFontColor() | 0xFF000000);
                    watermark.setAlpha(mKollusContent.getVideoWaterMarkAlpha() / 255.0f);
                    mVideoView.setVideoWaterMark(watermark);

                    mHandler.post(mVideoWaterMarkRunnable);
                } else {
                    mHandler.removeCallbacks(mVideoWaterMarkRunnable);
                    mVideoView.hideVideoWaterMark();
                }
            }

            if (mKollusContent.getChattingInfo() != null) {
                mChattingView.setVisibility(View.VISIBLE);
                mKeyboardLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        Rect rect = new Rect();
                        mRootView.getWindowVisibleDisplayFrame(rect);
                        int viewHeight = rect.bottom - rect.top;

                        float viewHeightRate = (float) viewHeight / mRootView.getRootView().getHeight();
                        int viewBottomMargin = mRootView.getRootView().getHeight() - viewHeight;
                        mChattingView.onVisibleHeightChanged(viewBottomMargin);
                    }
                };

                mChattingView.getViewTreeObserver().addOnGlobalLayoutListener(mKeyboardLayoutListener);
                mChattingView.setListener(mKollusContent.getChattingInfo(), new ChattingWebViewClient.Listener() {

                    @Override
                    public void onReady() {
                        mChattingView.onInit();
                    }

                    @Override
                    public void onBypassTap() {
                        mController.show();
                    }
                });
                mController.setChattingView(mChattingView);
            } else {
                mChattingView.setVisibility(View.GONE);
            }
            mController.setAvailableMediaRoute(false);

            //        if(Log.isDebug()) {
            //            LinearLayout layout = (LinearLayout)mRootView.findViewById(R.id.sub_surface_view_layer);
            //            LayoutInflater inflater = LayoutInflater.from(mContext);
            //
            //            String role = "left";
            //            View subScreenLayer = inflater.inflate(R.layout.multi_screen, mRootView, false);
            //            View chgScn = subScreenLayer.findViewById(R.id.change_screen);
            //            layout.addView(subScreenLayer, new ViewGroup.LayoutParams(300, 200));
            //            int index = mVideoView.addSubDataSource(role, "http://util.dev.kollus.com/auth_test/Helicopter_NoAudio_1.mkv", subScreenLayer.findViewById(R.id.sub_screen));
            //            if(index >= 0) {
            //                chgScn.setTag(role);
            //                chgScn.setOnClickListener(new View.OnClickListener() {
            //                    @Override
            //                    public void onClick(View view) {
            //                        try {
            //                            String role1 = (String) view.getTag();
            //                            String role2 = mVideoView.swapDisplay((String) view.getTag());
            //                            view.setTag(role2);
            //                            Log.d(TAG, String.format("swapDisplay : changeRole '%s'-> '%s'", role1, role2));
            //                        } catch (Exception e) {
            //                            e.printStackTrace();
            //                        }
            //                    }
            //                });
            //            }
            //
            //            role = "right";
            //            subScreenLayer = inflater.inflate(R.layout.multi_screen, mRootView, false);
            //            chgScn = subScreenLayer.findViewById(R.id.change_screen);
            //            layout.addView(subScreenLayer, new ViewGroup.LayoutParams(300, 200));
            //            index = mVideoView.addSubDataSource(role, "http://util.dev.kollus.com/auth_test/Helicopter_NoAudio_2.mkv", subScreenLayer.findViewById(R.id.sub_screen));
            //            if(index >= 0) {
            //                chgScn.setTag(role);
            //                chgScn.setOnClickListener(new View.OnClickListener() {
            //                    @Override
            //                    public void onClick(View view) {
            //                        try {
            //                            String role1 = (String) view.getTag();
            //                            String role2 = mVideoView.swapDisplay((String) view.getTag());
            //                            view.setTag(role2);
            //                            Log.d(TAG, String.format("swapDisplay : changeRole '%s'-> '%s'", role1, role2));
            //                        } catch (Exception e) {
            //                            e.printStackTrace();
            //                        }
            //                    }
            //                });
            //            }
            //        }
            if (KollusConstants.SUPPORT_REMOTE_MEDIA_ROUTE &&
                    mCastContext != null &&
                    mKollusContent.getContentType() == KollusContent.EXT_DRM_CONTENT &&
                    mCastContext.getCastState() != CastState.NO_DEVICES_AVAILABLE) {
                mController.setAvailableMediaRoute(true);
            }
        }
    }

    private void makeDefaultThumbnail(int width, int height) {
        if(mDefaultThumbnail == null) {
            mDefaultThumbnail = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            mDefaultThumbnail.eraseColor(0xff999999);
        }
    }

    private void initScreenShot() {
        if(mScreenShot != null)
            return;

        String screenShotPath = mKollusContent.getScreenShotPath();
        int end = screenShotPath.lastIndexOf('.');
        int start = screenShotPath.lastIndexOf('.', end-1);

        if(start < end) {
            String info = screenShotPath.substring(start+1, end);
            Scanner sc = new Scanner(info);
            sc.useDelimiter("x");
            mScreenShotWidth = sc.nextInt();
            mScreenShotHeight = sc.nextInt();
            mScreenShotCount = sc.nextInt();
            mScreenShotInterval = (float) (mKollusContent.getDuration()/mScreenShotCount/1000.);
            sc.close();

            try {
                mScreenShot = BitmapRegionDecoder.newInstance(screenShotPath, false);
                if(mScreenShot != null) {
                    Log.d(TAG, String.format("ScreenShot width %d height %d",
                            mScreenShot.getWidth(), mScreenShot.getHeight()));
                    mScreenShotOption = new BitmapFactory.Options();
                }
                else
                    Log.e(TAG, "ScreenShot null");
            } catch (Exception e) {
                Log.e(TAG, "ScreenShot Exception");
                e.printStackTrace();
            } catch (OutOfMemoryError e) {
                Log.e(TAG, "ScreenShot OutOfMemoryError");
                e.printStackTrace();
            }

            makeDefaultThumbnail(mScreenShotWidth, mScreenShotHeight);
            Log.d(TAG, String.format("ScreenShot w %d h %d count %d duration %d",
                    mScreenShotWidth, mScreenShotHeight, mScreenShotCount, mKollusContent.getDuration()));
        }
        else {
            mScreenShotWidth = 192;
            mScreenShotHeight = 108;
        }
        makeDefaultThumbnail(mScreenShotWidth, mScreenShotHeight);
    }

    private void lockScreenOrientation() {
        final int orientation = mContext.getResources().getConfiguration().orientation;
        final int rotation = mContext.getWindowManager().getDefaultDisplay().getOrientation();

        if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_90) {
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                mContext.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
            else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mContext.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
        }
        else if (rotation == Surface.ROTATION_180 || rotation == Surface.ROTATION_270) {
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                mContext.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
            }
            else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mContext.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
            }
        }
	}

	private void unlockScreenOrientation() {
        mContext.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
	}
	
	@Override
	public void onScreenRotateLock(boolean lock) {
		// TODO Auto-generated method stub
		if(lock)
			lockScreenOrientation();
		else
			unlockScreenOrientation();
        mScreenLocked = lock;
	}

	@Override
	public void onScreenSizeMode(int mode) {
		// TODO Auto-generated method stub
		if(mVideoView != null)
			mVideoView.toggleVideoSize(mode);
		
		Resources res = mContext.getResources();
		if(mode == KollusPlayerContentMode.ScaleAspectFill)
			showMessage(res.getString(R.string.FULL_SCREEN));
        else if(mode == KollusPlayerContentMode.ScaleAspectFillStretch)
            showMessage(res.getString(R.string.FULL_SCREEN_STRETCH));
		else if(mode == KollusPlayerContentMode.ScaleAspectFit)
			showMessage(res.getString(R.string.FIT_SCREEN));
		else
			showMessage(res.getString(R.string.REAL_SIZE_SCREEN));
	}

	@Override
	public void onRepeatAB(int direction) {
		// TODO Auto-generated method stub
		if(direction == ControllerOverlay.REPEAT_MODE_A) {
			mRepeatAMs = mVideoView.getCurrentPosition();
		}
		else if(direction == ControllerOverlay.REPEAT_MODE_B) {
			int timeMs = mVideoView.getCurrentPosition();
			if(mRepeatAMs < timeMs)
				mRepeatBMs = timeMs;
			else {
				int tempMs = mRepeatAMs;
				mRepeatAMs = timeMs;
				mRepeatBMs = tempMs;
			}
		}
		else if(direction == ControllerOverlay.REPEAT_MODE_DISABLE) {
			mRepeatAMs = mRepeatBMs = -1;
		}
		
		if(mRepeatAMs >= 0 && mRepeatAMs < mRepeatBMs) {
			mVideoView.seekToExact(mRepeatAMs);
		}
		
		mController.setRepeatAB(mRepeatAMs, mRepeatBMs);
	}

	@Override
	public void onRepeat(boolean enable) {
		// TODO Auto-generated method stub
		if(mVideoView != null)
			mVideoView.setLooping(enable);
	}

	@Override
    public void onAudioDelay(int timeMs) {
        mAudioDelayMs = timeMs;
        if(mVideoView != null)
            mVideoView.setAudioDelay(mAudioDelayMs);

        Resources resources = mContext.getResources();
        SharedPreferences.Editor preference = PreferenceManager.getDefaultSharedPreferences(mContext).edit();
        preference.putInt(resources.getString(R.string.preference_audio_delay_key), mAudioDelayMs);
        preference.commit();
    }

    @Override
    public void onTimeShiftOff() {
        if(mVideoView != null)
            mVideoView.seekTo(mVideoView.getDuration());
    }

	@Override
	public void onToggleMute() {
		// TODO Auto-generated method stub
        setMute(!mVolumeMute);
	}

    @Override
    public void onLayoutChange(View v, int left, int top, int right, int bottom) {
        if(mVideoView != null)
            mVideoView.adjustVideoWaterMarkPosition(left, top, right, bottom);
    }

    @Override
    public void onSelectedBandwidth(int bandwidth) {
        if(mVideoView != null)
            mVideoView.setBandwidth(bandwidth);
    }

    @Override
    public void onChatVisibleChanged(boolean visible) {
        mChattingView.onChatVisibleChanged(visible);
    }

    private void showMessage(String msg) {
		if(mToast != null)
			mToast.cancel();
		Log.d(TAG, "showMessage : "+msg);
		mToast = Toast.makeText(mContext, msg, Toast.LENGTH_SHORT);
		mToast.show();
	}
}


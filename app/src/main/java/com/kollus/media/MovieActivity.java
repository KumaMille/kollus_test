/*
 * Copyright (C) 2007 The Android Open Source Project
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

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.session.MediaSession;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.Settings.SettingNotFoundException;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityManager;
import android.widget.Toast;

import com.kollus.media.contents.MultiKollusContent;
import com.kollus.media.preference.KollusConstants;
import com.kollus.media.preference.ValuePreference;
import com.kollus.media.util.ActivityStack;
import com.kollus.media.view.KollusAlertDialog;
import com.kollus.sdk.media.content.KollusContent;
import com.kollus.sdk.media.util.ErrorCodes;
import com.kollus.sdk.media.util.KollusUri;
import com.kollus.sdk.media.util.Log;
import com.kollus.sdk.media.util.Utils;

import java.net.URLDecoder;
import java.util.ArrayList;

/**
 * This activity plays a video from a specified URI.
 *
 * The client of this activity can pass a logo bitmap in the intent (KEY_LOGO_BITMAP)
 * to set the action bar logo so the playback process looks more seamlessly integrated with
 * the original activity.
 */
public class MovieActivity extends BaseActivity {
    @SuppressWarnings("unused")
    private static final String TAG = "MovieActivity";
    public static final String KEY_LOGO_BITMAP = "logo-bitmap";
    public static final String KEY_TREAT_UP_AS_BACK = "treat-up-as-back";
    private final boolean TEST_MULTI_PLAY = false;

	private int mScrollNoActionTop;
	private int mScrollNoActionBottom;
    private static final int SCROLL_MODE_N = 0;
    private static final int SCROLL_MODE_V = 1;
    private static final int SCROLL_MODE_H = 2;

    private static final int CONTROL_INC = 1;
    private static final int CONTROL_DEC = 2;
    private static final int CHECK_EXIT = 100;

    private static final int BRIGHTNESS_MIN = 15;
    private static final int BRIGHTNESS_MAX = 255;
    private static final int BRIGHTNESS_UNIT = 24;

    private static final int SCROLL_SEEK_MOUNT = 90;

	private static final int SCROLL_H_DELICACY = 45;
	private static final int SCROLL_V_DELICACY = 30;

//	private WindowManager mWindowManager;
//	private LayoutInflater mInflater;
//	private View mRootGroup;

//	private Uri mUri;
    private Intent mIntent;
    private KollusAlertDialog mAlertDialog;

    private AudioManager mAudioManager;
	private int mSystemVolumeLevel;
	private int mSWVolumeLevel;
    private int mVolumeControlDistance;

    private int mScrollMode = SCROLL_MODE_N;
	private int mScrollAmountH = -1;
    private int mOriginSystemBrightnessMode;
    private int mOriginSystemBrightness;
    private int mSystemBrightness;
    private int mBrightnessControlDistance;

    private int mSeekControlDistance;
    private boolean mExit = false;
	private final long EXIT_TIME = 1500;

	private boolean mTalkbackEnabled;

	private ArrayList<String> mUrlList = new ArrayList<String>();
	private ArrayList<MultiKollusContent> mContentList = new ArrayList<MultiKollusContent>();
	private String mReturnUrl;
	private boolean mBundleNull;

	//etlim 20170902 Activity Exit ==> Broadcast Event
    private String ACTION_ACTIVITY_FINISH_MOVIE = "com.kollus.media.action.activity.finish.movie";
	private MovieActivityFinishBroadcastReceiver mMovieActivityFinishBR;

	//etlim fixed. 20170808 navigation bar orientation issue, nexus P6.
//    private final int HANDLER_ORIENTATION_LANDSCAPE = 1100;
//    private final int HANDLER_ORIENTATION_REVERSED_LANDSCAPE = 1101;
//    private SensorManager mSensorManager;
//    private Sensor mRotationSensor;
//    private int mOrientationType;
    private final int HANDLER_ORIENTATION_CHANGED = 1000;
    private SharedPreferences mPreference;
	private Resources mResources;
    private ActivityStack mActivityStack;

	private MediaSession mMediaSession;
	private MediaButtonReceiver mMediaButtonReceiver;

	private AudioManager.OnAudioFocusChangeListener mAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
		@Override
		public void onAudioFocusChange(int focusChange) {
			boolean bAudioFocusLoss = false;
			Log.d(TAG, "onAudioFocusChange "+focusChange);
			switch (focusChange) {
				case AudioManager.AUDIOFOCUS_GAIN:
				case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT:
				case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK:
					bAudioFocusLoss = false;
					break;
				case AudioManager.AUDIOFOCUS_LOSS:
				case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
				case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
					bAudioFocusLoss = true;
					break;
			}

			if(mBounded) {
				try {
					if (bAudioFocusLoss) {
						mMessenger.send(Message.obtain(null, MoviePlayerService.PAUSE));
//						mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
//								AudioManager.ADJUST_MUTE, AudioManager.RINGER_MODE_SILENT);
					}
					else {
                        if(MovieActivity.this.hasWindowFocus()) {
                            mMessenger.send(Message.obtain(null, MoviePlayerService.RESUME));
//                            mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
//                                    AudioManager.ADJUST_UNMUTE, AudioManager.RINGER_MODE_SILENT);
                        }
					}
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		}
	};
	private AudioFocusRequest mAudioFocusRequest;

	public class MediaButtonReceiver extends BroadcastReceiver {
		public MediaButtonReceiver() {
			super();
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(TAG, String.format("BluetoothHeadsetReceiver action %s", intent.getAction()));
			if(intent.getAction().equals(Intent.ACTION_MEDIA_BUTTON)) {
				KeyEvent event = (KeyEvent)intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
				if( event.getAction() == KeyEvent.ACTION_DOWN ) {
					Log.d(TAG, String.format("BluetoothHeadsetReceiver keyCode %d", event.getKeyCode()));
				}
			}
		}
	}

	private WindowInsets mWindowInsets;
	@Override
    public void onCreate(Bundle savedInstanceState) {
    	Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);

		setContentView(R.layout.movie_view);

        mActivityStack = ActivityStack.getInstance();
        mActivityStack.regOnCreateState(this);
		//etlim 20170902 Activity Exit ==> Broadcast Event
        MovieActivityFinishBroadcastRegister();
		registerBluetoothBR();

		mResources = getResources();
		int resourceId = mResources.getIdentifier("status_bar_height", "dimen", "android");
		if (resourceId > 0) {
			mScrollNoActionTop = mResources.getDimensionPixelSize(resourceId);
		}

		resourceId = mResources.getIdentifier("config_showNavigationBar", "bool", "android");
		if (resourceId > 0) {
			if(mResources.getBoolean(resourceId)) {
				resourceId = mResources.getIdentifier("navigation_bar_height", "dimen", "android");
				if (resourceId > 0) {
					mScrollNoActionBottom = mResources.getDimensionPixelSize(resourceId);
				}
			}
		}

        mPreference = PreferenceManager.getDefaultSharedPreferences(this);
		mIntent = getIntent();
        mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        try {
			mOriginSystemBrightnessMode = android.provider.Settings.System.getInt(getContentResolver(),
			        android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE);
			mOriginSystemBrightness = android.provider.Settings.System.getInt(getContentResolver(),
			        android.provider.Settings.System.SCREEN_BRIGHTNESS);
			mSystemBrightness = mPreference.getInt(mResources.getString(R.string.preference_brightness_level_key), mOriginSystemBrightness);
		} catch (SettingNotFoundException e) {
		}

		mSystemVolumeLevel = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int volumeLevel = mPreference.getInt(mResources.getString(R.string.preference_volume_level_key),
				mSystemVolumeLevel);
		mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volumeLevel, AudioManager.RINGER_MODE_SILENT);

		setSystemBrightness(mSystemBrightness);

//        if (mIntent.hasExtra(MediaStore.EXTRA_SCREEN_ORIENTATION)) {
//			int orientation = mIntent.getIntExtra(
//                    MediaStore.EXTRA_SCREEN_ORIENTATION,
//                    ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
//            if (orientation != getRequestedOrientation()) {
//                setRequestedOrientation(orientation);
//            }
//        }

        Window win = getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        winParams.buttonBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_OFF;
        winParams.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
        win.setAttributes(winParams);

        // We set the background in the theme to have the launching animation.
        // But for the performance (and battery), we remove the background here.
        win.setBackgroundDrawable(null);

        mGestureDetector = new GestureDetector(this, new SimpleGestureListener());
        mScaleGestureDetector = new ScaleGestureDetector(this, new SimpleScaleGestureListener());

        //etlim fixed. 20170808 navigation bar orientation issue, nexus P6.
//		mOrientationType = DisplayUtil.getOrientation(this);
//		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
//		mRotationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			mMediaSession = new MediaSession(getApplicationContext(), "BluetoothReceiver");
			mMediaSession.setCallback(new MediaSession.Callback() {
				@Override
				public boolean onMediaButtonEvent(final Intent mediaButtonIntent) {
					String intentAction = mediaButtonIntent.getAction();
					Log.d(TAG, "MediaSession action "+intentAction);
					if (Intent.ACTION_MEDIA_BUTTON.equals(intentAction)) {
						KeyEvent event = mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
						if (event != null) {
							if( event.getAction() == KeyEvent.ACTION_DOWN ) {
								return MoviePlayerService.onKeyDown(event.getKeyCode(), event) || super.onMediaButtonEvent(mediaButtonIntent);
							}
						}
					}
					return super.onMediaButtonEvent(mediaButtonIntent);
				}
			});

			mMediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS |
					MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
			mMediaSession.setActive(true);
		}
		else {
			try {
				mMediaButtonReceiver = new MediaButtonReceiver();
				registerReceiver(mMediaButtonReceiver, new IntentFilter(Intent.ACTION_MEDIA_BUTTON));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}


		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			mAudioFocusRequest =
					new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
					.setOnAudioFocusChangeListener(mAudioFocusChangeListener)
					.build();
			mAudioManager.requestAudioFocus(mAudioFocusRequest);
		}
		else {
			mAudioManager.requestAudioFocus(mAudioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
		}

		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			getWindow().setNavigationBarColor(mResources.getColor(R.color.darker_transparent));
		}

		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
			findViewById(R.id.movie_view_root).setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
				@Override
				public WindowInsets onApplyWindowInsets(View view, WindowInsets windowInsets) {
					mWindowInsets = windowInsets;
					return mWindowInsets;
				}
			});
		}
//		hideSystemUI();

//		[MP-173] -- 오레오 이하 버전에서 이어보기 팝업 미노출되는 현상
//		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
			mBundleNull = savedInstanceState == null;

		handleIntent();
    }

//    private void hideSystemUI() {
//		getWindow().getDecorView().setSystemUiVisibility(
//				View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
//						// Set the content to appear under the system bars so that the
//						// content doesn't resize when the system bars hide and show.
//						| View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//						| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//						| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//						// Hide the nav bar and status bar
//						| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
//						| View.SYSTEM_UI_FLAG_FULLSCREEN);
//	}

	@Override
	protected void onNewIntent(Intent intent) {
		// TODO Auto-generated method stub
    	Log.d(TAG, "onNewIntent");
		super.onNewIntent(intent);
		setIntent(intent);
		mIntent = getIntent();

		if(mIntent.getData() != null)
			handleIntent();
	}

//	@TargetApi(Build.VERSION_CODES.M)
	private void handleIntent() {
		if(KollusConstants.SECURE_MODE && Utils.isRooting()) {
			mAlertDialog = new KollusAlertDialog(MovieActivity.this)
					.setTitle(R.string.error_title)
					.setMessage(R.string.error_rooting)
					.setPositiveButton(R.string.confirm,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) {
									finish();
								}
							})
					.show();
			return;
		}

		final View rootView = findViewById(R.id.movie_view_root);
		Uri uri = mIntent.getData();

		mUrlList.clear();
        mContentList.clear();
		mReturnUrl = null;

		if(uri != null) {
	        Log.d(TAG, "Uri >>> 1 "+uri);
			try {
				mReturnUrl = URLDecoder.decode(KollusUri.parse(uri.toString()).getQueryParameter("ret_url"), "UTF-8");
			} catch (Exception e) {
			}

			if("kollus".equalsIgnoreCase(uri.getScheme())) {
	        	String sUri = uri.toString();
	        	if(sUri.contains("url=")) {
	        		sUri = sUri.substring(sUri.indexOf("url=")+4);
	        	}
	        	else {
	        		sUri = sUri.substring(9);
	        	}
				mUrlList.add(sUri);
				Log.d(TAG, "Uri >>> 2 "+sUri);
	        }
        }
        else {
			final String downloadUrl = mIntent.getStringExtra("download_play");
        	if(downloadUrl != null) {
				Log.d(TAG, "download_play : "+downloadUrl);
        		MultiKollusContent content = mMultiStorage.getDownloadKollusContent(downloadUrl);

        		if(content == null) {
        			String title = String.format("%s : %d", getResources().getString(R.string.ERROR_CODE),
        					ErrorCodes.ERROR_NOT_EXIST_DOWNLOADED_CONTENTS);
        			new KollusAlertDialog(MovieActivity.this).
        			setTitle(title).
        	        setMessage(ErrorCodes.getInstance(MovieActivity.this).getErrorString(ErrorCodes.ERROR_NOT_EXIST_DOWNLOADED_CONTENTS)).
        	        setPositiveButton(R.string.confirm,
        	                new DialogInterface.OnClickListener() {
        	                    public void onClick(DialogInterface dialog, int whichButton) {
									finish();
        	                    }
        	                }).
        	        show();
        		}
        		else {
        		    mContentList.add(content);
        		}
        	}
        	else {
                int diskIndex = mIntent.getIntExtra(mResources.getString(R.string.disk_index), 0);
				String mck = mIntent.getStringExtra(mResources.getString(R.string.media_content_key));
				KollusContent content = new KollusContent();
				if(mMultiStorage.getKollusContent(diskIndex, content, mck)) {
				    mContentList.add(new MultiKollusContent(mMultiStorage.getStorage(diskIndex), content));
                    Log.d(TAG, String.format("Play in use %dth disk's mediaContentKey '%s'", diskIndex, mck));
                }
                else {
                    String title = String.format("%s : %d", getResources().getString(R.string.ERROR_CODE),
                            ErrorCodes.ERROR_NOT_EXIST_DOWNLOADED_CONTENTS);
                    new KollusAlertDialog(MovieActivity.this).
                            setTitle(title).
                            setMessage(ErrorCodes.getInstance(MovieActivity.this).getErrorString(ErrorCodes.ERROR_NOT_EXIST_DOWNLOADED_CONTENTS)).
                            setPositiveButton(R.string.confirm,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            finish();
                                        }
                                    }).
                            show();
                }
        	}
        }

        if(TEST_MULTI_PLAY && Log.isDebug()) {
        	mUrlList.add("http://v.kr.dev.kollus.com/i/dDO1nzBy");
		}

        Log.d(TAG, "MoviePlayerService Bounded : " + mBounded);
		if(mBounded) {
			try {
				if (!mUrlList.isEmpty()) {
					mMessenger.send(Message.obtain(null, MoviePlayerService.PLAY_STREAM, mUrlList.get(0)));
					mUrlList.remove(0);
				}
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else {
            Intent intent = new Intent(this, MoviePlayerService.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onPause() {
    	Log.d(TAG, "onPause");
        mActivityStack.regOnPauseState(this);
    	if(mBounded) {
            try {
            	boolean supportBackgroundPlayback = false;
				if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
					if (KollusConstants.SUPPORT_BACKGROUND_PLAYBACK &&
							isInMultiWindowMode())
                    supportBackgroundPlayback = true;
                }

				if(KollusConstants.SUPPORT_BACKGROUND_PLAYBACK) {
					boolean backgroundPlay = mPreference.getBoolean(mResources.getString(R.string.preference_background_play_key),
							mResources.getBoolean(R.bool.default_background_play));
					if (backgroundPlay)
						supportBackgroundPlayback = true;
				}

				if(!supportBackgroundPlayback)
					mMessenger.send(Message.obtain(null, MoviePlayerService.PAUSE));

                mMessenger.send(Message.obtain(null, MoviePlayerService.APP_BACKGROUND));
            } catch (RemoteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    	if(mAlertDialog != null) {
    		mAlertDialog.dismiss();
    		mAlertDialog = null;
    	}

//        android.provider.Settings.System.putInt(getContentResolver(),
//                android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE, mOriginSystemBrightnessMode);
//
//        android.provider.Settings.System.putInt(getContentResolver(),
//                android.provider.Settings.System.SCREEN_BRIGHTNESS, mOriginSystemBrightness);

//		mSensorManager.unregisterListener(mRotationSensorListener);

        super.onPause();
    }

    @Override
    public void onResume() {
        mActivityStack.regOnResumeState(this);
    	Log.d(TAG, "onResume brightness "+mSystemBrightness);
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			AccessibilityManager am = (AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE);
			mTalkbackEnabled = am.isEnabled() && am.isTouchExplorationEnabled();
		}

    	if(mBounded) {
            if(KollusConstants.SECURE_MODE && Utils.isRooting()) {
                try {
                    mMessenger.send(Message.obtain(null, MoviePlayerService.PAUSE));
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                mAlertDialog = new KollusAlertDialog(MovieActivity.this)
                        .setTitle(R.string.error_title)
                        .setMessage(R.string.error_rooting)
                        .setPositiveButton(R.string.confirm,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        finish();
                                    }
                                })
                        .show();
            }

			try {
//				boolean supportBackgroundPlayback = false;
//				if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//					if (KollusConstants.SUPPORT_BACKGROUND_PLAYBACK &&
//							isInMultiWindowMode())
//						supportBackgroundPlayback = true;
//				}
//				if(!supportBackgroundPlayback)
					mMessenger.send(Message.obtain(null, MoviePlayerService.RESUME));
				mMessenger.send(Message.obtain(null, MoviePlayerService.APP_FORGROUND));
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}

//        android.provider.Settings.System.putInt(getContentResolver(),
//                android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE, 0);
//        android.provider.Settings.System.putInt(getContentResolver(),
//                android.provider.Settings.System.SCREEN_BRIGHTNESS, mSystemBrightness);

//        mSensorManager.registerListener(mRotationSensorListener, mRotationSensor, SensorManager.SENSOR_DELAY_NORMAL);
//		hideSystemUI();

		super.onResume();
		if(KollusConstants.SECURE_MODE && !Log.isDebug())
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
	}

    @Override
    public void onMultiWindowModeChanged(boolean isInMultiWindowMode, Configuration newConfig) {
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig);
    }

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		if(hasFocus) {
//			hideSystemUI();
			MoviePlayerService.setOrientation(mResources.getConfiguration().orientation);
		}
	}

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
		// TODO Auto-generated method stub
    	Log.d(TAG, "onDestroy");
        mActivityStack.regOnDestroyState(this);
    	finishPlayer(true);

		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			mMediaSession.setActive(false);
		}

		super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
		Log.d(TAG, String.format("onKeyDown action %d keyCode %d", event.getAction(), keyCode));
		if( event.getAction() == KeyEvent.ACTION_DOWN ) {
			if( keyCode == KeyEvent.KEYCODE_BACK ){
				if(!mExit) {
                    mExit = true;
                    Log.d(TAG, "Show Toast Exit");
                    Toast.makeText(this, R.string.repress_backkey_for_play, Toast.LENGTH_SHORT).show();
					mHandler.sendEmptyMessageDelayed(CHECK_EXIT, EXIT_TIME);
				}
				else {
					finishPlayer(false);
				}

				return true;
			}
			else if(keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
				setVolumeControl(CONTROL_INC);
				return true;
        	}
			else if(keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
				setVolumeControl(CONTROL_DEC);
				return true;
        	}
		}

    	if(MoviePlayerService.isPlayerNull())
    		return super.onKeyDown(keyCode, event);
    	else {
			switch (keyCode) {
				case KeyEvent.KEYCODE_DPAD_UP: {
					setVolumeControl(CONTROL_INC);
					return true;
				}
				case KeyEvent.KEYCODE_DPAD_DOWN: {
					setVolumeControl(CONTROL_DEC);
					return true;
				}
			}
			return MoviePlayerService.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
		}
    }

    private void finishPlayer(boolean bCallInDestroy) {
		Log.d(TAG, "finishPlayer bCallInDestroy:"+bCallInDestroy);
        mHandler.removeMessages(CHECK_EXIT);
		if(mBounded) {
			try {
				mMessenger.send(Message.obtain(null, MoviePlayerService.STOP));
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			unbindService(mConnection);
		}
		mBounded = false;

		//etlim 20170902 Activity Exit ==> Broadcast Event
		if(bCallInDestroy) {
			unregisterReceiver(mMovieActivityFinishBR);
			unregisterReceiver(mBluetoothBR);
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP && mMediaButtonReceiver != null)
				unregisterReceiver(mMediaButtonReceiver);

			int volumeLevel = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
			SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
			editor.putInt(mResources.getString(R.string.preference_volume_level_key), volumeLevel);
			editor.putInt(mResources.getString(R.string.preference_brightness_level_key), mSystemBrightness);
			editor.commit();
			mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mSystemVolumeLevel, AudioManager.RINGER_MODE_SILENT);

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				mAudioManager.abandonAudioFocusRequest(mAudioFocusRequest);
			} else {
				mAudioManager.abandonAudioFocus(mAudioFocusChangeListener);
			}

			//        Log.d(TAG, "Volume Save System "+mSystemVolumeLevel+", Preferences "+volumeLevel);
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			//		mWindowManager.removeView(mRootGroup);

			if(mReturnUrl != null) {
				try {
					String retUrl = new String(Base64.decode(mReturnUrl, Base64.DEFAULT));
					startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(retUrl)));
				} catch (Exception e) {}
			}
		}
		else {
			finish();
		}
	}

//	private SensorEventListener mRotationSensorListener = new SensorEventListener() {
//		@Override
//		public void onSensorChanged(SensorEvent event) {
//			if (mResources.getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
//			} else {
//				if (mOrientationType != DisplayUtil.getOrientation(MovieActivity.this)) {
//					mOrientationType = DisplayUtil.getOrientation(MovieActivity.this);
//					if (mOrientationType == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
//						mHandler.sendEmptyMessage(HANDLER_ORIENTATION_LANDSCAPE);
//					} else if (mOrientationType == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
//						mHandler.sendEmptyMessage(HANDLER_ORIENTATION_REVERSED_LANDSCAPE);
//					} else {
//						mHandler.sendEmptyMessage(HANDLER_ORIENTATION_LANDSCAPE);
//					}
//				}
//			}
//		}
//
//		@Override
//		public void onAccuracyChanged(Sensor sensor, int accuracy) {
//		}
//	};

    private void setVolumeControl(int direction) {
		if(mBounded) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
						AudioManager.ADJUST_UNMUTE, AudioManager.RINGER_MODE_SILENT);
			} else {
				mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
			}

			int curVolumeLevel = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
			int maxVolumeLevel = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
			if(direction == CONTROL_INC) {
				if(curVolumeLevel == maxVolumeLevel) {
					mSWVolumeLevel++;
					if(mSWVolumeLevel > KollusConstants.MAX_SW_VOLUME)
						mSWVolumeLevel = KollusConstants.MAX_SW_VOLUME;
				}
				else {
					mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
							AudioManager.ADJUST_RAISE, AudioManager.RINGER_MODE_SILENT);
				}

				int adjustVolumeLevel = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
				if(adjustVolumeLevel == curVolumeLevel && curVolumeLevel != maxVolumeLevel) {
					maxVolumeLevel = adjustVolumeLevel;
					mSWVolumeLevel++;
					if(mSWVolumeLevel > KollusConstants.MAX_SW_VOLUME)
						mSWVolumeLevel = KollusConstants.MAX_SW_VOLUME;
				}
			}
			else if(direction == CONTROL_DEC) {
				if(mSWVolumeLevel > 0) {
					mSWVolumeLevel--;
				}
				else {
					mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
							AudioManager.ADJUST_LOWER, AudioManager.RINGER_MODE_SILENT);
				}
			}
			curVolumeLevel = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
			MoviePlayerService.setVolumeLabel(mSWVolumeLevel+curVolumeLevel, maxVolumeLevel);
		}
    }

    private void setBrightnessControl(int direction) {
    	int brightness = mSystemBrightness;

    	if(direction == CONTROL_INC) {
    		brightness += BRIGHTNESS_UNIT;
    	}
    	else if(direction == CONTROL_DEC) {
    		brightness -= BRIGHTNESS_UNIT;
    	}

    	if(brightness > BRIGHTNESS_MAX)
    		brightness = BRIGHTNESS_MAX;
		else if(brightness < BRIGHTNESS_MIN)
			brightness = BRIGHTNESS_MIN;

		int nBrightnessLevel = brightness/BRIGHTNESS_UNIT;
		mSystemBrightness = nBrightnessLevel*BRIGHTNESS_UNIT+BRIGHTNESS_MIN;
		setSystemBrightness(mSystemBrightness);

		MoviePlayerService.setBrightnessLabel(nBrightnessLevel);
    }

	private void setSystemBrightness(int brightness) {
//		android.provider.Settings.System.putInt(getContentResolver(),
//				android.provider.Settings.System.SCREEN_BRIGHTNESS, brightness);

		Window win = getWindow();
		WindowManager.LayoutParams lp = win.getAttributes();
		lp.screenBrightness = (float)brightness/(float)BRIGHTNESS_MAX;
		win.setAttributes(lp);
	}

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
    	if(MoviePlayerService.isPlayerNull())
    		return super.onKeyUp(keyCode, event);
    	else
			return MoviePlayerService.onKeyUp(keyCode, event) || super.onKeyUp(keyCode, event);
    }



	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// TODO Auto-generated method stub
		Log.d(TAG, "onConfigurationChanged");
		super.onConfigurationChanged(newConfig);

//		MoviePlayerService.setOrientation(newConfig.orientation);
        mHandler.sendEmptyMessageDelayed(HANDLER_ORIENTATION_CHANGED, 100);
    }



    @Override
	public boolean onTouchEvent(MotionEvent event) {
		// TODO Auto-generated method stub
//		hideSystemUI();

		if(mTalkbackEnabled)
			return super.onTouchEvent(event);

    	if(event.getPointerCount() > 1) {
    		return mScaleGestureDetector.onTouchEvent(event);
    	}
    	else {
    		if(event.getAction() == MotionEvent.ACTION_UP) {
    			if(mScrollMode == SCROLL_MODE_H) {
    				DisplayMetrics displayMetrics = new DisplayMetrics();
					getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
					MoviePlayerService.setSeekLabel(displayMetrics.widthPixels, displayMetrics.heightPixels,
							(int)event.getX(), mScrollAmountH,
							mSeekControlDistance*1000, false);
					mSeekControlDistance = 0;
    			}
    			mScrollMode = SCROLL_MODE_N;
				mScrollAmountH = -1;
			}

    		return mGestureDetector.onTouchEvent(event);
    	}
	}

	private boolean mScaleStarted;
	private ScaleGestureDetector mScaleGestureDetector;
    private class SimpleScaleGestureListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

		@Override
		public boolean onScale(ScaleGestureDetector detector) {
			// TODO Auto-generated method stub
			Log.d(TAG, "onScale : "+detector);
			if(mScaleStarted)
	            MoviePlayerService.screenSizeScale(detector);

			return super.onScale(detector);
		}

		@Override
		public boolean onScaleBegin(ScaleGestureDetector detector) {
			// TODO Auto-generated method stub
			if(MoviePlayerService.isInPlaybackState()) {
				mScaleStarted = true;
				MoviePlayerService.screenSizeScaleBegin(detector);
			}
			return super.onScaleBegin(detector);
		}

		@Override
		public void onScaleEnd(ScaleGestureDetector detector) {
			// TODO Auto-generated method stub
			if(mScaleStarted)
	            MoviePlayerService.screenSizeScaleEnd(detector);
			mScaleStarted = false;
			super.onScaleEnd(detector);
		}

    }

	private GestureDetector mGestureDetector;
    private class SimpleGestureListener extends GestureDetector.SimpleOnGestureListener {

		@Override
		public boolean onSingleTapConfirmed(MotionEvent event) {
			// TODO Auto-generated method stub
			Log.d(TAG, "onSingleTapConfirmed");
			if(MoviePlayerService.isInPlaybackState())
                MoviePlayerService.toggleMediaControlsVisibility();

			return super.onSingleTapConfirmed(event);
		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2,
				float distanceX, float distanceY) {
			// TODO Auto-generated method stub
			if(MoviePlayerService.isInPlaybackState()) {
				DisplayMetrics displayMetrics = new DisplayMetrics();
				getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

				if(MoviePlayerService.isVRMode()) {
				}
				else if(MoviePlayerService.canMoveVideoScreen()) {
					MoviePlayerService.moveVideoFrame(-distanceX, -distanceY);
				}
				else if(e1 != null && e2 != null){
//				else if(!mPlayer.isControlsShowing()){
					int volumeChangeX  = displayMetrics.widthPixels/2;
					float moveX = Math.abs(e1.getX() - e2.getX());
					float moveY = Math.abs(e1.getY() - e2.getY());

					Log.d(TAG, String.format("onScroll e1 (%1.2f %1.2f) disp (%d %d)", e1.getX(), e1.getY(),
							displayMetrics.widthPixels, displayMetrics.heightPixels));

					if(mScrollMode == SCROLL_MODE_N) {
						boolean bIgnoreEvent = false;

						if(e1.getY() < mScrollNoActionTop)
							bIgnoreEvent = true;

						if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
							Log.d(TAG, String.format("onScroll SystemWindowInset %d %d %d %d",
									mWindowInsets.getSystemWindowInsetLeft(), mWindowInsets.getSystemWindowInsetTop(),
									mWindowInsets.getSystemWindowInsetRight(), mWindowInsets.getSystemWindowInsetBottom()));
							if(mWindowInsets.getSystemWindowInsetLeft() > 0 && e1.getX() < mScrollNoActionBottom)
								bIgnoreEvent = true;
							else if(mWindowInsets.getSystemWindowInsetRight() > 0 && e1.getX() > (displayMetrics.widthPixels-mScrollNoActionBottom))
								bIgnoreEvent = true;
							else if(mWindowInsets.getSystemWindowInsetBottom() > 0 && e1.getY() > (displayMetrics.heightPixels-mScrollNoActionBottom))
								bIgnoreEvent = true;
						}
						else {
							if(mResources.getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT &&
									e1.getY() > (displayMetrics.heightPixels-mScrollNoActionBottom))
								bIgnoreEvent = true;
							else if(mResources.getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE &&
									e1.getX() > (displayMetrics.widthPixels-mScrollNoActionBottom))
								bIgnoreEvent = true;
						}

						if(bIgnoreEvent)
							return super.onScroll(e1, e2, distanceX, distanceY);

						if(moveX > moveY)
							mScrollMode = SCROLL_MODE_H;
						else
							mScrollMode = SCROLL_MODE_V;
					}

					if(mScrollMode == SCROLL_MODE_V) {
						//Volume Control
						if(e1.getX() > volumeChangeX) {
							if((mVolumeControlDistance > 0 && distanceY < 0) ||
							   (mVolumeControlDistance < 0 && distanceY > 0))
								mVolumeControlDistance = 0;

							mVolumeControlDistance += distanceY;
							if(Math.abs(mVolumeControlDistance) > SCROLL_V_DELICACY) {
								if(distanceY < 0)
									setVolumeControl(CONTROL_DEC);
								else
									setVolumeControl(CONTROL_INC);
								mVolumeControlDistance = 0;
							}
						}
						//Brightness Control
						else {
							if((mBrightnessControlDistance > 0 && distanceY < 0) ||
							   (mBrightnessControlDistance < 0 && distanceY > 0))
								mBrightnessControlDistance = 0;

							mBrightnessControlDistance += distanceY;
							if(Math.abs(mBrightnessControlDistance) > SCROLL_H_DELICACY) {
								if(distanceY < 0)
									setBrightnessControl(CONTROL_DEC);
								else
									setBrightnessControl(CONTROL_INC);
								mBrightnessControlDistance = 0;
							}
						}
					}
					else {
						if(e2.getAction() == MotionEvent.ACTION_MOVE) {
							mSeekControlDistance = (int)((e2.getX() - e1.getX())*SCROLL_SEEK_MOUNT/displayMetrics.widthPixels);
							if(mScrollAmountH < 0)
								mScrollAmountH = (int)e2.getY();
							MoviePlayerService.setSeekLabel(displayMetrics.widthPixels, displayMetrics.heightPixels,
									(int)e2.getX(), mScrollAmountH,
									mSeekControlDistance*1000, true);
						}
					}
				}
			}
			return super.onScroll(e1, e2, distanceX, distanceY);
		}

		@Override
		public boolean onDoubleTap(MotionEvent event) {
			// TODO Auto-generated method stub
            if(MoviePlayerService.isInPlaybackState()) {
                SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(MovieActivity.this);
                @ValuePreference.DOUBLE_TAB_MODE int doubleTabMode = pref.getInt(mResources.getString(R.string.preference_double_tab_key), ValuePreference.DOUBLE_TAB_SCREEN_SIZE);
                if (doubleTabMode == ValuePreference.DOUBLE_TAB_SCREEN_SIZE) {
                    MoviePlayerService.toggleVideoSize();
                } else if (doubleTabMode == ValuePreference.DOUBLE_TAB_PLAY_PAUSE) {
                    try {
                        mMessenger.send(Message.obtain(null, MoviePlayerService.RESUME_PAUSE));
                    } catch (RemoteException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
			return super.onDoubleTap(event);
		}
    }

    Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			if(msg.what == CHECK_EXIT) {
                Log.d(TAG, "set Exit false");
				mExit = false;
			}
//			else if(msg.what == HANDLER_ORIENTATION_LANDSCAPE || msg.what == HANDLER_ORIENTATION_REVERSED_LANDSCAPE) {
//                MoviePlayerService.setOrientation(Configuration.ORIENTATION_LANDSCAPE);
//            }
            else if(msg.what == HANDLER_ORIENTATION_CHANGED) {
                MoviePlayerService.setOrientation(mResources.getConfiguration().orientation);
            }
		}
    };

	//etlim 20170902 Activity Exit ==> Broadcast Event
    private void MovieActivityFinishBroadcastRegister() {
    	mMovieActivityFinishBR = new MovieActivityFinishBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_ACTIVITY_FINISH_MOVIE);
        registerReceiver(mMovieActivityFinishBR, filter);
    }

    private class MovieActivityFinishBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context ctx, Intent intent) {
			Log.d(TAG, "onReceive >>> " + intent.getAction());
            String action = intent.getAction();

            if (action.equals(ACTION_ACTIVITY_FINISH_MOVIE)) {
                try {
                    finish();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (action.equals("")) {
                // do something
            }
        }
    }

	BluetoothBroadcastReceiver mBluetoothBR;
    private void registerBluetoothBR() {
		mBluetoothBR = new BluetoothBroadcastReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
		filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
		filter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
		filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
		registerReceiver(mBluetoothBR, filter);
    }

	private class BluetoothBroadcastReceiver extends BroadcastReceiver {
		private final String TAG = BluetoothBroadcastReceiver.class.getSimpleName();
		@Override
		public void onReceive(Context context, Intent intent)
		{
			String action = intent.getAction();
			int state;
			int majorDeviceClass;
			BluetoothDevice bluetoothDevice;

			switch(action)
			{
				case BluetoothAdapter.ACTION_STATE_CHANGED:
					state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
					if (state == BluetoothAdapter.STATE_OFF)
					{
						Log.d(TAG, "Bluetooth is off");
					}
					else if (state == BluetoothAdapter.STATE_TURNING_OFF)
					{
						Log.d(TAG, "Bluetooth is turning off");
					}
					else if(state == BluetoothAdapter.STATE_ON)
					{
						Log.d(TAG, "Bluetooth Bluetooth is on");
					}
					break;

				case BluetoothDevice.ACTION_ACL_CONNECTED:
					bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
					majorDeviceClass = bluetoothDevice.getBluetoothClass().getMajorDeviceClass();
					Log.d(TAG, "Bluetooth Connect to "+bluetoothDevice.getName());
					if(majorDeviceClass == BluetoothClass.Device.Major.AUDIO_VIDEO) {
						MoviePlayerService.setBluetoothConnectChanged(true);
					}
					break;

				case BluetoothDevice.ACTION_ACL_DISCONNECTED:
					bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
					majorDeviceClass = bluetoothDevice.getBluetoothClass().getMajorDeviceClass();
					Log.d(TAG, "Bluetooth DisConnect from "+bluetoothDevice.getName());
					if(majorDeviceClass == BluetoothClass.Device.Major.AUDIO_VIDEO) {
                        MoviePlayerService.setBluetoothConnectChanged(false);
					}
					break;
			}
		}
	}

    public class ClientHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MoviePlayerService.ADD_HANDLER:
                    try {
						MoviePlayerService.setActivity(MovieActivity.this);

                        if (!mUrlList.isEmpty()) {
							mMessenger.send(Message.obtain(null, MoviePlayerService.PLAY_STREAM, mBundleNull ? 0 : 1, 0, mUrlList.get(0)));
							mUrlList.remove(0);
						}
                        else if (!mContentList.isEmpty()) {
							mMessenger.send(Message.obtain(null, MoviePlayerService.PLAY_DOWNLOAD, mBundleNull ? 0 : 1, 0, mContentList.get(0)));
							mContentList.remove(0);
						}
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    break;
                case MoviePlayerService.COMPLETE:
					try {
						if (!mUrlList.isEmpty()) {
							mMessenger.send(Message.obtain(null, MoviePlayerService.PLAY_STREAM, mBundleNull ? 0 : 1, 0, mUrlList.get(0)));
							mUrlList.remove(0);
						}
						else if (!mContentList.isEmpty()) {
							mMessenger.send(Message.obtain(null, MoviePlayerService.PLAY_DOWNLOAD, mBundleNull ? 0 : 1, 0, mContentList.get(0)));
							mContentList.remove(0);
						}
						else
							finishPlayer(false);
					} catch (RemoteException e) {
						e.printStackTrace();
					}
                    break;
            }
        }
    }

	private Messenger mMessenger;
	private boolean mBounded;
	private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
        	Log.d(TAG, "PlayerService Bounded.");
            mMessenger = new Messenger(service);
            mBounded = true;
            try {
                mMessenger.send(Message.obtain(null, MoviePlayerService.ADD_HANDLER, new ClientHandler()));
            } catch (RemoteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBounded = false;
            mMessenger = null;
        }
    };
}

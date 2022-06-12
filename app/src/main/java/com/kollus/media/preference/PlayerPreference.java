package com.kollus.media.preference;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageView;

import com.kollus.media.BaseActivity;
import com.kollus.media.R;
import com.kollus.media.contents.MultiKollusStorage;
import com.kollus.media.util.ActivityStack;
import com.kollus.sdk.media.KollusStorage;
import com.kollus.sdk.media.util.Log;

public class PlayerPreference extends BaseActivity {
	private final String TAG = PlayerPreference.class.getSimpleName();

	private CpuInfoPreference mCpuInfo;
	private PlayerInfoPreference mPlayerInfo;
	private GeneralPreference mGeneralInfo;
	private CaptionPreference mCaptionInfo;
	private SortPreference mSortInfo;
	private StoragePreference mStorageInfo;
	private GuidePreference mGuideInfo;

	private ActivityStack mActivityStack;
	//etlim 20170902 Activity Exit ==> Broadcast Event
	private String ACTION_ACTIVITY_FINISH_PLAYER_PREFERENCE = "com.kollus.media.action.activity.finish.player.preference";
	private PlayerPreferenceActivityBroadcastReceiver mPlayerPreferenceActivityBR;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.setting_layout);

		mActivityStack = ActivityStack.getInstance();
		mActivityStack.regOnCreateState(this);
		//etlim 20170902 Activity Exit ==> Broadcast Event
		PlayerPreferenceActivityBroadcastRegister();

        ImageView btn = (ImageView)findViewById(R.id.btn_back);
		btn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				// TODO Auto-generated method stub
				setResult(RESULT_OK, getIntent());
				finish();
			}			
		});
		
		//PlayRatePreference rate = new PlayRatePreference(this);
		mGeneralInfo = new GeneralPreference(this);
		mCaptionInfo = new CaptionPreference(this);
		mSortInfo = new SortPreference(this);
		mStorageInfo = new StoragePreference(this);
		mCpuInfo = new CpuInfoPreference(this);
		mPlayerInfo = new PlayerInfoPreference(this);
		mGuideInfo = new GuidePreference(this);
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onRestoreInstanceState(savedInstanceState);
		mStorageInfo.invalidate();
	}

	//etlim 20170902 Activity Exit ==> Broadcast Event
	@Override
	protected void onDestroy() {
		unregisterReceiver(mPlayerPreferenceActivityBR);
		mMultiStorage = MultiKollusStorage.getInstance(this);
		mActivityStack.regOnDestroyState(this);
		super.onDestroy();
	}

	@Override
	protected void onResume() {
		super.onResume();
		mActivityStack.regOnResumeState(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		mActivityStack.regOnPauseState(this);
	}

	private void PlayerPreferenceActivityBroadcastRegister() {
		mPlayerPreferenceActivityBR = new PlayerPreferenceActivityBroadcastReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(ACTION_ACTIVITY_FINISH_PLAYER_PREFERENCE);
		registerReceiver(mPlayerPreferenceActivityBR, filter);
	}

	private class PlayerPreferenceActivityBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context ctx, Intent intent) {
			Log.d(TAG, "onReceive >>> " + intent.getAction());
			String action = intent.getAction();

			if (action.equals(ACTION_ACTIVITY_FINISH_PLAYER_PREFERENCE)) {
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

}

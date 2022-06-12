package com.kollus.media.preference;

import com.kollus.media.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;

public class PlayRatePreference extends View implements OnCheckedChangeListener {
	private Context mContext;
	private SharedPreferences mSharedPreferences;
	private Resources mResources;
	private RadioGroup mRateRadioGroup;
	private int mPlayRate;
	
	public PlayRatePreference(PlayerPreference root) {
		super(root);
		
		mContext = root;
		mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
		mResources = mContext.getResources();
		
		LinearLayout layout = (LinearLayout)root.findViewById(R.id.preference_root);
		View view = root.getLayoutInflater().inflate(R.layout.play_rate_preference, null);
		layout.addView(view);
		
		onBindView(view);
	}

	private void onBindView(View view) {
		mRateRadioGroup = (RadioGroup)view.findViewById(R.id.max_rate);
		mRateRadioGroup.setOnCheckedChangeListener(this);
		
		mPlayRate = mSharedPreferences.getInt(mResources.getString(R.string.preference_max_rate_key), 
				mResources.getInteger(R.integer.default_play_rate));
		
		if(mPlayRate == 2)
			mRateRadioGroup.check(R.id.rate_2);
		else
			mRateRadioGroup.check(R.id.rate_3);
	}

	@Override
	public void onCheckedChanged(RadioGroup group, int checkedId) {
		// TODO Auto-generated method stub
		int mPlayRate = mResources.getInteger(R.integer.default_play_rate);
		switch(checkedId) {
		case R.id.rate_2:
			mPlayRate = 2;
			break;
		case R.id.rate_3:
			mPlayRate = 3;
			break;
		}
		
		SharedPreferences.Editor editor = mSharedPreferences.edit();
		editor.putInt(mResources.getString(R.string.preference_max_rate_key), mPlayRate);
		editor.commit();
	}
}

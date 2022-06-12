package com.kollus.media.preference;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.kollus.media.GuideGestureActivity;
import com.kollus.media.GuideShortCutsActivity;
import com.kollus.media.R;
import com.kollus.sdk.media.MediaPlayer;
import com.kollus.sdk.media.util.CpuInfo;
import com.kollus.sdk.media.util.Log;
import com.kollus.sdk.media.util.Utils;

public class GuidePreference extends View {
	private Activity mActivity;
	public GuidePreference(PlayerPreference root) {
		super(root);

		LinearLayout layout = (LinearLayout)root.findViewById(R.id.preference_root);
		View view = root.getLayoutInflater().inflate(R.layout.guide_layout, null);
		layout.addView(view);
		mActivity = root;
		onBindView(view);
	}

	private void onBindView(View view) {
		Button btn = (Button)view.findViewById(R.id.btn_short_cuts);
		btn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mActivity.startActivity(new Intent(mActivity, GuideShortCutsActivity.class));
			}
		});

		btn = (Button)view.findViewById(R.id.btn_gesture);
		btn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mActivity.startActivity(new Intent(mActivity, GuideGestureActivity.class));
			}
		});
	}
}

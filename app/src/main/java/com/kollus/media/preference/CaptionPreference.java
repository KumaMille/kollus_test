package com.kollus.media.preference;

import yuku.ambilwarna.AmbilWarnaDialog;
import yuku.ambilwarna.AmbilWarnaDialog.OnAmbilWarnaListener;

import com.kollus.media.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

import androidx.appcompat.widget.SwitchCompat;

public class CaptionPreference extends View implements 
	OnClickListener, 
	OnCheckedChangeListener{

	private final String TEXT_SIZE_UNIT = "";
	private final int CAPTION_SIZE_MIN = 5;
	private final int CAPTION_SIZE_MAX = 30;
	
	private Context mContext;
	private SharedPreferences mSharedPreferences;
	private Resources mResources;
	
	TextView mColorView;
	TextView mStrokeColorView;
	SwitchCompat mStrokeSwitch;
	TextView mCaptionSizeView;
	TextView mCaptionTestView;
	Button mCaptionSizeDecrease;
	Button mCaptionSizeIncrease;
	int mColor;
	int mStrokeColor;
	boolean mStroke;
	SwitchCompat mBackgroundSwitch;

	int mCaptionSize;
	
	public CaptionPreference(PlayerPreference root) {
		super(root);
		// TODO Auto-generated constructor stub
		mContext = root;
		mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
		mResources = mContext.getResources();
		
		LinearLayout layout = (LinearLayout)root.findViewById(R.id.preference_root);
		View view = root.getLayoutInflater().inflate(R.layout.caption_preference, null);
		layout.addView(view);
		onBindView(view);
	}
	
	private void onBindView(View view) {
		mColor = mSharedPreferences.getInt(mResources.getString(R.string.preference_caption_color_key), 
				mResources.getColor(R.color.default_caption_color));
		mStrokeColor = mSharedPreferences.getInt(mResources.getString(R.string.preference_stroke_color_key), 
				mResources.getColor(R.color.default_stroke_color));
		mStroke = mSharedPreferences.getBoolean(mResources.getString(R.string.preference_stroke_key),
				mResources.getBoolean(R.bool.default_stroke));
		
		mCaptionSize = mSharedPreferences.getInt(mResources.getString(R.string.preference_caption_size_key), 
				mResources.getInteger(R.integer.default_caption_size));
		
		mCaptionTestView = (TextView)view.findViewById(R.id.caption_test_view);
		mCaptionSizeDecrease = (Button)view.findViewById(R.id.caption_size_decrease);
		mCaptionSizeIncrease = (Button)view.findViewById(R.id.caption_size_increase);
		mCaptionSizeDecrease.setOnClickListener(this);
		mCaptionSizeIncrease.setOnClickListener(this);

		if(mCaptionSize == CAPTION_SIZE_MIN)
			mCaptionSizeDecrease.setEnabled(false);
		else if(mCaptionSize == CAPTION_SIZE_MAX)
			mCaptionSizeIncrease.setEnabled(false);
		
		mCaptionSizeView = (TextView)view.findViewById(R.id.caption_size_view);
		mCaptionSizeView.setText(mCaptionSize+TEXT_SIZE_UNIT);
		
		mColorView = (TextView)view.findViewById(R.id.color_view);
		mColorView.setBackgroundColor(mColor);
		mColorView.setOnClickListener(this);
		
		mStrokeColorView = (TextView)view.findViewById(R.id.stroke_color_view);
		mStrokeColorView.setBackgroundColor(mStrokeColor);
		mStrokeColorView.setOnClickListener(this);
		
		mStrokeSwitch = (SwitchCompat)view.findViewById(R.id.stroke);
		mStrokeSwitch.setOnCheckedChangeListener(this);
		mStrokeSwitch.setChecked(mStroke);

		mBackgroundSwitch = (SwitchCompat)view.findViewById(R.id.caption_bg);
		mBackgroundSwitch.setOnCheckedChangeListener(this);
		mBackgroundSwitch.setChecked(mSharedPreferences.getBoolean(mResources.getString(R.string.preference_caption_bg_color_key),
				false));

		showCaptionTest();
	}
	
	private void showCaptionTest() {
//		int sizePt = (int) TypedValue.applyDimension(
//				TypedValue.COMPLEX_UNIT_PX, mCaptionSize, mContext.getResources().getDisplayMetrics());
		mCaptionTestView.setTextColor(mColor);
		mCaptionTestView.setTextSize(mCaptionSize);
		if(mStroke)
			mCaptionTestView.setShadowLayer(5, 0, 0, mStrokeColor);
		else
			mCaptionTestView.setShadowLayer(0, 0, 0, 0);
	}

	@Override
	public void onClick(View view) {
		// TODO Auto-generated method stub
		int id = view.getId();
		if(id == R.id.color_view) {
			AmbilWarnaDialog dialog = new AmbilWarnaDialog(getContext(), mColor, new OnAmbilWarnaListener()
			{
			@Override
			public void onCancel(AmbilWarnaDialog dialog) {
			}
	
			@Override
			public void onOk(AmbilWarnaDialog dialog, int color) {
				SharedPreferences.Editor editor = mSharedPreferences.edit();
				Context context = getContext();
				Resources mResources = context.getResources();
				
				mColor = color;				
				editor.putInt(mResources.getString(R.string.preference_caption_color_key), mColor);
				editor.commit();
				mColorView.setBackgroundColor(mColor|0xff000000);
				
				showCaptionTest();
			}
			});
			dialog.show();
		}
		else if(id == R.id.stroke_color_view) {
			AmbilWarnaDialog dialog = new AmbilWarnaDialog(getContext(), mStrokeColor, new OnAmbilWarnaListener()
			{
			@Override
			public void onCancel(AmbilWarnaDialog dialog) {
			}
	
			@Override
			public void onOk(AmbilWarnaDialog dialog, int color) {
				SharedPreferences.Editor editor = mSharedPreferences.edit();
				Context context = getContext();
				Resources mResources = context.getResources();
				
				mStrokeColor = color;				
				editor.putInt(mResources.getString(R.string.preference_stroke_color_key), mStrokeColor);
				editor.commit();
				mStrokeColorView.setBackgroundColor(mStrokeColor|0xff000000);
				
				showCaptionTest();
			}
			});
			dialog.show();
		}
		else if(id == R.id.caption_size_decrease) {
			if(mCaptionSize > CAPTION_SIZE_MIN) {
				mCaptionSize--;
				
				SharedPreferences.Editor editor = mSharedPreferences.edit();
				editor.putInt(mResources.getString(R.string.preference_caption_size_key), mCaptionSize);
				editor.commit();
				mCaptionSizeView.setText(mCaptionSize+TEXT_SIZE_UNIT);
				
				showCaptionTest();
			}

			if(mCaptionSize == CAPTION_SIZE_MIN)
				mCaptionSizeDecrease.setEnabled(false);
			mCaptionSizeIncrease.setEnabled(true);
		}
		else if(id == R.id.caption_size_increase) {
			if(mCaptionSize < CAPTION_SIZE_MAX) {
				mCaptionSize++;
				
				SharedPreferences.Editor editor = mSharedPreferences.edit();
				editor.putInt(mResources.getString(R.string.preference_caption_size_key), mCaptionSize);
				editor.commit();
				mCaptionSizeView.setText(mCaptionSize+TEXT_SIZE_UNIT);
				
				showCaptionTest();
			}

			if(mCaptionSize == CAPTION_SIZE_MAX)
				mCaptionSizeIncrease.setEnabled(false);
			mCaptionSizeDecrease.setEnabled(true);
		}
	}

	@Override
	public void onCheckedChanged(CompoundButton button, boolean checked) {
		// TODO Auto-generated method stub
		SharedPreferences.Editor editor = mSharedPreferences.edit();

		if(button == mStrokeSwitch) {
			mStroke = checked;
			editor.putBoolean(mResources.getString(R.string.preference_stroke_key), mStroke);
			showCaptionTest();
		}
		else if(button == mBackgroundSwitch) {
			editor.putBoolean(mResources.getString(R.string.preference_caption_bg_color_key), checked);
		}
		editor.commit();
	}
}

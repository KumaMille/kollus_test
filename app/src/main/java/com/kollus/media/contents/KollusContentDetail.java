package com.kollus.media.contents;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;

import com.kollus.media.BaseActivity;
import com.kollus.media.MovieActivity;
import com.kollus.media.R;
import com.kollus.media.preference.KollusConstants;
import com.kollus.media.preference.PlayerPreference;
import com.kollus.media.util.ActivityStack;
import com.kollus.media.view.KollusAlertDialog;
import com.kollus.sdk.media.KollusStorage;
import com.kollus.sdk.media.content.KollusContent;
import com.kollus.sdk.media.util.Log;
import com.kollus.sdk.media.util.Utils;

import java.util.Date;

public class KollusContentDetail extends BaseActivity implements OnClickListener {
	private static final String TAG = "ContentsInfoDetail";

	private KollusContent mKollusContent;
	private Bitmap mThumbnail;
	private ActivityStack mActivityStack;
	//etlim 20170902 Activity Exit ==> Broadcast Event
	private String ACTION_ACTIVITY_FINISH_KOLLUS_CONTENT_DETAIL = "com.kollus.media.action.activity.finish.kollus.content.detail";
	private KollusContentDetailActivityBroadcastReceiver mKollusContentDetailActivityBR;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.contents_detail);

		mActivityStack = ActivityStack.getInstance();
		mActivityStack.regOnCreateState(this);
		//etlim 20170902 Activity Exit ==> Broadcast Event
		KollusContentDetailActivityBroadcastRegister();

		ImageView btn = (ImageView)findViewById(R.id.btn_back);
		btn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				// TODO Auto-generated method stub
				setResult(RESULT_OK, getIntent());
				finish();
			}			
		});
		
		bindView();
	}

	@Override
	protected void onPause() {
		super.onPause();
		mActivityStack.regOnPauseState(this);
	}
	
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		Log.d(TAG, "onResume");
		super.onResume();
		mActivityStack.regOnResumeState(this);
		bindView();
	}

	private void bindView() {
		String mck = getIntent().getStringExtra(getResources().getString(R.string.media_content_key));
		int diskIndex = getIntent().getIntExtra(getResources().getString(R.string.disk_index), 0);
		mKollusContent = new KollusContent();

		boolean bRet = mMultiStorage.getKollusContent(diskIndex, mKollusContent, mck);
		if(!bRet) {
			setResult(RESULT_OK, getIntent());
			finish();
			return;
		}
		
		ImageView thumbImage = (ImageView)findViewById(R.id.detail_thumbnail);
		ImageView playImage = (ImageView)findViewById(R.id.detail_play);
		ImageView hangImage = (ImageView)findViewById(R.id.detail_hang);
		
        String thumbnail = mKollusContent.getThumbnailPath();
		if(thumbnail != null) {
        	if(thumbnail.startsWith("http://")) {
        	}
        	else {
        		mThumbnail = BitmapFactory.decodeFile(thumbnail);        		
        	}
        }
		if(mThumbnail == null)
			mThumbnail = BitmapFactory.decodeResource(getResources(), R.drawable.kollus);
		
		thumbImage.setImageBitmap(mThumbnail);
		thumbImage.setOnClickListener(this);
		
        TextView text = (TextView)findViewById(R.id.detail_title);
		String cource = mKollusContent.getCourse();
		String subcource = mKollusContent.getSubCourse();
		String title;
		if(cource != null && cource.length() > 0) {
        	if(subcource != null && subcource.length() > 0)
        		title = cource+"("+subcource+")";
        	else
        		title  = cource;
        }
        else
        	title = subcource;
		text.setText(title);
		
		if(mKollusContent.getExpirationDate() > 0 || mKollusContent.getTotalExpirationPlaytime() > 0 || mKollusContent.getTotalExpirationCount() > 0) {
			View drmInfo = findViewById(R.id.drm_info);
			drmInfo.setVisibility(View.VISIBLE);

			if(mKollusContent.getExpirationDate() > 0) {
				long millSecond = (long)mKollusContent.getExpirationDate()*(long)1000;
				text = (TextView)findViewById(R.id.detail_expire_date);
				Date date = new Date(millSecond);
				text.setText(String.format("%04d.%02d.%02d %02d:%02d",
						date.getYear()+1900, date.getMonth()+1, date.getDate(), date.getHours(), date.getMinutes()));
				findViewById(R.id.detail_expire_date_layer).setVisibility(View.VISIBLE);
			}
			else {
				findViewById(R.id.detail_expire_date_layer).setVisibility(View.GONE);
			}

			if(mKollusContent.isContentExpirated()) {
				text.setTextColor(Color.RED);
				text.setPaintFlags(text.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
			}
			else {
				text.setTextColor(Color.WHITE);
				text.setPaintFlags(text.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
			}

			if(mKollusContent.getTotalExpirationCount() > 0) {
				text = (TextView)findViewById(R.id.detail_expire_count);
				text.setText(String.format("%d(%d)", mKollusContent.getExpirationCount(),
						mKollusContent.getTotalExpirationCount()));
				findViewById(R.id.detail_expire_count_layer).setVisibility(View.VISIBLE);
			}
			else  {
				findViewById(R.id.detail_expire_count_layer).setVisibility(View.GONE);
			}

			if(mKollusContent.getTotalExpirationPlaytime() > 0) {
				text = (TextView)findViewById(R.id.detail_expire_playtime);
				Resources r = getResources();
				String dayString = r.getString(R.string.day);
				String hourString = r.getString(R.string.hours);
				String minString = r.getString(R.string.min);
				String secString = r.getString(R.string.sec);

				text.setText(String.format("%s(%s)",
						Utils.stringForTime(dayString, hourString, minString, secString, mKollusContent.getExpirationPlaytime()*1000),
						Utils.stringForTime(dayString, hourString, minString, secString, mKollusContent.getTotalExpirationPlaytime()*1000)));
				findViewById(R.id.detail_expire_playtime_layer).setVisibility(View.VISIBLE);
			}
			else  {
				findViewById(R.id.detail_expire_playtime_layer).setVisibility(View.GONE);
			}

			if(mKollusContent.isContentExpirated()) {
				text.setTextColor(Color.RED);
				text.setPaintFlags(text.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
			}
			else {
				text.setTextColor(Color.WHITE);
				text.setPaintFlags(text.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
			}

			boolean bExpired = false;
			long currentDate = System.currentTimeMillis()/1000;
	        if(mKollusContent.getTotalExpirationCount() > 0 && mKollusContent.getExpirationCount() <= 0)
	        	bExpired = true;

	        if(mKollusContent.getTotalExpirationPlaytime() > 0 && mKollusContent.getExpirationPlaytime() <= 0)
	        	bExpired = true;

	        if(mKollusContent.getExpirationDate() > 0 && currentDate > mKollusContent.getExpirationDate())
	        	bExpired = true;

			if(mKollusContent.isContentExpirated() || bExpired) {
				playImage.setVisibility(View.GONE);
				hangImage.setVisibility(View.VISIBLE);
			}
			else {
				if(!mKollusContent.isCompleted()) {
		        	playImage.setVisibility(View.GONE);
		        }
				else {
					playImage.setVisibility(View.VISIBLE);
				}
				hangImage.setVisibility(View.GONE);
			}
		}
		
		String detailInfoUrl = mKollusContent.getDetailInfoUrl();
		if(detailInfoUrl != null && detailInfoUrl.startsWith("http://")) {
			ConnectivityManager cManager; 
    		NetworkInfo mobile, wifi; 
    		 
    		cManager=(ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE); 
    		wifi = cManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI); 
    		mobile = cManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE); 
    		
    		if((wifi != null && wifi.isConnected()) || (mobile != null && mobile.isConnected())) {
    			WebView detailWebView = (WebView)findViewById(R.id.detail_info_web);
    			detailWebView.loadUrl(detailInfoUrl);
    			detailWebView.setVisibility(View.VISIBLE);
    		}
    		else {
    			text = (TextView)findViewById(R.id.no_detail);
    			text.setVisibility(View.VISIBLE);
    		}
		}
		else {
			text = (TextView)findViewById(R.id.no_detail);
			text.setVisibility(View.VISIBLE);
		}
	}

	private void startVideo() {
		Intent i = new Intent(KollusContentDetail.this, MovieActivity.class);
		i.putExtra(getResources().getString(R.string.media_content_key), mKollusContent.getMediaContentKey());
		i.putExtra(getResources().getString(R.string.disk_index),
				getIntent().getIntExtra(getResources().getString(R.string.disk_index), 0));
		startActivity(i);
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		if(v.getId() == R.id.detail_thumbnail) {
			boolean bExpired = false;
	        long currentDate = System.currentTimeMillis()/1000;
	        if(mKollusContent.getTotalExpirationCount() > 0 && mKollusContent.getExpirationCount() <= 0)
	        	bExpired = true;		        
	        if(mKollusContent.getExpirationDate() > 0 && currentDate > mKollusContent.getExpirationDate())
	        	bExpired = true;
	        
	        if(mKollusContent.getTotalExpirationPlaytime() > 0 && mKollusContent.getExpirationPlaytime() <= 0)
	        	bExpired = true;
	        
			if(bExpired && mKollusContent.getExpirationRefreshPopup()) {
				final int uriIndex = mKollusContent.getUriIndex();
				new KollusAlertDialog(KollusContentDetail.this).
		        setTitle(R.string.menu_info_str).
		        setMessage(R.string.download_drm_refresh).
		        setPositiveButton(R.string.confirm,
		                new DialogInterface.OnClickListener() {
		                    public void onClick(DialogInterface dialog, int whichButton) {
								startVideo();
		                    }
		                }).
		        setNegativeButton(R.string.cancel,
		                new DialogInterface.OnClickListener() {
		                    public void onClick(DialogInterface dialog, int whichButton) {
		                    }
		                }).
		        show();
			}
			else {
				startVideo();
			}
		}
	}
	
	@Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    	if( event.getAction() == KeyEvent.ACTION_DOWN ){
			if( keyCode == KeyEvent.KEYCODE_BACK ){
				setResult(RESULT_OK, getIntent());
			}
		}
    	
        return super.onKeyDown(keyCode, event);
    }

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		// TODO Auto-generated method stub
		super.onWindowFocusChanged(hasFocus);
		
//		if(hasFocus) {
//			ImageView image = (ImageView)findViewById(R.id.detail_thumbnail);
//			int width = mThumbnail.getWidth();
//			int height = mThumbnail.getHeight();
//
//			ViewGroup.LayoutParams params = image.getLayoutParams();
//			params.width = image.getWidth();
//			params.height = image.getWidth()*height/width;
//			image.setLayoutParams(params);
//		}
	}

	//etlim 20170902 Activity Exit ==> Broadcast Event
	@Override
	protected void onDestroy() {
		unregisterReceiver(mKollusContentDetailActivityBR);
		super.onDestroy();
		mActivityStack.regOnDestroyState(this);
	}

	private void KollusContentDetailActivityBroadcastRegister() {
		mKollusContentDetailActivityBR = new KollusContentDetailActivityBroadcastReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(ACTION_ACTIVITY_FINISH_KOLLUS_CONTENT_DETAIL);
		registerReceiver(mKollusContentDetailActivityBR, filter);
	}

	private class KollusContentDetailActivityBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context ctx, Intent intent) {
			Log.d(TAG, "onReceive >>> " + intent.getAction());
			String action = intent.getAction();

			if (action.equals(ACTION_ACTIVITY_FINISH_KOLLUS_CONTENT_DETAIL)) {
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

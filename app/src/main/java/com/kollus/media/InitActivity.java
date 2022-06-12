package com.kollus.media;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.Window;

import com.kollus.media.preference.KollusConstants;
import com.kollus.media.util.ActivityStack;
import com.kollus.media.util.DiskUtil;
import com.kollus.media.view.KollusAlertDialog;
import com.kollus.sdk.media.util.CpuInfo;
import com.kollus.sdk.media.util.ErrorCodes;
import com.kollus.sdk.media.util.Log;
import com.kollus.sdk.media.util.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;

public class InitActivity extends BaseActivity{
	private static final String TAG = InitActivity.class.getSimpleName();
	private static final int PERMISSION_REQUEST_STORAGE = 1;
	private static final int CODEC_MAJOR_VERSION = 1;
	private static final int CODEC_MINOR_VERSION = 2;

	private Uri mUri;

	//etlim 20170902 Activity Exit ==> Broadcast Event
    private String ACTION_ACTIVITY_FINISH_MOVIE = "com.kollus.media.action.activity.finish.movie";
	private String ACTION_ACTIVITY_FINISH_HISTORY = "com.kollus.media.action.activity.finish.history";
	private String ACTION_ACTIVITY_FINISH_KOLLUS_CONTENT_DETAIL = "com.kollus.media.action.activity.finish.kollus.content.detail";
	private String ACTION_ACTIVITY_FINISH_PLAYER_PREFERENCE = "com.kollus.media.action.activity.finish.player.preference";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		Log.i(TAG, "onCreate");
		super.onCreate(savedInstanceState);

		//NetworkOnMainThreadException
		if(Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD) {
			StrictMode.ThreadPolicy policy
					= new StrictMode.ThreadPolicy.Builder().permitAll().build();
			StrictMode.setThreadPolicy(policy);
		}

		setContentView(R.layout.init_layout);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			checkPermission();
		} else {
			if (init())
				handleIntent();
		}
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		// TODO Auto-generated method stub
		super.onWindowFocusChanged(hasFocus);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		Log.i(TAG, "onRestoreInstanceState");
		super.onRestoreInstanceState(savedInstanceState);
	}

	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		Log.i(TAG, "onStart");
		super.onStart();
	}

	@Override
	protected void onRestart() {
		// TODO Auto-generated method stub
		Log.i(TAG, "onRestart");
		super.onRestart();
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		Log.i(TAG, "onResume");
		super.onResume();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// TODO Auto-generated method stub
		Log.i(TAG, "onSaveInstanceState");
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		Log.i(TAG, "onPause");
		super.onPause();
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		Log.i(TAG, "onStop");
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		Log.i(TAG, "onDestroy");
		super.onDestroy();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		// TODO Auto-generated method stub
		Log.i(TAG, "onNewIntent");
		super.onNewIntent(intent);
		setIntent(intent);

		handleIntent();
	}

	@Override
	public void startActivity(Intent intent) {
		// TODO Auto-generated method stub
        super.startActivity(intent);
		finish();
	}

	@TargetApi(Build.VERSION_CODES.M)
	private void checkPermission() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//			if (!Settings.System.canWrite(this)) {
//				Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS,
//						Uri.parse("package:" + this.getPackageName()));
////				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//				startActivity(intent);
//				return;
//			}

			//int permissionReadStorage = checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
			int permissionWriteStorage = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
			if(/*permissionReadStorage == PackageManager.PERMISSION_DENIED || */
					permissionWriteStorage == PackageManager.PERMISSION_DENIED) {
				requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
						PERMISSION_REQUEST_STORAGE);
			}
			else {
				if(init())
					handleIntent();
			}
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		Log.d(TAG, String.format("onRequestPermissionsResult requesetCode %d permission length %d", requestCode, permissions.length));
		switch (requestCode) {
			case PERMISSION_REQUEST_STORAGE:
				for (int i = 0; i < permissions.length; i++) {
					String permission = permissions[i];
					int grantResult = grantResults[i];
					if (grantResult == PackageManager.PERMISSION_GRANTED) {
						Log.d(TAG, "onRequestPermissionsResult >>> PERMISSION_REQUEST_STORAGE >>> GRANTED");
						initStorage();

						if(init())
							handleIntent();
						else
							Log.w(TAG, "Init Fail");
					}
					else {
						Log.d(TAG, "onRequestPermissionsResult >>> PERMISSION_REQUEST_STORAGE >>> DENIED");
						finish();
					}
				}
				break;
		}
	}

	private boolean init() {
		if(KollusConstants.SECURE_MODE && Utils.isRooting()) {
        	new KollusAlertDialog(this)
            .setTitle(R.string.error_title)
            .setMessage(R.string.error_rooting)
            .setPositiveButton(R.string.confirm,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            /* If we get here, there is no onError listener, so
                             * at least inform them that the video is over.
                             */
                            finish();
                        }
                    })
            .setCancelable(false)
            .show();

        	return false;
        }

    	if(!checkSupportDevice()) {
    		new KollusAlertDialog(this)
            .setTitle(R.string.error_title)
            .setMessage(ErrorCodes.getInstance(InitActivity.this).getErrorString(ErrorCodes.ERROR_UNSUPPORTED_DEVICE))
            .setPositiveButton(R.string.confirm,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            /* If we get here, there is no onError listener, so
                             * at least inform them that the video is over.
                             */
                            finish();
                        }
                    })
            .setCancelable(false)
            .show();

    		return false;
    	}

		return true;
	}

	private boolean checkVersion() {
		final String updatePackageName = getUpdatePackageName(mMultiStorage.checkVersion());
		if(updatePackageName != null) {
			Log.d(TAG, "updatePackageName:"+updatePackageName);
			boolean bUpdate = true;
			if(!updatePackageName.equals(getPackageName())) {
				try {
					getInstallCodecVersion();
				} catch (NameNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					bUpdate = false;
				}
			}

			new KollusAlertDialog(this)
            .setTitle(R.string.menu_info_str)
            .setMessage(R.string.notify_move_market_for_update)
            .setPositiveButton(R.string.confirm,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            /* If we get here, there is no onError listener, so
                             * at least inform them that the video is over.
                             */
                        	startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + updatePackageName)));
                        }
                    })
            .setCancelable(false)
            .show();

			return false;
		}

		return true;
	}

	private boolean notify3GLTE() {
		SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(this);
		boolean notifyNoWifi = preference.getBoolean(getResources().getString(R.string.preference_notify_no_wifi_key), true);
		ConnectivityManager cManager;
		NetworkInfo wifi;
		NetworkInfo mobile;

		cManager=(ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
		wifi = cManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		mobile = cManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

		if(notifyNoWifi && (!wifi.isConnected() && (mobile != null && mobile.isConnected())))
			return true;

		return false;
	}

	private void handleIntent() {
		int disIndex = DiskUtil.getDiskIndex(this);
		if(!mMultiStorage.isReady(disIndex)) {
			Log.w(TAG, disIndex+"th StorageManager Not Ready");
			new KollusAlertDialog(this)
					.setTitle(R.string.error_title)
					.setMessage(ErrorCodes.getInstance(InitActivity.this).getErrorString(ErrorCodes.ERROR_WRITE_FILE))
					.setPositiveButton(R.string.confirm,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) {
									/* If we get here, there is no onError listener, so
									 * at least inform them that the video is over.
									 */
									finish();
								}
							})
					.setCancelable(false)
					.show();
			return;
		}

		String schema = null;
    	String host = null;
		mUri = getIntent().getData();
		if(mUri != null)
			mUri = Uri.parse(Uri.decode(mUri.toString()));

		Log.d(TAG, "handleIntent uri:"+mUri);
		if(mUri != null) {
	    	schema = mUri.getScheme();
	    	host = mUri.getHost();
		}

		if("kollus".equalsIgnoreCase(schema)) {
			if("list".equalsIgnoreCase(host)) {
				//etlim 20170902 Activity Exit ==> Broadcast Event
                ActivityStackFinish(ACTION_ACTIVITY_FINISH_HISTORY);
                Intent intent = new Intent(InitActivity.this, HistoryActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    			intent.setData(mUri);
	    		Log.i(TAG, "start Default History Activity");
				startActivity(intent);
	    		return;
    		}

    		if(notify3GLTE()) {
    			int messageId = 0;
    			if("download".equalsIgnoreCase(host))
    				messageId = R.string.notify_no_wifi_download;
    			else
    				messageId = R.string.notify_no_wifi_play;

	    		new KollusAlertDialog(this)
	            .setTitle(R.string.menu_info_str)
	            .setMessage(messageId)
	            .setPositiveButton(R.string.confirm,
	                    new DialogInterface.OnClickListener() {
	                        public void onClick(DialogInterface dialog, int whichButton) {
	                            downloadOrPlay(mUri);
	                        }
	                    })
	            .setNegativeButton(R.string.cancel,
	                    new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
						finish();
                    }
                })
	            .show();
    		}
    		else
    			downloadOrPlay(mUri);
    	}
		else {
			//etlim 20170902 Activity Exit ==> Broadcast Event
			if(isActivityRunning(MovieActivity.class)) {
				finish();
			}
			else {
				ActivityStackFinish(ACTION_ACTIVITY_FINISH_HISTORY);
				Intent newIntent = new Intent(InitActivity.this, HistoryActivity.class);
				newIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
				startActivity(newIntent);
			}
		}
	}

	private Boolean isActivityRunning(Class activityClass) {
		Log.d(TAG, "isActivityRunning className "+activityClass.getCanonicalName());
//		ActivityManager activityManager = (ActivityManager) getBaseContext().getSystemService(Context.ACTIVITY_SERVICE);
//		List<ActivityManager.RunningTaskInfo> tasks = activityManager.getRunningTasks(Integer.MAX_VALUE);
//
//		for (ActivityManager.RunningTaskInfo task : tasks) {
//			Log.d(TAG, "isActivityRunning taskName "+task.baseActivity.getClassName());
//			if (activityClass.getCanonicalName().equalsIgnoreCase(task.baseActivity.getClassName()))
//				return true;
//		}
		ActivityStack stack = ActivityStack.getInstance();
		String[] ids = stack.getAliveIDs();
		for(String id : ids) {
			if (activityClass.getCanonicalName().contains(stack.getActivity(id).getLocalClassName()))
				return true;
		}


		return false;
	}

	private String getUpdatePackageName(String playerInfo) {
		if(playerInfo != null) {
//			String jsonStr = new String(Base64.decode(playerInfo, Base64.DEFAULT));
			Log.d(TAG, "getUpdatePackageName json:"+playerInfo);
			try {
				JSONObject json = new JSONObject(playerInfo);

				if(json.getInt("error") != 0)
					return getUpdateCodecPakageName();

				JSONObject android = json.getJSONObject("result").getJSONObject("kollus_player_mobile_android");
				String serverPlayerVersion = android.getString("version");
				String serverCodecVersion = android.getJSONObject("codec").getString("version");

				int serverPlayerMajorVersion = Integer.parseInt(
						serverPlayerVersion.substring(0, serverPlayerVersion.indexOf('.')));
				int serverPlayerMinorVersion = Integer.parseInt(
						serverPlayerVersion.substring(serverPlayerVersion.indexOf('.')+1, serverPlayerVersion.lastIndexOf('.')));
				int serverPlayerMicroVersion = Integer.parseInt(
						serverPlayerVersion.substring(serverPlayerVersion.lastIndexOf('.')+1));

				int serverCodecMajorVersion = Integer.parseInt(
						serverCodecVersion.substring(0, serverCodecVersion.indexOf('.')));
				int serverCodecMinorVersion = Integer.parseInt(
						serverCodecVersion.substring(serverCodecVersion.indexOf('.')+1));

				//Check Player Version
				try {
					String curPlayerVersion = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
					Log.d(TAG, String.format("getUpdatePackageName player install version '%s'", curPlayerVersion));

					int curPlayerMajorVersion = Integer.parseInt(
							curPlayerVersion.substring(0, curPlayerVersion.indexOf('.')));
					int curPlayerMinorVersion = Integer.parseInt(
							curPlayerVersion.substring(curPlayerVersion.indexOf('.')+1, curPlayerVersion.lastIndexOf('.')));
					int curPlayerMicroVersion = Integer.parseInt(
							curPlayerVersion.substring(curPlayerVersion.lastIndexOf('.')+1));

					int nCurPlayerVersion = curPlayerMajorVersion*1000000+curPlayerMinorVersion*1000+curPlayerMicroVersion;
					int nNeedPlayerVersion = serverPlayerMajorVersion*1000000+serverPlayerMinorVersion*1000+serverPlayerMicroVersion;

					if(nCurPlayerVersion < nNeedPlayerVersion)
						return getPackageName();
				} catch (NameNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				//Chceck Codec Version
				String codecPackageName = getCodecPackageName();
				try {
					String curCodecVersion = getInstallCodecVersion();
					int curCodecMajorVersion = Integer.parseInt(
							curCodecVersion.substring(0, curCodecVersion.indexOf('.')));
					int curCodecMinorVersion = Integer.parseInt(
							curCodecVersion.substring(curCodecVersion.indexOf('.')+1));

					int nCurCodecVersion = curCodecMajorVersion*1000+curCodecMinorVersion;
					int nNeedCodecVersion = serverCodecMajorVersion*1000+serverCodecMinorVersion;

					if(nCurCodecVersion < nNeedCodecVersion) {
						Log.w(TAG, String.format("getUpdatePackageName codec install name '%s' version current %d.%d need %d.%d",
								codecPackageName, curCodecMajorVersion, curCodecMinorVersion,
								serverCodecMajorVersion, serverCodecMinorVersion));
						return codecPackageName;
					}
				} catch (NameNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return codecPackageName;
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return getUpdateCodecPakageName();
			}
			return null;
		}
		else {
			return getUpdateCodecPakageName();
		}
	}

	private String getCodecPackageName() {
		CpuInfo cpuInfo = CpuInfo.getInstance();
		String cpuName = cpuInfo.getCpuName().toLowerCase();
		Log.d(TAG, "getCodecPackageName CPU Name " + cpuName);

		if(cpuName.indexOf(' ') > 0)
			cpuName = cpuName.substring(0, cpuName.indexOf(' '));

		String codecPackageName = null;
		if(cpuName.compareTo("x86") == 0) {
			codecPackageName = "com.kollus.ffmpeg.x86";
		}
		else if(cpuName.compareTo("x86_64") == 0) {
			codecPackageName = "com.kollus.ffmpeg.x86_64";
		}
		else if(cpuName.compareTo("arm64") == 0) {
			codecPackageName = "com.kollus.ffmpeg.aarch64";
		}
		else if(cpuName.compareTo("arm") == 0) {
			if(cpuInfo.hasFeature("neon"))
				codecPackageName = "com.kollus.ffmpeg.v7.neon";
			else if(cpuInfo.hasFeature("vfp"))
				codecPackageName = "com.kollus.ffmpeg.v6.vfp";
			else
				codecPackageName = "com.kollus.ffmpeg.v6";
		}
		Log.d(TAG, "getCodecPackageName " + codecPackageName);

		return codecPackageName;
	}

	private String getInstallCodecVersion() throws NameNotFoundException {
		String codecPackageName = getCodecPackageName();
		try {
			PackageInfo info = getPackageManager().getPackageInfo(codecPackageName, 0);
			return info.versionName;
		}
		catch (NameNotFoundException e) {
			throw e;
		}
	}

	private String getUpdateCodecPakageName() {

		boolean bNeedUpdate = true;
		try {
			String ffmpegVersion = getInstallCodecVersion();
			int curCodecMajorVersion = Integer.parseInt(
					ffmpegVersion.substring(0, ffmpegVersion.indexOf('.')));
			int curCodecMinorVersion = Integer.parseInt(
					ffmpegVersion.substring(ffmpegVersion.indexOf('.')+1));
			int curCodecVersion = curCodecMajorVersion*1000+curCodecMinorVersion;
			int needCodecVersion = CODEC_MAJOR_VERSION*1000+CODEC_MINOR_VERSION;

			if(curCodecVersion >= needCodecVersion) {
				bNeedUpdate = false;
			}
			else {
				Log.w(TAG, String.format("getUpdateCodecPakageName version current %d.%d need %d.%d",
						curCodecMajorVersion, curCodecMinorVersion,
						CODEC_MAJOR_VERSION, CODEC_MINOR_VERSION));
			}
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			bNeedUpdate = false;
		}

		if(!bNeedUpdate)
			return getCodecPackageName();
		else
			return null;
	}

	private void downloadOrPlay(Uri uri) {
		String host = uri.getHost();

		if("download".equalsIgnoreCase(host)) {
			//etlim 20170902 Activity Exit ==> Broadcast Event
			ActivityStackFinish(ACTION_ACTIVITY_FINISH_HISTORY);
			Intent intent = new Intent(this, HistoryActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    		intent.setData(uri);
    		Log.i(TAG, "start History Activity for Download : " + uri);
			startActivity(intent);
		}
		else if("download_play".equalsIgnoreCase(host)) {
			String sUri = uri.toString();
			sUri = sUri.substring(sUri.indexOf("url=") + 4);

            //etlim 20170902 Activity Exit ==> Broadcast Event
            ActivityStackFinish(ACTION_ACTIVITY_FINISH_MOVIE);
            Intent intent = new Intent(this, MovieActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    		intent.putExtra("download_play", sUri);
			startActivity(intent);
		}
		else if ("path".equalsIgnoreCase(host)) {
            //etlim 20170902 Activity Exit ==> Broadcast Event
            ActivityStackFinish(ACTION_ACTIVITY_FINISH_MOVIE);
            Intent intent = new Intent(this, MovieActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
			intent.setData(uri);
    		Log.i(TAG, "start Play Activity");
			startActivity(intent);
		}
	}

	private boolean checkSupportDevice() {
		String cpuName = CpuInfo.getInstance().getCpuName().toLowerCase();
		if(cpuName.indexOf(' ') > 0)
			cpuName = cpuName.substring(0, cpuName.indexOf(' '));

		if(cpuName.compareTo("x86") == 0) {
			if(android.os.Build.VERSION.SDK_INT > 27) { /*Build.VERSION_CODES.O*/
				Log.e(TAG, "x86 device over 8.1");
//				return false;
			}
		}
		else if(cpuName.compareTo("x86_64") == 0) {
			if(android.os.Build.VERSION.SDK_INT > 27) { /*Build.VERSION_CODES.O*/
				Log.e(TAG, "x86_64 device over 8.1");
//				return false;
			}
		}
		else if(cpuName.compareTo("arm64") == 0) {
			if(android.os.Build.VERSION.SDK_INT > 27) { /*Build.VERSION_CODES.OREO_MR2*/
				Log.e(TAG, "arm64 device over 8.1");
//				return false;
			}
		}
		else if(cpuName.compareTo("arm") == 0) {
			if(android.os.Build.VERSION.SDK_INT > 27) { /*Build.VERSION_CODES.O*/
				Log.e(TAG, "arm device over 8.1");
//				return false;
			}
		}
		else {
			Log.e(TAG, "unsupported device:"+cpuName);
			return false;
		}

		return true;
	}

	//etlim 20170902 Activity Exit ==> Broadcast Event
    private void ActivityStackFinish(String StartActivityBroadcastActionName) {
        Log.d(TAG, "ActivityStackFinish "+StartActivityBroadcastActionName);
        if (!StartActivityBroadcastActionName.equals(ACTION_ACTIVITY_FINISH_HISTORY)) {
            try {
                Intent intent = new Intent();
                intent.setAction(ACTION_ACTIVITY_FINISH_HISTORY);
                sendBroadcast(intent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (!StartActivityBroadcastActionName.equals(ACTION_ACTIVITY_FINISH_KOLLUS_CONTENT_DETAIL)) {
            try {
                Intent intent = new Intent();
                intent.setAction(ACTION_ACTIVITY_FINISH_KOLLUS_CONTENT_DETAIL);
                sendBroadcast(intent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (!StartActivityBroadcastActionName.equals(ACTION_ACTIVITY_FINISH_PLAYER_PREFERENCE)) {
            try {
                Intent intent = new Intent();
                intent.setAction(ACTION_ACTIVITY_FINISH_PLAYER_PREFERENCE);
                sendBroadcast(intent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (!StartActivityBroadcastActionName.equals(ACTION_ACTIVITY_FINISH_MOVIE)) {
            try {
                Intent intent = new Intent();
                intent.setAction(ACTION_ACTIVITY_FINISH_MOVIE);
                sendBroadcast(intent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

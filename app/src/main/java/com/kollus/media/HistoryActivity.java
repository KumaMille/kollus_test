package com.kollus.media;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.AnimationDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.PopupMenu;
import androidx.mediarouter.app.MediaRouteButton;
import androidx.mediarouter.media.MediaRouteSelector;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.CastState;
import com.google.android.gms.cast.framework.CastStateListener;
import com.google.android.gms.cast.framework.SessionManagerListener;
import com.kollus.media.contents.KollusContentAdapter;
import com.kollus.media.contents.KollusContentAdapter.OnItemListener;
import com.kollus.media.contents.KollusContentDetail;
import com.kollus.media.contents.MultiKollusContent;
import com.kollus.media.contents.MultiKollusStorage;
import com.kollus.media.download.DownloadAdapter;
import com.kollus.media.download.DownloadAdapter.OnDownloadCancelListener;
import com.kollus.media.download.DownloadDRM;
import com.kollus.media.download.DownloadInfo;
import com.kollus.media.download.DownloadService;
import com.kollus.media.preference.KollusConstants;
import com.kollus.media.preference.PlayerPreference;
import com.kollus.media.preference.SortPreference;
import com.kollus.media.util.ActivityStack;
import com.kollus.media.util.DiskUtil;
import com.kollus.media.util.PropertyUtil;
import com.kollus.media.view.KollusAlertDialog;
import com.kollus.sdk.media.KollusPlayerDRMListener;
import com.kollus.sdk.media.KollusStorage;
import com.kollus.sdk.media.content.FileManager;
import com.kollus.sdk.media.content.KollusContent;
import com.kollus.sdk.media.util.ErrorCodes;
import com.kollus.sdk.media.util.KollusUri;
import com.kollus.sdk.media.util.Log;
import com.kollus.sdk.media.util.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

public class HistoryActivity extends BaseActivity {
	private static final String TAG = HistoryActivity.class.getSimpleName();
	//private static final int DATA_CHANGED = 100;
	private static final int DATA_DOWNLOAD = 101;
	private static final int DATA_DOWNLOAD_CANCEL = 102;
	private static final int CHECK_EXIT = 103;

	private static final int MENU_NONE = 0;
//	private static final int MENU_PLAY	= 1;
	private static final int MENU_CUT = 2;
	private static final int MENU_PASTE = 3;
	private static final int MENU_DELETE = 4;
	private static final int MENU_CANCEL = 5;
	private static final int MENU_RENAME = 6;

	private static final int MESSAGE_DELAY_MS = 200;

	private static final int CREATE_FOLDER_DIALOG = 1;
	private static final int RENAME_DIALOG = 2;
	private static final int DOWNLOAD_DIALOG = 3;

	private TextView mHistoryTitle;

	private RecyclerView mListView;
	private GridLayoutManager mLayoutManager;
	private MyListItemListener mMyListItemListener;
	private KollusContentAdapter mAdapter;
	private ArrayList<MultiKollusContent> mContentsList;
	private Intent mIntent;

	private KollusAlertDialog mDownloadDialog;
	private ViewGroup mDownloadView;
	private RecyclerView mDownloadListView;
	private View mDownloadInitLayer;
	private TextView mDownloadFileView;
	private TextView mDownloadTimeView;
	private DownloadAdapter mDownloadAdapter;
	private ArrayList<MultiKollusContent> mDownloadList;

	private FileManager mFileManager;
	private FileManager mCurFileManager;
	private FileManager mCopyModeFileManager;
	private Vector<FileManager> mDirectoryList;
	private Vector<FileManager> mSelectFileList;

	private KollusAlertDialog mAlertMessage;

	private ArrayList<String> mDownloadUrlList = new ArrayList<String>();
	private int mDownloadIndex;
	private int mDownloadCompleteCount;
	private int mTotalDownloadCount;
	private long mDownloadingFileSize;
	private long mDownloadedFileSize;

	private int mCopyMode;
	private ImageView mBtnSetting;
	private ImageView mBtnAddFolder;
	private ImageView mBtnEditMenu;
	private ImageView mBtnMoreMenu;

	private int mSortType;
	private boolean mSortAscent;
	private SharedPreferences mPreference;
	private Toast mRepressForFinish;
	private boolean mExit;
	private final long EXIT_TIME = 1500;

	private final int DOWNLOAD_CALC_TIME = 10000;
	private final int DOWNLOAD_CHECK_TIME = 1000;
	private long mDownloadBitrate;
	private long mDownloadCheckTime;
	private ActivityStack mActivityStack;
	//etlim 20170902 Activity Exit ==> Broadcast Event
	private String ACTION_ACTIVITY_FINISH_HISTORY = "com.kollus.media.action.activity.finish.history";
	private HistoryActivityBroadcastReceiver mHistoryActivityBR;

	private MultiKollusStorage.SDCardStateChangeListener mSDCardStateChangeListener = new MultiKollusStorage.SDCardStateChangeListener() {
		@Override
		public void onStateChaged(String path, boolean mounted) {
			Log.d(TAG, String.format("%s Mounted %b", path, mounted));
			refreshContentList();
		}
	};

	private Runnable mDownloadChecker = new Runnable() {
		@Override
		public void run() {
			// TODO Auto-generated method stub
			long currentTimeMillis = System.currentTimeMillis();
			if (mDownloadBitrate == 0 || (currentTimeMillis - mDownloadCheckTime) >= DOWNLOAD_CALC_TIME) {
				mDownloadBitrate = (long) (mDownloadedFileSize / ((currentTimeMillis - mDownloadCheckTime) / 1000.));
				mDownloadCheckTime = currentTimeMillis;
				mDownloadedFileSize = 0;
			}
			if (mDownloadBitrate > 0) {
				long remainTime = mDownloadingFileSize / mDownloadBitrate * 1000;
				mDownloadTimeView.setText(String.format(getResources().getString(R.string.download_time),
						Utils.stringForTimeHMMSS((int) remainTime)));
			}

			mHandler.postDelayed(mDownloadChecker, DOWNLOAD_CHECK_TIME);
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		Log.i(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.history_layout);

		mActivityStack = ActivityStack.getInstance();
		mActivityStack.regOnCreateState(this);
		//etlim 20170902 Activity Exit ==> Broadcast Event
		HistoryActivityEventBroadcastRegister();

		mPreference = PreferenceManager.getDefaultSharedPreferences(this);
		mRepressForFinish = Toast.makeText(this, R.string.repress_backkey_for_finish, Toast.LENGTH_SHORT);
		mAlertMessage = new KollusAlertDialog(this);

		mBtnSetting = (ImageView) findViewById(R.id.btn_setting);
		mBtnSetting.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				// TODO Auto-generated method stub
				Intent intent = new Intent();
				intent.setClass(HistoryActivity.this, PlayerPreference.class);
				startActivity(intent);
			}
		});

		mBtnAddFolder = (ImageView) findViewById(R.id.btn_add_folder);
		mBtnAddFolder.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				// TODO Auto-generated method stub
				createDialog(CREATE_FOLDER_DIALOG);
			}
		});

		mBtnEditMenu = (ImageView) findViewById(R.id.btn_edit_menu);
		mBtnEditMenu.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				// TODO Auto-generated method stub
				mBtnSetting.setVisibility(View.GONE);
				mBtnAddFolder.setVisibility(View.GONE);
				mBtnEditMenu.setVisibility(View.GONE);
				mBtnMoreMenu.setVisibility(View.VISIBLE);
				mAdapter.setSelectMode(KollusContentAdapter.MODE_SELECT);
			}
		});

		mBtnMoreMenu = (ImageView) findViewById(R.id.btn_more_menu);
		mBtnMoreMenu.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				// TODO Auto-generated method stub
				PopupMenu popup = new PopupMenu(HistoryActivity.this, mBtnMoreMenu);
				Menu menu = popup.getMenu();
				if(!mSelectFileList.isEmpty()) {
					if (mCopyMode == MENU_CUT) {
						menu.add(Menu.NONE, MENU_PASTE, Menu.NONE, R.string.paste);
					} else {
//						menu.add(Menu.NONE, MENU_PLAY, Menu.NONE, R.string.play);
						menu.add(Menu.NONE, MENU_CUT, Menu.NONE, R.string.cut);
					}

					if (mCopyModeFileManager == null || mCurFileManager == mCopyModeFileManager) {
						menu.add(Menu.NONE, MENU_DELETE, Menu.NONE, R.string.delete);
						if (mSelectFileList.size() == 1 && mSelectFileList.get(0).getType() == FileManager.DIRECTORY)
							menu.add(Menu.NONE, MENU_RENAME, Menu.NONE, R.string.rename);
					}
				}

				menu.add(Menu.NONE, MENU_CANCEL, Menu.NONE, R.string.cancel);

				for (int i = 0; i < menu.size(); i++) {
					MenuItem item = menu.getItem(i);
					switch (item.getItemId()) {
//						case MENU_PLAY:
//							item.setIcon(R.drawable.ic_menu_cut);
//							break;
						case MENU_CUT:
							item.setIcon(R.drawable.ic_menu_cut);
							break;
						case MENU_PASTE:
							item.setIcon(R.drawable.ic_menu_paste);
							break;
						case MENU_DELETE:
							item.setIcon(R.drawable.ic_menu_delete);
							break;
						case MENU_CANCEL:
							item.setIcon(R.drawable.ic_menu_cancel);
							break;
						case MENU_RENAME:
							item.setIcon(R.drawable.ic_menu_edit);
							break;
					}
				}

				popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
					public boolean onMenuItemClick(MenuItem item) {
						int itemId = item.getItemId();

//		        		if(itemId == MENU_PLAY) {
//							ArrayList<PlayInfo> mckList = new ArrayList<PlayInfo>();
//							for (FileManager file : mSelectFileList) {
//								if (file.getType() == FileManager.DIRECTORY) {
//									Vector<String> keyList = new Vector<String>();
//									file.findAllFile(keyList);
//									for(String key : keyList) {
//										MultiKollusContent content = findKollusContent(key);
//										PlayInfo info = new PlayInfo();
//										info.mck = content.getKollusContent().getMediaContentKey();
//										info.disIndex = content.getDiskIndex();
//										mckList.add(info);
//									}
//								}
//								else {
//									MultiKollusContent content = findKollusContent(file.getKey());
//									PlayInfo info = new PlayInfo();
//									info.mck = content.getKollusContent().getMediaContentKey();
//									info.disIndex = content.getDiskIndex();
//									mckList.add(info);
//								}
//							}
//							cleanSelectList();
//							Intent i = new Intent(HistoryActivity.this, MovieActivity.class);
//							i.putExtra("mck_list", mckList);
//							startActivity(i);
//		        		}
//		        		else
						if (itemId == MENU_CUT) {
							mCopyMode = itemId;
							mCopyModeFileManager = mCurFileManager;
							mAdapter.setSelectMode(KollusContentAdapter.MODE_CUT);
						} else if (itemId == MENU_PASTE) {
							boolean isChild = false;
							for (FileManager file : mSelectFileList) {
								if (file.isChildDirectory(mCurFileManager)) {
									isChild = true;
									break;
								}
							}

							if (mCopyModeFileManager == mCurFileManager) {
								cleanSelectList();
								mAdapter.notifyDataSetChanged();
								return true;
							}

							if (isChild) {
								new KollusAlertDialog(HistoryActivity.this)
										.setTitle(R.string.error_title)
										.setMessage(R.string.error_msg_child)
										.setPositiveButton(R.string.VideoView_error_button,
												new DialogInterface.OnClickListener() {
													public void onClick(DialogInterface dialog, int whichButton) {
														cleanSelectList();
														mAdapter.notifyDataSetChanged();
													}
												})
										.setCancelable(false)
										.show();
								return true;
							}

							if (mCopyMode == MENU_CUT && mCopyModeFileManager != null) {
								mCopyModeFileManager.removeFiles(mSelectFileList);
							}
							mCopyModeFileManager = null;

							mCurFileManager.getFileList().addAll(mSelectFileList);

							cleanSelectList();
							saveFileManager();
							loadFileManager(false);

							mAdapter.notifyDataSetChanged();
						} else if (itemId == MENU_DELETE) {
							Vector<FileManager> deleteUrlList = new Vector<FileManager>();
							mCurFileManager.findAllFile(mSelectFileList, deleteUrlList);
							for (FileManager file : deleteUrlList) {
								MultiKollusContent info = findKollusContent(file);
								if (info != null //kssong 2020.02.26 -- Delete immediately because there is no copy.
                                        /*&& mFileManager.getFileCount(info.getKollusContent().getMediaContentKeyMD5()) == 1*/) {
									info.getKollusStorage().remove(info.getKollusContent().getMediaContentKey());
									mContentsList.remove(info);
								}
							}
							mCurFileManager.removeFiles(mSelectFileList);
							cleanSelectList();
							saveFileManager();
							loadFileManager(false);
							mAdapter.notifyDataSetChanged();
						} else if (itemId == MENU_CANCEL) {
							cleanSelectList();
							mAdapter.notifyDataSetChanged();
						} else if (itemId == MENU_RENAME) {
							createDialog(RENAME_DIALOG);
						}
						return true;
					}
				});

				enablePopupMenuIcon(popup);
				popup.show();
			}
		});

		if(KollusConstants.SUPPORT_MULTI_STORAGE)
			mContentsList = mMultiStorage.getDownloadList(-1);
		else
			mContentsList = mMultiStorage.getDownloadList(DiskUtil.getDiskIndex(this));

		mFileManager = new FileManager(FileManager.DIRECTORY);
		mFileManager.setName("/");

		mDirectoryList = new Vector<FileManager>();
		mDirectoryList.add(mFileManager);

		mHistoryTitle = (TextView) findViewById(R.id.history_title);
		setHistoryTitle();

		loadFileManager(true);
		configureFileManager();

		mSelectFileList = new Vector<FileManager>();
		mMyListItemListener = new MyListItemListener();

		mListView = (RecyclerView) findViewById(R.id.contents_list);
		mLayoutManager = new GridLayoutManager(this, 1);
		mListView.setLayoutManager(mLayoutManager);
		setLayoutManager();
//		mListView.addItemDecoration(new SimpleItemDecoration(this, 6));

		mCurFileManager = mFileManager;
		sort(mSortType, mSortAscent);
//		Arrays.sort(mCurFileManager.getFileList().toArray(), new SortFileManager(mSortType, mSortAscent));
		mAdapter = new KollusContentAdapter(this, mCurFileManager, mContentsList);
		mAdapter.setEmptyView(findViewById(R.id.no_list));
		mAdapter.setSelectedList(mSelectFileList);
		mAdapter.setOnItemListener(mMyListItemListener);

		mListView.setAdapter(mAdapter);
		mListView.setHasFixedSize(true);
		RecyclerView.ItemAnimator animator = mListView.getItemAnimator();
		if (animator instanceof SimpleItemAnimator) {
			((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
		}

		mDownloadView = (ViewGroup) LayoutInflater.from(this).inflate(R.layout.download_layout, null);
		mDownloadListView = (RecyclerView) mDownloadView.findViewById(R.id.contents_list);
		mDownloadListView.setLayoutManager(new LinearLayoutManager(this));
		mDownloadListView.setHasFixedSize(true);
		mDownloadInitLayer = mDownloadView.findViewById(R.id.download_init_layer);
		animator = mDownloadListView.getItemAnimator();
		if (animator instanceof SimpleItemAnimator) {
			((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
		}
//		mDownloadListView.addItemDecoration(new SimpleItemDecoration(this, 6));

		mDownloadFileView = (TextView) mDownloadView.findViewById(R.id.download_file);
		mDownloadTimeView = (TextView) mDownloadView.findViewById(R.id.download_time);
		mDownloadList = new ArrayList<MultiKollusContent>();
		mDownloadAdapter = new DownloadAdapter(this, mDownloadList, new OnDownloadCancelListener() {

			@Override
			public void onDownloadCancel(MultiKollusContent content) {
				// TODO Auto-generated method stub
				try {
					Log.d(TAG, String.format("onDownloadCancel InnerDisk %b MediaKey '%s'",
							mMultiStorage.isInnerDisk(content.getKollusStorage()), content.getKollusContent().getMediaContentKey()));
					mMessenger.send(Message.obtain(null, DownloadService.DOWNLOAD_CANCEL, 0, 0, content));
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		});
		mDownloadListView.setAdapter(mDownloadAdapter);
		mIntent = getIntent();
		mMultiStorage.registerSDStateChangeListener(mSDCardStateChangeListener);

		handleIntent();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// TODO Auto-generated method stub
		super.onConfigurationChanged(newConfig);
		setLayoutManager();
	}

	private void setLayoutManager() {
		Resources r = getResources();
		int portrait_width_pixel=Math.min(r.getDisplayMetrics().widthPixels, r.getDisplayMetrics().heightPixels);
		int dots_per_virtual_inch = r.getDisplayMetrics().densityDpi;
		float virutal_width_inch = portrait_width_pixel/dots_per_virtual_inch;

		Configuration config = r.getConfiguration();
		boolean isTablet = (virutal_width_inch > 2) ||
				((config.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE);

		int orientation = getResources().getConfiguration().orientation;
		if(orientation == Configuration.ORIENTATION_LANDSCAPE && isTablet)
			mLayoutManager.setSpanCount(2);
		else
			mLayoutManager.setSpanCount(1);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		// TODO Auto-generated method stub
		Log.d(TAG, "onNewIntent");
		super.onNewIntent(intent);
		setIntent(intent);
		mIntent = getIntent();
		if(mIntent.getData() != null)
			handleIntent();

		if (!mDownloadUrlList.isEmpty() && mBounded) {
			Message tmp = mHandler.obtainMessage(DATA_DOWNLOAD, mDownloadUrlList.get(0));
			mHandler.sendMessageDelayed(tmp, MESSAGE_DELAY_MS);
		}
	}

	private void handleIntent() {
		Log.d(TAG, "handleIntent");
		Uri uri = mIntent.getData();

		Log.d(TAG, "URL:" + uri);
		if (uri != null) {
			String schema = uri.getScheme();
			String host = uri.getHost();

			if ("kollus".equalsIgnoreCase(schema)) {
				if ("list".equalsIgnoreCase(host)) {
					KollusUri kUri = KollusUri.parse(uri.toString());
					configureContentListByName(Uri.decode(kUri.getQueryParameter("folder")));
				} else if ("download".equalsIgnoreCase(host)) {
					String[] tokens = uri.toString().split("url=");
					for (String token : tokens) {
//	        			token = Uri.decode(token);
						if (token.endsWith("&"))
							token = token.substring(0, token.length() - 1);
						Uri tmp = Uri.parse(token);
						if ("http".equalsIgnoreCase(tmp.getScheme()) || "https".equalsIgnoreCase(tmp.getScheme())) {
							int queryIndex = token.indexOf('?');
							if (queryIndex > 0)
								token += "&download";
							else
								token += "?download";

							if (mDownloadUrlList.contains(token)) {
								Log.w(TAG, "exist DownloadList " + token);
							} else {
								mDownloadUrlList.add(token);

								createDialog(DOWNLOAD_DIALOG);
								if (!DownloadService.isDownloading())
									mDownloadInitLayer.setVisibility(View.VISIBLE);
								Log.d(TAG, String.format("addDownloadList DownloadCount %d URL:%s", mDownloadUrlList.size(), token));
							}
						}
					}
				}
			}
		}

		mIntent.replaceExtras(new Bundle());
		mIntent.setAction("");
		mIntent.setData(null);
		mIntent.setFlags(0);
	}

	private void enablePopupMenuIcon(PopupMenu popup) {
		try {
			Field[] fields = popup.getClass().getDeclaredFields();
			for (Field field : fields) {
				if ("mPopup".equals(field.getName())) {
					field.setAccessible(true);
					Object menuPopupHelper = field.get(popup);
					Class<?> classPopupHelper = Class.forName(menuPopupHelper
							.getClass().getName());
					Method setForceIcons = classPopupHelper.getMethod(
							"setForceShowIcon", boolean.class);
					setForceIcons.invoke(menuPopupHelper, true);
					break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private class MyListItemListener implements OnItemListener {

		@Override
		public void onItemClick(int position) {
			// TODO Auto-generated method stub
			FileManager file = mCurFileManager.getFileList().elementAt(position);

			if (mSelectFileList.contains(file)) {
				addAndRemoveSelectList(file, false);
				mAdapter.notifyDataSetChanged();
			} else {
				if (file.getType() == FileManager.DIRECTORY) {
					mDirectoryList.add(file);
					setHistoryTitle();

					mCurFileManager = file;
					sort(mSortType, mSortAscent);
					mAdapter.setFileManager(mCurFileManager);
					mAdapter.notifyDataSetChanged();
				} else if (file.getType() == FileManager.FILE) {
					cleanSelectList();
					startPlay(file.getDiskIndex(), file.getKey());
				}
			}
		}

		@Override
		public void onItemDetail(String key) {
			// TODO Auto-generated method stub
			cleanSelectList();
			for (int i = 0; i < mContentsList.size(); i++) {
				MultiKollusContent content = mContentsList.get(i);
				if (content.getKollusContent().getMediaContentKeyMD5().equalsIgnoreCase(key)) {
					Intent intent = new Intent(HistoryActivity.this, KollusContentDetail.class);
					intent.putExtra(getResources().getString(R.string.media_content_key),
							content.getKollusContent().getMediaContentKey());
                    intent.putExtra(getResources().getString(R.string.disk_index),
                            mMultiStorage.getStorageIndex(content.getKollusStorage()));
					startActivity(intent);

					break;
				}
			}
		}

		@Override
		public void onItemCheckChange(int position, boolean isChecked) {
			FileManager file = mCurFileManager.getFileList().elementAt(position);
			addAndRemoveSelectList(file, isChecked);
		}
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
		super.onStart();// ATTENTION: This was auto-generated to implement the App Indexing API.
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
		mActivityStack.regOnResumeState(this);
		mMultiStorage = MultiKollusStorage.getInstance(getApplicationContext());

		mSortType = mPreference.getInt(getResources().getString(R.string.preference_sort_type_key), SortPreference.SORT_BY_TITLE);
		mSortAscent = mPreference.getBoolean(getResources().getString(R.string.preference_sort_order_key), true);

		if(mBounded) {
			try {
				mMessenger.send(Message.obtain(null, DownloadService.APP_FORGROUND));
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else {
			Intent intent = new Intent(this, DownloadService.class);
			bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
		}

		refreshContentList();
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
		mActivityStack.regOnPauseState(this);
		if(mMessenger != null) {
			try {
				mMessenger.send(Message.obtain(null, DownloadService.APP_BACKGROUND));
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

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
		mActivityStack.regOnDestroyState(this);
		//etlim 20170902 Activity Exit ==> Broadcast Event
		unregisterReceiver(mHistoryActivityBR);
		mMultiStorage.unregisterSDStateChangeListener(mSDCardStateChangeListener);
		super.onDestroy();
		if (mBounded) {
			unbindService(mConnection);
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		if (event.getAction() == KeyEvent.ACTION_DOWN) {
			if (keyCode == KeyEvent.KEYCODE_BACK) {
				if (mDirectoryList.size() > 1) {
					mDirectoryList.remove(mDirectoryList.lastElement());
					setHistoryTitle();

					mCurFileManager = mDirectoryList.lastElement();
					sort(mSortType, mSortAscent);
					mAdapter.setFileManager(mCurFileManager);
					mAdapter.notifyDataSetChanged();

					Log.d(TAG, ">>>>>>>>>> Root >>>>>>>>>>");
					mFileManager.dump();
					Log.d(TAG, "<<<<<<<<<< Root <<<<<<<<<<");
					Log.d(TAG, ">>>>>>>>>> Current >>>>>>>>>>");
					mCurFileManager.dump();
					Log.d(TAG, "<<<<<<<<<< Current <<<<<<<<<<");
				} else {
					if (!mExit) {
						mRepressForFinish.show();
						mExit = true;
						mHandler.sendEmptyMessageDelayed(CHECK_EXIT, EXIT_TIME);
					} else {
						finish();
					}
				}

				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	private void startPlay(int diskIndex, String key) {
	    KollusStorage storage = mMultiStorage.getStorage(diskIndex);
		for (final MultiKollusContent content : mContentsList) {
			if (content.getKollusStorage() == storage &&
                    content.getKollusContent().getMediaContentKeyMD5().equalsIgnoreCase(key)) {
				boolean bExpired = false;
				long currentDate = System.currentTimeMillis() / 1000;
				if (content.getKollusContent().getTotalExpirationCount() > 0 && content.getKollusContent().getExpirationCount() <= 0)
					bExpired = true;
				if (content.getKollusContent().getExpirationDate() > 0 && currentDate > content.getKollusContent().getExpirationDate())
					bExpired = true;
				if (content.getKollusContent().getTotalExpirationPlaytime() > 0 && content.getKollusContent().getExpirationPlaytime() <= 0)
					bExpired = true;

				if (bExpired && content.getKollusContent().getExpirationRefreshPopup()) {
					final String mediaContentKey = content.getKollusContent().getMediaContentKey();
					new KollusAlertDialog(HistoryActivity.this).
							setTitle(R.string.menu_info_str).
							setMessage(R.string.download_drm_refresh).
							setPositiveButton(R.string.confirm,
									new DialogInterface.OnClickListener() {
										public void onClick(DialogInterface dialog, int whichButton) {
											Intent i = new Intent(HistoryActivity.this, MovieActivity.class);
											i.putExtra("media_content_key", mediaContentKey);
											i.putExtra("storage_instance",
                                                    mMultiStorage.getStorageIndex(content.getKollusStorage()));
											startActivity(i);
										}
									}).
							setNegativeButton(R.string.cancel,
									new DialogInterface.OnClickListener() {
										public void onClick(DialogInterface dialog, int whichButton) {
										}
									}).
							show();
				} else {
					Intent i = new Intent(this, MovieActivity.class);
					i.putExtra("media_content_key", content.getKollusContent().getMediaContentKey());
                    i.putExtra("disk_index", mMultiStorage.getStorageIndex(content.getKollusStorage()));
					startActivity(i);
				}
				break;
			}
		}
	}

	private void nextDownload() {
		if (mDownloadUrlList != null) {
			if (!mDownloadUrlList.isEmpty()) {
				mDownloadUrlList.remove(0);
			}

			if (!mDownloadUrlList.isEmpty()) {
				Message msg = mHandler.obtainMessage(DATA_DOWNLOAD, mDownloadUrlList.get(0));
				mHandler.sendMessageDelayed(msg, MESSAGE_DELAY_MS);
			}
		}
	}

	private void startDownload(String url) {
		KollusUri uri = KollusUri.parse(url);
		String location = null;
		String path;
		int queryIndex = url.indexOf('?');
		if (queryIndex > 0)
			path = url.substring(0, queryIndex);
		else
			path = url;

		Set<String> keySet = uri.getQueryParameterNames();
		boolean first = true;
		for (String key : keySet) {
			Log.d(TAG, String.format("startDownload '%s' ==> '%s'", key, uri.getQueryParameter(key)));
			if (key.equalsIgnoreCase("folder")) {
				location = Uri.decode(uri.getQueryParameter(key));
			}
			else {
				if (first)
					path += "?";
				else
					path += "&";

				path += key;
				path += "=";
				path += uri.getQueryParameter(key);

				first = false;
			}
		}

		FileManager downloadLocation = mFileManager;
		if (location != null) {
			String[] items = location.split("/");
			for (String item : items) {
				downloadLocation = downloadLocation.addDirectory(item);
			}
		}

		Log.d(TAG, "startDownload downStart --> folder [" + location + "] url [" + path + "]");
		DownloadInfo info = new DownloadInfo(mMultiStorage.getStorage(DiskUtil.getDiskIndex(this)), location, path);
		try {
			mMessenger.send(Message.obtain(null, DownloadService.DOWNLOAD_START, info));
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void refreshContentList() {
		Log.d(TAG, "refreshContentList");
		cleanSelectList();
		mContentsList.clear();
		if(KollusConstants.SUPPORT_MULTI_STORAGE)
			mContentsList = mMultiStorage.getDownloadList(-1);
		else
			mContentsList = mMultiStorage.getDownloadList(DiskUtil.getDiskIndex(this));
		mAdapter.setContentsList(mContentsList);
		Log.d(TAG, "refreshContentList size : " + mContentsList.size());

		ArrayList<String> downloadKeyList = new ArrayList<String>();
		for (MultiKollusContent content : mDownloadList)
			downloadKeyList.add(content.getKollusContent().getMediaContentKey());
		mDownloadList.clear();
		for (String key : downloadKeyList) {
			for (MultiKollusContent content : mContentsList) {
				if (key.equals(content.getKollusContent().getMediaContentKey())) {
					mDownloadList.add(content);
					break;
				}
			}
		}
		mDownloadAdapter.notifyDataSetChanged();

		loadFileManager(true);

		configureFileManager();
		configureContentListByName(mHistoryTitle.getText().toString());

		mAdapter.notifyDataSetChanged();
	}

	class SortFileManager implements Comparator<SortFileManager> {
		int sortType;
		boolean ascent;
		FileManager fileManager;
		SortFileManager(int sortType, boolean ascent, FileManager fileManager) {
			this.sortType = sortType;
			this.ascent = ascent;
			this.fileManager = fileManager;
		}

		@Override
		public int compare(SortFileManager f1, SortFileManager f2) {
			int compare = 0;
			if(f1.fileManager.getType() == FileManager.DIRECTORY) {
				if(f2.fileManager.getType() == FileManager.DIRECTORY) {
					compare = f1.fileManager.getName().compareToIgnoreCase(f2.fileManager.getName());
					if (!ascent) {
						compare *= -1;
					}
				}
				else if(f2.fileManager.getType() == FileManager.FILE) {
					//TODO Nothing
				}
			}
			else {
				if(f2.fileManager.getType() == FileManager.DIRECTORY) {
					compare = -1;
				}
				else {
					KollusContent c1 = getKollusContent(mContentsList,
                            mMultiStorage.getStorage(f1.fileManager.getDiskIndex()),
                            f1.fileManager.getKey()).getKollusContent();
					KollusContent c2 = getKollusContent(mContentsList,
                            mMultiStorage.getStorage(f2.fileManager.getDiskIndex()),
                            f2.fileManager.getKey()).getKollusContent();
					switch (sortType) {
						case SortPreference.SORT_BY_DATE:
							compare = (c1.getStartAt() < c2.getStartAt())?1:(c1.getStartAt() > c2.getStartAt())?-1:0;
							break;
						case SortPreference.SORT_BY_DURATION:
							compare = (c1.getDuration() < c2.getDuration())?1:(c1.getDuration() > c2.getDuration())?-1:0;
							break;
						case SortPreference.SORT_BY_SIZE:
							compare = (c1.getFileSize() < c2.getFileSize())?1:(c1.getFileSize() > c2.getFileSize())?-1:0;
							break;
						default:
							compare = compare = f1.fileManager.getName().compareToIgnoreCase(f2.fileManager.getName());
							break;
					}

					if (!ascent) {
						compare *= -1;
					}
				}
			}
			return compare;
		}
	}

	private void sort(int sortType, boolean ascent) {
		Log.d(TAG, String.format("sort type %d ascent %b", sortType, ascent));
		Vector<FileManager> fileList = mCurFileManager.getFileList();
		for(MultiKollusContent content : mContentsList)
			Log.d(TAG, String.format("sort >>> disk %d mck %s name %s",
                    mMultiStorage.getStorageIndex(content.getKollusStorage()),
					content.getKollusContent().getMediaContentKey(),
					content.getKollusContent().getSubCourse()));

		if(fileList.size() < 2) {
			Log.d(TAG, "No Need Sort. Count is 1.");
			return;
		}

		for (int i = 0; i < fileList.size(); i++) {
			for (int j = i+1; j < fileList.size(); j++) {
				if (fileList.get(i).getType() == FileManager.DIRECTORY &&
						fileList.get(j).getType() == FileManager.DIRECTORY) {
					if (sortType == SortPreference.SORT_BY_TITLE) {
						int compare = sortByTitle(fileList.get(i), fileList.get(j));
						if (ascent) {
							if (compare > 0) {
								fileList.add(i, fileList.remove(j));
							}
						} else {
							if (compare < 0) {
								fileList.add(i, fileList.remove(j));
							}
						}
					} else if (fileList.get(i).getName().compareToIgnoreCase(fileList.get(j).getName()) > 0)
						fileList.add(i, fileList.remove(j));
				} else if (fileList.get(i).getType() == FileManager.DIRECTORY &&
						fileList.get(j).getType() == FileManager.FILE) {
					//TODO Nothing
				} else if (fileList.get(i).getType() == FileManager.FILE &&
						fileList.get(j).getType() == FileManager.DIRECTORY) {
					fileList.add(i, fileList.remove(j));
				} else {
					int compare;
					switch (sortType) {
						case SortPreference.SORT_BY_TITLE:
							compare = sortByTitle(fileList.get(i), fileList.get(j));
							break;
						case SortPreference.SORT_BY_DATE:
							compare = sortByDownloadDate(fileList.get(i), fileList.get(j));
							break;
						case SortPreference.SORT_BY_DURATION:
							compare = sortByDuration(fileList.get(i), fileList.get(j));
							break;
						case SortPreference.SORT_BY_SIZE:
							compare = sortBySize(fileList.get(i), fileList.get(j));
							break;
						default:
							compare = sortByTitle(fileList.get(i), fileList.get(j));
							break;
					}

					if (ascent) {
						if (compare > 0) {
							fileList.add(i, fileList.remove(j));
						}
					} else {
						if (compare < 0) {
							fileList.add(i, fileList.remove(j));
						}
					}
				}
			}
		}
	}

	private int sortByTitle(FileManager f1, FileManager f2) {
		if (f1.getType() == FileManager.DIRECTORY) {
			if (f2.getType() == FileManager.DIRECTORY)
				return f1.getName().compareToIgnoreCase(f2.getName());
			else
				return -1;
		} else {
			if (f2.getType() == FileManager.DIRECTORY)
				return 1;
			else {
				if(getKollusContent(mContentsList, mMultiStorage.getStorage(f1.getDiskIndex()), f1.getKey()) == null ||
						getKollusContent(mContentsList, mMultiStorage.getStorage(f2.getDiskIndex()), f2.getKey()) == null) {
					return 1;
				}

				KollusContent c1 = getKollusContent(mContentsList,
                        mMultiStorage.getStorage(f1.getDiskIndex()),
                        f1.getKey()).getKollusContent();
				KollusContent c2 = getKollusContent(mContentsList,
                        mMultiStorage.getStorage(f2.getDiskIndex()),
                        f2.getKey()).getKollusContent();

				String title1;
				if (c1.getCourse() != null && c1.getCourse().length() > 0) {
					if (c1.getSubCourse() != null && c1.getSubCourse().length() > 0)
						title1 = c1.getCourse() + "(" + c1.getSubCourse() + ")";
					else
						title1 = c1.getCourse();
				} else
					title1 = c1.getSubCourse();

				String title2;
				if (c2.getCourse() != null && c2.getCourse().length() > 0) {
					if (c1.getSubCourse() != null && c2.getSubCourse().length() > 0)
						title2 = c2.getCourse() + "(" + c2.getSubCourse() + ")";
					else
						title2 = c2.getCourse();
				} else
					title2 = c2.getSubCourse();

				return title1.compareToIgnoreCase(title2);
			}

		}
	}

	private int sortByDownloadDate(FileManager f1, FileManager f2) {
		if (f1.getType() == FileManager.DIRECTORY) {
			if (f2.getType() == FileManager.DIRECTORY)
				return sortByTitle(f1, f2);
			else
				return -1;
		} else {
			if (f2.getType() == FileManager.DIRECTORY)
				return 1;
			else {
				if(getKollusContent(mContentsList, mMultiStorage.getStorage(f1.getDiskIndex()), f1.getKey()) == null ||
						getKollusContent(mContentsList, mMultiStorage.getStorage(f2.getDiskIndex()), f2.getKey()) == null) {
					return 1;
				}

				KollusContent c1 = getKollusContent(mContentsList,
                        mMultiStorage.getStorage(f1.getDiskIndex()),
                        f1.getKey()).getKollusContent();
				KollusContent c2 = getKollusContent(mContentsList,
                        mMultiStorage.getStorage(f2.getDiskIndex()),
                        f2.getKey()).getKollusContent();

				if (c1.getStartAt() < c2.getStartAt())
					return 1;
				else if (c1.getStartAt() > c2.getStartAt())
					return -1;
				else
					return 0;
			}

		}
	}

	private int sortByDuration(FileManager f1, FileManager f2) {
		if (f1.getType() == FileManager.DIRECTORY) {
			if (f2.getType() == FileManager.DIRECTORY)
				return sortByTitle(f1, f2);
			else
				return -1;
		} else {
			if (f2.getType() == FileManager.DIRECTORY)
				return 1;
			else {
				if(getKollusContent(mContentsList, mMultiStorage.getStorage(f1.getDiskIndex()), f1.getKey()) == null ||
						getKollusContent(mContentsList, mMultiStorage.getStorage(f2.getDiskIndex()), f2.getKey()) == null) {
					return 1;
				}

				KollusContent c1 = getKollusContent(mContentsList,
                        mMultiStorage.getStorage(f1.getDiskIndex()),
                        f1.getKey()).getKollusContent();
				KollusContent c2 = getKollusContent(mContentsList,
                        mMultiStorage.getStorage(f2.getDiskIndex()),
                        f2.getKey()).getKollusContent();

				if (c1.getDuration() < c2.getDuration())
					return -1;
				else if (c1.getDuration() > c2.getDuration())
					return 1;
				else
					return 0;
			}

		}
	}

	private int sortBySize(FileManager f1, FileManager f2) {
		if (f1.getType() == FileManager.DIRECTORY) {
			if (f2.getType() == FileManager.DIRECTORY)
				return sortByTitle(f1, f2);
			else
				return -1;
		} else {
			if (f2.getType() == FileManager.DIRECTORY)
				return 1;
			else {
				if(getKollusContent(mContentsList, mMultiStorage.getStorage(f1.getDiskIndex()), f1.getKey()) == null ||
						getKollusContent(mContentsList, mMultiStorage.getStorage(f2.getDiskIndex()), f2.getKey()) == null) {
					return 1;
				}

				MultiKollusContent c1 = getKollusContent(mContentsList,
                        mMultiStorage.getStorage(f1.getDiskIndex()),
                        f1.getKey());
				MultiKollusContent c2 = getKollusContent(mContentsList,
                        mMultiStorage.getStorage(f2.getDiskIndex()),
                        f2.getKey());

				if (c1.getKollusContent().getFileSize() < c2.getKollusContent().getFileSize())
					return -1;
				else if (c1.getKollusContent().getFileSize() > c2.getKollusContent().getFileSize())
					return 1;
				else
					return 0;
			}

		}
	}

	private MultiKollusContent getKollusContent(ArrayList<MultiKollusContent> list, KollusStorage storage, String key) {
		for(MultiKollusContent iter : list) {
			if (iter.getKollusStorage() == storage &&
					iter.getKollusContent().getMediaContentKeyMD5().equalsIgnoreCase(key)) {
				return iter;
			}
		}

		Log.d(TAG, String.format("Content List Count %d diskIndex %d Key %s",
                list.size(), mMultiStorage.getStorageIndex(storage), key));
		return null;
	}

    private void copyKollusContent(MultiKollusContent src) {
        MultiKollusContent dst = getKollusContent(mContentsList, src.getKollusStorage(), src.getKollusContent().getMediaContentKeyMD5());
		if(dst != null)
            PropertyUtil.copyKollusContent(dst.getKollusContent(), src.getKollusContent());

        dst = getKollusContent(mDownloadList, src.getKollusStorage(), src.getKollusContent().getMediaContentKeyMD5());
        if(dst != null)
			PropertyUtil.copyKollusContent(dst.getKollusContent(), src.getKollusContent());
	}

	private void updateKollusContent(MultiKollusContent multiContent, boolean bError) {
		if(multiContent ==  null)
			return;

		KollusContent content = multiContent.getKollusContent();
		if (content.getReceivingSize() != content.getFileSize() && !bError) {
			mDownloadingFileSize -= (content.getReceivingSize() - content.getReceivedSize());
			mDownloadedFileSize += (content.getReceivingSize() - content.getReceivedSize());
			Log.d(TAG, String.format("updateContent DownloadingFileSize %d FileSize %d ReceivingSize %d ReceivedSize %d",
					mDownloadingFileSize,
					content.getFileSize(),
					content.getReceivingSize(),
					content.getReceivedSize()));
			content.setReceivedSize(content.getReceivingSize());
		}
		copyKollusContent(multiContent);

		Vector<FileManager> fileList = mCurFileManager.getFileList();
		int fileListSize = fileList.size();
		for (int i = 0; i < fileListSize; i++) {
			FileManager file = fileList.elementAt(i);
			if (file.getType() == FileManager.FILE &&
					file.getDiskIndex() == mMultiStorage.getStorageIndex(multiContent.getKollusStorage()) &&
					file.getKey().equals(content.getMediaContentKeyMD5())) {
				mAdapter.notifyItemChanged(i);
				break;
			}
		}

		int idx = 0;
		for (MultiKollusContent iter : mDownloadList) {
			if (iter.getKollusStorage() == multiContent.getKollusStorage() &&
					iter.getKollusContent().getMediaContentKey().equals(content.getMediaContentKey())) {
				mDownloadAdapter.notifyItemChanged(idx);
				//[MP-170] -- 다운로드중 앱 강제종료 후 이어받기하면 다운로드 취소 불가한 현상
				if(mDownloadIndex != idx+1) {
					mDownloadIndex = idx+1;
					setDownloadTitle();
				}
				break;
			}
			idx++;
		}
	}

	private void setHistoryTitle() {
		String title = "";
		for (FileManager dir : mDirectoryList) {
			title += dir.getName();

			if (dir != mFileManager && dir != mDirectoryList.lastElement())
				title += "/";
		}
		mHistoryTitle.setText(title);
	}

	private MultiKollusContent findKollusContent(FileManager file) {
        MultiKollusContent content = null;
		for (MultiKollusContent iter : mContentsList) {
			if (mMultiStorage.getStorageIndex(iter.getKollusStorage()) == file.getDiskIndex() &&
					iter.getKollusContent().getMediaContentKeyMD5().equalsIgnoreCase(file.getKey())) {
				content = iter;
				break;
			}
		}
		return content;
	}

	private void loadFileManager(boolean bFirst) {
		if (KollusConstants.SUPPORT_MULTI_STORAGE) {
			Vector<String> dirs = DiskUtil.getExternalMounts(this);
			for (int i = 0; i < dirs.size(); i++) {
				String dbName = dirs.get(i) + "/directory.db";
				Log.d(TAG, "loadFileManager dbName "+dbName);

				if (i == 0) {
					String fileJsonString = Utils.getDirectoryJSONByPath(this,
							dirs.get(i),
							getResources().getString(R.string.preference_file_json_string));
					Log.d(TAG, "loadFileManager fileJsonString "+fileJsonString);
					try {
						mFileManager.load(fileJsonString, i);
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						return;
					}
				} else {
					File dbFile = new File(dbName);
					if(dbFile.exists()){
						String fileJsonString = Utils.getDirectoryJSONByPath(this,
								dirs.get(i),
								getResources().getString(R.string.preference_file_json_string));
						Log.d(TAG, "loadFileManager fileJsonString "+fileJsonString);
						Log.d(TAG, "loadFileManager >>>> need merge FileManager");
						dbFile.delete();

						FileManager fileManager = new FileManager(FileManager.DIRECTORY);
						fileManager.setName("/");
						try {
							fileManager.load(fileJsonString, i);
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							return;
						}

						FileManager.merge(fileManager, mFileManager);
						saveFileManager();
					}
				}
			}
			Log.d(TAG, "loadFileManager Dump [[");
			mFileManager.dump();
			Log.d(TAG, "loadFileManager Dump ]]");
		}
		else {
			String fileJsonString = Utils.getDirectoryJSON(this, getResources().getString(R.string.preference_file_json_string));
			try {
				mFileManager.load(fileJsonString, DiskUtil.getDiskIndex(this));
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				return;
			}
		}

		if (bFirst) {
			mCurFileManager = mFileManager;
		} else {
			StringTokenizer tokenizer = new StringTokenizer((String) mHistoryTitle.getText(), "/");

			mDirectoryList.clear();

			FileManager tmp = mFileManager;
			mDirectoryList.add(tmp);

			while (tokenizer.hasMoreElements()) {
				tmp = FileManager.findDirectory(tmp, tokenizer.nextToken());
				if (tmp == null)
					break;
				mDirectoryList.add(tmp);
			}

			mCurFileManager = (tmp == null ? mFileManager : tmp);
			sort(mSortType, mSortAscent);
			mAdapter.setFileManager(mCurFileManager);
		}
	}

	private void saveFileManager() {
		JSONObject root = new JSONObject();
		try {
			FileManager.save(root, mFileManager);
			String dbPath = Utils.getStoragePath(this);
			if (KollusConstants.SUPPORT_MULTI_STORAGE)
				dbPath = DiskUtil.getExternalMounts(this).get(0);
			Utils.saveDirectoryJSON(this, dbPath, root.toString());
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void cleanSelectList() {
		mSelectFileList.clear();
		mBtnSetting.setVisibility(View.VISIBLE);
		mBtnAddFolder.setVisibility(View.VISIBLE);
		mBtnEditMenu.setVisibility(View.VISIBLE);
		mBtnMoreMenu.setVisibility(View.GONE);
		mAdapter.setSelectMode(KollusContentAdapter.MODE_NONE);
		mCopyMode = MENU_NONE;
	}

	private void addAndRemoveSelectList(FileManager file, boolean bAdd) {
		if (bAdd) {
			mSelectFileList.add(file);
		} else {
			mSelectFileList.remove(file);
		}

//		if (mSelectFileList.size() == 0) {
//			mBtnAddFolder.setVisibility(View.VISIBLE);
//			mBtnMoreMenu.setVisibility(View.GONE);
//		} else {
//			mBtnAddFolder.setVisibility(View.GONE);
//			mBtnMoreMenu.setVisibility(View.VISIBLE);
//		}
	}

	private void configureContentListByName(String folder) {
		if (folder != null) {
			folder = Uri.decode(folder);
			Log.d(TAG, String.format("configureContentListByName %s", folder));
			StringTokenizer tokenizer = new StringTokenizer(folder, "/");
			mDirectoryList.clear();

			FileManager tmp = mFileManager;
			mDirectoryList.add(tmp);

			while (tokenizer.hasMoreElements()) {
				tmp = FileManager.findDirectory(tmp, tokenizer.nextToken());
				if (tmp == null)
					break;
				mDirectoryList.add(tmp);
			}

			mCurFileManager = mDirectoryList.lastElement();
			sort(mSortType, mSortAscent);
			mAdapter.setFileManager(mCurFileManager);
			setHistoryTitle();
		}
	}

	private void configureFileManager() {
		boolean bChanged = false;
		Vector<FileManager> dbFileList = new Vector<FileManager>();
		mFileManager.findAllFile(dbFileList);

		Vector<String> realFileList = new Vector<String>();
		for (MultiKollusContent content : mContentsList) {
			boolean bExist = false;
			for (FileManager file : dbFileList) {
				if (mMultiStorage.getStorage(file.getDiskIndex()) == content.getKollusStorage() &&
				        file.getKey().compareToIgnoreCase(content.getKollusContent().getMediaContentKeyMD5()) == 0) {
					bExist = true;
					break;
				}
			}

			if(!bExist) {
				mFileManager.addFile(mMultiStorage.getStorageIndex(content.getKollusStorage()),
                        content.getKollusContent().getMediaContentKeyMD5());
				bChanged = true;
			}
		}

		for (FileManager file : dbFileList) {
			boolean bExist = false;
			for (MultiKollusContent content : mContentsList) {
				if (mMultiStorage.getStorage(file.getDiskIndex()) == content.getKollusStorage() &&
                        file.getKey().compareToIgnoreCase(content.getKollusContent().getMediaContentKeyMD5()) == 0) {
					bExist = true;
					break;
				}
			}

			if (!bExist) {
				mFileManager.removeFiles(file.getDiskIndex(), file.getKey());
				bChanged = true;
			}
		}

		if (bChanged) {
			saveFileManager();
		}
	}

	private void alertCancelDownload(final boolean bFinish) {
		new KollusAlertDialog(HistoryActivity.this).
				setTitle(R.string.menu_info_str).
				setMessage(R.string.notify_close_while_download).
				setPositiveButton(R.string.confirm,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								Log.w(TAG, "Cancel Download [[");
								mDownloadUrlList.clear();

								for (MultiKollusContent content : mDownloadList) {
									try {
										if (content.getKollusContent().isDownloading()) {
											mMessenger.send(Message.obtain(null, DownloadService.DOWNLOAD_CANCEL,
													0, 0, content));
										}
									} catch (RemoteException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
								}
								Log.w(TAG, "Cancel Download ]]");

								if (bFinish) {
//								setResult(RESULT_OK, mIntent);
									finish();
								}
							}
						}).
				setNegativeButton(R.string.cancel,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
							}
						}).
				show();
	}

	private void createDialog(int id) {
		// TODO Auto-generated method stub
		switch (id) {
			case CREATE_FOLDER_DIALOG: {
				LayoutInflater factory = LayoutInflater.from(this);
				final View view = factory.inflate(R.layout.add_folder, null);

				new KollusAlertDialog(this)
						.setTitle(R.string.add_folder)
						.setView(view)
						.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
							/* User clicked OK so do some stuff */
								EditText folderTextView = (EditText) view.findViewById(R.id.folder_name);
								String newFolder = folderTextView.getText().toString();
								if (!mCurFileManager.existDirectory(newFolder)) {
									mCurFileManager.addDirectory(newFolder);
									saveFileManager();
									loadFileManager(false);
									mAdapter.setFileManager(mCurFileManager);
									mAdapter.notifyDataSetChanged();
								}
								InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
								imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
							}
						})
						.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
		                    /* User clicked cancel so do some stuff */
							InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
							imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
							}
						})
						.show();
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
				break;
			}
			case RENAME_DIALOG: {
				LayoutInflater factory = LayoutInflater.from(this);
				final View view = factory.inflate(R.layout.add_folder, null);
				final EditText folderTextView = (EditText) view.findViewById(R.id.folder_name);
				folderTextView.setText(mSelectFileList.get(0).getName());
				new KollusAlertDialog(this)
						.setTitle(R.string.rename)
						.setView(view)
						.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
		                    /* User clicked OK so do some stuff */
								String newFolder = folderTextView.getText().toString();
								if (!mCurFileManager.existDirectory(newFolder)) {
									mSelectFileList.get(0).setName(newFolder);
									saveFileManager();
									loadFileManager(false);
									cleanSelectList();
									mAdapter.notifyDataSetChanged();
								}
								InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
								imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
								cleanSelectList();
							}
						})
						.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
		                    /* User clicked cancel so do some stuff */
							InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
							imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
							cleanSelectList();
							}
						})
						.show();
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
				break;
			}
			case DOWNLOAD_DIALOG: {
				if (mDownloadDialog == null) {
					mDownloadDialog = new KollusAlertDialog(this)
							.setTitle(R.string.download_list)
							.setView(mDownloadView)
							.setOnKeyListener(new OnKeyListener() {

								@Override
								public boolean onKey(DialogInterface dialog, int keyCode,
													 KeyEvent event) {
									// TODO Auto-generated method stub
									if (event.getAction() == KeyEvent.ACTION_DOWN) {
										if (keyCode == KeyEvent.KEYCODE_BACK) {
											if (!mDownloadUrlList.isEmpty()) {
												Toast.makeText(HistoryActivity.this, getResources().getString(R.string.initializing_str), Toast.LENGTH_SHORT).show();
											} else {
												Message msg = mHandler.obtainMessage();
												msg.what = DATA_DOWNLOAD_CANCEL;
												msg.arg1 = 0;
												mHandler.sendMessage(msg);
											}
										}
									}
									return true;
								}

							})
							.setCancelable(false);
				}

				mDownloadDialog.show();
				break;
			}
		}
	}

	private void closeDialog(int id) {
		switch (id) {
			case DOWNLOAD_DIALOG:
				Log.d(TAG, String.format("closeDialog Download list empty %b isDownloading %b", mDownloadUrlList.isEmpty(), DownloadService.isDownloading()));
				//[MP-170] -- 다운로드중 앱 강제종료 후 이어받기하면 다운로드 취소 불가한 현상
				if (mDownloadUrlList.isEmpty() && !DownloadService.isDownloading()) {
					mDownloadList.clear();
					mDownloadAdapter.notifyDataSetChanged();

					mDownloadIndex = 0;
					mDownloadCompleteCount = 0;
					mTotalDownloadCount = 0;
					mDownloadingFileSize = 0;
					mDownloadedFileSize = 0;
					mDownloadBitrate = 0;
					mHandler.removeCallbacks(mDownloadChecker);

					if (mDownloadDialog != null) {
						mDownloadDialog.cancel();
						mDownloadDialog.dismiss();
					}
				}
				break;
		}
	}

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				//case DATA_CHANGED:
				//	mAdapter.notifyDataSetChanged();
				//	break;
				case DATA_DOWNLOAD:
					startDownload((String) msg.obj);
					break;
				case DATA_DOWNLOAD_CANCEL:
					alertCancelDownload(msg.arg1 == 1);
					break;
				case CHECK_EXIT:
					mExit = false;
					break;
			}
			super.handleMessage(msg);
		}
	};

	private void setDownloadTitle() {
		mDownloadFileView.setText(String.format(getResources().getString(R.string.download_file),
				mDownloadIndex, mTotalDownloadCount+mDownloadUrlList.size()));
	}

	public class DownloadHander extends Handler {

		@Override
		public void handleMessage(Message msg) {
			Log.d(TAG, "DownloadHander handleMessage what : "+msg.what);
			switch (msg.what) {
				case DownloadService.ADD_HANDLER: {
//            	Toast.makeText(HistoryActivity.this, msg.obj.toString(), Toast.LENGTH_LONG).show();
					if (!mDownloadUrlList.isEmpty()) {
						Message tmp = mHandler.obtainMessage(DATA_DOWNLOAD, mDownloadUrlList.get(0));
						mHandler.sendMessageDelayed(tmp, MESSAGE_DELAY_MS);
					}
				}
				break;

				case DownloadService.DISK_UNMOUNT:
					refreshContentList();
					closeDialog(DOWNLOAD_DIALOG);
					break;

				case DownloadService.DOWNLOAD_LOADED: {
					mDownloadInitLayer.setVisibility(View.GONE);
					DownloadInfo info = (DownloadInfo) msg.obj;
                    MultiKollusContent content = info.getMultiKollusContent();
					boolean exist = false;

					Log.d(TAG, "Download Loaded -- "+content.getKollusContent());
					for (MultiKollusContent iter : mContentsList) {
						if (iter.getKollusStorage() == content.getKollusStorage() &&
                                iter.getKollusContent().getMediaContentKey().equals(content.getKollusContent().getMediaContentKey())) {
							exist = true;
							break;
						}
					}

					if (!exist) {
						//				findViewById(R.id.no_list).setVisibility(View.GONE);
						FileManager downloadLocation = mFileManager;
						if (info.getFolder() != null) {
							String[] items = info.getFolder().split("/");
							for (String item : items) {
								downloadLocation = downloadLocation.addDirectory(item);
							}
						}

						downloadLocation.addFile(DiskUtil.getDiskIndex(HistoryActivity.this), content.getKollusContent().getMediaContentKeyMD5());
						saveFileManager();

						mContentsList.add(content);
						sort(mSortType, mSortAscent);
						mAdapter.notifyDataSetChanged();
						mListView.requestLayout();
						Log.d(TAG, "Content List ADD -- "+content.getKollusContent().getMediaContentKey());
					}
					content.getKollusContent().setDownloading(true);

					exist = false;
					for (MultiKollusContent iter : mDownloadList) {
						if (iter.getKollusStorage() == content.getKollusStorage() &&
								iter.getKollusContent().getMediaContentKey().equals(content.getKollusContent().getMediaContentKey())) {
							exist = true;
							break;
						}
					}

					if(!exist) {
						mDownloadList.add(content);
						mDownloadAdapter.notifyDataSetChanged();
						mDownloadListView.requestLayout();
						createDialog(DOWNLOAD_DIALOG);
					}

					nextDownload();
					if(!exist) {
						mTotalDownloadCount++;
						mDownloadingFileSize += content.getKollusContent().getFileSize() - content.getKollusContent().getReceivedSize();
						setDownloadTitle();
					}

					if (mTotalDownloadCount == 1) {
						mDownloadCheckTime = System.currentTimeMillis();
						mHandler.postDelayed(mDownloadChecker, DOWNLOAD_CHECK_TIME);
					}
				}
				break;

				case DownloadService.DOWNLOAD_ALREDY_LOADED:
					Log.d(TAG, "Already Loaded. hasNextDownload " + mDownloadUrlList.isEmpty());
					nextDownload();
					break;

				case DownloadService.DOWNLOAD_STARTED:
					mDownloadIndex++;
					setDownloadTitle();
					break;

				case DownloadService.DOWNLOAD_CANCELED: {
					String mediaContentKey = (String) msg.obj;
					for (MultiKollusContent content : mDownloadList) {
						if (content.getKollusContent().getMediaContentKey().equals(mediaContentKey)) {
							Log.d(TAG, "DOWNLOAD_CANCELED:" + content.getKollusContent().getMediaContentKey());
							content.getKollusContent().setDownloadCanceled();
							content.getKollusContent().setDownloading(false);
							mDownloadList.remove(content);
							mDownloadAdapter.notifyDataSetChanged();

							mDownloadIndex = mDownloadCompleteCount + 1;
							mTotalDownloadCount--;
							mDownloadingFileSize -= (content.getKollusContent().getFileSize() - content.getKollusContent().getReceivedSize());
							setDownloadTitle();

							refreshContentList();
							break;
						}
					}

					Log.i(TAG, "Close Download Dialog in cancel");
					closeDialog(DOWNLOAD_DIALOG);
				}
				break;

				case DownloadService.DOWNLOAD_LOAD_ERROR:
				case DownloadService.DOWNLOAD_ERROR: {
					int diskIndex = msg.arg1;
					int errorCode = msg.arg2;

					if (msg.what == DownloadService.DOWNLOAD_ERROR) {
						MultiKollusContent content = (MultiKollusContent) msg.obj;
						updateKollusContent(content, true);
					}

					Resources r = getResources();
					String title;
					String message = mMultiStorage.getLastError(diskIndex);
					Log.d(TAG, "LastError : "+message);

					title = r.getString(R.string.error_title);
					if (errorCode != ErrorCodes.ERROR_ALREADY_DOWNLOADED &&
							errorCode != ErrorCodes.ERROR_ALREADY_DOWNLOADING)
						title = r.getString(R.string.error_title)+" : " + errorCode;

					if (message == null) {
						message = ErrorCodes.getInstance(HistoryActivity.this).getErrorString(errorCode);
						Log.d(TAG, String.format("ErrorCode %d ==> %s ", errorCode, message));
					}

					if (!mAlertMessage.isShowing()) {
						Log.i(TAG, "onDownloadFail not showing errorCode:" + errorCode);
						mAlertMessage.setTitle(title)
								.setMessage(message)
								.setPositiveButton(R.string.VideoView_error_button,
										new DialogInterface.OnClickListener() {
											public void onClick(DialogInterface dialog, int whichButton) {
											}
										})
								.setCancelable(false)
								.show();

					}

					Log.i(TAG, "Close Download Dialog in Download Error");
					nextDownload();
					closeDialog(DOWNLOAD_DIALOG);
					setDownloadTitle();
				}
				break;

				case DownloadService.DOWNLOAD_PROCESS: {
					MultiKollusContent content = (MultiKollusContent) msg.obj;
					updateKollusContent(content, false);
					boolean exist = false;
					for (MultiKollusContent iter : mDownloadList) {
						if (iter.getKollusStorage() == content.getKollusStorage() &&
								iter.getKollusContent().getMediaContentKey().equals(content.getKollusContent().getMediaContentKey())) {
							exist = true;
							break;
						}
					}

					if(!exist) {
						mDownloadList.add(content);
						mDownloadAdapter.notifyDataSetChanged();
						mDownloadListView.requestLayout();
						createDialog(DOWNLOAD_DIALOG);
					}
				}
				break;

				case DownloadService.DOWNLOAD_COMPLETE: {
					MultiKollusContent content = (MultiKollusContent) msg.obj;
					mMultiStorage.getKollusContent(mMultiStorage.getStorageIndex(content.getKollusStorage()),
                            content.getKollusContent(), content.getKollusContent().getMediaContentKey());
					updateKollusContent(content, false);
					Log.i(TAG, "Close Download Dialog in Download Complete");
					closeDialog(DOWNLOAD_DIALOG);
					mDownloadCompleteCount++;
				}
				break;

				case DownloadService.DOWNLOAD_DRM: {
					DownloadDRM drm = (DownloadDRM) msg.obj;
					Log.d(TAG, String.format("onDRM diskIndex %d Request '%s' Response '%s'",
                            mMultiStorage.getStorageIndex(drm.getKollusStorage()), drm.getRequest(), drm.getResponse()));
				}
				break;

				case DownloadService.DOWNLOAD_DRM_INFO: {
					int diskIndex = msg.arg1;
					int nInfoCode = msg.arg2;
					KollusContent content = (KollusContent) msg.obj;
					String message = content.getServiceProviderMessage();

					if (nInfoCode == KollusPlayerDRMListener.DCB_INFO_DELETE) {
						if (!mAlertMessage.isShowing()) {
							int errorCode = ErrorCodes.ERROR_FORCE_DELETE;
							String title = getResources().getString(R.string.ERROR_CODE);
							title += " : " + errorCode;

							if (message == null || message.length() == 0)
								message = ErrorCodes.getInstance(HistoryActivity.this).getErrorString(errorCode);

							mAlertMessage.setTitle(title)
									.setMessage(message)
									.setPositiveButton(R.string.VideoView_error_button,
											new DialogInterface.OnClickListener() {
												public void onClick(DialogInterface dialog, int whichButton) {
												}
											})
									.setCancelable(false)
									.show();

						}

						synchronized (mContentsList) {
							for (MultiKollusContent iter : mContentsList) {
								if (mMultiStorage.getStorageIndex(iter.getKollusStorage()) == diskIndex &&
										iter.getKollusContent().getMediaContentKey().equals(content.getMediaContentKey())) {
									mContentsList.remove(iter);
									break;
								}
							}
						}

						synchronized (mDownloadList) {
							int idx = 0;
							for (MultiKollusContent iter : mDownloadList) {
								if (iter.getKollusContent().getMediaContentKey().equals(content.getMediaContentKey())) {
									mDownloadAdapter.notifyItemChanged(idx);
									break;
								}
								idx++;
							}
						}

						configureFileManager();
						configureContentListByName(mHistoryTitle.getText().toString());
						mAdapter.notifyDataSetChanged();
						Log.i(TAG, "Close Download Dialog in DRM Delete");
						closeDialog(DOWNLOAD_DIALOG);
					}
				}
				break;

				default:
					break;
			}

			super.handleMessage(msg);
		}
	}

	Messenger mMessenger;
	boolean mBounded;

	ServiceConnection mConnection = new ServiceConnection() {

		public void onServiceDisconnected(ComponentName name) {
//        	Toast.makeText(HistoryActivity.this, "DownloadService is disconnected", Toast.LENGTH_LONG).show();
			mBounded = false;
			mMessenger = null;
		}

		public void onServiceConnected(ComponentName name, IBinder service) {
//            Toast.makeText(HistoryActivity.this, "DownloadService is connected", Toast.LENGTH_LONG).show();
			mMessenger = new Messenger(service);
			mBounded = true;
			try {
				mMessenger.send(Message.obtain(null, DownloadService.ADD_HANDLER, new DownloadHander()));
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	};

	//etlim 20170902 Activity Exit ==> Broadcast Event
	private void HistoryActivityEventBroadcastRegister() {
		mHistoryActivityBR = new HistoryActivityBroadcastReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(ACTION_ACTIVITY_FINISH_HISTORY);
		registerReceiver(mHistoryActivityBR, filter);
	}

	private class HistoryActivityBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context ctx, Intent intent) {
			Log.d(TAG, "onReceive >>> " + intent.getAction());
			String action = intent.getAction();
			if (action.equals(ACTION_ACTIVITY_FINISH_HISTORY)) {
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
package com.kollus.media.preference;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;

import com.kollus.media.R;
import com.kollus.media.contents.MultiKollusStorage;
import com.kollus.media.util.DiskUtil;
import com.kollus.sdk.media.KollusStorage;
import com.kollus.sdk.media.util.Log;
import com.kollus.sdk.media.util.Utils;

import java.util.Vector;

public class StoragePreference extends View implements 
	OnClickListener, OnCheckedChangeListener {
	private static final String TAG = StoragePreference.class.getSimpleName();
	
	private static final int RESOURCE_STORAGE_BASE_ID = 100;
	private Context mContext;
	private MultiKollusStorage mMultiStorage;
	private String mGUIDSha1;
	private String mGUIDMd5;
	private Vector<String> mExternalStorages;
	private TextView mDownloadSizeTextView;
	private TextView mEtcSizeTextView;
	private TextView mEmptySizeTextView;
	private TextView mCacheSizeTextView;

	private LinearLayout mLocationLayer;
	private RadioGroup mStorageGroup;
	private MultiKollusStorage.SDCardStateChangeListener mSDCardStateChangeListener = new MultiKollusStorage.SDCardStateChangeListener() {
		@Override
		public void onStateChaged(String path, boolean mounted) {
			mStorageGroup.removeAllViews();
			mExternalStorages = MultiKollusStorage.getInstance(mContext).getStoragePathList();

			int storagIndex = 0;
			String storageLocation = Utils.getStoragePath(mContext);
			if(mExternalStorages.size() > 1) {
				mLocationLayer.setVisibility(View.VISIBLE);
				int index = 0;

				for(String iter : mExternalStorages) {
					RadioButton btn = new RadioButton(mContext);
					if(index == 0)
						btn.setText(R.string.inner_storage);
					else
						btn.setText(R.string.outer_storage);
					btn.setId(RESOURCE_STORAGE_BASE_ID+index);
					mStorageGroup.addView(btn);
					Log.i(TAG, String.format("path %s storageLocation %s", iter, storageLocation));
					if(storageLocation.startsWith(iter)) {
						storagIndex = index;
						mStorageGroup.check(RESOURCE_STORAGE_BASE_ID+storagIndex);
					}

					index++;
				}
			}
			else {
				mLocationLayer.setVisibility(View.GONE);
			}

			chagedStorageIndex(storagIndex);
		}
	};

	public StoragePreference(PlayerPreference root) {
		super(root);
		// TODO Auto-generated constructor stub
		mContext = root;
		mMultiStorage = MultiKollusStorage.getInstance(mContext);
		mGUIDSha1 = KollusConstants.getPlayerId(mContext);
		mGUIDMd5 = KollusConstants.getPlayerIdWithMD5(mContext);
		mStorageGroup = new RadioGroup(mContext);

		LinearLayout layout = (LinearLayout)root.findViewById(R.id.preference_root);
		View view = root.getLayoutInflater().inflate(R.layout.storage_preference, null);
		layout.addView(view);
		mLocationLayer = (LinearLayout)((ViewGroup)view).findViewById(R.id.storage_location_layer);

		onBindView(view);

		mStorageGroup.setOnCheckedChangeListener(this);
		mMultiStorage.registerSDStateChangeListener(mSDCardStateChangeListener);
	}
	
	@Override
	protected void finalize() throws Throwable {
		// TODO Auto-generated method stub
//		mStorage.releaseInstance();
		mMultiStorage.unregisterSDStateChangeListener(mSDCardStateChangeListener);
		super.finalize();
	}

	private void chagedStorageIndex(int index) {
		KollusStorage kollusStorage = mMultiStorage.getStorage(index);
		String sizeText = DiskUtil.getStringSize(kollusStorage.getUsedSize(KollusStorage.TYPE_CACHE));
		mCacheSizeTextView.setText(sizeText);

		String storagePath = mExternalStorages.get(index);
		long usedSize = kollusStorage.getUsedSize(KollusStorage.TYPE_DOWNLOAD);
		long freeSize = Utils.getAvailableMemorySize(storagePath);
		long totalSize = Utils.getTotalMemorySize(storagePath);
		long etcSize = totalSize - usedSize - freeSize;

		mDownloadSizeTextView.setText(DiskUtil.getStringSize(usedSize));
		mEtcSizeTextView.setText(DiskUtil.getStringSize(etcSize));
		mEmptySizeTextView.setText(DiskUtil.getStringSize(freeSize));
		Utils.setStoragePath(mContext, storagePath);
		Log.d(TAG, String.format("chagedStorageIndex %dth --> total %d used %d etc %d free %d",
				index, totalSize, usedSize, etcSize, freeSize));
	}

	private void onBindView(View view) {
		Button button = (Button)((ViewGroup)view).findViewById(R.id.current_storage_empty);
		button.setOnClickListener(this);

		mStorageGroup = (RadioGroup) ((ViewGroup)view).findViewById(R.id.storage_group);
		mStorageGroup.setOnCheckedChangeListener(this);

		mDownloadSizeTextView = (TextView)((ViewGroup)view).findViewById(R.id.storage_download_size);
		mEtcSizeTextView = (TextView)((ViewGroup)view).findViewById(R.id.storage_etc_size);
		mEmptySizeTextView = (TextView)((ViewGroup)view).findViewById(R.id.storage_empty_size);
		mCacheSizeTextView = (TextView)((ViewGroup)view).findViewById(R.id.storage_cache_size);
		mExternalStorages = MultiKollusStorage.getInstance(mContext).getStoragePathList();

		int storagIndex = 0;
		if(mExternalStorages.size() > 1) {
			mLocationLayer.setVisibility(View.VISIBLE);
			String extDir = android.os.Environment.getExternalStorageDirectory().toString();
			String storageLocation = Utils.getStoragePath(mContext);
			int index = 0;

			for(String path : mExternalStorages) {
				RadioButton btn = new RadioButton(mContext);
				if(path.startsWith(extDir))
					btn.setText(R.string.inner_storage);
				else
					btn.setText(R.string.outer_storage);
				btn.setId(RESOURCE_STORAGE_BASE_ID+index);
				mStorageGroup.addView(btn);
				Log.i(TAG, String.format("path %s storageLocation %s", path, storageLocation));
				if(storageLocation.startsWith(path)) {
					storagIndex = index;
					mStorageGroup.check(RESOURCE_STORAGE_BASE_ID+storagIndex);
				}
				
				index++;
			}
		}
		else {
			mLocationLayer.setVisibility(View.GONE);
		}

		chagedStorageIndex(storagIndex);
	}
	
	@Override
	public void onClick(View view) {
		// TODO Auto-generated method stub
		int id = view.getId();
		if(id == R.id.current_storage_empty) {
			int storageIndex;
			String savedPath = Utils.getStoragePath(mContext);
			for(storageIndex=0; storageIndex<mExternalStorages.size(); storageIndex++) {
				if(savedPath.contains(mExternalStorages.get(storageIndex)))
					break;
			}
			KollusStorage kollusStorage = mMultiStorage.getStorage(storageIndex);
			kollusStorage.clearCache();
			chagedStorageIndex(storageIndex);
		}
	}

	@Override
	public void onCheckedChanged(RadioGroup group, int checkId) {
		// TODO Auto-generated method stub
		Log.i(TAG, "onCheckedChanged id:"+checkId);
		if(checkId >= RESOURCE_STORAGE_BASE_ID && checkId < (RESOURCE_STORAGE_BASE_ID+mExternalStorages.size())) {
			chagedStorageIndex(checkId - RESOURCE_STORAGE_BASE_ID);
		}
	}
}

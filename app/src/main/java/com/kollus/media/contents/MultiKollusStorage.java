package com.kollus.media.contents;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.kollus.media.listener.MultiKollusPlayerDRMListener;
import com.kollus.media.listener.MultiKollusStorageListener;
import com.kollus.media.util.DiskUtil;
import com.kollus.sdk.media.KollusPlayerDRMListener;
import com.kollus.sdk.media.KollusPlayerDRMUpdateListener;
import com.kollus.sdk.media.KollusStorage;
import com.kollus.sdk.media.StoredLMSListener;
import com.kollus.sdk.media.content.KollusContent;
import com.kollus.sdk.media.util.ErrorCodes;
import com.kollus.sdk.media.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class MultiKollusStorage {
    private final String TAG = MultiKollusStorage.class.getSimpleName();

    private Context mContext;
    private String mKey;
    private String mExpireDate;
    private String mPlayerId;
    private String mPlayerIdMd5;
    private boolean mIsTablet;
    private List<MultiKollusPlayerDRMListener> mKollusPlayerDRMListener = new ArrayList<MultiKollusPlayerDRMListener>();
    private List<MultiKollusStorageListener> mMultiKollusStorageListener = new ArrayList<MultiKollusStorageListener>();

    private BroadcastReceiver mBRSdcard;
    private Vector<SDCardStateChangeListener> mSDStateChageListener = new Vector<SDCardStateChangeListener>();
    private int mErrorCode = ErrorCodes.ERROR_OK;

    class StorageInfo {
        private int mDiskIndex;
        private String mPath;
        private KollusStorage mStorage;
        private StorageInfo(String path, KollusStorage storage) {
            this.mPath = path;
            this.mStorage = storage;

            mStorage.registerKollusPlayerDRMListener(new KollusPlayerDRMListener() {
                @Override
                public void onDRM(String request, String response) {
                    synchronized (mKollusPlayerDRMListener) {
                        for (MultiKollusPlayerDRMListener listener : mKollusPlayerDRMListener) {
                            listener.onDRM(mStorage, request, response);
                        }
                    }
                }

                @Override
                public void onDRMInfo(KollusContent content, int nInfoCode) {
                    synchronized (mKollusPlayerDRMListener) {
                        for (MultiKollusPlayerDRMListener listener : mKollusPlayerDRMListener) {
                            listener.onDRMInfo(mStorage, content, nInfoCode);
                        }
                    }
                }
            });
            mStorage.registerKollusStorageListener(new KollusStorage.OnKollusStorageListener() {
                @Override
                public void onComplete(KollusContent kollusContent) {
                    kollusContent.setDownloadCompleted(true);
                    kollusContent.setDownloading(false);
                    synchronized (mMultiKollusStorageListener) {
                        for (MultiKollusStorageListener listener : mMultiKollusStorageListener) {
                            listener.onComplete(mStorage, kollusContent);
                        }
                    }
                }

                @Override
                public void onProgress(KollusContent kollusContent) {
                    long percent = kollusContent.getDownloadPercent();
                    if(kollusContent.getFileSize() > 0)
                        percent = kollusContent.getReceivingSize()*100/kollusContent.getFileSize();
                    kollusContent.setDownloading(true);
                    kollusContent.setDownloadPercent((int)percent);
                    synchronized (mMultiKollusStorageListener) {
                        for (MultiKollusStorageListener listener : mMultiKollusStorageListener) {
                            listener.onProgress(mStorage, kollusContent);
                        }
                    }
                }

                @Override
                public void onError(KollusContent kollusContent, int nErrorCode) {
                    kollusContent.setDownloading(false);
                    kollusContent.setDownloadError(true);
                    synchronized (mMultiKollusStorageListener) {
                        for (MultiKollusStorageListener listener : mMultiKollusStorageListener) {
                            listener.onError(mStorage, kollusContent, nErrorCode);
                        }
                    }
                }
            });
        }
    }

    private static MultiKollusStorage mInstance = null;
    Vector<StorageInfo> mStorageList = new Vector<StorageInfo>();

    private MultiKollusStorage(Context context) {
        mContext = context;

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_REMOVED);
        filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_EJECT);
        filter.addAction(Intent.ACTION_MEDIA_NOFS);
        filter.addDataScheme("file");
        mBRSdcard = new SDCardBroadcastReceiver(this);
        mContext.registerReceiver (mBRSdcard, filter);

    }

    public static synchronized MultiKollusStorage getInstance(Context context) {
        if(mInstance == null) {
            mInstance = new MultiKollusStorage(context);
        }

        return mInstance;
    }

    public void setCertification(String key, String expireDate, String playerId, String playerIdMd5, boolean bTablet) {
        mKey = key;
        mExpireDate = expireDate;
        mPlayerId = playerId;
        mPlayerIdMd5 = playerIdMd5;
        mIsTablet = bTablet;

        Vector<String> diskList = DiskUtil.getExternalMounts(mContext);
        for (String path : diskList) {
            addStoragePath(path);
        }
    }

    private void addStoragePath(String path) {
        KollusStorage storage = null;
        for (StorageInfo storageInfo : mStorageList) {
            if(storageInfo.mPath.startsWith(path)) {
                storage = storageInfo.mStorage;
                break;
            }
        }

        if(storage == null) {
            Log.d(TAG, "addStoragePath "+path);
            storage = new KollusStorage(mContext);
            mErrorCode = storage.initialize(mKey, mExpireDate, mContext.getPackageName());
            if (mErrorCode == ErrorCodes.ERROR_OK) {
                int nRet = storage.setDevice(path, mPlayerId, mPlayerIdMd5, mIsTablet);
                if(nRet == ErrorCodes.ERROR_OK) {
                    StorageInfo storageInfo = new StorageInfo(path, storage);
                    mStorageList.add(storageInfo);
                }
                else {
                    Log.e(TAG, "addStoragePath >>> setDevice >>> error "+nRet);
                    storage.finish();
                    if(mStorageList.isEmpty())
                        mErrorCode = nRet;
                }
            }
            else {
                Log.e(TAG, "addStoragePath >>> initialize >>> error "+mErrorCode);
            }
        }
    }

    private void removeStroagePath(String path) {
        for (StorageInfo storage : mStorageList) {
            if(storage.mPath.startsWith(path)) {
                Log.d(TAG, "removeStroagePath "+path);
                storage.mStorage.finish();
                mStorageList.remove(storage);
            }
        }
    }

    public Vector<String> getStoragePathList() {
        Vector<String> pathList = new Vector<String>();
        for (StorageInfo storage : mStorageList) {
            pathList.add(storage.mPath);
        }
        return pathList;
    }

    public int getErrorCode() {
        return mErrorCode;
    }

    public boolean isInnerDisk(KollusStorage storage) {
        return mStorageList.get(0).mStorage == storage;
    }

    public KollusStorage getStorage(int index) {
        if(mStorageList.size() > index)
            return mStorageList.get(index).mStorage;
        else
            return null;
    }

    public int getStorageIndex(KollusStorage storage) {
        for (int i=0; i<mStorageList.size(); i++) {
            if(mStorageList.get(i).mStorage ==  storage)
                return i;
        }

        return 0;
    }

    public boolean isReady(int index) {
        if(mStorageList.size() <= index) {
            return false;
        }
        return mStorageList.get(index).mStorage.isReady();
    }

    public void setNetworkTimeout(int timeout, int retryCount) {
        for (StorageInfo storage : mStorageList)
            storage.mStorage.setNetworkTimeout(timeout, retryCount);
    }

    public String getLastError(int diskIndex) {
        return mStorageList.get(diskIndex).mStorage.getLastError();
    }

    public boolean getKollusContent(int diskIndex, KollusContent content, String mck) {
        return mStorageList.get(diskIndex).mStorage.getKollusContent(content, mck);
    }

    public MultiKollusContent getDownloadKollusContent(String path) {
        for(int i=0; 1<mStorageList.size(); i++) {
            KollusContent content = mStorageList.get(i).mStorage.getDownloadKollusContent(path);
            if (content != null)
                return new MultiKollusContent(mStorageList.get(i).mStorage, content);
        }

        return null;
    }

    public String checkVersion() {
        return mStorageList.get(0).mStorage.checkVersion();
    }

    public void registerKollusPlayerDRMListener(MultiKollusPlayerDRMListener listener) {
        synchronized (mKollusPlayerDRMListener) {
            if(!mKollusPlayerDRMListener.contains(listener))
                mKollusPlayerDRMListener.add(listener);
        }
    }

    public void unregisterKollusPlayerDRMListener(MultiKollusPlayerDRMListener listener) {
        synchronized (mKollusPlayerDRMListener) {
            if(mKollusPlayerDRMListener.contains(listener))
                mKollusPlayerDRMListener.remove(listener);
        }
    }

    public void registerKollusStorageListener(MultiKollusStorageListener listener) {
        synchronized (mMultiKollusStorageListener) {
            if(!mMultiKollusStorageListener.contains(listener))
                mMultiKollusStorageListener.add(listener);
        }
    }

    public void unregisterKollusStorageListener(MultiKollusStorageListener listener) {
        synchronized (mMultiKollusStorageListener) {
            if(mMultiKollusStorageListener.contains(listener))
                mMultiKollusStorageListener.remove(listener);
        }
    }

    public ArrayList<MultiKollusContent> getDownloadList(int diskIndex) {
        ArrayList<MultiKollusContent> multiList = new ArrayList<MultiKollusContent>();

        if(diskIndex == -1) {
            for (int i = 0; i < mStorageList.size(); i++) {
                ArrayList<KollusContent> list = mStorageList.get(i).mStorage.getDownloadContentList();
                Log.d(TAG, String.format("multi storage %dth disk content count %d", i, list.size()));
                for (KollusContent content : list) {
                    multiList.add(new MultiKollusContent(mStorageList.get(i).mStorage, content));
                    Log.d(TAG, String.format("multi storage %dth disk mck '%s'", i, content.getMediaContentKey()));
                }
            }
        }
        else {
            ArrayList<KollusContent> list = mStorageList.get(diskIndex).mStorage.getDownloadContentList();
            Log.d(TAG, String.format("single storage %dth disk content count %d", diskIndex, list.size()));
            for (KollusContent content : list) {
                multiList.add(new MultiKollusContent(mStorageList.get(diskIndex).mStorage, content));
                Log.d(TAG, String.format("single storage %dth disk mck '%s'", diskIndex, content.getMediaContentKey()));
            }
        }
        return multiList;
    }

    public void sendStoredLms(StoredLMSListener listener) {
        for (int i = 0; i < mStorageList.size(); i++) {
            mStorageList.get(i).mStorage.sendStoredLMS(listener);
        }
    }

    public void updateDownloadDRMInfo(int diskIndex, KollusPlayerDRMUpdateListener listener, boolean bAll) {
        if(diskIndex == -1) {
            for (int i = 0; i < mStorageList.size(); i++) {
                mStorageList.get(i).mStorage.updateDownloadDRMInfo(listener, bAll);
            }
        }
        else {
            mStorageList.get(diskIndex).mStorage.updateDownloadDRMInfo(listener, bAll);
        }
    }

    public void registerSDStateChangeListener(SDCardStateChangeListener listener) {
        synchronized (mSDStateChageListener) {
            mSDStateChageListener.add(listener);
        }
    }

    public void unregisterSDStateChangeListener(SDCardStateChangeListener listener) {
        synchronized (mSDStateChageListener) {
            for(SDCardStateChangeListener iter : mSDStateChageListener) {
                if(iter == listener) {
                    mSDStateChageListener.remove(iter);
                    break;
                }
            }
        }
    }

    class SDCardBroadcastReceiver extends BroadcastReceiver {
        private MultiKollusStorage mMultiKollusStorage;
        SDCardBroadcastReceiver(MultiKollusStorage multiKollusStorage) {
            mMultiKollusStorage = multiKollusStorage;
        }

        @Override
        public void onReceive (Context context, Intent intent){
            String action = intent.getAction();
            String path = intent.getDataString().substring(7)+"/Android/data/"+context.getPackageName();
            Log.d(TAG, path+" ==> "+action);
            if (action.equals(Intent.ACTION_MEDIA_MOUNTED)){
                Log.d(TAG, "addStoragePath ["+path+"] call.");
                mMultiKollusStorage.addStoragePath(path);
                synchronized (mSDStateChageListener) {
                    for(SDCardStateChangeListener iter : mSDStateChageListener) {
                        iter.onStateChaged(path, true);
                    }
                }
            }
            else if(action.equals(Intent.ACTION_MEDIA_UNMOUNTED) ||
                    action.equals(Intent.ACTION_MEDIA_EJECT)) {
                mMultiKollusStorage.removeStroagePath(path);
                synchronized (mSDStateChageListener) {
                    for(SDCardStateChangeListener iter : mSDStateChageListener) {
                        iter.onStateChaged(path, false);
                    }
                }
            }
        }
    }

    public interface SDCardStateChangeListener {
        void onStateChaged(String path, boolean mounted);
    }
}

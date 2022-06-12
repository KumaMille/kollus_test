package com.kollus.media.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.preference.PreferenceManager;

import androidx.core.content.ContextCompat;

import com.kollus.media.contents.MultiKollusStorage;
import com.kollus.sdk.media.util.Log;
import com.kollus.sdk.media.util.Utils;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Vector;

/**
 * Created by Song on 2017-09-07.
 */

public class DiskUtil {
    private static final String TAG = DiskUtil.class.getSimpleName();
    private static final double ONE_KILOBYTE = 1024;
    private static final double ONE_MEGABYTE = ONE_KILOBYTE*1024;
    private static final double ONE_GIGABYTE = ONE_MEGABYTE*1024;
    private static StorageManager mStorageManager = null;

    /**
     * 주어진 사이즈 String으로 가져오는 함수
     * 예 : 1.00GB, 1.00MB, 1.00KB, 1B
     * @param size
     * @return
     */
    public static String getStringSize(long size) {
        if(size >= ONE_GIGABYTE)
            return String.format("%1.2fGB", size/ONE_GIGABYTE);
        else if(size > ONE_MEGABYTE)
            return String.format("%1.2fMB", size/ONE_MEGABYTE);
        else if(size > ONE_KILOBYTE)
            return String.format("%1.2fKB", size/ONE_KILOBYTE);
        else if(size > 0)
            return String.format("%dB", size);
        else
            return "0B";
    }/**
     * 외부 SD 카드 경로를 배열로 가져오는 함수
     * @param context
     * @return SD카드 경로
     */
    public static Vector<String> getExternalMounts(Context context) {
        final Vector<String> out = new Vector<String>();
        File[] dirs = ContextCompat.getExternalFilesDirs(context, null);
        for(File iter : dirs) {
            if(iter != null) {
                Log.d(TAG, "getExternalMounts "+iter.getAbsolutePath());
                if(new File(iter.getParent()).canWrite()) {
                    out.add(iter.getParent());
                    Log.d(TAG, "SDCard Path : " + iter.getParent() + " Mounted.");
                }
                else {
                    Log.e(TAG, "SDCard Path : " + iter.getParent() + " Unmounted.");
                }
            }
        }

        return out;
    }

    public static int getDiskIndex(Context context) {
        int storagIndex = 0;
        String storageLocation = Utils.getStoragePath(context);
        Vector<String> storageList = MultiKollusStorage.getInstance(context).getStoragePathList();

        Log.d(TAG, String.format("getDiskIndex pathList '%s'", storageList.toString()));
        for(String path : storageList) {
            if(storageLocation.startsWith(path))
                break;
            storagIndex++;
        }

        if(storageList.size() <= storagIndex)
            storagIndex = 0;

        Log.d(TAG, String.format("getDiskIndex '%s' --> %d", storageLocation, storagIndex));
        return storagIndex;
    }
}

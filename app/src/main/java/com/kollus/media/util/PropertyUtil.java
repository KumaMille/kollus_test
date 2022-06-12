package com.kollus.media.util;

import android.util.Log;

import com.kollus.sdk.media.content.KollusContent;

import java.lang.reflect.Method;
import java.util.HashMap;

public class PropertyUtil {
    private static final HashMap<String, String> sSystemPropertyMap = new HashMap<String, String>();

    public static  String getSystemPropertyCached(String key) {
        String prop = sSystemPropertyMap.get(key);
        if (prop == null) {
            prop = getSystemProperty(key, "none");
            sSystemPropertyMap.put(key, prop);
        }
        return prop;
    }

    private static  String getSystemProperty(String key, String def) {
        try {
            final ClassLoader cl = ClassLoader.getSystemClassLoader();
            final Class<?> SystemProperties = cl.loadClass("android.os.SystemProperties");
            final Class<?>[] paramTypes = new Class[] { String.class, String.class };
            final Method get = SystemProperties.getMethod("get", paramTypes);
            final Object[] params = new Object[] { key, def };
            return (String) get.invoke(SystemProperties, params);
        } catch (Exception e){
            return def;
        }
    }

    public static void copyKollusContent(KollusContent dst, KollusContent src) {
        dst.setCompany(src.getCompany());
        dst.setCourse(src.getCourse());
        dst.setSubCourse(src.getSubCourse());
        dst.setTeacher(src.getTeachar());
        dst.setMediaContentKey(src.getMediaContentKey());
        dst.setMediaContentKeyMD5(src.getMediaContentKeyMD5());
        dst.setScreenShotPath(src.getScreenShotPath());
        dst.setThumbnailPath(src.getThumbnailPath());
        dst.setSynopsis(src.getSynopsis());
        dst.setDetailInfoUrl(src.getDetailInfoUrl());
        dst.setUriIndex(src.getUriIndex());
        dst.setPlaytime(src.getPlaytime());
        dst.setStartAt(src.getStartAt());
        dst.setDuration(src.getDuration());
        dst.setReceivedSize(src.getReceivedSize());
        dst.setReceivingSize(src.getReceivingSize());
        dst.setFileSize(src.getFileSize());
        dst.setDownloadPercent(src.getDownloadPercent());
        dst.setDownloadCompleted(src.isCompleted());
        dst.setExpirationDate(src.getExpirationDate());
        dst.setTotalExpirationCount(src.getTotalExpirationCount());
        dst.setExpirationCount(src.getExpirationCount());
        dst.setExpirationRefreshPopup(src.getExpirationRefreshPopup());
        dst.setContentExpired(src.isContentExpirated());
        dst.setVideoWidth(src.getVideoWidth());
        dst.setVideoHeight(src.getVideoHeight());
        dst.setBitrate(src.getBitrate());
    }
}

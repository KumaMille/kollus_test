package com.kollus.media.download;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.kollus.media.contents.MultiKollusStorage;
import com.kollus.sdk.media.KollusStorage;
import com.kollus.sdk.media.content.KollusContent;
import com.kollus.sdk.media.util.Log;
import com.kollus.sdk.media.util.Utils;

import java.util.Vector;

public class DownloadDBHelper extends SQLiteOpenHelper {
    private final String TAG = DownloadDBHelper.class.getSimpleName();
    private final static String DB_NAME = "contents.db";
    private final String TABLE_NAME = "downloading";
    private final static int DB_VERSION = 1;
    private static DownloadDBHelper mInstance = null;
    private MultiKollusStorage mMultiKollusStorage;

    public static DownloadDBHelper getInstance(Context context) {
        if(mInstance == null)
            mInstance = new DownloadDBHelper(context);

        return mInstance;
    }

    private DownloadDBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        mMultiKollusStorage = MultiKollusStorage.getInstance(context);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        String sql = "Create Table "+TABLE_NAME+" (location integer, folder text, path text, mck text)";
        sqLiteDatabase.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        onCreate(sqLiteDatabase);
    }

    public void add(DownloadInfo info) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues value = new ContentValues();

        int location = mMultiKollusStorage.getStorageIndex(info.getKollusStorge());
        value.put("location", new Integer(location));
        value.put("folder", info.getFolder());
        value.put("path", info.getUrl());
        value.put("mck", info.getMultiKollusContent().getKollusContent().getMediaContentKey());

        db.insert(TABLE_NAME, null, value);
        db.close();
        Log.d(TAG, String.format("add location %d mck '%s'", location, info.getMultiKollusContent().getKollusContent().getMediaContentKey()));
    }

    public void delete(DownloadInfo info) {
        if(info.getMultiKollusContent() != null && info.getMultiKollusContent().getKollusContent() != null) {
            SQLiteDatabase db = this.getWritableDatabase();
            int location = mMultiKollusStorage.getStorageIndex(info.getKollusStorge());
            db.delete(TABLE_NAME, "location=? and mck=?", new String[]{
                    Integer.toString(location),
                    info.getMultiKollusContent().getKollusContent().getMediaContentKey()}
            );
            db.close();
            Log.d(TAG, String.format("delete location %d mck '%s'", location, info.getMultiKollusContent().getKollusContent().getMediaContentKey()));
        }
    }

    public void deleteAll() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("delete from "+ TABLE_NAME);
        db.close();
    }

    public Vector<DownloadInfo> list() {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {"location", "folder", "path", "mck"};
        Cursor cursor = db.query(TABLE_NAME, columns,
                null, null,
                null, null, null);

        Vector<DownloadInfo> list = new Vector<DownloadInfo>();
        int recordCount = cursor.getCount();
        for(int i=0; i<recordCount; i++) {
            cursor.moveToNext();
            int location = Integer.parseInt(cursor.getString(0));
            String folder = cursor.getString(1);
            String path = cursor.getString(2);
            String mck = cursor.getString(3);
            KollusStorage storage = mMultiKollusStorage.getStorage(location);
            DownloadInfo info = new DownloadInfo(storage, folder, path);
            if(storage != null && info.getMultiKollusContent() != null) {
                boolean isFounded = storage.getKollusContent(info.getMultiKollusContent().getKollusContent(), mck);
                if (!isFounded)
                    info.getMultiKollusContent().getKollusContent().setMediaContentKey(mck);
                    Log.d(TAG, String.format("List %dth --> location %d folder '%s' path '%s' mck '%s'",
                        i, location, folder, path, mck));
                list.add(info);
            }
         }

        cursor.close();
        db.close();
        return list;
    }
}

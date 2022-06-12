package com.kollus.media.util;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;

import java.io.FileOutputStream;
import java.io.IOException;

import static android.content.Context.WINDOW_SERVICE;

/**
 * Created by Song on 2018-03-20.
 */

public class DisplayUtil {
    public static int getOrientation(Context context) {
//		Display display = getWindowManager().getDefaultDisplay();
        Display display = ((WindowManager) context.getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
        int rotation = display.getRotation();

        Point size = new Point();
        size.x = display.getWidth();
        size.y = display.getHeight();

        int lock = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;

        if (rotation == Surface.ROTATION_0
                || rotation == Surface.ROTATION_180) {
            // if rotation is 0 or 180 and width is greater than height, we have
            // a tablet
            if (size.x > size.y) {
                if (rotation == Surface.ROTATION_0) {
                    lock = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                } else {
                    lock = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                }
            } else {
                // we have a phone
                if (rotation == Surface.ROTATION_0) {
                    lock = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                } else {
                    lock = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                }
            }
        } else {
            // if rotation is 90 or 270 and width is greater than height, we
            // have a phone
            if (size.x > size.y) {
                if (rotation == Surface.ROTATION_90) {
                    lock = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                } else {
                    lock = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                }
            } else {
                // we have a tablet
                if (rotation == Surface.ROTATION_90) {
                    lock = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                } else {
                    lock = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                }
            }
        }
        return lock;
    }

    public static boolean saveScreenShot(View view, int width, int height, String fileName) {
        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache(true);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);

        FileOutputStream fos;
        try {
            fos = new FileOutputStream(fileName);
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos);
            fos.flush();
            fos.close();

            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    public static boolean saveScreenShot(Bitmap bitmap, String fileName) {
        Canvas canvas = new Canvas(bitmap);

        FileOutputStream fos;
        try {
            fos = new FileOutputStream(fileName);
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos);
            fos.flush();
            fos.close();

            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }
}

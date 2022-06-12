/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kollus.media;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import com.kollus.sdk.media.content.KollusBookmark;
import com.kollus.sdk.media.util.Log;
import com.kollus.sdk.media.util.Utils;

import java.util.ArrayList;

import static android.content.ContentValues.TAG;

/**
 * The time bar view, which includes the current and total time, the progress
 * bar, and the scrubber.
 */
public class TimeBar extends View {

    public interface Listener {
        void onScrubbingStart();

        void onScrubbingMove(int time);

        void onScrubbingEnd(int time, int start, int end);
    }

    // The total padding, top plus bottom
    private static final int TEXT_SIZE_IN_DP = 14;

    private final int PLAYED_COLOR = 0xCC3F8EC4;

    private final int BOOKMAKR_INDDEX_WIDTH = 3;
    private final float STROK_WIDTH = 5;

    protected Listener mListener;

    // the bars we use for displaying the progress
    protected Rect mProgressBar;
    protected Rect mPlayedBar;
    protected Rect mSeekableBar;
    protected Rect mRepeatARect;
    protected Rect mRepeatBRect;

    protected Paint mProgressPaint;
    protected Paint mPlayedPaint;
    protected Paint mSeekablePaint;
    protected Paint mTimeTextPaint;
    protected Paint mTimeTextStrokPaint;
    protected Paint mBookmarkIndexPaint;

    protected int mTotalTime = -1;
    protected int mCurrentTime;
    protected int mSeekableEndTime;

    protected Rect mTimeBounds;

    protected int mScrubberLeft;
    protected boolean mDragging = false;
    protected int mDraggingTime;
    protected boolean mSeekable = true;
    protected ArrayList<KollusBookmark> mBookmarkList;
    
    private int mRepeatAMs = -1;
    private int mRepeatBMs = -1;
    private boolean mTimeShift;

    public TimeBar(Context context) {
        super(context);        
        init(context);
    }
    
    public TimeBar(Context context, AttributeSet attrs) {
        super(context, attrs);        
        init(context);
    }
    
    public TimeBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);        
        init(context);
    }
    
    private void init(Context context) {
        mBookmarkList = new ArrayList<KollusBookmark>();
        mProgressBar = new Rect();
        mPlayedBar = new Rect();
        mSeekableBar = new Rect();
        mRepeatARect = new Rect();
        mRepeatBRect = new Rect();

        mProgressPaint = new Paint();
        mProgressPaint.setColor(0x7F000000);
        mPlayedPaint = new Paint();
        mPlayedPaint.setColor(PLAYED_COLOR);
        mSeekablePaint = new Paint();
        mSeekablePaint.setColor(getSeekableColor(PLAYED_COLOR));
        mBookmarkIndexPaint = new Paint();
        mBookmarkIndexPaint.setColor(Color.WHITE);
        
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        float textSizeInPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_IN_DP, metrics);
        mTimeTextPaint = new Paint();
        mTimeTextPaint.setColor(0xFFCECECE);
        mTimeTextPaint.setTextSize(textSizeInPx);
        mTimeTextPaint.setAntiAlias(true);

        mTimeTextStrokPaint = new Paint();
        mTimeTextStrokPaint.setColor(Color.BLACK);
        mTimeTextStrokPaint.setTextSize(textSizeInPx);
        mTimeTextStrokPaint.setStyle(Paint.Style.STROKE);
        mTimeTextStrokPaint.setAntiAlias(true);
        mTimeTextStrokPaint.setStrokeWidth(STROK_WIDTH);

        mTimeBounds = new Rect();
        setPadding(24, 16, 24, 16);
    }
    
    public void setListener(Listener listener) {
    	mListener = Utils.checkNotNull(listener);
    }
    
    public void setPlayedColor(int color) {
    	mPlayedPaint.setColor(color);
        mSeekablePaint.setColor(getSeekableColor(color));
    }
    
    public void setProgressColor(int color) {
    	mProgressPaint.setColor(color);
    }

    public void setIndexColor(int color) {
        mBookmarkIndexPaint.setColor(color);
    }
    
    public void setSeekable(boolean enable) {
    	mSeekable = enable;
    }

    public void setBookmarkList(ArrayList<KollusBookmark> list) {
        mBookmarkList.addAll(list);
    }
    
    public void setRepeatAB(int repeatAMs, int repeatBMs) {
    	mRepeatAMs = repeatAMs;
    	mRepeatBMs = repeatBMs;
    }

    private int getSeekableColor(int color) {
        int alpha = Color.alpha(color);
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);

        int nSetIndex = 2;
        float fSetColor = hsv[nSetIndex];
        fSetColor *= 0.8f;

        if(fSetColor < 0f)
            fSetColor = 0.f;
        else if(fSetColor > 1.0f)
            fSetColor = 1.0f;
        hsv[nSetIndex] = fSetColor;

        color = Color.HSVToColor(alpha, hsv);

        return color;
    }

    private void update() {
        mPlayedBar.set(mProgressBar);
        mSeekableBar.set(mProgressBar);

        if (mTotalTime > 0) {
        	if(mDragging) {
        		mPlayedBar.right =
                        mPlayedBar.left + (int) ((mProgressBar.width() * (long) mDraggingTime) / mTotalTime);
        		mSeekableBar.left = mPlayedBar.right;
                mSeekableBar.right =
                        mSeekableBar.left + (int) ((mProgressBar.width() * (long) (mSeekableEndTime-mDraggingTime)) / mTotalTime);
        	}
        	else {
        		mPlayedBar.right =
                    mPlayedBar.left + (int) ((mProgressBar.width() * (long) mCurrentTime) / mTotalTime);
        		mSeekableBar.left = mPlayedBar.right;
        		mSeekableBar.right =
        				mSeekableBar.left + (int) ((mProgressBar.width() * (long) (mSeekableEndTime-mCurrentTime)) / mTotalTime);
        	}
        	
        	if(mRepeatAMs >= 0) {
        		mRepeatARect.left = (int) ((mProgressBar.width() * (long) mRepeatAMs) / mTotalTime);
        		mRepeatARect.top = mProgressBar.top;
        		mRepeatARect.right = mRepeatARect.left+2;
        		mRepeatARect.bottom = mProgressBar.bottom;
        	}
        	
        	if(mRepeatBMs >= 0) {
        		mRepeatBRect.left = (int) ((mProgressBar.width() * (long) mRepeatBMs) / mTotalTime);
        		mRepeatBRect.top = mProgressBar.top;
        		mRepeatBRect.right = mRepeatBRect.left+2;
        		mRepeatBRect.bottom = mProgressBar.bottom;
        	}
        } else {
            if(mCurrentTime == mTotalTime) {
                mPlayedBar.right = mProgressBar.left + mProgressBar.width();
                mSeekableBar.right = mPlayedBar.right;
            }
            else {
                mPlayedBar.right = mProgressBar.left;
                mSeekableBar.right = mProgressBar.left;
            }
        }

        invalidate();
    }

    public int getBarHeight() {
        return mProgressBar.height();
    }
    
    public int getBarWidth() {
    	return mProgressBar.width();
    }

    public void setTime(int currentTime, int seekableEndTime, int totalTime,
            int trimStartTime, int trimEndTime) {
        if (mCurrentTime == currentTime && mSeekableEndTime == seekableEndTime && mTotalTime == totalTime) {
            return;
        }

        if(mTotalTime != totalTime) {
            String duration;
            if(totalTime >= 10*60*60*1000) {
                duration = Utils.stringForTimeHHMMSS(totalTime);
            }
            else if(totalTime >= 60*60*1000) {
                duration = Utils.stringForTimeHMMSS(totalTime);
            }
            else {
                duration = Utils.stringForTimeMMSS(totalTime);
            }
            mTimeTextPaint.getTextBounds(duration, 0, duration.length(), mTimeBounds);
        }

        mCurrentTime = currentTime;
        mSeekableEndTime = seekableEndTime;
        mTotalTime = totalTime;
        update();
    }
    
    private void clampScrubber() {
        int max = mProgressBar.right;
        int min = mProgressBar.left;
        mScrubberLeft = Math.min(max, Math.max(min, mScrubberLeft));
    }

    public int getScrubberTime() {
        Log.d(TAG, String.format("getScrubberTime ScrubberLeft %d TotalTime %d ProgressBar (%d %d)",
                mScrubberLeft, mTotalTime, mProgressBar.left, mProgressBar.width()));
        int position = (int) ((long) (mScrubberLeft + - mProgressBar.left)
                * mTotalTime / mProgressBar.width());

        if(mTimeShift)
            position = position-mTotalTime;
        return position;
    }
    
    public int getScrubberX() {
        int position;
    	if(mDragging)
            position = (int)((float)mDraggingTime / (float)mTotalTime * (float)mProgressBar.width());
    	else
            position = (int)((float)mCurrentTime / (float)mTotalTime * (float)mProgressBar.width());

        if(mTimeShift)
            position = position-mTotalTime;

        return position;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int w = r - l;
        int h = b - t;
        
        mProgressBar.set(0, 0, w, h);
        update();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // draw progress bars
    	Rect rect = new Rect();
        if(mSeekableEndTime >= 0)
            rect.left = Math.max(mSeekableBar.right, mPlayedBar.right);
        else
        	rect.left = mPlayedBar.right;
    	rect.top = mProgressBar.top;
    	rect.right = mProgressBar.right;
    	rect.bottom = mProgressBar.bottom;

        canvas.drawRect(rect, mProgressPaint);
        canvas.drawRect(mPlayedBar, mPlayedPaint);
        canvas.drawRect(mSeekableBar, mSeekablePaint);

        for(KollusBookmark bookmark : mBookmarkList) {
            if(bookmark.getLevel() == KollusBookmark.PROVIDER_LEVEL) {
                rect = new Rect();
                rect.left = (mProgressBar.width() * bookmark.getTime()) / mTotalTime;
                rect.top = mProgressBar.top;
                rect.right = rect.left + BOOKMAKR_INDDEX_WIDTH;
                rect.bottom = mProgressBar.bottom;
                canvas.drawRect(rect, mBookmarkIndexPaint);
            }
        }
        
        // draw timers
    	String strPlayTime;
    	String strDuration;
    	int playTime, duration;
        if(mTimeShift) {
            playTime = -mTotalTime;
            duration = mCurrentTime-mTotalTime;
        }
        else {
            playTime = mCurrentTime;
            duration = mTotalTime;
        }

        if(mTotalTime >= 10*60*60*1000) {
            strPlayTime = Utils.stringForTimeHHMMSS(playTime);
            strDuration = Utils.stringForTimeHHMMSS(duration);
        }
    	else if(mTotalTime >= 60*60*1000) {
            strPlayTime = Utils.stringForTimeHMMSS(playTime);
            strDuration = Utils.stringForTimeHMMSS(duration);
        }
    	else {
            strPlayTime = Utils.stringForTimeMMSS(playTime);
            strDuration = Utils.stringForTimeMMSS(duration);
    	}

        canvas.drawText(
                strPlayTime,
                getPaddingLeft(),
                (mProgressBar.height()+mTimeBounds.height())/2,
                mTimeTextStrokPaint);
        canvas.drawText(
                strPlayTime,
                getPaddingLeft(),
                (mProgressBar.height()+mTimeBounds.height())/2,
                mTimeTextPaint);

        canvas.drawText(
                strDuration,
                getWidth() - getPaddingRight() - mTimeBounds.width(),
                (mProgressBar.height()+mTimeBounds.height())/2,
                mTimeTextStrokPaint);
        canvas.drawText(
                strDuration,
                getWidth() - getPaddingRight() - mTimeBounds.width(),
                (mProgressBar.height()+mTimeBounds.height())/2,
                mTimeTextPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
    	if(mSeekable) {
	    	int x = (int) event.getX();
	        int y = (int) event.getY();
	
	        switch (event.getAction()) {
	            case MotionEvent.ACTION_DOWN: {
					mListener.onScrubbingStart();
					mDragging = true;
	            }
	            // fall-through
	            case MotionEvent.ACTION_MOVE: {
	            	mScrubberLeft = x;
	                clampScrubber();
	                mDraggingTime = getScrubberTime();
                    Log.d(TAG, "move getScrubberTime "+mDraggingTime);
	                mListener.onScrubbingMove(mDraggingTime);
	//                invalidate();
	                requestLayout();
	                return true;
	            }
	            case MotionEvent.ACTION_CANCEL:
	            case MotionEvent.ACTION_UP: {
                    mDraggingTime = getScrubberTime();
                    Log.d(TAG, "moved getScrubberTime "+mDraggingTime);
	                mListener.onScrubbingEnd(mDraggingTime, 0, 0);
	                mDragging = false;
	                return true;
	            }
	        }
    	}

        return true;
    }

    public void setTimeShift(boolean bTimeShift) {
        mTimeShift = bTimeShift;
    }
}

package com.kollus.media.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.RadioButton;

import com.kollus.sdk.media.util.Log;

public class SegmentedControlButton extends RadioButton {

    private float mCenterX;
    private float mCenterY;
    private int mCount;
    private int mBackgroundCheckedId, mBackgroundNormalId;

    public SegmentedControlButton(Context context) {
        super(context);
    }

    public SegmentedControlButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SegmentedControlButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    private static final float TEXT_SIZE = 16.0f;
    
    public void setCount(int count) {
    	mCount = count;
        invalidate();
    }

    @Override
    public void onDraw(Canvas canvas) {

        String text = this.getText().toString();
        Paint textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setTextSize(getTextSize());
        textPaint.setTextAlign(Paint.Align.CENTER);
        float currentHeight = textPaint.measureText("X");

        if (isChecked()) {
            Drawable back = getContext().getResources().getDrawable(mBackgroundCheckedId);
            back.setBounds(0, 0, this.getWidth(), this.getHeight());
            back.draw(canvas);
            textPaint.setColor(0xff3598db);
            text += "("+mCount+")";
        } else {
            Drawable back = getContext().getResources().getDrawable(mBackgroundNormalId);
            back.setBounds(0, 0, this.getWidth(), this.getHeight());
            back.draw(canvas);
            textPaint.setColor(Color.WHITE);
        }
        
        canvas.drawText(text, mCenterX, mCenterY+currentHeight/2, textPaint);
    }

    public void setBackgroundResource(int checkedId, int normalId) {
		// TODO Auto-generated method stub
    	mBackgroundCheckedId = checkedId;
    	mBackgroundNormalId = normalId;
	}

	@Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        mCenterX = w * 0.5f; // remember the center of the screen
        mCenterY = h * 0.5f;
    }

}

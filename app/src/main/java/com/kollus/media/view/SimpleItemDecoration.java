package com.kollus.media.view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import com.kollus.media.R;

/**
 * Created by Song on 2016-09-28.
 */

public class SimpleItemDecoration extends RecyclerView.ItemDecoration {
    private final int mDivHeight;
    private Drawable mDivider;

    public SimpleItemDecoration(Context context, int dp) {
        Resources r = context.getResources();
        mDivider = r.getDrawable(R.drawable.line_divider);

        DisplayMetrics metrics = r.getDisplayMetrics();
        mDivHeight = Math.round(dp*(metrics.xdpi/DisplayMetrics.DENSITY_DEFAULT));
    }

    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
        int left = parent.getPaddingLeft();
        int right = parent.getWidth() - parent.getPaddingRight();

        int childCount = parent.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = parent.getChildAt(i);

            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();

            int top = child.getBottom() + params.bottomMargin;
            int bottom = top + mDivider.getIntrinsicHeight();

            mDivider.setBounds(left, top, right, bottom);
            mDivider.draw(c);
        }
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);
        outRect.top = mDivHeight;
    }
}

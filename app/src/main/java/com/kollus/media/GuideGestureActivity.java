package com.kollus.media;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.viewpager.widget.ViewPager;
import androidx.viewpager.widget.PagerAdapter;

import com.kollus.media.util.ActivityStack;

public class GuideGestureActivity extends BaseActivity {
    private ViewPager mViewPager;
    private ActivityStack mActivityStack;

    final private int GUIDE_TOTAL_COUNT = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.guide_gesture_layout);

        mActivityStack = ActivityStack.getInstance();
        mActivityStack.regOnCreateState(this);

        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setOffscreenPageLimit(1);    // TODO: Check
        mViewPager.setAdapter(new GesturePagerAdapter(this));

        ImageView btn = (ImageView)findViewById(R.id.btn_back);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO Auto-generated method stub
                setResult(RESULT_OK, getIntent());
                finish();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mActivityStack.regOnResumeState(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mActivityStack.regOnPauseState(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mActivityStack.regOnDestroyState(this);
    }

    private class GesturePagerAdapter extends PagerAdapter {
        private LayoutInflater mInflater;

        private GesturePagerAdapter(Context context) {
            super();
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return GUIDE_TOTAL_COUNT;
        }

        @Override
        public Object instantiateItem(ViewGroup pager, int position) {
            View view = getItem(position);
            pager.addView(view, 0);
            return view;
        }

        @Override
        public void destroyItem(ViewGroup pager, int position, Object view) {
            pager.removeView((View) view);
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        private View getItem(int position) {
            View view = mInflater.inflate(R.layout.guide_gesture_view, null);

            ImageView imageView = (ImageView) view.findViewById(R.id.guide_image);
            imageView.setImageResource(rArrayGuideImage[position]);

            TextView guideText = (TextView) view.findViewById(R.id.guide_text);
            guideText.setText(rArrayGuideText[position]);

            int curPage = position + 1;
            TextView textPageNum = (TextView) view.findViewById(R.id.guide_page_num);
            textPageNum.setText(curPage + "/" + GUIDE_TOTAL_COUNT);

            return view;
        }
    }

    final private int[] rArrayGuideImage = {
        R.drawable.gesture_zoom,
        R.drawable.gesture_brightness,
        R.drawable.gesture_sound,
        R.drawable.gesture_seek
    };

    final private int[] rArrayGuideText = {
        R.string.help_size_control,
        R.string.help_bright_control,
        R.string.help_volume_control,
        R.string.help_seek
    };
}

package com.kollus.media;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import com.kollus.media.util.ActivityStack;

public class GuideShortCutsActivity extends BaseActivity {
    private ActivityStack mActivityStack;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.guide_short_cuts);

        mActivityStack = ActivityStack.getInstance();
        mActivityStack.regOnCreateState(this);

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
}

package com.kollus.media.view;

import java.util.Vector;

import com.kollus.media.R;
import android.content.Context;
import android.content.res.Resources;
import android.database.DataSetObserver;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

public class BookmarkScrollView extends HorizontalScrollView implements OnClickListener {

	private final static String TAG = BookmarkScrollView.class.getSimpleName();
    
    private ArrayAdapter mAdapter;
    private int mItemCount;
    private LinearLayout mLayout;
    private Vector<View> mRecycler;
    private OnItemClickListener mItemClickListener;
    
    private int mLayoutWidth = -1;
    private int mLayoutHeight = -1;
    private int mChildWidth = -1;
    private int mChildHeight = -1;
    private int mMarginSmall;
    private int mSelected = -1;
    
    private AdapterDataSetObserver mDataSetObserver;
    
    public BookmarkScrollView(Context context) {
 		super(context);
 		init(context);
 	}

 	public BookmarkScrollView(Context context, AttributeSet attrs) {
  		super(context, attrs);
  		init(context);
 	}
 
  	public BookmarkScrollView(Context context, AttributeSet attrs, int defStyle) {
   		super(context, attrs, defStyle);
   		init(context);
  	}
  	
  	private void init(Context context) {
  		final float DEFAULT_DENSITY = 1.5f;
  		Resources r = context.getResources();
  		DisplayMetrics metrics = r.getDisplayMetrics();
  		float scale = metrics.density/DEFAULT_DENSITY;
  		mMarginSmall =  (int) (r.getDimensionPixelSize(R.dimen.margin_small)*scale);
  	}
  	
  	public void setLayout(LinearLayout layout) {
  		mLayout = layout;
  	}
  	
  	public void setAdapter(ArrayAdapter adapter) {
  		mAdapter = adapter;
  		mDataSetObserver = new AdapterDataSetObserver();
  		mAdapter.registerDataSetObserver(mDataSetObserver);
  		mRecycler = new Vector<View>();
  		
  		int i=0;
  		if(mAdapter.getCount() > 0) {
  			mRecycler.add(mAdapter.getView(i, null, BookmarkScrollView.this));
  			i++;
  		}
  	}
  	
  	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		// TODO Auto-generated method stub
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		
		mLayout.removeAllViews();
		mItemCount = mAdapter == null ? 0 : mAdapter.getCount();

		if(mRecycler.size() == mItemCount) {
			for(int i=0; i<mItemCount; i++) {
				mLayout.addView(mAdapter.getView(i, mRecycler.get(i), BookmarkScrollView.this));
			}
		}
		else {
			mRecycler.clear();
			for(int i=0; i<mItemCount; i++) {
				View view = mAdapter.getView(i, null, BookmarkScrollView.this);
				view.setOnClickListener(this);
				mLayout.addView(view);
				mRecycler.add(view);
				
				if(i > 0) {
					LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)view.getLayoutParams();
					params.leftMargin = 10;
					view.setLayoutParams(params);
				}
			}
		}
	}
  	
  	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		// TODO Auto-generated method stub
		super.onLayout(changed, l, t, r, b);
		
    	mLayoutWidth = r-1;
    	mLayoutHeight = b-t;
        if(mRecycler.size() > 1 && mChildWidth < 0) {
        	View view = mRecycler.get(1);
        	mChildWidth = view.getWidth();
        	mChildHeight = view.getHeight();
        	
//        	BookmarkViewHolder holder = (BookmarkViewHolder)view.getTag();
//        	Log.d(TAG, String.format("Bookmark Child (%d %d) Title(%d %d)", 
//        			mChildWidth, mChildHeight,
//        			holder.title.getMeasuredWidth(), holder.title.getMeasuredHeight()));
        }
        updateView();
	}
  	
  	private void updateView() {
        if(mSelected >= 0) {
//            int childsWidth = (mChildWidth+mMarginSmall)*mRecycler.size();
    		int xPos = (mChildWidth+mMarginSmall)*mSelected - mLayoutWidth/2 + mChildWidth/2 + 
    				getPaddingLeft() + getPaddingRight();
    		scrollTo(xPos, 0);
        }
        mSelected = -1;
  	}

	public void setOnItemClickListener(OnItemClickListener listener) {
  		mItemClickListener = listener;
  	}

  	class AdapterDataSetObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            super.onChanged();
            
            mLayout.removeAllViews();
			mItemCount = mAdapter == null ? 0 : mAdapter.getCount();

        	mRecycler.clear();
			for(int i=0; i<mItemCount; i++) {
				View view = mAdapter.getView(i, null, BookmarkScrollView.this);
				view.setOnClickListener(BookmarkScrollView.this);
				mLayout.addView(view);
				mRecycler.add(view);
				if(i > 0) {
					LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)view.getLayoutParams();
					params.leftMargin = mMarginSmall;
					view.setLayoutParams(params);
				}
			}	            
        }
 
        @Override
        public void onInvalidated() {
            super.onInvalidated();
        }
    }
  	
  	public void setSelected(int position) {
  		mSelected = position;
  		invalidate();
  	}

	@Override
	public void onClick(View view) {
		// TODO Auto-generated method stub
		if(mItemClickListener != null) {
			for(int i=0; i<mRecycler.size(); i++) {
				if(mRecycler.elementAt(i) == view) {
					mItemClickListener.onItemClick(null, view, i, i);
					mSelected = i;
					updateView();
				}
			}
		}
	}
}


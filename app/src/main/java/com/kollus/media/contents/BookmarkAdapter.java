package com.kollus.media.contents;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Rect;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.kollus.media.R;
import com.kollus.sdk.media.BuildConfig;
import com.kollus.sdk.media.content.KollusBookmark;
import com.kollus.sdk.media.util.Log;
import com.kollus.sdk.media.util.Utils;

import java.util.ArrayList;
import java.util.List;

public class BookmarkAdapter extends ArrayAdapter<KollusBookmark> implements OnClickListener {
	private static final String TAG = BookmarkAdapter.class.getSimpleName();
	private static final int ALL = 0;
	private static final int BOOKMARK = 1;
	private static final int INDEX = 2;

    private LayoutInflater mInflater;
    private Context mContext;
	private boolean mSetThumbnail;
	private ArrayList<KollusBookmark> mBookmarkList;
    private ArrayList<KollusBookmark> mCurBookmarkList;
    private BitmapRegionDecoder mScreenShot;
    private Bitmap				mDefaultBitmap;
    private int SCREEN_SHOT_WIDTH = 190;
    private int SCREEN_SHOT_HEIGHT = 120;
    private final float DEFAULT_DENSITY = 1.5f;
    private int mMarginSmall = 6;
    private int mDeleteBtnSize = 36;
	private int mContentDuration;
	private int mScreenShotWidth;
    private int mScreenShotHeight;
    private int mScreenShotCount;
    private float mScreenShotInterval;
    private boolean mShowRemoveBtn;
    private int mBookmarkKind = ALL;
    private ArrayList<String> mBookmarkLabels;
    private float mScale;
    private float mDensity;
    private int mSmallestScreenWidthDp;
 
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
	public BookmarkAdapter(Context context) {
        // Cache the LayoutInflate to avoid asking for a new one each time.
    	super(context, R.layout.bookmark_list);
    	
    	mContext = context;
        mInflater = LayoutInflater.from(context);
        mShowRemoveBtn = false;
        
        try {
        	mSmallestScreenWidthDp = mContext.getResources().getConfiguration().smallestScreenWidthDp;
        } catch (java.lang.Error e) {}
        
        if(mSmallestScreenWidthDp < 600)
        	SCREEN_SHOT_HEIGHT = 80;
        
        mDensity = mContext.getResources().getDisplayMetrics().density;
        mScale = mDensity/DEFAULT_DENSITY;
        mMarginSmall =  (int) (mMarginSmall*mScale);
        mDeleteBtnSize =  (int) (mDeleteBtnSize*mScale);
        SCREEN_SHOT_HEIGHT = (int) (SCREEN_SHOT_HEIGHT*mScale);
        
        mBookmarkLabels = new ArrayList<String>();
    }
    
    synchronized public void  setArrayList(ArrayList<KollusBookmark> bookmarkList, boolean writable) {
    	mBookmarkList = bookmarkList;
		mBookmarkLabels.clear();

    	for(KollusBookmark bookmark : mBookmarkList) {
    		if(bookmark.getLevel() == KollusBookmark.USER_LEVEL) {
    			bookmark.setLabel(mContext.getResources().getString(R.string.bookmark_user));

    			if(!mBookmarkLabels.contains(mContext.getResources().getString(R.string.bookmark_user)))
    				mBookmarkLabels.add(0, mContext.getResources().getString(R.string.bookmark_user));
    		}
    		else {
	    		if(!mBookmarkLabels.contains(bookmark.getLabel())) {
	    			mBookmarkLabels.add(bookmark.getLabel());
	    		}
    		}
    	}

		if(writable && !mBookmarkLabels.contains(mContext.getResources().getString(R.string.bookmark_user))) {
    		mBookmarkLabels.add(0, mContext.getResources().getString(R.string.bookmark_user));
    	}

		if(mBookmarkLabels.size() == 0)
			mBookmarkLabels.add(0, mContext.getResources().getString(R.string.bookmark_user));
    	
    	if(mBookmarkLabels.size() > 1)
    		mBookmarkLabels.add(0, mContext.getResources().getString(R.string.bookmark_all));

    	mCurBookmarkList = new ArrayList<KollusBookmark>();

		if(mSetThumbnail && mBookmarkList != null) {
			for(KollusBookmark bookmark : mBookmarkList) {
				bookmark.setThumbnail(getBitmap(bookmark.getTime()));
			}

			notifyDataSetChanged();
		}
	}
    
    public List<String> getBookmarkLableList() {
    	return mBookmarkLabels;
    }
    
    public void setBookmarkKind(int kind) {
    	if(mCurBookmarkList == null)
    		return;

    	mBookmarkKind = kind;
    	mCurBookmarkList.clear();
    	switch(mBookmarkKind) {
    	case ALL:
    	default:
    		mCurBookmarkList.addAll(mBookmarkList);
    		break;
    	case BOOKMARK:
    	{
    		for(KollusBookmark bookmark : mBookmarkList) {
    			if(bookmark.getLevel() == KollusBookmark.USER_LEVEL)
    				mCurBookmarkList.add(bookmark);
    		}
    		break;
    	}
    	case INDEX:
    	{
    		for(KollusBookmark bookmark : mBookmarkList) {
    			if(bookmark.getLevel() == KollusBookmark.PROVIDER_LEVEL)
    				mCurBookmarkList.add(bookmark);
    		}
    		break;
    	}
    	}
    	
    	if(mCurBookmarkList.isEmpty()) {
    		KollusBookmark noData = new KollusBookmark();
    		noData.setLevel(KollusBookmark.NO_LEVEL);
    		mCurBookmarkList.add(noData);
    	}
    	super.notifyDataSetChanged();
    }
    
    public int getBookmarkKind() {
    	return mBookmarkKind;
    }
    
    public int getBookmarkTime(int index) {
		KollusBookmark info = mCurBookmarkList.get(index);
		if(BuildConfig.DEBUG) {
			if(info.getLevel() == KollusBookmark.USER_LEVEL) {
				info.setTitle(String.format("%d - %d", info.getTime() / 1000, (System.currentTimeMillis() / 1000) % 10));
				notifyDataSetChanged();
			}
		}

		return info.getTime();
    }
    
    @Override
	public void notifyDataSetChanged() {
		// TODO Auto-generated method stub
    	setBookmarkKind(mBookmarkKind);
	}

	synchronized public void setThumbnailInfo(BitmapRegionDecoder decoder,  Bitmap defaultScreenShot, int duration, int width, int height, int count, float interval) {
		mScreenShot = decoder;
		mContentDuration = duration;
    	mScreenShotWidth = width;
    	mScreenShotHeight = height;
    	mScreenShotCount = count;
    	mScreenShotInterval = interval;
    	
    	if(mScreenShot == null && defaultScreenShot != null) {
			mDefaultBitmap = defaultScreenShot;
    		mScreenShotWidth = mDefaultBitmap.getWidth();
        	mScreenShotHeight = mDefaultBitmap.getHeight();
    	}

		if(mScreenShotHeight > 0)
			SCREEN_SHOT_WIDTH = mScreenShotWidth*SCREEN_SHOT_HEIGHT/mScreenShotHeight;

		if(mBookmarkList != null) {
			for(KollusBookmark bookmark : mBookmarkList) {
				bookmark.setThumbnail(getBitmap(bookmark.getTime()));
			}

			notifyDataSetChanged();
		}
		mSetThumbnail = true;
	}
	
    public void updateBookmarkRemoveView() {
    	mShowRemoveBtn = !mShowRemoveBtn;
    	notifyDataSetChanged();
    }
    
    public void hideBookmarkRemoveView() {
    	mShowRemoveBtn = false;
    	notifyDataSetChanged();
    }
    
    @TargetApi(Build.VERSION_CODES.GINGERBREAD_MR1)
	public int add(int timeMilli) {
    	int index = 0;
    	for(KollusBookmark iter : mBookmarkList) {
    		int iterTimeSec = iter.getTime()/1000;
    		int addTimeSec = timeMilli/1000;
    		if(iterTimeSec > addTimeSec) 
    			break;
    		if(iterTimeSec == addTimeSec && iter.getLevel() == KollusBookmark.USER_LEVEL) {
    			return index;
    		}
    		index++;
    	}
  		
    	KollusBookmark info = new KollusBookmark();
    	info.setLevel(KollusBookmark.USER_LEVEL);
    	info.setLabel(mContext.getResources().getString(R.string.bookmark_user));
    	info.setTime(timeMilli);
    	info.setThumbnail(getBitmap(timeMilli));

    	mBookmarkList.add(index, info);

		if(mBookmarkKind == ALL) {
			mCurBookmarkList.clear();
			mCurBookmarkList.addAll(mBookmarkList);
		}

		notifyDataSetChanged();
    	
    	return index;
    }
    
    public void remove(int index) {
    	KollusBookmark bookmark = mCurBookmarkList.get(index);
		Bitmap bitmap = bookmark.getThumbnail();
		if(bitmap != null && bitmap != mDefaultBitmap) {
			bitmap.recycle();
		}
    	mBookmarkList.remove(bookmark);

		if(mBookmarkKind == ALL) {
			mCurBookmarkList.clear();
			mCurBookmarkList.addAll(mBookmarkList);
		}

		notifyDataSetChanged();
    }
    
    public int getBookmarkCount() {
    	if(mCurBookmarkList == null)
    		return 0;
    	
    	if(mCurBookmarkList.size() == 1) {
    		if(mCurBookmarkList.get(0).getLevel() == KollusBookmark.NO_LEVEL)
    			return 0;
    	}

		return mCurBookmarkList.size();
    }
  
    /**
     * The number of items in the list is determined by the number of speeches
     * in our array.
     *
     * @see android.widget.ListAdapter#getCount()
     */
    public int getCount() {
    	if(mCurBookmarkList == null)
    		return 0;
    	
    	return mCurBookmarkList.size();
    }

    /**
     * Since the data comes from an array, just returning the index is
     * sufficent to get at the data. If we were using a more complex data
     * structure, we would return whatever object represents one row in the
     * list.
     *
     * @see android.widget.ListAdapter#getItem(int)
     */
    public KollusBookmark getItem(int position) {
        return mCurBookmarkList.get(position);
    }

    /**
     * Use the array index as a unique id.
     *
     * @see android.widget.ListAdapter#getItemId(int)
     */
    public long getItemId(int position) {
        return position;
    }
    
    public interface onBookmarkRemoveListener {
    	void onBookmarkRemove(int position);
    }
    private onBookmarkRemoveListener mBookmarkRemoveListener;
    public void setOnBookmarkRemoveListener(onBookmarkRemoveListener l) {
    	mBookmarkRemoveListener = l;
    }

    private Bitmap getBitmap(int timeMs) {
		Bitmap bitmap = null;
		if(mScreenShot != null) {
			int column = 0, row = 0, x = 0, y = 0;
			int bitmapIndex = Math.round((timeMs)/1000/mScreenShotInterval)+3;
			if(bitmapIndex >= mScreenShotCount)
				bitmapIndex = mScreenShotCount-1;
			try {
				column = bitmapIndex%10;
				row = bitmapIndex/10;
				x = mScreenShotWidth*column;
				y = mScreenShotHeight*row;
				Rect rect = new Rect(x, y, x+mScreenShotWidth, y+mScreenShotHeight);

				bitmap = mScreenShot.decodeRegion(rect, new BitmapFactory.Options());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		else {
			bitmap = mDefaultBitmap;
		}

		return bitmap;
	}
    
    private View newView(int position, ViewGroup parent) {
    	View view = mInflater.inflate(R.layout.bookmark_item, parent, false);
    	View itemLayout = view.findViewById(R.id.bookmark_item_layout);
    	View item = view.findViewById(R.id.bookmark_item);
    	
    	BookmarkViewHolder holder = new BookmarkViewHolder();
    	holder.label = (TextView) view.findViewById(R.id.bookmark_label);
    	holder.titleLayer = view.findViewById(R.id.bookmark_title_layout);
    	holder.title = (TextView) view.findViewById(R.id.bookmark_title);
        holder.thumbnail = (ImageView) view.findViewById(R.id.bookmark_thumbnail);
        holder.time = (TextView) view.findViewById(R.id.bookmark_time);
        holder.delete = (ImageButton) view.findViewById(R.id.bookmark_delete_btn);
        
        FrameLayout.LayoutParams params0 = (FrameLayout.LayoutParams)itemLayout.getLayoutParams();
        params0.width = SCREEN_SHOT_WIDTH+mDeleteBtnSize/2;
    	itemLayout.setLayoutParams(params0);
    	
        LinearLayout.LayoutParams params1 = (LinearLayout.LayoutParams)item.getLayoutParams();
    	params1.width = SCREEN_SHOT_WIDTH+mDeleteBtnSize/2;
    	params1.height = SCREEN_SHOT_HEIGHT+mDeleteBtnSize/2;
    	item.setLayoutParams(params1);
//    	Log.d(TAG, String.format("new Bookmark %d %d", params1.width, params1.height));
    	
    	View label = view.findViewById(R.id.bookmark_label_layout);
    	LinearLayout.LayoutParams params2 = (LinearLayout.LayoutParams)label.getLayoutParams();
    	params2.leftMargin = mDeleteBtnSize/2;
    	label.setLayoutParams(params2);
    	
    	View title = view.findViewById(R.id.bookmark_title_layout);
    	RelativeLayout.LayoutParams params3 = (RelativeLayout.LayoutParams)title.getLayoutParams();
    	params3.leftMargin = mDeleteBtnSize/2;
    	title.setLayoutParams(params3);

    	RelativeLayout.LayoutParams params4 = (RelativeLayout.LayoutParams)holder.thumbnail.getLayoutParams();
    	params4.leftMargin = mDeleteBtnSize/2;
    	params4.topMargin = mDeleteBtnSize/2;
    	params4.width = SCREEN_SHOT_WIDTH;
    	params4.height = SCREEN_SHOT_HEIGHT;
    	holder.thumbnail.setLayoutParams(params4);
  		
    	LinearLayout.LayoutParams params5 = (LinearLayout.LayoutParams)holder.time.getLayoutParams();
    	params5.leftMargin = mDeleteBtnSize/2;
    	holder.time.setLayoutParams(params5);
  		
    	RelativeLayout.LayoutParams params6 = (RelativeLayout.LayoutParams)holder.delete.getLayoutParams();
    	params6.width = mDeleteBtnSize;
    	params6.height = mDeleteBtnSize;
    	holder.delete.setLayoutParams(params6);
  		
  		view.setTag(holder);
    	
    	return view;
    }
    
    /**
     * Make a view to hold each row.
     *
     * @see android.widget.ListAdapter#getView(int, android.view.View,
     *      android.view.ViewGroup)
     */
    public View getView(int position, View convertView, ViewGroup parent) {
        // A ViewHolder keeps references to children views to avoid unneccessary calls
        // to findViewById() on each row.
    	BookmarkViewHolder holder;
        
        // When convertView is not null, we can reuse it directly, there is no need
        // to reinflate it. We only inflate a new View when the convertView supplied
        // by ListView is null.
        if (convertView == null) {
        	convertView = newView(position, parent);
        }
        holder = (BookmarkViewHolder) convertView.getTag();
        
        if(mCurBookmarkList.get(position).getLevel() == KollusBookmark.NO_LEVEL) {
        	holder.label.setVisibility(View.INVISIBLE);
        	holder.titleLayer.setVisibility(View.INVISIBLE);
        	holder.title.setVisibility(View.INVISIBLE);
        	holder.thumbnail.setVisibility(View.INVISIBLE);
        	holder.time.setVisibility(View.INVISIBLE);
        	holder.delete.setVisibility(View.INVISIBLE);
        }
        else {
        	float nTime = mCurBookmarkList.get(position).getTime();
        	int nLevel = mCurBookmarkList.get(position).getLevel();
        	String label = mCurBookmarkList.get(position).getLabel();
        	String title = mCurBookmarkList.get(position).getTitle();
	        String time;
	        if(mContentDuration >= 10*60*60*1000)
				time = Utils.stringForTimeHHMMSS((int)nTime);
	        else if(mContentDuration >= 60*60*1000)
				time = Utils.stringForTimeHMMSS((int)nTime);
	        else
				time = Utils.stringForTimeMMSS((int)nTime);
	        
	        if(nLevel != KollusBookmark.USER_LEVEL) {
	        	if(label == null || label.length() == 0) {
		        	holder.label.setVisibility(View.INVISIBLE);
		        }
		        else {
		        	holder.label.setText(label);
		        	holder.label.setVisibility(View.VISIBLE);
		        }
	        }
	        else {
	        	holder.label.setVisibility(View.INVISIBLE);
	        }
	        
	        if(title == null || title.length() == 0) {
	        	holder.title.setVisibility(View.INVISIBLE);
	        	holder.titleLayer.setVisibility(View.INVISIBLE);
	        }
	        else {
	        	holder.title.setText(title);
	        	holder.title.setVisibility(View.VISIBLE);
	        	holder.titleLayer.setVisibility(View.VISIBLE);
	        }
	        holder.time.setText(time);
	        holder.time.setVisibility(View.VISIBLE);
	        
	        Bitmap bitmap = mCurBookmarkList.get(position).getThumbnail();
	        if(bitmap != null) {
				holder.thumbnail.setImageBitmap(bitmap);
				holder.thumbnail.setVisibility(View.VISIBLE);
	        }
        }
        
        if(mShowRemoveBtn && mCurBookmarkList.get(position).getLevel() == KollusBookmark.USER_LEVEL)
        	holder.delete.setVisibility(View.VISIBLE);
        else
        	holder.delete.setVisibility(View.GONE);
        holder.delete.setTag(position);
        holder.delete.setOnClickListener(this);
 		
  		return convertView;
    }
    
    @Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
    	if(v.getId() == R.id.bookmark_delete_btn) {
			if(mBookmarkRemoveListener != null)
				mBookmarkRemoveListener.onBookmarkRemove((Integer) v.getTag());
		}
	}
}
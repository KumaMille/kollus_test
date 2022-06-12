package com.kollus.media.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Transformation;
import android.widget.Gallery;

public class BookmarkGallery extends Gallery {

	private final static String TAG = BookmarkGallery.class.getSimpleName();
    
    /**
     * The Centre of the Coverflow 
     */   
    private int mCoveflowCenter;
    
    private int mChildResourceId;
    
    public BookmarkGallery(Context context) {
 		super(context);
 		this.setStaticTransformationsEnabled(true);
 	}

 	public BookmarkGallery(Context context, AttributeSet attrs) {
  		super(context, attrs);
  		this.setStaticTransformationsEnabled(true);
 	}
 
  	public BookmarkGallery(Context context, AttributeSet attrs, int defStyle) {
   		super(context, attrs, defStyle);
   		this.setStaticTransformationsEnabled(true);
  	}
  	
  	public void setChildResourceId(int childResourceId) {
  		mChildResourceId = childResourceId;
  	}
 
 	/**
     * Get the Centre of the Coverflow
     * @return The centre of this Coverflow.
     */
    private int getCenterOfCoverflow() {
		return (getWidth() - getPaddingLeft() - getPaddingRight()) / 2 + getPaddingLeft();
    }
    
    /**
     * Get the Centre of the View
     * @return The centre of the given view.
     */
    private static int getCenterOfView(View view) {
        return view.getLeft() + view.getWidth() / 2;
    }  
    
    /**
  	 * {@inheritDoc}
  	 *
  	 * @see #setStaticTransformationsEnabled(boolean) 
  	*/ 
    protected boolean getChildStaticTransformation(View child, Transformation t) {
    	
		final int childCenter = getCenterOfView(child);

		t.clear();
		t.setTransformationType(Transformation.TYPE_MATRIX);
	  
		transformImageBitmap2(child.findViewById(mChildResourceId), t, childCenter);
		
		return true;
	}

	/**
	  * This is called during layout when the size of this view has changed. If
	  * you were just added to the view hierarchy, you're called with the old
	  * values of 0.
	  *
	  * @param w Current width of this view.
	  * @param h Current height of this view.
	  * @param oldw Old width of this view.
	  * @param oldh Old height of this view.
     */
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		mCoveflowCenter = getCenterOfCoverflow();
		super.onSizeChanged(w, h, oldw, oldh);
	}
  
	private void transformImageBitmap2(View child, Transformation t, int childCenter) {
		if(child == null)
			return;
		
		if(mCoveflowCenter == childCenter)
			child.setPressed(true);
		else
			child.setPressed(false);

		return;
 	}
}


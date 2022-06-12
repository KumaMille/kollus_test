package com.kollus.media.contents;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kollus.media.R;
import com.kollus.media.preference.KollusConstants;
import com.kollus.sdk.media.KollusStorage;
import com.kollus.sdk.media.content.FileManager;
import com.kollus.sdk.media.content.KollusContent;
import com.kollus.sdk.media.util.Log;
import com.kollus.sdk.media.util.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Vector;

public class KollusContentAdapter extends RecyclerView.Adapter<ContentsViewHolder> implements
		OnClickListener, View.OnLongClickListener, View.OnTouchListener {
	private static final String TAG = KollusContentAdapter.class.getSimpleName();
	public static final int MODE_NONE		= 0;
	public static final int MODE_SELECT	= 1;
	public static final int MODE_CUT		= 2;
	
    private Context mContext;
    private FileManager mFileManager;
    private Vector<FileManager> mSelectedList;
    private MultiKollusStorage mMultiStorage;
    private ArrayList<MultiKollusContent> mContentsList;
	private View mEmptyView;
	private int mSelectMode;

	private boolean mHover;
	private View mHoverView;
	private Rect mHoverRect;
	private MultiKollusContent mHoverKollusContent;
	private BitmapRegionDecoder mHoverScreenShot;
	private BitmapFactory.Options mHoverScreenShotOption;
	private int mHoverScreenShotWidth;
	private int mHoverScreenShotHeight;
	private int mHoverScreenShotCount;
	private int mHoverCallCount;
    
    public KollusContentAdapter(Context context, FileManager fileManager,
    		ArrayList<MultiKollusContent> contentsList) {
        // Cache the LayoutInflate to avoid asking for a new one each time.
//    	super(context, R.layout.file_list, contentsList);
    	mContext = context;
        mFileManager = fileManager;
        mContentsList = contentsList;
		mMultiStorage = MultiKollusStorage.getInstance(mContext);
		this.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
			@Override
			public void onChanged() {
				super.onChanged();
				if(getItemCount() > 0) {
					mEmptyView.setVisibility(View.GONE);
				}
				else {
					mEmptyView.setVisibility(View.VISIBLE);
				}
			}
		});
    }

	public void setEmptyView(View view) {
		mEmptyView = view;
	}

    public void setFileManager(FileManager fileManager) {
    	mFileManager = fileManager;
    }
    
    public void setSelectedList(Vector<FileManager> selectList) {
    	mSelectedList = selectList;
    }
    
    public void setContentsList(ArrayList<MultiKollusContent> contentsList) {
    	mContentsList = contentsList;
    }

	public void setSelectMode(int mode) {
		mSelectMode = mode;
		this.notifyDataSetChanged();
	}
    
    private MultiKollusContent getKollusContent(int diskIndex, String key) {
    	KollusStorage storage = mMultiStorage.getStorage(diskIndex);
    	for(MultiKollusContent content : mContentsList) {
        	if(storage == content.getKollusStorage() &&
					content.getKollusContent().getMediaContentKeyMD5().equalsIgnoreCase(key)) {
        		return content;
        	}
        }
    	
    	return null;
    }

	@Override
	public ContentsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(mContext).inflate(R.layout.file_list, parent, false);
		ContentsViewHolder viewHolder = new ContentsViewHolder(view);
		return viewHolder;
	}

	@Override
	public void onBindViewHolder(ContentsViewHolder holder, int position) {
		onBindViewHolder(holder, position, null);
	}

	@Override
	public void onBindViewHolder(ContentsViewHolder holder, int position, List<Object> payloads) {
		FileManager fileManager = mFileManager.getFileList().get(position);

//		if(!mBindView)
		{
			holder.listItem.setOnClickListener(this);
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1) {
				holder.listItem.setOnLongClickListener(this);
				holder.listItem.setOnTouchListener(this);
			}
			holder.listItem.setTag("" + position);

			holder.check.setOnClickListener(this);
			holder.check.setTag("" + position);
		}

		if(fileManager.getType() == FileManager.DIRECTORY) {
			holder.folderLayer.setVisibility(View.VISIBLE);
			holder.fileLayer.setVisibility(View.GONE);

			holder.folderName.setText(fileManager.getName());
		}
		else if(fileManager.getType() == FileManager.FILE) {
			holder.folderLayer.setVisibility(View.GONE);
			holder.fileLayer.setVisibility(View.VISIBLE);

			holder.btnDetail.setOnClickListener(this);
			holder.btnDetail.setTag(fileManager.getKey());

			// Bind the data efficiently with the holder.
			MultiKollusContent content = getKollusContent(fileManager.getDiskIndex(), fileManager.getKey());
			if(content != null) {
				int playtime = content.getKollusContent().getPlaytime();
				int duration = content.getKollusContent().getDuration();
				int percent = content.getKollusContent().getDownloadPercent();
				boolean bComplete = content.getKollusContent().isCompleted();

//				if(!mBindView)
				{
					String thumbnail = content.getKollusContent().getThumbnailPath();
					String company = content.getKollusContent().getCompany();
					String cource = content.getKollusContent().getCourse();
					String subcource = content.getKollusContent().getSubCourse();
					String description = content.getKollusContent().getSynopsis();
					String title;
					if(cource != null && cource.length() > 0) {
						if(subcource != null && subcource.length() > 0)
							title = cource+"("+subcource+")";
						else
							title  = cource;
					}
					else
						title = subcource;

					if (thumbnail != null && !thumbnail.startsWith("http://")) {
						try {
							Bitmap bm = BitmapFactory.decodeFile(thumbnail);
							if (bm != null)
								holder.icon.setImageBitmap(bm);
						} catch (Exception e) {
							e.printStackTrace();
						} catch (Error e) {
							e.printStackTrace();
						}
					}

					holder.fileName.setText(title);
					holder.timeBar.setMax(duration);

					if(KollusConstants.SUPPORT_MULTI_STORAGE) {
					    MultiKollusStorage multiKollusStorage = MultiKollusStorage.getInstance(mContext);
					    if(multiKollusStorage.isInnerDisk(content.getKollusStorage()))
							holder.location.setVisibility(View.GONE);
						else
							holder.location.setVisibility(View.VISIBLE);
					}
					else {
						holder.location.setVisibility(View.GONE);
					}
				}

				if (duration < 60 * 60 * 1000) {
					holder.playTime.setText(Utils.stringForTimeMMSS(playtime));
					holder.duration.setText(Utils.stringForTimeMMSS(duration));
				} else {
					holder.playTime.setText(Utils.stringForTimeHMMSS(playtime));
					holder.duration.setText(Utils.stringForTimeHMMSS(duration));
				}
				holder.timeBar.setProgress(playtime);

				if(!bComplete) {
					holder.icDrm.setVisibility(View.GONE);

					holder.txtPercent.setVisibility(View.VISIBLE);
					holder.txtPercent.setText(percent+"%");
				}
				else {
					if(content.getKollusContent().isDrm() || content.getKollusContent().getContentType() == KollusContent.EXT_DRM_CONTENT)
						holder.icDrm.setVisibility(View.VISIBLE);
					else
						holder.icDrm.setVisibility(View.GONE);

					holder.txtPercent.setVisibility(View.GONE);
				}

				boolean bExpired = false;
				long currentDate = System.currentTimeMillis()/1000;
				if(content.getKollusContent().getTotalExpirationCount() > 0 && content.getKollusContent().getExpirationCount() <= 0)
					bExpired = true;

				if(content.getKollusContent().getExpirationDate() > 0 && currentDate > content.getKollusContent().getExpirationDate())
					bExpired = true;

				if(content.getKollusContent().getTotalExpirationPlaytime() > 0 && content.getKollusContent().getExpirationPlaytime() <= 0)
					bExpired = true;

				if(content.getKollusContent().isContentExpirated() || bExpired)
					holder.icHang.setVisibility(View.VISIBLE);
				else
					holder.icHang.setVisibility(View.GONE);
			}
		}

		if(mSelectMode == MODE_NONE) {
			holder.check.setVisibility(View.GONE);
		}
		else {
			holder.check.setChecked(false);
			holder.check.setVisibility(View.VISIBLE);
			if(mSelectedList.contains(fileManager)) {
				holder.check.setChecked(true);
			}
			else {
				holder.check.setChecked(false);
			}
		}
	}

	@Override
	public int getItemCount() {
		int nCount = 0;
		if(mContentsList == null || mFileManager == null)
			nCount = 0;
		else
			nCount = mFileManager.getFileList().size();

		return nCount;
	}

	public interface OnItemListener {
		public void onItemClick(int position);
    	public void onItemDetail(String key);
		public void onItemCheckChange(int position, boolean isChecked);
    }
    private OnItemListener mOnItemListener;
    public void setOnItemListener(OnItemListener l) {
    	mOnItemListener = l;
    }

    @Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		if(mOnItemListener != null) {
			if (v.getId() == R.id.list_item) {
				int position = Integer.parseInt((String) v.getTag());
				if(mSelectMode == MODE_NONE) {
					mOnItemListener.onItemClick(position);
				}
				else if(mSelectMode == MODE_CUT) {
					FileManager fileManager = mFileManager.getFileList().get(position);
					if(fileManager.getType() == FileManager.DIRECTORY && !mSelectedList.contains(fileManager)) {
						mOnItemListener.onItemClick(position);
					}
				}
				else {
					FileManager fileManager = mFileManager.getFileList().get(position);
					if(mSelectedList.contains(fileManager)) {
						mOnItemListener.onItemCheckChange(position, false);
					}
					else {
						mOnItemListener.onItemCheckChange(position, true);
					}
					this.notifyItemChanged(position);
				}
			}
			if (v.getId() == R.id.btn_detail) {
				mOnItemListener.onItemDetail((String) v.getTag());
			} else if (v.getId() == R.id.list_check) {
				CheckBox check = (CheckBox) v;
				mOnItemListener.onItemCheckChange(Integer.parseInt((String) check.getTag()), check.isChecked());
			}
		}
	}

	@Override
	@TargetApi(Build.VERSION_CODES.GINGERBREAD_MR1)
	public boolean onLongClick(View v) {
		if(mSelectMode == MODE_NONE) {
			FileManager fileManager = mFileManager.getFileList().get(Integer.parseInt((String) v.getTag()));
			if(fileManager.getType() != FileManager.FILE)
				return false;

			mHover = true;
			mHoverView = v;
			mHoverRect = new Rect(v.getLeft(), v.getTop(), v.getRight(), v.getBottom());
			mHoverKollusContent = getKollusContent(fileManager.getDiskIndex(), fileManager.getKey());
			String screenShotPath = mHoverKollusContent.getKollusContent().getScreenShotPath();
			int end = screenShotPath.lastIndexOf('.');
			int start = screenShotPath.lastIndexOf('.', end-1);

			if(start < end) {
				String info = screenShotPath.substring(start + 1, end);
				Scanner sc = new Scanner(info);
				sc.useDelimiter("x");
				mHoverScreenShotWidth = sc.nextInt();
				mHoverScreenShotHeight = sc.nextInt();
				mHoverScreenShotCount = sc.nextInt();
				sc.close();

				try {
					mHoverScreenShot = BitmapRegionDecoder.newInstance(screenShotPath, true);
					if (mHoverScreenShot != null) {
						mHoverScreenShotOption = new BitmapFactory.Options();
					}
				} catch (Exception e) {
					mHover = false;
					e.printStackTrace();
				} catch (OutOfMemoryError e) {
					mHover = false;
					e.printStackTrace();
				}
			}

			if(mHover) {
				mHandler.post(mHoverRunnable);
			}
		}
		return false;
	}

	@Override
	@TargetApi(Build.VERSION_CODES.GINGERBREAD_MR1)
	public boolean onTouch(View v, MotionEvent event) {
		if(mHover) {
			if(event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
				ImageView icon = (ImageView) mHoverView.findViewById(R.id.icon);
				Bitmap bm = BitmapFactory.decodeFile(mHoverKollusContent.getKollusContent().getThumbnailPath());
				if (bm != null)
					icon.setImageBitmap(bm);
				mHover = false;
				mHoverCallCount = 0;
				if(mHoverScreenShot != null)
					mHoverScreenShot.recycle();
				mHoverScreenShot = null;
			}
//			else {
//				v.getHitRect(mHoverRect);
//				if(!mHoverRect.contains(
//						Math.round(v.getLeft() + event.getX()),
//						Math.round(v.getTop() + event.getY()))) {
//					ImageView icon = (ImageView) mHoverView.findViewById(R.id.icon);
//					Bitmap bm = BitmapFactory.decodeFile(mHoverKollusContent.getThumbnailPath());
//					if (bm != null)
//						icon.setImageBitmap(bm);
//					mHover = false;
//					mHoverCallCount = 0;
//					if(mHoverScreenShot != null)
//						mHoverScreenShot.recycle();
//					mHoverScreenShot = null;
//				}
//			}
			return true;
		}
		return false;
	}

	private Handler mHandler = new Handler();
	private Runnable mHoverRunnable = new Runnable() {
		@Override
		@TargetApi(Build.VERSION_CODES.GINGERBREAD_MR1)
		public void run() {
			if(mHover) {
				ImageView icon = (ImageView) mHoverView.findViewById(R.id.icon);
				try {
					int column = 0, row = 0, x = 0, y = 0;
					column = mHoverCallCount%10;
					row = mHoverCallCount/10;
					x = mHoverScreenShotWidth*column;
					y = mHoverScreenShotHeight*row;
					Rect rect = new Rect(x, y, x+mHoverScreenShotWidth, y+mHoverScreenShotHeight);
					icon.setImageBitmap(mHoverScreenShot.decodeRegion(rect, mHoverScreenShotOption));

					mHoverCallCount++;
					if(mHoverCallCount >= mHoverScreenShotCount)
						mHoverCallCount = 0;
				} catch (Exception e) {
					e.printStackTrace();
					mHover = false;
				}

				if(mHover) {
					mHandler.postDelayed(mHoverRunnable, 100);
				}
			}
		}
	};
}
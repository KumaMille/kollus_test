package com.kollus.media.preference;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.kollus.media.R;

public class SortPreference extends View implements AdapterView.OnItemSelectedListener {
	private static final String TAG = SortPreference.class.getSimpleName();
	public static final int SORT_BY_TITLE 		= 10;
	public static final int SORT_BY_DATE		= 11;
	public static final int SORT_BY_DURATION	= 12;
	public static final int SORT_BY_SIZE		= 13;

	private Context mContext;

	private SharedPreferences mSharedPreferences;
	private Resources mResources;
	private Spinner mSortType;
	private Spinner mSortOrder;

	public SortPreference(PlayerPreference root) {
		super(root);
		
		mContext = root;
		mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
		mResources = mContext.getResources();

		LinearLayout layout = (LinearLayout)root.findViewById(R.id.preference_root);
		View view = root.getLayoutInflater().inflate(R.layout.sort_preference, null);
		layout.addView(view);		
		onBindView(view);
	}
	
	private void onBindView(View view) {
		int sortType = mSharedPreferences.getInt(mResources.getString(R.string.preference_sort_type_key), SortPreference.SORT_BY_TITLE);
		boolean sortAscend = mSharedPreferences.getBoolean(mResources.getString(R.string.preference_sort_order_key), true);

		mSortType = (Spinner)view.findViewById(R.id.spinner_sort_type);
		mSortOrder = (Spinner)view.findViewById(R.id.spinner_sort_order);

		ArrayAdapter<String> aSortType = new ArrayAdapter<String>(
													mContext,
													R.layout.spinner_item,
													mResources.getStringArray(R.array.sort_type_array)
											);
		ArrayAdapter<String> aSortOrder = new ArrayAdapter<String>(
													mContext,
													R.layout.spinner_item,
													mResources.getStringArray(R.array.sort_order_array)
											);

		mSortType.setAdapter(aSortType);
		mSortOrder.setAdapter(aSortOrder);

		int typeIndex = 0;
		switch (sortType) {
			case SORT_BY_TITLE:
				typeIndex = 0;
				break;
			case SORT_BY_DATE:
				typeIndex = 1;
				break;
			case SORT_BY_DURATION:
				typeIndex = 2;
				break;
			case SORT_BY_SIZE:
				typeIndex = 3;
				break;
		}
		mSortType.setSelection(typeIndex);

		int orderIndex = 0;
		if(sortAscend)
			orderIndex = 0;
		else
			orderIndex = 1;
		mSortOrder.setSelection(orderIndex);

		mSortType.setOnItemSelectedListener(this);
		mSortOrder.setOnItemSelectedListener(this);
	}
	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
		if(parent == mSortType) {
			int type = SORT_BY_TITLE;
			switch (position) {
				case 0:
					type = SORT_BY_TITLE;
					break;
				case 1:
					type = SORT_BY_DATE;
					break;
				case 2:
					type = SORT_BY_DURATION;
					break;
				case 3:
					type = SORT_BY_SIZE;
					break;
			}
			SharedPreferences.Editor editor = mSharedPreferences.edit();
			editor.putInt(mResources.getString(R.string.preference_sort_type_key), type);
			editor.commit();
		}
		else if(parent == mSortOrder) {
			boolean ascend = true;
			switch (position) {
				case 0:
					ascend = true;
					break;
				case 1:
					ascend = false;
					break;
			}
			SharedPreferences.Editor editor = mSharedPreferences.edit();
			editor.putBoolean(mResources.getString(R.string.preference_sort_order_key), ascend);
			editor.commit();
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) {

	}
}

package com.kollus.media.contents;

import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.kollus.media.R;

public class ContentsViewHolder extends RecyclerView.ViewHolder{
	public View listItem;
	public CheckBox check;

	public View folderLayer;
	public TextView folderName;

	public View fileLayer;
	public ImageView icon;
	public ImageView location;
	public ImageView icDrm;
	public ImageView icHang;
	public TextView txtPercent;
	public TextView fileName;
	public TextView playTime;
	public TextView duration;
	public ProgressBar  timeBar;
	public TextView btnDetail;

	public ContentsViewHolder(View itemView) {
		super(itemView);

		listItem = itemView.findViewById(R.id.list_item);
		check = (CheckBox) itemView.findViewById(R.id.list_check);

		folderLayer = itemView.findViewById(R.id.folder_field);
		folderName = (TextView) itemView.findViewById(R.id.folder_name);

		fileLayer = itemView.findViewById(R.id.file_field);

		icon = (ImageView) itemView.findViewById(R.id.icon);
		location = (ImageView) itemView.findViewById(R.id.list_location);
		icDrm = (ImageView) itemView.findViewById(R.id.list_drm);
		txtPercent = (TextView) itemView.findViewById(R.id.list_percent);
		icHang = (ImageView) itemView.findViewById(R.id.list_hang);
		fileName = (TextView) itemView.findViewById(R.id.file_name);
		playTime = (TextView) itemView.findViewById(R.id.play_time);
		duration = (TextView) itemView.findViewById(R.id.duration);
		timeBar = (ProgressBar)itemView.findViewById(R.id.time_progress);
		btnDetail = (TextView) itemView.findViewById(R.id.btn_detail);
	}
}

package com.kollus.media.preference;

import java.util.Vector;

import com.kollus.media.R;
import com.kollus.sdk.media.util.CpuInfo;

import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

public class CpuInfoPreference  extends View{
	public CpuInfoPreference(PlayerPreference root) {
		super(root);
		
		LinearLayout layout = (LinearLayout)root.findViewById(R.id.preference_root);
		View view = root.getLayoutInflater().inflate(R.layout.cpu_info_preference, null);
		layout.addView(view);		
		onBindView(view);
	}
	
	private void onBindView(View view) {
		CpuInfo cpu = CpuInfo.getInstance();
		TextView text = (TextView)((ViewGroup)view).findViewById(R.id.cpu_processor);
		text.setText(cpu.getCpuName());
		
		text = (TextView)((ViewGroup)view).findViewById(R.id.cpu_frequence);
		
		Vector<String> frequences = cpu.getFrequence();
		String frequence = "";
		for(String freq : frequences) {
			if(frequence.length() > 0)
				frequence += "\n";
			frequence += freq;
		}
		text.setText(frequence);
		
//		int cpu_count = cpu.getCpuCount();
//		text = (TextView)((ViewGroup)view).findViewById(R.id.cpu_count);
//		if(cpu_count == 1)
//			text.setText(cpu_count+" Core");
//		else
//			text.setText(cpu_count+" Cores");
	}
}

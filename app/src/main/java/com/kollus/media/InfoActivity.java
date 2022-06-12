package com.kollus.media;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.view.Window;
import android.widget.TextView;

public class InfoActivity extends BaseActivity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.info_layout);
        
        TextView copyrightView = (TextView)findViewById(R.id.info_copyright);
        copyrightView.setText(getResources().getString(R.string.copyright)+"\n"+
        		getResources().getString(R.string.copyright_ment));        
        
        try {
			PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			TextView appName = (TextView)findViewById(R.id.info_name);
	        TextView version = (TextView)findViewById(R.id.info_version);
	        
	        appName.setText(R.string.app_name);
			version.setText(packageInfo.versionName);
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
}

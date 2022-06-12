package com.kollus.media.download;

import com.kollus.media.contents.MultiKollusContent;
import com.kollus.media.contents.MultiKollusStorage;
import com.kollus.sdk.media.KollusStorage;
import com.kollus.sdk.media.content.KollusContent;

public class DownloadInfo {
	private KollusStorage mKollusStorge;
	private String mFolder;
	private String mUrl;
	private MultiKollusContent mContent;
	
	public DownloadInfo(KollusStorage storage, String folder, String url) {
		mKollusStorge = storage;
		mFolder = folder;
		mUrl = url;
		mContent = new MultiKollusContent(storage, new KollusContent());
	}

	public KollusStorage getKollusStorge() {
		return mKollusStorge;
	}

	public String getFolder() {
		return mFolder;
	}
	
	public String getUrl() {
		return mUrl;
	}
	
	public MultiKollusContent getMultiKollusContent() {
		return mContent;
	}
}

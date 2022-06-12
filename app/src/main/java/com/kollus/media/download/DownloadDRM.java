package com.kollus.media.download;

import com.kollus.sdk.media.KollusStorage;

public class DownloadDRM {
	private KollusStorage mKollusStorage;
	private String mRequest;
	private String mResponse;
	
	public DownloadDRM(KollusStorage storage, String request, String response) {
		mKollusStorage = storage;
		mRequest = request;
		mResponse = response;
	}

	public KollusStorage getKollusStorage() {
		return mKollusStorage;
	}
	
	public String getRequest() {
		return mRequest;
	}
	
	public String getResponse() {
		return mResponse;
	}
}

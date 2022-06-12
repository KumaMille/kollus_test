package com.kollus.media.listener;

import com.kollus.sdk.media.KollusStorage;
import com.kollus.sdk.media.content.KollusContent;

public interface MultiKollusPlayerDRMListener {
	/**
	 * load시에 DRM 콜백 KIND 2, 3로 컨텐츠 삭제 요청을 받고, 실제 컨텐츠 관련 삭제후  알림
	 */
	public static final int DCB_INFO_DELETE = 0; 
	/**
	 * KIND 3에 대한 응답으로 강제 만료를 받고 강제 만료 정보를 컨텐츠에 저장후  알림
	 */
	public static final int DCB_INFO_EXPIRED = 1; 
	/**
	 * KIND 3에 대한 응답으로 컨텐츠 reset시에 reset 정보 컨텐츠에 저장후 알림
	 */
	public static final int DCB_INFO_RESET = 2;
	
	/**
	 * DRM 관련하여 서버에 전송한 데이터와 그 결과값을 String으로 응답받는 함수 
	 * @param request 서버에 전송한 데이터
	 * @param response 서버로부터 응답받은 데이터
	 */
	void onDRM(KollusStorage storage, String request, String response);
	
	/**
	 * DRM 콜백 Kind 응답 정보(컨텐츠 삭제, 컨텐츠 수정, 컨텐츠 reset등)
	 * @param content 컨텐트 정보
	 * @param nInfoCode DCB_INFO_DELETE, DCB_INFO_EXPIRED, DCB_INFO_RESET중 하나의 값
	 */
	void onDRMInfo(KollusStorage storage, KollusContent content, int nInfoCode);
}

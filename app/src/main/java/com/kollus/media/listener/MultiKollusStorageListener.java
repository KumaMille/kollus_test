package com.kollus.media.listener;

import com.kollus.sdk.media.KollusStorage;
import com.kollus.sdk.media.content.KollusContent;

public interface MultiKollusStorageListener {
    void onComplete(KollusStorage storage, KollusContent content);

    void onProgress(KollusStorage storage, KollusContent content);

    void onError(KollusStorage storage, KollusContent content, int nErrorCode);
}

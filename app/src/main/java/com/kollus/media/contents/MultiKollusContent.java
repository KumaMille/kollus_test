package com.kollus.media.contents;

import com.kollus.sdk.media.KollusStorage;
import com.kollus.sdk.media.content.KollusContent;

public class MultiKollusContent {
    private KollusStorage mKollusStorage;
    private KollusContent mKollusContent;

    public MultiKollusContent(KollusStorage storage, KollusContent content) {
        mKollusStorage = storage;
        mKollusContent = content;
    }

    public KollusStorage getKollusStorage() {
        return mKollusStorage;
    }

    public KollusContent getKollusContent() {
        return mKollusContent;
    }
}

package com.kollus.media.preference;

import androidx.annotation.IntDef;

public class ValuePreference {
    @IntDef({
            DOUBLE_TAB_SCREEN_SIZE,
            DOUBLE_TAB_PLAY_PAUSE
    })
    public @interface DOUBLE_TAB_MODE {}

    public static final int DOUBLE_TAB_SCREEN_SIZE = 0;
    public static final int DOUBLE_TAB_PLAY_PAUSE = 1;

    @IntDef({
            PLAYING_RATE_UP,
            PLAYING_RATE_1,
            PLAYING_RATE_DOWN
    })
    public @interface PLAYING_RATE_MODE {}

    public static final int PLAYING_RATE_UP = 1;
    public static final int PLAYING_RATE_1 = 0;
    public static final int PLAYING_RATE_DOWN = -1;

    @IntDef({
            QUICK,
            EXACT
    })
    public @interface SeekType {}

    public static final int QUICK = 0;
    public static final int EXACT = 1;
}

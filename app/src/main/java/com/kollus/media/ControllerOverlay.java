/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kollus.media;

import android.graphics.Bitmap;
import android.view.View;
import android.widget.ArrayAdapter;

import com.kollus.media.chat.ChattingView;
import com.kollus.media.preference.ValuePreference;
import com.kollus.sdk.media.content.BandwidthItem;
import com.kollus.sdk.media.content.KollusBookmark;
import com.kollus.sdk.media.content.KollusContent.SubtitleInfo;
import com.kollus.sdk.media.util.SkinManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public interface ControllerOverlay {
    public enum State {
        PLAYING,
        PAUSED,
        BUFFERING,
        ENDED,
        ERROR,
        LOADING
    }

    public enum  MediaRouteState {
        STARTING,
        STARTED,
        RESUME,
        END
    }

    interface Listener {
        void onPlayPause();
        void onRew();
        void onFf();
        void onSeekStart();
        void onSeekMove(int time);
        void onSeekEnd(int time, int trimStartTime, int trimEndTime);
        void onVRCardView(boolean enable);
        void onScreenCapture();
        void onBookmarkKind(int type);
        void onBookmarkAdd();
        void onBookmarkRemoveView();
        void onBookmarkSeek(int position);
        void onCaptionSelected(int position);
        void onCaptionHide();
        void onShown();
        void onHidden();
        void onReplay();
        void onPlayingRate(@ValuePreference.PLAYING_RATE_MODE int mode);
        void onBookmarkHidden();
        void onToggleMute();
        void onSkip();

        void onScreenRotateLock(boolean lock);
        void onScreenSizeMode(int mode);
        void onRepeatAB(int direction);
        void onRepeat(boolean enable);
        void onAudioDelay(int timeMs);
        void onTimeShiftOff();

        void onLayoutChange(View v, int left, int top, int right, int bottom);
        void onSelectedBandwidth(int bandwidth);

        void onChatVisibleChanged(boolean visible);
    }

    public static final int REPEAT_MODE_DISABLE = 0;
    public static final int REPEAT_MODE_A = 1;
    public static final int REPEAT_MODE_B = 2;

    void dettachController();

    void setSkinManager(SkinManager skin);

    void setListener(Listener listener);

    void setCanReplay(boolean canReplay);

    void show();

    void showBookmark();

    void showCaption();

    void showResolution();

    void hide();

    void hideBookmark();

    void hideCaption();

    void hideResolution();

    void setAvailableMediaRoute(boolean visible);

    void setStateMediaRoute(MediaRouteState state);

    void setBookmarkList(ArrayList<KollusBookmark> list);

    void setBookmarkLableList(List<String> labels);

    void setBookmarkable(boolean isBookable);

    void setBookmarkAdapter(ArrayAdapter adapter);

    void setBookmarkSelected(int position);

    void setBookmarkCount(int type, int count);

    void setBookmarkWritable(boolean bWritable);

    void setDeleteButtonEnable(boolean enable);

    void setCaptionList(Vector<SubtitleInfo> list);

    boolean isShowing();

    void showPlaying();

    void showPaused();

    void showEnded();

//    void showLoading();

//    void showBuffering();
//
//    void hideBuffering(boolean playing);

    void setState(State state);

    void showWaterMark();

    void hideWaterMark();

    void showSkip(int sec);

    void hideSkip();

    void showSeekingTime(int seekTo, int seekAmount, int durationMs);

    void setSeekable(boolean enable);

    void setLive(boolean bLive, boolean bTimeShift);

    void setOrientation(int orientation);

    void setTitleText(String title);

    void setMute(boolean mute);

    void setScreenShotEnabled(boolean exist);

    void setScreenShot(Bitmap bm, int posionMs, int durationMs);

    void setTimes(int currentTime, int cachedTime, int totalTime,
                  int trimStartTime, int trimEndTime);

    void setPlayingRateText(double playing_rate);

    void setMoviePlayer(MoviePlayer parent);

    void setVolumeLabel(int level);

    void setSeekLabel(int maxX, int maxY, int x, int y, Bitmap bm, int positionMs, int durationMs, boolean bShow);

    void setBrightnessLabel(int level);

    void setPlayerTypeText(String type);

    void setCodecText(String codec);

    void setResolutionText(int width, int height);

    void setFrameRateText(int frameRate, int rejectRate);

    void toggleScreenLock();

    void toggleScreenSizeMode();

    void screenSizeScaleBegin();

    void toggleRepeatAB();

    void toggleRepeat();

    int getProgressbarHeight();

    void supportVR360(boolean support);

    void setTalkbackEnabled(boolean bTalkback);

    void setBluetoothConnectChanged(boolean connect);

    void setBandwidthItemList(List<BandwidthItem> list);

    void setCurrentBandwidth(String bandwidthName);

    void setChattingView(ChattingView view);
}

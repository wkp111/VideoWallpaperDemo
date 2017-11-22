package com.wkp.videowallpaper.receive;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;

import com.wkp.videowallpaper.service.VideoWallpaper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by user on 2017/11/22.
 */

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
public class VideoRespReceive extends BroadcastReceiver {
    private static final List<OnVideoWallpaperRespListener> mListeners = new ArrayList<>();
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            int respState = intent.getIntExtra(VideoWallpaper.EXTRA_RESPONSE, 0);
            File file = (File) intent.getSerializableExtra(VideoWallpaper.EXTRA_FILE);
            for (OnVideoWallpaperRespListener mListener : mListeners) {
                if (mListener != null) {
                    mListener.onVideoWallpaperResp(file,respState);
                }
            }
        }
    }

    /**
     * 设置视频桌面响应监听
     */
    public interface OnVideoWallpaperRespListener{
        void onVideoWallpaperResp(@Nullable File file, int respState);
    }

    /**
     * 添加设置视频桌面响应监听
     * @param listener
     */
    public void addOnVideoWallpaperRespListener(OnVideoWallpaperRespListener listener) {
        mListeners.add(listener);
    }

    /**
     * 移除设置视频桌面响应监听
     * @param listener
     */
    public void removeOnVideoWallpaperRespListener(OnVideoWallpaperRespListener listener) {
        mListeners.remove(listener);
    }

    /**
     * 移除所有设置视频桌面响应监听
     */
    public void removeAllOnVideoWallpaperRespListener() {
        mListeners.clear();
    }
}

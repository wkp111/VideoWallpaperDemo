package com.wkp.videowallpaper.service;

import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.Build;
import android.service.wallpaper.WallpaperService;
import android.support.annotation.RequiresApi;
import android.view.SurfaceHolder;
import android.widget.Toast;

import java.io.File;

/**
 * Created by user on 2017/11/17.
 */
@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
public class VideoWallpaper extends WallpaperService {
    public static final String ACTION_VIDEO_REQUEST = "com.wkp.video.request";      //服务接收数据的广播action
    public static final String ACTION_VIDEO_RESPONSE = "com.wkp.video.response";    //服务发送数据的广播action
    public static final String EXTRA_FILE = "extra_file";                           //服务接收/发送数据的广播key
    public static final String EXTRA_VOLUME = "extra_volume";                       //服务接收数据的广播key
    public static final String EXTRA_TOAST = "extra_toast";                         //服务接收数据的广播key
    public static final String EXTRA_RESPONSE = "extra_response";                   //服务发送数据的广播key
    public static final int RESP_SUCCESS = 100;                                     //桌面设置成功
    public static final int RESP_FAILED_NULL = 101;                                 //视频文件为空
    public static final int RESP_FAILED_ERROR = 102;                                //视频播放异常
    public static final int RESP_FAILED_INIT = 103;                                 //引擎无播放控件
    private VideoEngine mVideoEngine;
    private File mFile;
    private boolean mIsVolume;
    private boolean mIsToast;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mVideoEngine == null || mVideoEngine.mHolder == null) {
            startVideo();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * 开启视频播放
     */
    private void startVideo() {
        Intent intent = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
        intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, new ComponentName(this, VideoWallpaper.class));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    /**
     * 创建引擎
     *
     * @return
     */
    @Override
    public Engine onCreateEngine() {
        mVideoEngine = new VideoEngine();
        return mVideoEngine;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mVideoEngine = null;
    }

    /**
     * 视频播放引擎
     */
    class VideoEngine extends Engine {
        private SurfaceHolder mHolder;
        private MediaPlayer mPlayer;

        /**
         * 广播接收数据
         */
        private BroadcastReceiver mVideoReceive = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent != null) {
                    mFile = (File) intent.getSerializableExtra(EXTRA_FILE);
                    mIsVolume = intent.getBooleanExtra(EXTRA_VOLUME, false);
                    mIsToast = intent.getBooleanExtra(EXTRA_TOAST, false);
                    if (mFile != null) {
                        startVideo(mFile, mIsVolume);
                    } else {
                        sendResp(EXTRA_RESPONSE, RESP_FAILED_NULL);
                        if (mIsToast) {
                            Toast.makeText(VideoWallpaper.this, "文件为空", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        };

        /**
         * 发送响应广播
         *
         * @param key
         * @param value
         */
        private void sendResp(String key, int value) {
            Intent respIntent = new Intent();
            respIntent.setAction(ACTION_VIDEO_RESPONSE);
            respIntent.putExtra(key, value);
            respIntent.putExtra(EXTRA_FILE, mFile);
            sendBroadcast(respIntent);
        }

        /**
         * 开始播放视频
         *
         * @param file
         * @param isVolume
         */
        private void startVideo(File file, boolean isVolume) {
            if (mHolder != null) {
                try {
                    if (mPlayer != null) {
                        mPlayer.release();
                    }
                    mPlayer = new MediaPlayer();
                    mPlayer.setSurface(mHolder.getSurface());
                    mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mp) {
                            sendResp(EXTRA_RESPONSE, RESP_SUCCESS);
                            if (mIsToast) {
                                Toast.makeText(VideoWallpaper.this, "桌面设置成功", Toast.LENGTH_SHORT).show();
                            }
                            mp.start();
                        }
                    });
                    mPlayer.setDataSource(file.getAbsolutePath());
                    mPlayer.setVolume(isVolume ? 1.0f : 0, isVolume ? 1.0f : 0);
                    mPlayer.setLooping(true);
                    mPlayer.prepareAsync();
                } catch (Exception e) {
                    e.printStackTrace();
                    sendResp(EXTRA_RESPONSE, RESP_FAILED_ERROR);
                    if (mIsToast) {
                        Toast.makeText(VideoWallpaper.this, "无法播放该文件", Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                sendResp(EXTRA_RESPONSE, RESP_FAILED_INIT);
                if (mIsToast) {
                    Toast.makeText(VideoWallpaper.this, "初始化失败", Toast.LENGTH_SHORT).show();
                }
            }
        }

        /**
         * 创建引擎
         *
         * @param surfaceHolder
         */
        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_VIDEO_REQUEST);
            registerReceiver(mVideoReceive, filter);
        }

        /**
         * 创建播放器
         *
         * @param holder
         */
        @Override
        public void onSurfaceCreated(SurfaceHolder holder) {
            super.onSurfaceCreated(holder);
            mHolder = holder;
            if (mFile != null) {
                startVideo(mFile, mIsVolume);
            }
        }

        /**
         * 可见性改变
         *
         * @param visible
         */
        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            if (mPlayer != null) {
                if (visible) {
                    mPlayer.start();
                } else {
                    mPlayer.pause();
                }
            }
        }

        /**
         * 销毁播放器
         *
         * @param holder
         */
        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            mHolder = null;
            if (mPlayer != null) {
                mPlayer.release();
                mPlayer = null;
            }
        }

        /**
         * 销毁引擎
         */
        @Override
        public void onDestroy() {
            super.onDestroy();
            unregisterReceiver(mVideoReceive);
        }
    }

}

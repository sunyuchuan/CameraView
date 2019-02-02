package com.example.ffcmd.ffcmd_demo.view.player;

import android.annotation.TargetApi;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

import com.example.ffcmd.ffcmd_demo.view.gpuimage.GPUImageRenderer;

import java.io.IOException;

/**
 * Created by sunyc on 19-1-24.
 */

public class XmMediaPlayer implements MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener, GPUImageRenderer.OnVideoSurfacePrepareListener {
    private static final String TAG = "XmMediaPlayer";
    private MediaPlayer mp;
    private boolean isSurfaceCreated;
    private boolean isDataSourceSet;
    private int width = 0;
    private int height = 0;

    public XmMediaPlayer() {
    }

    public void init() {
        stop();
        release();
        mp = new MediaPlayer();
        mp.setScreenOnWhilePlaying(true);
        mp.setOnCompletionListener(this);
        mp.setOnPreparedListener(this);
    }

    public void setDataSource(String url) {
        reset();
        try {
            mp.setDataSource(url);

            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(url);
            width = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
            height = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));

            isDataSourceSet = true;
            if (isSurfaceCreated) {
                prepareAsync();
            }
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.i(TAG, "onPrepared");
        start();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.i(TAG, "onCompletion");
        release();
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void surfacePrepared(Surface surface) {
        setSurface(surface);
        isSurfaceCreated = true;
        if (isDataSourceSet) {
            prepareAsync();
        }
    }

    public void setSurface(Surface surface) {
        if (mp != null) {
            mp.setSurface(surface);
            surface.release();
        }
    }

    public void prepareAsync() {
        if (mp != null) {
            mp.prepareAsync();
        }
    }

    public void start() {
        if (mp != null) {
            mp.start();
        }
    }

    public void pause() {
        if (mp != null) {
            mp.pause();
        }
    }

    public void stop() {
        if (mp != null) {
            mp.stop();
        }
    }

    public void reset() {
        if (mp != null) {
            mp.reset();
        }
    }

    public void release() {
        if (mp != null) {
            mp.release();
            isSurfaceCreated = false;
            isDataSourceSet = false;
            mp = null;
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}

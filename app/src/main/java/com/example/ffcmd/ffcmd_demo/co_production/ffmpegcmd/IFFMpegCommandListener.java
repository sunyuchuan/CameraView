package com.example.ffcmd.ffcmd_demo.co_production.ffmpegcmd;

/**
 * Created by sunyc on 18-11-25.
 */

public interface IFFMpegCommandListener {
    void onInfo(int arg1, int arg2, Object obj);
    void onError(int arg1, int arg2, Object obj);
}

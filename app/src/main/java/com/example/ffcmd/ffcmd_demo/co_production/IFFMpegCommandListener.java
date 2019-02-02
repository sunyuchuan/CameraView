package com.example.ffcmd.ffcmd_demo.co_production;

/**
 * Created by sunyc on 18-11-25.
 */

interface IFFMpegCommandListener {
    void onInfo(int arg1, int arg2, Object obj);
    void onError(int arg1, int arg2, Object obj);
}

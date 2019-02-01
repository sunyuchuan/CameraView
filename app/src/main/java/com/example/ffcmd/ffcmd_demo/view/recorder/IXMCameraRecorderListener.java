package com.example.ffcmd.ffcmd_demo.view.recorder;

/**
 * Created by sunyc on 18-10-26.
 */

public interface IXMCameraRecorderListener {
    void onRecorderPrepared();
    void onRecorderStarted();
    void onRecorderStopped();
    void onPreviewStarted();
    void onPreviewStopped();
}

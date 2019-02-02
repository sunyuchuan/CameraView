package com.example.ffcmd.ffcmd_demo.view.recorder;

/**
 * Created by sunyc on 18-10-26.
 */

public interface IXMCameraRecorderListener {
    void onImageReaderPrepared();
    void onRecorderPrepared();
    void onRecorderStarted();
    void onRecorderStopped();
    void onRecorderError();
    void onPreviewStarted();
    void onPreviewStopped();
    void onPreviewError();
}

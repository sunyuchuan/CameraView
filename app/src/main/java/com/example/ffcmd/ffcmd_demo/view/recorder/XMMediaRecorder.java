package com.example.ffcmd.ffcmd_demo.view.recorder;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.lang.ref.WeakReference;

/**
 * Created by sunyc on 18-10-26.
 */

public class XMMediaRecorder {
    private static final String TAG = "XMMediaRecorder";
    private EventHandler mEventHandler;
    private IXMCameraRecorderListener mListener = null;
    private long mNativeXMMediaRecorder = 0;
    private boolean mUseSoftEncoder = false;
    private boolean mAudioEnable = false;
    private boolean mVideoEnable = false;

    private static XMMediaRecorder mRecorder = null;
    private static final int RECORDER_NOP = 0;
    private static final int RECORDER_PREPARED = 1;
    private static final int RECORDER_COMPLETED = 2;
    private static final int RECORDER_ERROR = 100;
    private static final int RECORDER_INFO = 200;

    private static final int MR_MSG_ERROR = 100;
    private static final int MR_MSG_STARTED = 200;
    private static final int MR_MSG_STOPPED = 300;

    public boolean mRecording = false;

    static {
        String ABI = Build.CPU_ABI;
        Log.d(TAG, "ABI " + ABI);
        System.loadLibrary("ijkffmpeg-" + ABI);
        System.loadLibrary("ijksdl-" + ABI);
        System.loadLibrary("ijkplayer-" + ABI);
        System.loadLibrary("xmrecorder-" + ABI);
    }

    public static XMMediaRecorder getInstance(boolean useSoftEncoder, boolean hasAudio, boolean hasVideo) {
        if(mRecorder != null)
            return mRecorder;

        mRecorder = new XMMediaRecorder(useSoftEncoder, hasAudio, hasVideo);
        return mRecorder;
    }

    private void initRecorder() {
        Looper looper;
        if ((looper = Looper.myLooper()) != null) {
            mEventHandler = new XMMediaRecorder.EventHandler(this, looper);
        } else if ((looper = Looper.getMainLooper()) != null) {
            mEventHandler = new XMMediaRecorder.EventHandler(this, looper);
        } else {
            mEventHandler = null;
        }

        native_setup(new WeakReference<XMMediaRecorder>(this), mUseSoftEncoder, mAudioEnable, mVideoEnable);
    }

    private XMMediaRecorder(boolean useSoftEncoder, boolean hasAudio, boolean hasVideo)
    {
        mUseSoftEncoder = useSoftEncoder;
        mAudioEnable = hasAudio;
        mVideoEnable = hasVideo;
        initRecorder();
    }

    public boolean setConfigParams(int[] intParams, String[] stringParams) {
        return _setConfigParams(intParams, stringParams);
    }

    public void setListener(IXMCameraRecorderListener l)
    {
        mListener = l;
    }

    public void prepareAsync() {
        _prepareAsync();
    }

    public void start() {
        _start();
    }

    public void stop() {
        _stop();
    }

    public void release() {
        _release();
        mRecorder = null;
    }

    public void put(byte[] data, int w, int h, int rotate_degrees, boolean flipHorizontal, boolean flipVertical) {
        if(data != null && w > 0 && h > 0)
            _put(data, w, h, rotate_degrees, flipHorizontal, flipVertical);
    }

    public static native void NV21toABGR(byte[] yuv, int width, int height, byte[] gl_out);
    private native boolean _setConfigParams(int[] intParams, String[] stringParams);
    private native void _start();
    private native void _stop();
    private native void _put(byte[] data, int w, int h, int rotate_degrees, boolean flipHorizontal, boolean flipVertical);
    private native void native_setup(Object CameraRecoder_this, boolean useSoftEncoder, boolean audioEnable, boolean videoEnable);
    public native void native_finalize();
    private native void _reset();
    private native void _release();
    private native void _prepareAsync();

    private static void postEventFromNative(Object weakThiz, int what,
                                            int arg1, int arg2, Object obj) {
        if (weakThiz == null)
            return;

        XMMediaRecorder recorder = (XMMediaRecorder) ((WeakReference) weakThiz).get();
        if (recorder == null) {
            return;
        }

        if (recorder.mEventHandler != null) {
            Message m = recorder.mEventHandler.obtainMessage(what, arg1, arg2, obj);
            recorder.mEventHandler.sendMessage(m);
        }
    }

    private final void onRecorderPrepared() {
        if(mListener != null)
            mListener.onRecorderPrepared();
    }

    private final void onRecorderStarted() {
        if(mListener != null)
            mListener.onRecorderStarted();
    }

    private final void onRecorderStopped() {
        if(mListener != null)
            mListener.onRecorderStopped();
    }

    private static class EventHandler extends Handler {
        private final WeakReference<XMMediaRecorder> mWeakRecoder;

        public EventHandler(XMMediaRecorder recorder, Looper looper) {
            super(looper);
            mWeakRecoder = new WeakReference<XMMediaRecorder>(recorder);
        }

        @Override
        public void handleMessage(Message msg) {
            XMMediaRecorder recoder = mWeakRecoder.get();
            if (recoder == null || recoder.mNativeXMMediaRecorder == 0) {
                return;
            }

            switch (msg.what) {
                case RECORDER_NOP:
                    Log.d(TAG, "msg_loop nop");
                    break;
                case RECORDER_PREPARED:
                    recoder.onRecorderPrepared();
                    return;
                case RECORDER_INFO:
                    if(msg.arg1 == MR_MSG_STARTED)
                        recoder.onRecorderStarted();
                    else if(msg.arg1 == MR_MSG_STOPPED)
                        recoder.onRecorderStopped();
                    break;
                case RECORDER_COMPLETED:
                    Log.d(TAG, "RECORDER_COMPLETED");
                    return;
                default:
                    Log.i(TAG, "Unknown message type " + msg.what);
            }
        }
    }
}

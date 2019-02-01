package com.example.ffcmd.ffcmd_demo.co_production.ffmpegcmd;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.lang.ref.WeakReference;

/**
 * Created by sunyc on 18-11-24.
 */

public class FFmpegCommand {
    private static final String TAG = "FFmpegCommand";

    private EventHandler mEventHandler;
    private IFFMpegCommandListener mListener = null;
    private long mNativeFFmpegCommand = 0;

    private static final int FFCMD_NOP = 0;
    private static final int FFCMD_ERROR = 1;
    private static final int FFCMD_INFO = 2;

    public static final int FFCMD_INFO_PREPARED = 100;
    public static final int FFCMD_INFO_STARTED = 200;
    public static final int FFCMD_INFO_PROGRESS= 300;
    public static final int FFCMD_INFO_STOPPED = 400;
    public static final int FFCMD_INFO_COMPLETED = 500;

    private boolean mRunning = false;

    private void initXMFFCmd() {
        Looper looper;
        if ((looper = Looper.myLooper()) != null) {
            mEventHandler = new EventHandler(this, looper);
        } else if ((looper = Looper.getMainLooper()) != null) {
            mEventHandler = new EventHandler(this, looper);
        } else {
            mEventHandler = null;
        }

        native_setup(new WeakReference<FFmpegCommand>(this));
    }

    public FFmpegCommand()
    {
        initXMFFCmd();
    }

    public void setListener(IFFMpegCommandListener l)
    {
        mListener = l;
    }

    public int prepareAsync() {
        return native_prepareAsync();
    }

    public void setStatus(boolean running)
    {
        mRunning = running;
    }

    public boolean getStatus()
    {
        return mRunning;
    }

    public void start(int argc, String[] argv) {
        if(argc >= 2)
            native_start(argc, argv);
    }

    public void stop() {
        native_stop();
    }

    public void release() {
        native_release();
    }

    private void CmdInfo(int arg1, int arg2, Object obj) {
        if(mListener != null)
        {
            mListener.onInfo(arg1, arg2, obj);
        }
    }

    private void CmdError(int arg1, int arg2, Object obj) {
        if(mListener != null)
        {
            mListener.onError(arg1, arg2, obj);
        }
    }

    private static void postEventFromNative(Object weakThiz, int what,
                                            int arg1, int arg2, Object obj) {
        if (weakThiz == null)
            return;

        FFmpegCommand cmd = (FFmpegCommand) ((WeakReference) weakThiz).get();
        if (cmd == null) {
            return;
        }

        if (cmd.mEventHandler != null) {
            Message m = cmd.mEventHandler.obtainMessage(what, arg1, arg2, obj);
            cmd.mEventHandler.sendMessage(m);
        }
    }

    private static class EventHandler extends Handler {
        private final WeakReference<FFmpegCommand> mWeakCmd;

        public EventHandler(FFmpegCommand cmd, Looper looper) {
            super(looper);
            mWeakCmd = new WeakReference<FFmpegCommand>(cmd);
        }

        @Override
        public void handleMessage(Message msg) {
            FFmpegCommand cmd = mWeakCmd.get();
            if (cmd == null || cmd.mNativeFFmpegCommand == 0) {
                return;
            }

            switch (msg.what) {
                case FFCMD_NOP:
                    Log.d(TAG, "msg_loop nop");
                    break;
                case FFCMD_INFO:
                    cmd.CmdInfo(msg.arg1, msg.arg2, msg.obj);
                    break;
                case FFCMD_ERROR:
                    cmd.CmdError(msg.arg1, msg.arg2, msg.obj);
                    Log.d(TAG, "ffcmd error");
                    return;
                default:
                    Log.i(TAG, "Unknown message type " + msg.what);
            }
        }
    }

    private native void native_setup(Object CameraRecoder_this);
    private native void native_finalize();
    private native int native_prepareAsync();
    private native void native_start(int argc, String[] argv);
    private native void native_stop();
    private native void native_release();
}

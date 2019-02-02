package com.example.ffcmd.ffcmd_demo.co_production;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.example.ffcmd.ffcmd_demo.AccessedByNative;
import com.example.ffcmd.ffcmd_demo.IjkLibLoader;

import java.lang.ref.WeakReference;

/**
 * Created by sunyc on 18-11-24.
 */

class FFmpegCommand {
    private static final String TAG = "FFmpegCommand";

    private EventHandler mEventHandler;
    private IFFMpegCommandListener mListener = null;
    @AccessedByNative
    private long mNativeFFmpegCommand = 0;
    private static boolean mIsLibLoaded = false;

    private static final int FFCMD_NOP = 0;
    private static final int FFCMD_ERROR = 1;
    private static final int FFCMD_INFO = 2;

    public static final int FFCMD_INFO_PREPARED = 100;
    public static final int FFCMD_INFO_STARTED = 200;
    public static final int FFCMD_INFO_PROGRESS= 300;
    public static final int FFCMD_INFO_STOPPED = 400;
    public static final int FFCMD_INFO_COMPLETED = 500;

    private static final IjkLibLoader sLocalLibLoader = new IjkLibLoader() {
        @Override
        public void loadLibrary(String libName) throws UnsatisfiedLinkError, SecurityException {
            String ABI = Build.CPU_ABI;
            Log.i(TAG, "ABI " + ABI + " libName " +libName);
            System.loadLibrary(libName + "-" + ABI);
        }
    };

    private static void loadLibrariesOnce(IjkLibLoader libLoader) {
        synchronized (FFmpegCommand.class) {
            if (!mIsLibLoaded) {
                if (libLoader == null)
                    libLoader = sLocalLibLoader;

                libLoader.loadLibrary("ijkffmpeg");
                libLoader.loadLibrary("ijksdl" );
                libLoader.loadLibrary("xmffcmd");
                mIsLibLoaded = true;
            }
        }
    }

    public FFmpegCommand() {
        loadLibrariesOnce(sLocalLibLoader);
        initXMFFCmd();
    }

    public FFmpegCommand(IjkLibLoader libLoader) {
        loadLibrariesOnce(libLoader);
        initXMFFCmd();
    }

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

    public void setLogLevel(int level) {
        native_setLogLevel(level);
    }

    public void setListener(IFFMpegCommandListener l) {
        mListener = l;
    }

    public int prepareAsync() {
        return native_prepareAsync();
    }

    public void start(int argc, String[] argv) {
        if (argc >= 2)
            native_start(argc, argv);
    }

    public void stop() {
        native_stop();
    }

    public void release() {
        native_release();
    }

    private void CmdInfo(int arg1, int arg2, Object obj) {
        if (mListener != null) {
            mListener.onInfo(arg1, arg2, obj);
        }
    }

    private void CmdError(int arg1, int arg2, Object obj) {
        if (mListener != null) {
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

    private native void native_setLogLevel(int level);
    private native void native_setup(Object weakobj);
    private native void native_finalize();
    private native int native_prepareAsync();
    private native void native_start(int argc, String[] argv);
    private native void native_stop();
    private native void native_release();

    @Override
    protected void finalize() throws Throwable {
        Log.i(TAG, "finalize");
        try {
            native_finalize();
        } finally {
            super.finalize();
        }
    }
}

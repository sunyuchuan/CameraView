package com.example.ffcmd.ffcmd_demo.view;

import android.content.Context;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;

import com.example.ffcmd.ffcmd_demo.view.gpuimage.GPUImageRenderer;
import com.example.ffcmd.ffcmd_demo.view.gpuimage.filter.GPUImageFilter;
import com.example.ffcmd.ffcmd_demo.view.recorder.IXMCameraRecorderListener;
import com.example.ffcmd.ffcmd_demo.view.recorder.XMMediaRecorder;
import com.example.ffcmd.ffcmd_demo.view.utils.CameraManager;


/**
 * Created by sunyc on 18-11-20.
 */

public class CameraView extends GLSurfaceView {
    private static final String TAG = "CameraView";
    private GPUImageRenderer mGPUImageRenderer = null;
    private CameraManager mCamera = null;
    private boolean useSoftEncoder = true;
    private boolean hasAudio = false;
    private boolean hasVideo = true;
    private boolean needReleaseRecorder = false;
    protected XMMediaRecorder mRecorder = null;
    private ICameraViewListener mListener = null;
    private int mInputWidth = 960;
    private int mInputHeight = 540;
    private int mOuptutFps = 15;
    private String mOutputPath = null;
    private RecorderParams params = new RecorderParams(700000, 5, 23, 1000, "veryfast");
    private boolean mRecorderStopped = true;

    public CameraView(Context context) {
        this(context, null);
    }

    public CameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    private void initView() {
        mCamera = CameraManager.getInstance();
        mCamera.setCameraView(this);
        mCamera.setListener(onXMCameraRecorderListener);
        mRecorder = XMMediaRecorder.getInstance(useSoftEncoder, hasAudio, hasVideo);
        mRecorder.setListener(onXMCameraRecorderListener);
        if(mGPUImageRenderer == null) {
            mGPUImageRenderer = new GPUImageRenderer(getContext().getApplicationContext());
            mGPUImageRenderer.setListener(onXMCameraRecorderListener);
        }
    }

    protected void setSurfaceView() {
        mGPUImageRenderer.setGLSurfaceView(this);
    }

    public void setWindowRotation(int windowRotation) {
        if (mCamera != null)
            mCamera.setWindowRotation(windowRotation);
    }

    public void setExpectedFps(int fps) {
        if (mCamera != null)
            mCamera.setExpectedFps(fps);
    }

    public void setExpectedResolution(int w, int h) {
        if (mCamera != null)
            mCamera.setExpectedResolution(w, h);
    }

    public void setListener(ICameraViewListener l) {
        mListener = l;
    }

    /*Turn on camera preview*/
    public void startCameraPreview() {
        if (mCamera != null)
            mCamera.onResume();
    }

    /*stop camera preview*/
    public void stopCameraPreview() {
        if (mCamera != null)
            mCamera.onRelease();
    }

    public void setUpCamera(final Camera camera, final int degrees, final boolean flipHorizontal,
                            final boolean flipVertical) {
        mInputWidth = camera.getParameters().getPreviewSize().width;
        mInputHeight = camera.getParameters().getPreviewSize().height;
        int[] range = new int[2];
        camera.getParameters().getPreviewFpsRange(range);
        mOuptutFps = range[0] / 1000;
        if(range[1] != range[0])
        {
            Log.w(TAG, "camera output fps is dynamic, range from " + range[0] + " to " + range[1]);
            mOuptutFps = 15;
        }
        Log.i(TAG, "mInputWidth " + mInputWidth + " mInputHeight " + mInputHeight + " mOuptutFps " + mOuptutFps);

        if (mGPUImageRenderer != null)
            mGPUImageRenderer.setUpCamera(camera, degrees, flipHorizontal, flipVertical);
    }

    /*Release CameraView
     *include Recorder/opengl/camera preview, etc.
     */
    public void release() {
        releaseRecorder();
        releaseOpengl();
        releaseCamera();
    }

    /*Request to start recording*/
    public void startRecorder(String outputPath) {
        mOutputPath = outputPath;
        needReleaseRecorder = false;
        mRecorderStopped = false;
        setRecorderStatus(mRecorder, false);
        if (mRecorder != null) {
            int[] intParams = new int[7];
            intParams[0] = getOutputWidth(); //width
            intParams[1] = getOutputHeight(); //height
            intParams[2] = (int) (700000 * ((float)(intParams[0] * intParams[1]) / (float)(540*960))); //bitrate
            intParams[3] = mOuptutFps; //fps
            intParams[4] = params.gop_size * mOuptutFps; //gop size
            intParams[5] = params.crf; //crf
            intParams[6] = params.multiple; //time_base is params.multiple * mOuptutFps
            String[] charParams = new String[2];
            charParams[0] = mOutputPath;
            charParams[1] = params.preset; //preset
            if(!mRecorder.setConfigParams(intParams, charParams))
            {
                Log.e(TAG, "setConfigParams failed, exit");
                return;
            }

            mRecorder.prepareAsync();
            //setRecorderStatus(mRecorder, true);
        }
    }

    /*Request to stop recording*/
    public void stopRecorder() {
        setRecorderStatus(mRecorder, false);
        if (mRecorder != null) {
            mRecorder.stop();
            needReleaseRecorder = false;
        }
    }

    /*Switch to front or rear camera*/
    public void switchCamera() {
        if (mCamera != null) {
            mCamera.switchCamera();
            requestRender();
        }
    }

    /*Set filters, such as beauty, etc.*/
    public void setFilter(final GPUImageFilter filter) {
        if (mGPUImageRenderer != null) {
            mGPUImageRenderer.setFilter(filter);
            boolean status = false;
            if(mRecorder != null)
                status = mRecorder.mRecording;
            mGPUImageRenderer.setRecorderStatus(mRecorder, status);
        }
    }

    /*The callback interface that the APP needs to set*/
    public interface ICameraViewListener {
        /*
         *The video recording process has started working
         */
        void onRecorderStarted();

        /*
        *The recording process has stopped working,
        *and the recorded video has been generated.
        *The video file was generated successfully.
        */
        void onRecorderStopped();

        /*Camera preview has started*/
        void onPreviewStarted();

        /*Camera preview has stopped*/
        void onPreviewStopped();
    }

    /*Demo app test interface*/
    public void testAPISetSurfaceView() {
        mGPUImageRenderer.setGLSurfaceView(this);
    }

    private void setRecorderStatus(XMMediaRecorder recorder, boolean status) {
        if (recorder != null)
            recorder.mRecording = status;

        if (mGPUImageRenderer != null)
            mGPUImageRenderer.setRecorderStatus(recorder, status);
    }

    private int getOutputWidth() {
        int w = 0;
        if (mGPUImageRenderer != null)
            w = mGPUImageRenderer.getOutputWidth();

        return w;
    }

    private int getOutputHeight() {
        int h = 0;
        if (mGPUImageRenderer != null)
            h = mGPUImageRenderer.getOutputHeight();

        return h;
    }


    private int getInputWidth() {
        int w = 0;
        if (mGPUImageRenderer != null)
            w = mGPUImageRenderer.getInputWidth();

        return w;
    }

    private int getInputHeight() {
        int h = 0;
        if (mGPUImageRenderer != null)
            h = mGPUImageRenderer.getInputHeight();

        return h;
    }

    private void releaseCamera() {
        if (mCamera != null)
            mCamera.onRelease();
    }

    private void releaseRecorder() {
        setRecorderStatus(mRecorder, false);
        if (mRecorder != null) {
            if (mRecorderStopped) {
                mRecorder.release();
                mRecorder.setListener(null);
                mRecorder = null;
                Log.i(TAG, "releaseRecorder");
            } else {
                mRecorder.stop();
                needReleaseRecorder = true;
            }
        }
    }

    private void releaseOpengl() {
        if (mGPUImageRenderer != null)
            mGPUImageRenderer.onStop();
    }

    private IXMCameraRecorderListener onXMCameraRecorderListener = new IXMCameraRecorderListener() {
        /*
        *notification
        *The preparation for the recorder is ready,
        *and Request recorder to start coding.
        */
        @Override
        public void onRecorderPrepared() {
            Log.i(TAG, "onRecorderPrepared");
            mRecorder.start();
        }

        /*The recorder really start notification*/
        @Override
        public void onRecorderStarted() {
            Log.i(TAG, "onRecorderStarted");
            setRecorderStatus(mRecorder, true);
            mListener.onRecorderStarted();
        }

        /*The recorder really stop notification*/
        @Override
        public void onRecorderStopped() {
            Log.i(TAG, "onRecorderStopped");
            mRecorderStopped = true;
            if(needReleaseRecorder) {
                needReleaseRecorder = false;
                mRecorder.release();
                mRecorder.setListener(null);
                mRecorder = null;
                Log.i(TAG, "release mRecorder");
                return;
            }
            mListener.onRecorderStopped();
        }

        /*Camera preview on notification*/
        @Override
        public void onPreviewStarted() {
            Log.i(TAG, "onPreviewStarted");
            mListener.onPreviewStarted();
        }

        /*Stop camera preview notification*/
        @Override
        public void onPreviewStopped() {
            Log.i(TAG, "onPreviewStopped");
            mListener.onPreviewStopped();
        }
    };

    private class RecorderParams {
        public int bit_rate;
        public int gop_size;
        public int crf;
        public int multiple;
        public String preset;

        public RecorderParams(int bitrate, int gopsize, int crf, int multiple, String preset) {
            bit_rate = bitrate;
            gop_size = gopsize;
            this.crf = crf;
            this.multiple = multiple;
            this.preset = preset;
        }
    }
}

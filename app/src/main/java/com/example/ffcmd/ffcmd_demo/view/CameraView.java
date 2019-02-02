package com.example.ffcmd.ffcmd_demo.view;

import android.content.Context;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;

import com.example.ffcmd.ffcmd_demo.view.player.XmMediaPlayer;
import com.example.ffcmd.ffcmd_demo.view.encoder.video.ImageEncoderCore;
import com.example.ffcmd.ffcmd_demo.view.gpuimage.GPUImageRenderer;
import com.example.ffcmd.ffcmd_demo.view.recorder.IXMCameraRecorderListener;
import com.example.ffcmd.ffcmd_demo.view.recorder.XMMediaRecorder;
import com.example.ffcmd.ffcmd_demo.view.utils.CameraManager;
import com.example.ffcmd.ffcmd_demo.view.utils.XMFilterType;

/**
 * Created by sunyc on 18-11-20.
 */

public class CameraView extends GLSurfaceView {
    private static final String TAG = "CameraView";
    private static final float VIEW_ASPECT_RATIO = 4f / 3f;
    private GPUImageRenderer mGPUImageRenderer = null;
    private CameraManager mCamera = null;
    private boolean useSoftEncoder = true;
    private boolean hasAudio = false;
    private boolean hasVideo = true;
    private XMMediaRecorder mRecorder = null;
    private ICameraViewListener mListener = null;
    private int mInputWidth = 960;
    private int mInputHeight = 540;
    private int mOuptutFps = 15;
    private String mOutputPath = null;
    private RecorderParams params = new RecorderParams(700000, 1, 23, 1000, 0, "veryfast");
    private volatile boolean isRecording = false;
    private boolean mImageReaderPrepared = false;
    private boolean mXMMediaRecorderPrepared = false;
    private XmMediaPlayer mMediaPlayer = null;
    private float videoAspectRatio = VIEW_ASPECT_RATIO;

    public CameraView(Context context) {
        this(context, null);
    }

    public CameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    private void initView() {
        mCamera = new CameraManager();
        mCamera.setCameraView(this);
        mCamera.setListener(onXMCameraRecorderListener);
        mRecorder = new XMMediaRecorder(useSoftEncoder, hasAudio, hasVideo);
        mRecorder.setListener(onXMCameraRecorderListener);
        mMediaPlayer = new XmMediaPlayer();
        mGPUImageRenderer = new GPUImageRenderer(getContext().getApplicationContext(), mRecorder);
        mGPUImageRenderer.setListener(onXMCameraRecorderListener);
        mGPUImageRenderer.setOnVideoSurfacePrepareListener(mMediaPlayer);
    }

    private void calculateVideoAspectRatio(int videoWidth, int videoHeight) {
        if (videoWidth > 0 && videoHeight > 0) {
            videoAspectRatio = (float) videoWidth / videoHeight;
        }

        requestLayout();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        double currentAspectRatio = (double) widthSize / heightSize;
        if (currentAspectRatio > videoAspectRatio) {
            widthSize = (int) (heightSize * videoAspectRatio);
        } else {
            heightSize = (int) (widthSize / videoAspectRatio);
        }

        super.onMeasure(MeasureSpec.makeMeasureSpec(widthSize, widthMode),
                MeasureSpec.makeMeasureSpec(heightSize, heightMode));
    }

    protected void setSurfaceView() {
        if(mGPUImageRenderer != null)
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

    public void setOnImageEncoderListener(ImageEncoderCore.OnImageEncoderListener l) {
        if(mGPUImageRenderer != null)
            mGPUImageRenderer.setOnImageEncoderListener(l);
    }

    /*Turn on camera preview*/
    public void startCameraPreview() {
        if(mGPUImageRenderer != null)
            mGPUImageRenderer.cleanRunOnDraw();

        if (mCamera != null)
            mCamera.onResume();

        clearAnimation();
        setVisibility(VISIBLE);
    }

    /*stop camera preview*/
    public void stopCameraPreview() {
        if(mGPUImageRenderer != null)
            mGPUImageRenderer.cleanRunOnDraw();

        setVisibility(GONE);
        clearAnimation();

        if (mCamera != null)
            mCamera.onRelease();
    }

    public void setPipRectCoordinate(float[] buffer) {
        if(mGPUImageRenderer != null) {
            mGPUImageRenderer.setPipRectCoordinate(buffer);
        }
    }

    public void startVideoPlayer(String videoPath) {
        if(mMediaPlayer != null) {
            mMediaPlayer.init();
            mMediaPlayer.setDataSource(videoPath);
            if(mGPUImageRenderer != null) {
                mGPUImageRenderer.prepareVideoSurface();
            }
            calculateVideoAspectRatio(mMediaPlayer.getWidth(), mMediaPlayer.getHeight());
        }
    }

    public void stopVideoPlayer() {
        if(mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
        }
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
        synchronized (this) {
            releaseVideoPlayer();
            releaseOpengl();
            releaseRecorder();
            releaseCamera();
            mListener = null;
            params = null;
        }
    }

    /*Request to start recording*/
    public void startRecorder(String outputPath) {
        synchronized (this) {
            if(getStatus())
            {
                Log.w(TAG, "Recorder is running, exit");
                mListener.onRecorderStopped();
                return;
            }

            if (mRecorder != null) {
                Log.i(TAG, "startRecorder outputPath " + outputPath);
                mOutputPath = outputPath;
                mGPUImageRenderer.changeVideoEncoderStatus(true);
                mImageReaderPrepared = false;
                mXMMediaRecorderPrepared = false;

                int[] intParams = new int[8];
                intParams[0] = getOutputWidth(); //width
                intParams[1] = getOutputHeight(); //height
                intParams[2] = (int) (700000 * ((float) (intParams[0] * intParams[1]) / (float) (540 * 960))); //bitrate
                intParams[3] = mOuptutFps; //fps
                intParams[4] = params.gop_size * mOuptutFps; //gop size
                intParams[5] = params.crf; //crf
                intParams[6] = params.multiple; //time_base is params.multiple * mOuptutFps
                intParams[7] = params.max_b_frames;
                String[] charParams = new String[2];
                charParams[0] = mOutputPath;
                charParams[1] = params.preset; //preset
                if (!mRecorder.setConfigParams(intParams, charParams)) {
                    Log.e(TAG, "setConfigParams failed, exit");
                    mGPUImageRenderer.changeVideoEncoderStatus(false);
                    mListener.onRecorderStopped();
                    return;
                }

                mRecorder.prepareAsync();
                setStatus(true);
            }
        }
    }

    /*Request to stop recording*/
    public void stopRecorder() {
        synchronized (this) {
            mGPUImageRenderer.startPutData(false);
            mGPUImageRenderer.changeVideoEncoderStatus(false);
            if (mRecorder != null) {
                mRecorder.stop();
            }
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
    public void setFilter(final XMFilterType filtertype) {
        Log.i(TAG,"setFilter filter type " + filtertype);
        if (mGPUImageRenderer != null) {
            mGPUImageRenderer.setFilter(filtertype);
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

        void onRecorderError();

        /*Camera preview has started*/
        void onPreviewStarted();

        /*Camera preview has stopped*/
        void onPreviewStopped();

        void onPreviewError();
    }

    /*Demo app test interface*/
    public void testAPISetSurfaceView() {
        mGPUImageRenderer.setGLSurfaceView(this);
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

    private void setStatus(boolean running) {
        isRecording = running;
    }

    private boolean getStatus() {
        return isRecording;
    }

    private void releaseVideoPlayer() {
        if (mMediaPlayer != null)
            mMediaPlayer.release();
    }

    private void releaseCamera() {
        if (mCamera != null)
            mCamera.releaseInstance();
    }

    private void releaseRecorder() {
        if (mRecorder != null) {
            mRecorder.release();
            mRecorder.setListener(null);
            mRecorder = null;
            Log.i(TAG, "releaseRecorder");
        }
    }

    private void releaseOpengl() {
        if (mGPUImageRenderer != null) {
            mGPUImageRenderer.startPutData(false);
            mGPUImageRenderer.changeVideoEncoderStatus(false);
            mGPUImageRenderer.onStop();
        }
        mGPUImageRenderer = null;
    }

    private IXMCameraRecorderListener onXMCameraRecorderListener = new IXMCameraRecorderListener() {

        @Override
        public void onImageReaderPrepared() {
            Log.i(TAG, "onImageReaderPrepared");
            mImageReaderPrepared = true;
            if(mXMMediaRecorderPrepared)
                mRecorder.start();
        }

        /*
        *notification
        *The preparation for the recorder is ready,
        *and Request recorder to start coding.
        */
        @Override
        public void onRecorderPrepared() {
            Log.i(TAG, "onRecorderPrepared");
            mXMMediaRecorderPrepared = true;
            if(mImageReaderPrepared)
                mRecorder.start();
        }

        /*The recorder really start notification*/
        @Override
        public void onRecorderStarted() {
            Log.i(TAG, "onRecorderStarted");
            mListener.onRecorderStarted();
            synchronized (this) {
                mGPUImageRenderer.startPutData(true);
            }
        }

        /*The recorder really stop notification*/
        @Override
        public void onRecorderStopped() {
            Log.i(TAG, "onRecorderStopped");
            synchronized (this) {
                setStatus(false);
            }
            mListener.onRecorderStopped();
        }

        /*The recorder run error*/
        @Override
        public void onRecorderError() {
            Log.e(TAG, "onRecorderError");
            synchronized (this) {
                setStatus(false);
                mGPUImageRenderer.startPutData(false);
                mGPUImageRenderer.changeVideoEncoderStatus(false);
                if (mRecorder != null) {
                    mRecorder.stop();
                }
            }
            mListener.onRecorderError();
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

        @Override
        public void onPreviewError() {
            Log.e(TAG, "onPreviewError");
            mListener.onPreviewError();
        }
    };

    private class RecorderParams {
        public int bit_rate;
        public int gop_size;
        public int crf;
        public int multiple;
        public int max_b_frames;
        public String preset;

        public RecorderParams(int bitrate, int gopsize, int crf, int multiple, int max_b_frames, String preset) {
            bit_rate = bitrate;
            gop_size = gopsize;
            this.crf = crf;
            this.multiple = multiple;
            this.preset = preset;
            this.max_b_frames = max_b_frames;
        }
    }
}

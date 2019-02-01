package com.example.ffcmd.ffcmd_demo.view.utils;

import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.Build;
import android.util.Log;

import com.example.ffcmd.ffcmd_demo.view.CameraView;
import com.example.ffcmd.ffcmd_demo.view.recorder.IXMCameraRecorderListener;

import java.util.List;

/**
 * Created by sunyc on 18-10-26.
 */

public class CameraManager {
    private final static String TAG = "CameraManager";
    private static CameraManager mInstance = null;
    private int mExpectedWidth = 960;
    private int mExpectedHeight = 540;
    private int mFps = 15;
    private int mCurrentCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
    private Camera mCameraInstance;
    private CameraHelper mCameraHelper;
    private CameraView mCameraView;
    private int mWindowRotation = 0;
    private IXMCameraRecorderListener mListener = null;

    public static CameraManager getInstance() {
        if (mInstance == null) {
            mInstance = new CameraManager();
        }
        return mInstance;
    }

    private CameraManager() {
        mCameraHelper = new CameraHelper();
    }

    public void setCameraView(CameraView cameraView)
    {
        mCameraView = cameraView;
    }

    public void setWindowRotation(int rotation)
    {
        mWindowRotation = rotation;
        Log.i(TAG,"mWindowRotation " + rotation);
    }

    public void setExpectedFps(int fps) {
        mFps = fps;
        Log.i(TAG, "mFps " + fps);
    }

    public void setExpectedResolution(int w, int h) {
        mExpectedWidth = w;
        mExpectedHeight = h;
        Log.i(TAG,"mExpectedWidth " + w + ", mExpectedHeight " + h);
    }

    public void onResume() {
        setUpCamera(mCurrentCameraId);
    }

    public void onRelease() {
        releaseCamera();
    }

    public void startPreview() {
        if(mCameraInstance != null)
            mCameraInstance.startPreview();
    }

    public void stopPreview() {
        if(mCameraInstance != null)
            mCameraInstance.stopPreview();
    }

    public void switchCamera() {
        releaseCamera();
        mCurrentCameraId = (mCurrentCameraId + 1) % mCameraHelper.getNumberOfCameras();
        setUpCamera(mCurrentCameraId);
    }

    public void setListener(IXMCameraRecorderListener l) {
        mListener = l;
    }

    private void setUpCamera(final int id) {
        mCameraInstance = getCameraInstance(id);

        Camera.Parameters parameters = mCameraInstance.getParameters();
        List<String> supportedFocusModes = parameters.getSupportedFocusModes();
        if (supportedFocusModes != null && !supportedFocusModes.isEmpty()) {
            if (supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            } else if (supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                mCameraInstance.autoFocus(null);
            } else {
                parameters.setFocusMode(supportedFocusModes.get(0));
            }
        }

        Camera.Size preSize = getPreviewSize(mCameraInstance.new Size(mExpectedWidth, mExpectedHeight), parameters.getSupportedPreviewSizes());
        parameters.setPreviewSize(preSize.width, preSize.height);

        int[] range = getFpsRange(mFps, parameters.getSupportedPreviewFpsRange());
        parameters.setPreviewFpsRange(range[0], range[1]);

        int format = ImageFormat.NV21;
        parameters.setPreviewFormat(format);

        List<String> supportedFlashModes = parameters.getSupportedFlashModes();
        if (supportedFlashModes != null && !supportedFlashModes.isEmpty()) {
            if (supportedFlashModes.contains(Camera.Parameters.FLASH_MODE_OFF)) {
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            }
        }

        List<String> supportedWhiteBalance = parameters.getSupportedWhiteBalance();
        if (supportedWhiteBalance != null && !supportedWhiteBalance.isEmpty()) {
            if (supportedWhiteBalance.contains(Camera.Parameters.WHITE_BALANCE_AUTO)) {
                parameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
            }
        }

        List<String> supportedSceneModes = parameters.getSupportedSceneModes();
        if (supportedSceneModes != null && !supportedSceneModes.isEmpty()) {
            if (supportedSceneModes.contains(Camera.Parameters.SCENE_MODE_AUTO)) {
                parameters.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            parameters.setRecordingHint(true);
        }

        mCameraInstance.setParameters(parameters);

        int orientation = mCameraHelper.getCameraDisplayOrientation(mWindowRotation, mCurrentCameraId);
        Log.i(TAG,"mWindowRotation " + mWindowRotation + " mCurrentCameraId " + mCurrentCameraId + ", result of orientation is " + orientation);
        CameraHelper.CameraInfo2 cameraInfo = new CameraHelper.CameraInfo2();
        mCameraHelper.getCameraInfo(mCurrentCameraId, cameraInfo);
        boolean flipHorizontal = cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT;

        mCameraView.setUpCamera(mCameraInstance, orientation, flipHorizontal, false);
    }

    private Camera getCameraInstance(final int id) {
        Camera c = null;
        try {
            c = mCameraHelper.openCamera(id);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return c;
    }

    private Camera.Size getPreviewSize(Camera.Size resolution, List<Camera.Size> sizes) {
        float diff = 100f;
        float xdy = (float) resolution.width / (float) resolution.height;
        Camera.Size best = null;
        for (Camera.Size size : sizes) {
            if (size.equals(resolution)) {
                return size;
            }
            float tmp = Math.abs(((float) size.width / (float) size.height) - xdy);
            if (tmp < diff) {
                diff = tmp;
                best = size;
            }
        }
        return best;
    }

    private int[] getFpsRange(int expectedFps, List<int[]> fpsRanges) {
        expectedFps *= 1000;
        int[] closestRange = fpsRanges.get(0);
        int measure = Math.abs(closestRange[0] - expectedFps) + Math.abs(closestRange[1] - expectedFps);
        for (int[] range : fpsRanges) {
            if (range[0] == range[1]) {
                int curMeasure = Math.abs(range[0] - expectedFps) + Math.abs(range[1] - expectedFps);
                if (curMeasure < measure) {
                    closestRange = range;
                    measure = curMeasure;
                }
            }
        }
        return closestRange;
    }

    private void releaseCamera() {
        if(mCameraInstance != null) {
            mCameraInstance.setPreviewCallbackWithBuffer(null);
            mCameraInstance.setPreviewCallback(null);
            mCameraInstance.stopPreview();
            mCameraInstance.release();
            mCameraInstance = null;
        }
        mListener.onPreviewStopped();
    }
}

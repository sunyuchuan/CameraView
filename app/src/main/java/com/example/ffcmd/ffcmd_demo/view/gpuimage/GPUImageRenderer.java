/*
 * Copyright (C) 2012 CyberAgent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.ffcmd.ffcmd_demo.view.gpuimage;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import com.example.ffcmd.ffcmd_demo.view.gpuimage.filter.GPUImageFilter;
import com.example.ffcmd.ffcmd_demo.view.gpuimage.filter.GPUImageFilterFactory;
import com.example.ffcmd.ffcmd_demo.view.recorder.IXMCameraRecorderListener;
import com.example.ffcmd.ffcmd_demo.view.recorder.XMMediaRecorder;
import com.example.ffcmd.ffcmd_demo.view.utils.GPUImageParams;
import com.example.ffcmd.ffcmd_demo.view.utils.OpenGlUtils;
import com.example.ffcmd.ffcmd_demo.view.utils.Rotation;
import com.example.ffcmd.ffcmd_demo.view.utils.TextureRotationUtil;
import com.example.ffcmd.ffcmd_demo.view.utils.XMFilterType;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.LinkedList;
import java.util.Queue;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class GPUImageRenderer implements GLSurfaceView.Renderer, Camera.PreviewCallback, SurfaceTexture.OnFrameAvailableListener {
    private static final String TAG = "GPUImageRenderer";
    private Context mContext = null;
    private GLSurfaceView mGlSurfaceView = null;
    private GPUImageFilter mFilter = null;
    private SurfaceTexture mSurfaceTexture = null;
    private int mGLTextureId = OpenGlUtils.NO_TEXTURE;
    private final FloatBuffer mGLCubeBuffer;
    private final FloatBuffer mGLTextureBuffer;
    private int mOutputWidth;
    private int mOutputHeight;
    private int mImageWidth;
    private int mImageHeight;
    private final Queue<Runnable> mRunOnDraw;
    private Rotation mRotation;
    private boolean mFlipHorizontal;
    private boolean mFlipVertical;
    private ScaleType mScaleType = ScaleType.CENTER_CROP;
    private boolean updateTexImage = false;
    private XMMediaRecorder mRecorder = null;

    private int render_w;
    private int render_h;
    private byte[] mYuvPreviewFrame;
    private ByteBuffer mRenderBuffer = null;
    private int mCameraTextureId = OpenGlUtils.NO_TEXTURE;
    private boolean needAdjustImageScaling = false;
    private IXMCameraRecorderListener mListener = null;

    public static final float CUBE[] = {
            -1.0f, -1.0f,
            1.0f, -1.0f,
            -1.0f, 1.0f,
            1.0f, 1.0f,
    };

    private boolean supportsOpenGLES2(final Context context) {
        final ActivityManager activityManager = (ActivityManager)
                context.getSystemService(Context.ACTIVITY_SERVICE);
        final ConfigurationInfo configurationInfo =
                activityManager.getDeviceConfigurationInfo();
        return configurationInfo.reqGlEsVersion >= 0x20000;
    }

    public GPUImageRenderer(final Context context) {
        if (!supportsOpenGLES2(context)) {
            throw new IllegalStateException("OpenGL ES 2.0 is not supported on this phone.");
        }

        mContext = context;
        GPUImageParams.context = context;
        mFilter = GPUImageFilterFactory.CreateFilter(XMFilterType.NONE);
        mRunOnDraw = new LinkedList<Runnable>();

        mGLCubeBuffer = ByteBuffer.allocateDirect(CUBE.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mGLCubeBuffer.put(CUBE).position(0);

        mGLTextureBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.TEXTURE_NO_ROTATION.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mGLTextureBuffer.put(TextureRotationUtil.TEXTURE_NO_ROTATION).position(0);
        setRotation(Rotation.NORMAL, false, false);
    }

    public void setGLSurfaceView(final GLSurfaceView view) {
        mGlSurfaceView = view;
        mGlSurfaceView.setEGLContextClientVersion(2);
        mGlSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        mGlSurfaceView.getHolder().setFormat(PixelFormat.RGBA_8888);
        mGlSurfaceView.setRenderer(this);
        mGlSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        mGlSurfaceView.requestRender();
    }

    private void requestRender() {
        if (mGlSurfaceView != null) {
            mGlSurfaceView.requestRender();
        }
    }

    public void setListener(IXMCameraRecorderListener l) {
        mListener = l;
    }

    @Override
    public void onSurfaceCreated(final GL10 unused, final EGLConfig config) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        mFilter.init();
    }

    @Override
    public void onSurfaceChanged(final GL10 gl, final int width, final int height) {
        mOutputWidth = width;
        mOutputHeight = height;
        GLES20.glViewport(0, 0, width, height);
        GLES20.glUseProgram(mFilter.getProgram());
        mFilter.onOutputSizeChanged(mOutputWidth, mOutputHeight);
        adjustImageScaling();
    }

    @Override
    public void onDrawFrame(final GL10 gl) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        runAll(mRunOnDraw);
        mFilter.onDraw(mGLTextureId, mGLCubeBuffer, mGLTextureBuffer);
        synchronized(this) {
            if (mSurfaceTexture != null && updateTexImage) {
                mSurfaceTexture.updateTexImage();
                updateTexImage = false;
            }
        }
    }

    public void setUpCamera(final Camera camera) {
        setUpCamera(camera, 0, false, false);
    }

    public void setUpCamera(final Camera camera, final int degrees, final boolean flipHorizontal,
            final boolean flipVertical) {
        cleanAll(mRunOnDraw);
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                final Camera.Size previewSize = camera.getParameters().getPreviewSize();
                mImageWidth = previewSize.width;
                mImageHeight = previewSize.height;

                Rotation rotation = Rotation.NORMAL;
                switch (degrees) {
                    case 90:
                        rotation = Rotation.ROTATION_90;
                        break;
                    case 180:
                        rotation = Rotation.ROTATION_180;
                        break;
                    case 270:
                        rotation = Rotation.ROTATION_270;
                        break;
                }
                setRotationCamera(rotation, flipHorizontal, flipVertical);

                if(mCameraTextureId != OpenGlUtils.NO_TEXTURE)
                {
                    GLES20.glDeleteTextures(1, new int[]{ mCameraTextureId }, 0);
                    mCameraTextureId = OpenGlUtils.NO_TEXTURE;
                }
                if(mGLTextureId != OpenGlUtils.NO_TEXTURE)
                {
                    GLES20.glDeleteTextures(1, new int[]{ mGLTextureId }, 0);
                    mGLTextureId = OpenGlUtils.NO_TEXTURE;
                }

                int[] textures = new int[1];
                GLES20.glGenTextures(1, textures, 0);
                mCameraTextureId = textures[0];
                mYuvPreviewFrame = new byte[previewSize.width * previewSize.height * 3 / 2];
                mSurfaceTexture = new SurfaceTexture(mCameraTextureId);
                mSurfaceTexture.setOnFrameAvailableListener(GPUImageRenderer.this);
                try {
                    camera.setPreviewTexture(mSurfaceTexture);
                    camera.addCallbackBuffer(mYuvPreviewFrame);
                    camera.setPreviewCallbackWithBuffer(GPUImageRenderer.this);
                    camera.startPreview();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        mListener.onPreviewStarted();
        requestRender();
    }

    public void setFilter(final GPUImageFilter filter) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                final GPUImageFilter oldFilter = mFilter;
                mFilter = filter;
                if (oldFilter != null) {
                    oldFilter.destroy();
                }
                if(mRecorder != null) {
                    mFilter.setRecorderStatus(mRecorder, mRecorder.mRecording);
                }
                mFilter.init();
                GLES20.glUseProgram(mFilter.getProgram());
                mFilter.onOutputSizeChanged(mOutputWidth, mOutputHeight);
            }
        });
        requestRender();
    }

    public void onStop()
    {
        if(mCameraTextureId != OpenGlUtils.NO_TEXTURE)
        {
            GLES20.glDeleteTextures(1, new int[]{ mCameraTextureId }, 0);
            mCameraTextureId = OpenGlUtils.NO_TEXTURE;
        }
        if(mGLTextureId != OpenGlUtils.NO_TEXTURE)
        {
            GLES20.glDeleteTextures(1, new int[]{ mGLTextureId }, 0);
            mGLTextureId = OpenGlUtils.NO_TEXTURE;
        }
    }

    public void setRecorderStatus(XMMediaRecorder recorder, boolean status) {
        mRecorder = recorder;
        mFilter.setRecorderStatus(recorder, status);
    }

    @Override
    public void onPreviewFrame(final byte[] data, final Camera camera) {
        final Camera.Size previewSize = camera.getParameters().getPreviewSize();
        if (mRenderBuffer == null) {
            mRenderBuffer = ByteBuffer.allocate(previewSize.width * previewSize.height * 4);
        }

        if (mRunOnDraw.isEmpty()) {
            runOnDraw(new Runnable() {
                @Override
                public void run() {
                    render_w = previewSize.width;
                    render_h = previewSize.height;
                    XMMediaRecorder.NV21toABGR(data, render_w, render_h, mRenderBuffer.array());

                    mGLTextureId = OpenGlUtils.loadTexture(mRenderBuffer, render_w, render_h, mGLTextureId);
                    camera.addCallbackBuffer(mYuvPreviewFrame);

                    if (mImageWidth != previewSize.width || needAdjustImageScaling) {
                        mImageWidth = previewSize.width;
                        mImageHeight = previewSize.height;
                        needAdjustImageScaling = false;
                        adjustImageScaling();
                    }
                }
            });
        } else {
            camera.addCallbackBuffer(mYuvPreviewFrame);
        }
    }

    @Override
    synchronized public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        requestRender();
        updateTexImage = true;
    }

    private void adjustImageScaling() {
        float outputWidth = mOutputWidth;
        float outputHeight = mOutputHeight;
        if (mRotation == Rotation.ROTATION_270 || mRotation == Rotation.ROTATION_90) {
            outputWidth = mOutputHeight;
            outputHeight = mOutputWidth;
        }

        float ratio1 = outputWidth / mImageWidth;
        float ratio2 = outputHeight / mImageHeight;
        float ratioMax = Math.max(ratio1, ratio2);
        int imageWidthNew = Math.round(mImageWidth * ratioMax);
        int imageHeightNew = Math.round(mImageHeight * ratioMax);

        float ratioWidth = imageWidthNew / outputWidth;
        float ratioHeight = imageHeightNew / outputHeight;

        float[] cube = CUBE;
        float[] textureCords = TextureRotationUtil.getRotation(mRotation, mFlipHorizontal, mFlipVertical);
        if (mScaleType == ScaleType.CENTER_CROP) {
            float distHorizontal = (1 - 1 / ratioWidth) / 2;
            float distVertical = (1 - 1 / ratioHeight) / 2;
            textureCords = new float[]{
                    addDistance(textureCords[0], distHorizontal), addDistance(textureCords[1], distVertical),
                    addDistance(textureCords[2], distHorizontal), addDistance(textureCords[3], distVertical),
                    addDistance(textureCords[4], distHorizontal), addDistance(textureCords[5], distVertical),
                    addDistance(textureCords[6], distHorizontal), addDistance(textureCords[7], distVertical),
            };
        } else {
            cube = new float[]{
                    cube[0] / ratioHeight, cube[1] / ratioWidth,
                    cube[2] / ratioHeight, cube[3] / ratioWidth,
                    cube[4] / ratioHeight, cube[5] / ratioWidth,
                    cube[6] / ratioHeight, cube[7] / ratioWidth,
            };
        }

        mGLCubeBuffer.clear();
        mGLCubeBuffer.put(cube).position(0);
        mGLTextureBuffer.clear();
        mGLTextureBuffer.put(textureCords).position(0);
    }

    private float addDistance(float coordinate, float distance) {
        return coordinate == 0.0f ? distance : 1 - distance;
    }

    private void setRotationCamera(final Rotation rotation, final boolean flipHorizontal,
                                  final boolean flipVertical) {
        if(rotation == Rotation.ROTATION_90 || rotation == Rotation.ROTATION_270) {
            setRotation(rotation, flipVertical, flipHorizontal);
        } else {
            setRotation(rotation, flipHorizontal, flipVertical);
        }
    }

    public void setRotation(final Rotation rotation,
                            final boolean flipHorizontal, final boolean flipVertical) {
        mFlipHorizontal = flipHorizontal;
        mFlipVertical = flipVertical;
        mRotation = rotation;
        //adjustImageScaling();
        needAdjustImageScaling = true;
    }

    public int getOutputWidth() {
        return mOutputWidth;
    }

    public int getOutputHeight() {
        return mOutputHeight;
    }

    public int getInputWidth() {
        return mImageWidth;
    }

    public int getInputHeight() {
        return mImageHeight;
    }

    public Rotation getRotation() {
        return mRotation;
    }

    public boolean isFlippedHorizontally() {
        return mFlipHorizontal;
    }

    public boolean isFlippedVertically() {
        return mFlipVertical;
    }

    private void cleanAll(Queue<Runnable> queue) {
        synchronized (queue) {
            while (!queue.isEmpty()) {
                queue.poll();
            }
        }
    }

    private void runAll(Queue<Runnable> queue) {
        synchronized (queue) {
            while (!queue.isEmpty()) {
                queue.poll().run();
            }
        }
    }

    private void runOnDraw(final Runnable runnable) {
        synchronized (mRunOnDraw) {
            mRunOnDraw.add(runnable);
        }
    }

    private enum ScaleType { CENTER_INSIDE, CENTER_CROP }
}

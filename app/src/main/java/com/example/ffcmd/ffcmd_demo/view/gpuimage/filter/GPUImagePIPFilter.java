/**
 * Created by sunyc on 19-1-24.
 */

package com.example.ffcmd.ffcmd_demo.view.gpuimage.filter;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import com.example.ffcmd.ffcmd_demo.view.utils.OpenGlUtils;
import java.nio.FloatBuffer;

public class GPUImagePIPFilter extends GPUImageFilter {
    private static final String VERTEX_SHADER = "" +
            "precision highp float;\n" +
            "attribute vec4 position;\n" +
            "attribute vec4 inputTextureCoordinate;\n" +
            " \n" +
            "varying vec2 textureCoordinate;\n" +
            "varying highp vec4 g_position;\n" +
            " \n" +
            "void main()\n" +
            "{\n" +
            "    gl_Position = position;\n" +
            "    textureCoordinate = inputTextureCoordinate.xy;\n" +
            "    g_position = gl_Position;\n" +
            "}";

    public static final String FRAGMENT_SHADER = "" +
            "#extension GL_OES_EGL_image_external : require\n" +
            "precision highp float;\n" +
            "varying highp vec2 textureCoordinate;\n" +
            "varying highp vec4 g_position;\n" +
            "\n" +
            "uniform sampler2D inputImageTexture;\n" +
            "uniform samplerExternalOES inputVideoImageTexture;\n" +
            "uniform lowp vec2 leftTop;\n" +
            "uniform lowp vec2 rightBottom;\n" +
            "uniform highp float whiteFrameWidth;\n" +
            "uniform highp float whiteFrameHeight;\n" +
            "\n" +
            "void main()\n" +
            "{\n" +
            "lowp vec2 pipTextureCoordinateUse;\n" +
            "if (g_position.x >= leftTop.x - whiteFrameWidth && g_position.y >= leftTop.y - whiteFrameHeight\n" +
            "    && g_position.x <= rightBottom.x + whiteFrameWidth && g_position.y <= rightBottom.y + whiteFrameHeight) {\n" +
            "\n" +
            "    if (g_position.x >= leftTop.x && g_position.y >= leftTop.y\n" +
            "        && g_position.x <= rightBottom.x && g_position.y <= rightBottom.y) {\n" +
            "\n" +
            "        pipTextureCoordinateUse = vec2((g_position.x - leftTop.x) / (rightBottom.x - leftTop.x),\n" +
            "            1.0 - (g_position.y - leftTop.y) / (rightBottom.y - leftTop.y));\n" +
            "        gl_FragColor = texture2D(inputImageTexture, pipTextureCoordinateUse);\n" +
            "    } else {\n" +
            "        gl_FragColor = vec4(255, 255, 255, 1);\n" +
            "    }\n" +
            "} else {\n" +
            "    gl_FragColor = texture2D(inputVideoImageTexture, textureCoordinate);\n" +
            "}\n" +
            "\n" +
            "}\n";

    private static final float PIXEL_DIFF = 4.0f;
    protected int mGLUniformVideoTexture;
    protected int mLeftTop;
    protected int mRightBottom;
    protected int mWhiteFrameWidth;
    protected int mWhiteFrameHeight;

    public int mVideoTexture = OpenGlUtils.NO_TEXTURE;
    private FloatBuffer mLeftTopBuffer = null;
    private FloatBuffer mRightBottomBuffer = null;

    public GPUImagePIPFilter() {
        this(VERTEX_SHADER, FRAGMENT_SHADER);
    }

    public GPUImagePIPFilter(String vertexShader, String fragmentShader) {
        super(vertexShader, fragmentShader);
        float rectangle[] = {0.0f, 0.0f, 0.0f, 0.0f};
        setRectangleCoordinate(rectangle);
    }

    @Override
    public void onInit() {
        super.onInit();

        mGLUniformVideoTexture = GLES20.glGetUniformLocation(getProgram(), "inputVideoImageTexture");
        mLeftTop = GLES20.glGetUniformLocation(getProgram(), "leftTop");
        mRightBottom = GLES20.glGetUniformLocation(getProgram(), "rightBottom");
        mWhiteFrameWidth = GLES20.glGetUniformLocation(getProgram(), "whiteFrameWidth");
        mWhiteFrameHeight = GLES20.glGetUniformLocation(getProgram(), "whiteFrameHeight");
    }

    @Override
    public void onOutputSizeChanged(final int width, final int height) {
        super.onOutputSizeChanged(width, height);
        GLES20.glUseProgram(getProgram());
        GLES20.glUniform1f(mWhiteFrameWidth, PIXEL_DIFF / (float)mOutputWidth);
        GLES20.glUniform1f(mWhiteFrameHeight, PIXEL_DIFF / (float)mOutputHeight);
    }

    @Override
    public void setVideoTextureId(final int textureId) {
        super.setVideoTextureId(textureId);
        mVideoTexture = textureId;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mVideoTexture = OpenGlUtils.NO_TEXTURE;
    }

    @Override
    protected void onDrawArraysPre() {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mVideoTexture);
        GLES20.glUniform1i(mGLUniformVideoTexture, 3);
        GLES20.glUniform2fv(mLeftTop, 1, mLeftTopBuffer);
        GLES20.glUniform2fv(mRightBottom, 1, mRightBottomBuffer);
    }

    private void rectCoordinateTransform(FloatBuffer leftTopBuffer, FloatBuffer rightBottomBuffer) {
        float[] left = leftTopBuffer.array();
        float[] right = rightBottomBuffer.array();
        left[0] = -(left[0]*2 - 1.0f);
        left[1] = -(left[1]*2 - 1.0f);
        right[0] = -(right[0]*2 - 1.0f);
        right[1] = -(right[1]*2 - 1.0f);

        float tmp = left[0];
        left[0] = right[0];
        right[0] = tmp;

        tmp = left[1];
        left[1] = right[1];
        right[1] = tmp;

        leftTopBuffer.position(0);
        rightBottomBuffer.position(0);
    }

    @Override
    public void setRectangleCoordinate(float buffer[]) {
        if(mLeftTopBuffer == null)
            mLeftTopBuffer = FloatBuffer.allocate(2);
        if(mRightBottomBuffer == null)
            mRightBottomBuffer = FloatBuffer.allocate(2);

        float[] left = {buffer[0], buffer[1]};
        mLeftTopBuffer.put(left);
        mLeftTopBuffer.flip();

        float[] right = {buffer[2], buffer[3]};
        mRightBottomBuffer.put(right);
        mRightBottomBuffer.flip();

        rectCoordinateTransform(mLeftTopBuffer, mRightBottomBuffer);
    }
}

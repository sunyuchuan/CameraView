package com.example.ffcmd.ffcmd_demo;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.example.ffcmd.ffcmd_demo.co_production.VideoSynthesis;

import java.util.ArrayList;

/**
 * Created by sunyc on 18-11-24.
 */

public class VideoPostProcessingActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "VideoPostProcessingActivity";
    private boolean has_btn_pip = false;
    private boolean has_btn_concat = false;
    private Button mPipButton = null;
    private Button mConcatButton = null;
    private VideoSynthesis mVideoSynthesis = null;
    private VideoSynthesis.MetaData mRawVideoMetaData = null;
    private VideoSynthesis.MetaData mCameraVideoMetaData = null;
    private VideoSynthesis.MetaData mWatermarkMetaData = null;
    private ArrayList<VideoSynthesis.MetaData> mMetaDataList = new ArrayList<VideoSynthesis.MetaData>();
    private ArrayList<String> mVideoConcatPathList = new ArrayList<String>();
    private String mOutputPath = "/sdcard/mix_output.mp4";

    public static Intent newIntent(Context context) {
        Intent intent = new Intent(context, VideoPostProcessingActivity.class);
        return intent;
    }

    public static void intentTo(Context context) {
        context.startActivity(newIntent(context));
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_post_processing);
        mPipButton = (Button)findViewById(R.id.button_ffmpeg_pip);
        mPipButton.setOnClickListener(this);
        mConcatButton = (Button)findViewById(R.id.button_ffmpeg_concat);
        mConcatButton.setOnClickListener(this);
        findViewById(R.id.button_camera_prew).setOnClickListener(this);
    }

    private void resume() {
        if(mVideoSynthesis == null) {
            mVideoSynthesis = VideoSynthesis.getInstance();

            mRawVideoMetaData = new VideoSynthesis.MetaData();
            mRawVideoMetaData.mType = VideoSynthesis.RAW_VIDEO_TYPE;
            mRawVideoMetaData.mPath = "/sdcard/y_bg.mp4";
            mMetaDataList.add(mRawVideoMetaData);

            mCameraVideoMetaData = new VideoSynthesis.MetaData();
            mCameraVideoMetaData.mType = VideoSynthesis.CAMERA_VIDEO_TYPE;
            mCameraVideoMetaData.mPath = "/sdcard/y_test.mp4";
            mCameraVideoMetaData.mRect = new VideoSynthesis.Rect(0.1f, 0.1f, 0.4f, 0.4f);
            mMetaDataList.add(mCameraVideoMetaData);

            mWatermarkMetaData = new VideoSynthesis.MetaData();
            mWatermarkMetaData.mType = VideoSynthesis.WATERMARK_TYPE;
            mWatermarkMetaData.mPath = "/sdcard/watermark_white.jpg";
            mMetaDataList.add(mWatermarkMetaData);

            mVideoConcatPathList.add("/sdcard/bg.mp4");
            mVideoConcatPathList.add("/sdcard/bg.mp4");
            mVideoConcatPathList.add("/sdcard/bg.mp4");
            mVideoConcatPathList.add("/sdcard/bg.mp4");
        }
    }

    @Override
    public void onClick(final View v) {
        switch (v.getId()) {
            case R.id.button_ffmpeg_pip:
                if (!has_btn_pip) {
                    Log.i(TAG, "button pip down");
                    has_btn_pip = true;
                    mVideoSynthesis.pipMergeVideo(mMetaDataList, mOutputPath, onVideoSynthesisListener);
                    mPipButton.setBackgroundResource(R.color.green);
                } else {
                    Log.i(TAG, "button pip up");
                    has_btn_pip = false;
                    mPipButton.setBackgroundResource(R.color.filter_color_red);
                    mVideoSynthesis.stop();
                }
                break;

            case R.id.button_ffmpeg_concat:
                if (!has_btn_concat) {
                    Log.i(TAG, "button concat down");
                    has_btn_concat = true;
                    mVideoSynthesis.videoConcat(mVideoConcatPathList, mOutputPath, onVideoSynthesisListener);
                    mConcatButton.setBackgroundResource(R.color.green);
                } else {
                    Log.i(TAG, "button concat up");
                    has_btn_concat = false;
                    mPipButton.setBackgroundResource(R.color.filter_color_red);
                    mVideoSynthesis.stop();
                }
                break;

            case R.id.button_camera_prew:
                Log.i(TAG, "button camera preview view start");
                CameraPreviewActivity.intentTo(getApplicationContext());
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        resume();
    }

    @Override
    protected void onStop() {
        if(mVideoSynthesis != null)
            mVideoSynthesis.release();
        mVideoSynthesis = null;
        super.onStop();
    }

    private VideoSynthesis.IVideoSynthesisListener onVideoSynthesisListener = new VideoSynthesis.IVideoSynthesisListener() {
        @Override
        public void onStarted() {
            Log.i(TAG, "VideoSynthesis started");
        }

        @Override
        public void onStopped() {
            Log.i(TAG, "VideoSynthesis stopped");
            has_btn_concat = false;
            has_btn_pip = false;
            mConcatButton.setBackgroundResource(R.color.gray);
            mPipButton.setBackgroundResource(R.color.gray);
        }

        @Override
        public void onProgress(int progress) {
            Log.i(TAG, "VideoSynthesis progress : " + progress);
        }

        @Override
        public void onCompleted() {
            Log.i(TAG, "VideoSynthesis completed");
            has_btn_concat = false;
            has_btn_pip = false;
            mConcatButton.setBackgroundResource(R.color.gray);
            mPipButton.setBackgroundResource(R.color.gray);
        }

        @Override
        public void onError() {
            Log.e(TAG, "VideoSynthesis error");
            has_btn_concat = false;
            has_btn_pip = false;
            mConcatButton.setBackgroundResource(R.color.gray);
            mPipButton.setBackgroundResource(R.color.gray);
            mVideoSynthesis.release();
            mVideoSynthesis = null;
        }
    };
}

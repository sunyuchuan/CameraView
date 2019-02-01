package com.example.ffcmd.ffcmd_demo;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.example.ffcmd.ffcmd_demo.camera_preview.FilterAdapter;
import com.example.ffcmd.ffcmd_demo.view.CameraView;
import com.example.ffcmd.ffcmd_demo.view.gpuimage.filter.GPUImageFilterFactory;
import com.example.ffcmd.ffcmd_demo.view.utils.XMFilterType;

public class CameraPreviewActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "CameraPreviewActivity";
    private static final int VFPS = 15;
    private static final int PREVIEW_W = 960;
    private static final int PREVIEW_H = 540;

    private CameraView mCameraView;
    private LinearLayout mPasterLayout;
    private RecyclerView mPasterListView;
    private FilterAdapter mFilterAdapter;
    private boolean has_btn_paster = false;
    private boolean has_btn_record = false;
    private boolean has_btn_preview = false;
    private ImageButton mRecordButton = null;
    private static final String mOutputPath = "/sdcard/camera_test.mp4";

    private final XMFilterType[] pasterTypes = new XMFilterType[] {
            XMFilterType.NONE,
            XMFilterType.FILTER_BEAUTY
    };

    public static Intent newIntent(Context context) {
        Intent intent = new Intent(context, CameraPreviewActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    public static void intentTo(Context context) {
        context.startActivity(newIntent(context));
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_preview);
        findViewById(R.id.button_choose_filter).setOnClickListener(this);
        findViewById(R.id.button_preview).setOnClickListener(this);
        mRecordButton = (ImageButton)findViewById(R.id.button_record);
        mRecordButton.setOnClickListener(this);

        //
        View cameraSwitchView = findViewById(R.id.img_switch_camera);
        cameraSwitchView.setOnClickListener(this);

        //paster
        mPasterLayout = (LinearLayout)findViewById(R.id.filter_list);
        mPasterListView = (RecyclerView) findViewById(R.id.base_filter_listView);
        LinearLayoutManager pasterLinearLayoutManager = new LinearLayoutManager(this);
        pasterLinearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        mPasterListView.setLayoutManager(pasterLinearLayoutManager);
        mFilterAdapter = new FilterAdapter(this, pasterTypes);
        mPasterListView.setAdapter(mFilterAdapter);
        mFilterAdapter.setOnFilterChangeListener(onFilterChangeListener);
    }

    private void resume() {
        if(mCameraView == null) {
            mCameraView = (CameraView) findViewById(R.id.CameraView);
            mCameraView.testAPISetSurfaceView();
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            mCameraView.setWindowRotation(rotation);
            mCameraView.setExpectedFps(VFPS);
            mCameraView.setExpectedResolution(PREVIEW_W, PREVIEW_H);
            mCameraView.setListener(onCameraRecorderListener);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        mCameraView.release();
        mCameraView = null;
        super.onStop();
    }

    @Override
    public void onClick(final View v) {
        switch (v.getId()) {
            case R.id.button_choose_filter:
                if(!has_btn_paster) {
                    has_btn_paster = true;
                    showFilters(mPasterLayout);
                } else {
                    has_btn_paster = false;
                    hideFilters(mPasterLayout);
                }
                break;

            case R.id.button_record:
                if (!has_btn_record) {
                    Log.i(TAG, "button record down");
                    has_btn_record = true;
                    mCameraView.startRecorder(mOutputPath);
                } else {
                    Log.i(TAG, "button record up");
                    has_btn_record = false;
                    mCameraView.stopRecorder();
                }
                break;

            case R.id.img_switch_camera:
                mCameraView.switchCamera();
                break;

            case R.id.button_preview:
                if (!has_btn_preview) {
                    Log.i(TAG, "button start preview");
                    has_btn_preview = true;
                    mCameraView.startCameraPreview();
                } else {
                    Log.i(TAG, "button stop preview");
                    has_btn_preview = false;
                    mCameraView.stopCameraPreview();
                }
                break;
        }
    }

    private FilterAdapter.onFilterChangeListener onFilterChangeListener = new FilterAdapter.onFilterChangeListener(){
        @Override
        public void onFilterChanged(XMFilterType filterType, boolean show, boolean switch_filter) {
            Log.i(TAG, "onFilterChanged setFilter filterType "+filterType.getValue());
            if(show)
                mCameraView.setFilter(GPUImageFilterFactory.CreateFilter(filterType));
            else if(!switch_filter)
                mCameraView.setFilter(GPUImageFilterFactory.CreateFilter(XMFilterType.NONE));
        }
    };

    private void showFilters(final LinearLayout layout) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(layout, "translationY", layout.getHeight(), 0);
        animator.setDuration(200);
        animator.addListener(new Animator.AnimatorListener() {

            @Override
            public void onAnimationStart(Animator animation) {
                layout.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {

            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }
        });
        animator.start();
    }

    private void hideFilters(final LinearLayout layout) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(layout, "translationY", 0 ,  layout.getHeight());
        animator.setDuration(200);
        animator.addListener(new Animator.AnimatorListener() {

            @Override
            public void onAnimationStart(Animator animation) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                // TODO Auto-generated method stub
                layout.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                // TODO Auto-generated method stub
                layout.setVisibility(View.INVISIBLE);
            }
        });
        animator.start();
    }

    private CameraView.ICameraViewListener onCameraRecorderListener = new CameraView.ICameraViewListener() {
        @Override
        public void onRecorderStarted() {
            Log.i(TAG, "onRecorderStarted");
            mRecordButton.setImageDrawable(getResources().getDrawable(R.drawable.icon_video));
        }

        @Override
        public void onRecorderStopped() {
            Log.i(TAG, "onRecorderStopped");
            mRecordButton.setImageDrawable(getResources().getDrawable(R.drawable.record_video));
        }

        @Override
        public void onPreviewStarted() {
            Log.i(TAG, "onPreviewStarted");
        }

        @Override
        public void onPreviewStopped() {
            Log.i(TAG, "onPreviewStopped");
        }
    };
}

package com.example.ffcmd.ffcmd_demo.co_production;

import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.example.ffcmd.ffcmd_demo.FFmpegMediaMetadataRetriever;
import com.example.ffcmd.ffcmd_demo.IjkLibLoader;
import com.example.ffcmd.ffcmd_demo.co_production.ffmpegcmd.FFmpegCommand;
import com.example.ffcmd.ffcmd_demo.co_production.ffmpegcmd.IFFMpegCommandListener;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

/**
 * Created by sunyc on 18-11-27.
 */

public class VideoSynthesis {
    private static final int PIXEL_DIFF = 3;
    private static final String TAG = "VideoSynthesis";
    public static final String RAW_VIDEO_TYPE = "rawVideo";
    public static final String CAMERA_VIDEO_TYPE = "cameraVideo";
    public static final String WATERMARK_TYPE = "watermark";
    private static VideoSynthesis mInstance = null;
    private FFmpegCommand ffcmd = null;
    private IVideoSynthesisListener mListener = null;
    private MetaData mRawVideoMetaData = null;
    private MetaData mCameraVideoMetaData = null;
    private MetaData mWatermarkMetaData = null;
    private FFCmdType mFFCmdType = FFCmdType.NONE;
    private PipParams mPipParams = null;
    private String mOutputPath = null;
    private String mConcatListFilePath = null;
    private static boolean mIsLibLoaded = false;
    private int mBitrateDefault = 700;
    private int mWidthxHeightDefault = 540*960;
    private int mBitrate = 700;
    private boolean needRelease = false;

    private static final IjkLibLoader sLocalLibLoader = new IjkLibLoader() {
        @Override
        public void loadLibrary(String libName) throws UnsatisfiedLinkError, SecurityException {
            String ABI = Build.CPU_ABI;
            Log.d(TAG, "ABI " + ABI);
            System.loadLibrary(libName + "-" + ABI);
        }
    };

    private static void loadLibrariesOnce(IjkLibLoader libLoader) {
        synchronized (VideoSynthesis.class) {
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

    public static VideoSynthesis newInstance() {
        mInstance = new VideoSynthesis();
        return mInstance;
    }

    private VideoSynthesis() {
        loadLibrariesOnce(sLocalLibLoader);

        ffcmd = new FFmpegCommand();
        ffcmd.setListener(onFFMpegCmdListener);
    }

    /*
    * Video stitching synthesis of the same encoding format
    * params:
    * inputVideoList, List of videos that need to be stitched
    * outputpath, The video path of the generated video, including the file name
    * IVideoSynthesisListener, Callback.
    */
    public void videoConcat(List<String> inputVideoList, String outputpath, IVideoSynthesisListener l) {
        if(!ConcatInputParamsisValid(inputVideoList, outputpath))
        {
            Log.e(TAG, "video concat Input Params is inValid, exit");
            return;
        }
        mListener = l;
        mOutputPath = outputpath;
        mConcatListFilePath = createConcatListFile(inputVideoList, outputpath);
        mFFCmdType = FFCmdType.VIDEO_CONCAT;
        start();
    }

    /*
    * picture-in-picture video synthesis
    * params:
    * list, input videos and watermarks etc
    * output, The video path of the generated video, including the file name
    * IVideoSynthesisListener, Callback.
    */
    public void pipMergeVideo(List<MetaData> list, String output, IVideoSynthesisListener l) {
        mListener = l;
        mOutputPath = output;
        for (MetaData data : list) {
            if (data.mType.equals(RAW_VIDEO_TYPE)) {
                mRawVideoMetaData = data;
            } else if (data.mType.equals(CAMERA_VIDEO_TYPE)) {
                mCameraVideoMetaData = data;
            } else if (data.mType.equals(WATERMARK_TYPE)) {
                mWatermarkMetaData = data;
            }
        }

        if(!PipInputParamsisValid())
        {
            Log.e(TAG, "Pip Input Params is inValid, exit");
            return;
        }
        if(!CalculatePipParameters())
        {
            Log.e(TAG, "Pip Input Params is inValid, exit");
            return;
        }

        mFFCmdType = FFCmdType.PIP_MERGE;
        mBitrate = (int) (((float)(mPipParams.raw_w*mPipParams.raw_h)/(float)mWidthxHeightDefault)*mBitrateDefault);
        start();
    }

    private boolean ConcatInputParamsisValid(List<String> inputVideoList, String outputpath) {
        if(inputVideoList == null || TextUtils.isEmpty(outputpath))
            return false;
        return true;
    }

    private String createConcatListFile(List<String> inputVideoList, String outputpath) {
        if(!ConcatInputParamsisValid(inputVideoList, outputpath))
        {
            Log.e(TAG, "video concat Input Params is inValid, exit");
            return null;
        }

        String listFilePath = outputpath.substring(0, outputpath.lastIndexOf("/") + 1);
        String listFileName = outputpath.substring(outputpath.lastIndexOf("/") + 1, outputpath.lastIndexOf("."));
        String concatListFilePath = listFilePath + listFileName + ".txt";
        try {
            File file = new File(concatListFilePath);
            if(!file.getParentFile().exists()) {
                if (!file.getParentFile().mkdirs()) {
                    Log.e(TAG, "mkdir " + listFilePath + " create failed");
                    return null;
                }
            }
            if (!file.exists()) {
                if(!file.createNewFile()) {
                    Log.e(TAG, concatListFilePath+ " create failed");
                    return null;
                }
            } else {
                file.delete();
                if(!file.createNewFile()) {
                    Log.e(TAG, concatListFilePath+ " create failed");
                    return null;
                }
            }
            file.setReadable(true, false);
            FileOutputStream outStream = new FileOutputStream(file);
            outStream.write("ffconcat version 1.0\n".getBytes());
            for(String str : inputVideoList)
            {
                Log.i(TAG, "input video path " + str);
                outStream.write(("file \'" + str + "\'\n").getBytes());
            }
            outStream.close();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return concatListFilePath;
    }

    private boolean PipInputParamsisValid() {
        if(mRawVideoMetaData == null
                || mCameraVideoMetaData == null
                || mCameraVideoMetaData.mRect == null
                || mWatermarkMetaData == null
                || TextUtils.isEmpty(mRawVideoMetaData.mPath)
                || TextUtils.isEmpty(mCameraVideoMetaData.mPath)
                || TextUtils.isEmpty(mWatermarkMetaData.mPath)
                || TextUtils.isEmpty(mOutputPath))
            return false;
        return true;
    }

    private Boolean CalculatePipParameters() {
        if(!PipInputParamsisValid())
        {
            Log.e(TAG, "Pip Input Params is inValid, exit");
            return false;
        }
        mPipParams = new PipParams();
        //FFmpegMediaMetadataRetriever mediaMetadataRetriever = new FFmpegMediaMetadataRetriever(sLocalLibLoader);

        //mediaMetadataRetriever.setDataSource(mRawVideoMetaData.mPath);
        //FFmpegMediaMetadataRetriever.Metadata data = mediaMetadataRetriever.getMetadata();

        mPipParams.raw_w = 1280;//data.getInt(FFmpegMediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
        mPipParams.raw_h = 720;//data.getInt(FFmpegMediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
        mPipParams.raw_duration = 111000;//data.getLong(FFmpegMediaMetadataRetriever.METADATA_KEY_DURATION);

        //mediaMetadataRetriever.setDataSource(mCameraVideoMetaData.mPath);
        //data = mediaMetadataRetriever.getMetadata();
        mPipParams.camera_w = 640;//data.getInt(FFmpegMediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
        mPipParams.camera_h = 640;//data.getInt(FFmpegMediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
        mPipParams.camera_duration = 9190;//data.getLong(FFmpegMediaMetadataRetriever.METADATA_KEY_DURATION);

        int camera_rect_w = (int) (mPipParams.raw_w * (mCameraVideoMetaData.mRect.right_x - mCameraVideoMetaData.mRect.left_x));
        int camera_rect_h = (int) (mPipParams.raw_h * (mCameraVideoMetaData.mRect.right_y - mCameraVideoMetaData.mRect.left_y));
        mPipParams.camera_overlay_x = (int) (mPipParams.raw_w * mCameraVideoMetaData.mRect.left_x);
        mPipParams.camera_overlay_y = (int) (mPipParams.raw_h - mPipParams.raw_h * mCameraVideoMetaData.mRect.right_y);
        float camera_rect_aspect_ratio = (float)camera_rect_w/(float)camera_rect_h;
        float camera_video_aspect_ratio = (float)mPipParams.camera_w/(float)mPipParams.camera_h;

        if(camera_video_aspect_ratio > camera_rect_aspect_ratio)
        {
            //int camera_overlay_y_diff = camera_rect_h - (int) ((float)camera_rect_w / camera_video_aspect_ratio);
            //mPipParams.camera_overlay_y += camera_overlay_y_diff/2;
            camera_rect_h = (int) ((float)camera_rect_w / camera_video_aspect_ratio);
        } else {
            //int camera_overlay_x_diff = camera_rect_w - (int) ((float)camera_rect_h * camera_video_aspect_ratio);
            //mPipParams.camera_overlay_x += camera_overlay_x_diff/2;
            camera_rect_w = (int) ((float)camera_rect_h * camera_video_aspect_ratio);
        }
        mPipParams.camera_w = camera_rect_w;
        mPipParams.camera_h = camera_rect_h;

        mPipParams.watermark_w = mPipParams.camera_w + PIXEL_DIFF*2;
        mPipParams.watermark_h = mPipParams.camera_h + PIXEL_DIFF*2;
        mPipParams.watermark_overlay_x = mPipParams.camera_overlay_x - PIXEL_DIFF;
        mPipParams.watermark_overlay_y = mPipParams.camera_overlay_y - PIXEL_DIFF;

        return true;
    }

    private void start() {
        if(ffcmd != null) {
            ffcmd.prepareAsync();
            ffcmd.setStatus(true);
        }
    }

    /*
    *Stop video synthesis in running,
    *including stitching and picture-in-picture
    */
    public void stop() {
        if(ffcmd != null) {
            ffcmd.stop();
            needRelease = false;
        }
    }

    /*Release the instance after stopping the video synthesis*/
    public void release() {
        if(ffcmd != null) {
            if(ffcmd.getStatus()) {
                ffcmd.stop();
                needRelease = true;
            } else {
                ffcmd.release();
                ffcmd.setListener(null);
                ffcmd = null;
            }
        }
        mInstance = null;
    }

    private IFFMpegCommandListener onFFMpegCmdListener = new IFFMpegCommandListener() {
        @Override
        public void onInfo(int arg1, int arg2, Object obj) {
            switch (arg1) {
                /*The native layer ffmepg is ready to start synthesis*/
                case FFmpegCommand.FFCMD_INFO_PREPARED:
                    Log.d(TAG, "XMFFmpegCommand prepared");
                    if(ffcmd != null) {
                        ffcmd.setStatus(true);
                    }
                    FFCmdStart(mFFCmdType);
                    break;

                /*Native layer ffmepg has started*/
                case FFmpegCommand.FFCMD_INFO_STARTED:
                    Log.d(TAG, "XMFFmpegCommand start");
                    if(mListener != null)
                        mListener.onStarted();
                    break;

                /*video synthetic percentage*/
                case FFmpegCommand.FFCMD_INFO_PROGRESS:
                    Log.d(TAG, "XMFFmpegCommand progress " + arg2);
                    if(mFFCmdType == FFCmdType.PIP_MERGE
                            && mPipParams != null
                            && mListener != null
                            && mPipParams.raw_duration != 0) {
                        int progress = (int) (100 * ((float) arg2 / (float) mPipParams.raw_duration));
                        mListener.onProgress(progress);
                    }
                    break;

                /*Native layer ffmepg video synthesis has stopped*/
                case FFmpegCommand.FFCMD_INFO_STOPPED:
                    if(ffcmd != null) {
                        ffcmd.setStatus(false);
                    }

                    if(mListener != null) {
                        mListener.onStopped();
                    }

                    if(needRelease)
                    {
                        needRelease = false;
                        ffcmd.release();
                        ffcmd.setListener(null);
                        ffcmd = null;
                    }
                    Log.d(TAG, "XMFFmpegCommand stop");
                    break;

                /*Native layer ffmepg video synthesis has completed*/
                case FFmpegCommand.FFCMD_INFO_COMPLETED:
                    if(ffcmd != null) {
                        ffcmd.setStatus(false);
                    }

                    if(mListener != null) {
                        mListener.onCompleted();
                    }
                    if(!TextUtils.isEmpty(mConcatListFilePath)) {
                        File file = new File(mConcatListFilePath);
                        if (file.exists())
                            file.delete();
                    }
                    Log.d(TAG, "XMFFmpegCommand completed");
                    break;
                default:
                    Log.i(TAG, "Unknown message type " + arg1);
                    break;
            }
        }

        @Override
        public void onError(int arg1, int arg2, Object obj) {
            if(ffcmd != null) {
                ffcmd.setStatus(false);
            }

            if(mListener != null)
                mListener.onError();
            Log.e(TAG, "XMFFmpegCommand error arg1 " + arg1 + " arg2 " + arg2 + ", please release VideoSynthesis.");
        }
    };

    private void FFCmdStart(FFCmdType type) {
        switch(type) {
            case PIP_MERGE:
                FFMpegPipMerge();
                break;
            case VIDEO_CONCAT:
                FFMpegConcat();
                break;
            default:
                Log.i(TAG, "Unknown FFCmdType " + type);
                break;
        }
    }

    private void FFMpegPipMerge() {
        if(mPipParams == null)
        {
            Log.e(TAG, "mPipParams is null,FFMpegPipMerge stop");
            if(mListener != null) {
                mListener.onStopped();
            }
            return;
        }

        String[] cmd = {
                "ffmpeg",
                "-i", mRawVideoMetaData.mPath,
                "-i", mCameraVideoMetaData.mPath,
                "-i", mWatermarkMetaData.mPath,
                "-filter_complex",
                "[0:v] fps=15,scale="+mPipParams.raw_w+":"+mPipParams.raw_h+" [base];" +
                "[1:v] fps=15,scale="+mPipParams.camera_w+":"+mPipParams.camera_h+" [camera];" +
                "[2:v] scale="+mPipParams.watermark_w+":"+mPipParams.watermark_h+" [watermark];" +
                "[base][watermark] overlay=x='if(gte("+Math.floor((double)mPipParams.camera_duration/(double)1000)+",t),"+mPipParams.watermark_overlay_x+","+Integer.MAX_VALUE+")'" +
                                         ":y='if(gte("+Math.floor((double)mPipParams.camera_duration/(double)1000)+",t),"+mPipParams.watermark_overlay_y+","+Integer.MAX_VALUE+")' [tmp];" +
                "[tmp][camera] overlay=repeatlast=0:x="+mPipParams.camera_overlay_x+":y="+mPipParams.camera_overlay_y+" [vout]",
                "-map", "[vout]",
                "-map", "0:a",
                "-c:v", "libx264",
                "-tune", "zerolatency",
                "-r", "15",
                "-force_key_frames", "expr:gte(t,n_forced*5)",
                "-pix_fmt", "yuv420p",
                "-vb", mBitrate+"k",
                "-c:a", "copy",
                "-shortest",
                "-movflags", "faststart",
                "-preset", "veryfast",
                "-crf", "23.0",
                "-f", "mp4",
                "-y", mOutputPath
        };

        if(ffcmd != null)
            ffcmd.start(cmd.length, cmd);
    }

    private void FFMpegConcat() {
        if(TextUtils.isEmpty(mConcatListFilePath)
                || TextUtils.isEmpty(mOutputPath))
        {
            Log.e(TAG, "concat : input is invalid,FFMpegConcat stop");
            if(mListener != null) {
                mListener.onStopped();
            }
            return;
        }

        String[] cmd = {
                "ffmpeg",
                "-f", "concat",
                "-safe", "0",
                "-i", mConcatListFilePath,
                "-an",
                "-c:v", "copy",
                "-movflags", "faststart",
                "-f", "mp4",
                "-y", mOutputPath
        };

        if(ffcmd != null)
            ffcmd.start(cmd.length, cmd);
    }

    public interface IVideoSynthesisListener {
        /*Native layer ffmepg has started*/
        void onStarted();

        /*Native layer ffmepg has started*/
        void onStopped();

        /*Progress percentage*/
        void onProgress(int progress);

        /*Successful video synthesis*/
        void onCompleted();

        /*Native layer ffmepg error*/
        void onError();
    }

    public class MetaData {
        /*video type,example RAW_VIDEO_TYPE/CAMERA_VIDEO_TYPE etc.*/
        public String mType;
        /*Video file path*/
        public String mPath;
        /*The position of the picture-in-picture video in the background video*/
        public Rect   mRect;

        public MetaData() {
        }

        public MetaData(String type, String path, Rect rect) {
            mType = type;
            mPath = path;
            mRect = rect;
        }
    }

    /*Rectangular position coordinates*/
    public class Rect {
        public float left_x;
        public float left_y;
        public float right_x;
        public float right_y;

        public Rect() {
        }

        public Rect(float left_x, float left_y, float right_x, float right_y) {
            this.left_x = left_x;
            this.left_y = left_y;
            this.right_x = right_x;
            this.right_y = right_y;
        }
    }

    private enum FFCmdType {
        NONE(-1),
        PIP_MERGE(0),
        VIDEO_CONCAT(1);

        private final int value;

        FFCmdType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    private class PipParams {
        public int raw_w;
        public int raw_h;
        public long raw_duration;
        public int camera_w;
        public int camera_h;
        public long camera_duration;
        public int camera_overlay_x;
        public int camera_overlay_y;
        public int watermark_w;
        public int watermark_h;
        public int watermark_overlay_x;
        public int watermark_overlay_y;
    }
}

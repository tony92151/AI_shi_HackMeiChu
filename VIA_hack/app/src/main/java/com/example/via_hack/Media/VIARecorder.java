package com.example.via_hack.Media;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.view.Surface;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by HankWu on 2017/10/13.
 */

public class VIARecorder {

    public enum Mode {
        Surface,
        YUV420SemiPlanar,
    }

    private int COLOR_FORMAT = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar;

    MediaMuxer mMediaMuxer = null;
    AvcEncoder mAvcEncoder = null;
    int mVideoTrack = -1;
    int mFrameCount = 0;
    int mFPS = 0;
    int mFrameDiffTimes = 0;
    long mTime = 0;
    int mWidth = 0;
    int mHeight = 0;

    MediaFormat mMediaFormat;
    boolean bFormatReady = false;
    long mPeriodicTimeInSec;
    String mPath = "";
    Object mLock = new Object();
    long mFileStartTime = -1;
    String outputPath;
    FileListener mFileListener = null;
    Mode mMode = Mode.YUV420SemiPlanar;

    public interface FileListener {
        void OnFileComplete(String filePath);
        void OnFileCreate(String filePath);
    }

    public void setFileListener(FileListener f) {
        mFileListener = f;
    }

    public void stop() {
        synchronized (mLock) {
            if (mMediaMuxer != null) {
                mMediaMuxer.stop();
                mMediaMuxer.release();
                mMediaMuxer = null;
            }

            if(mAvcEncoder != null) {
                mAvcEncoder.close();
                mAvcEncoder = null;
            }
        }
    }

    private void createRecordFile() {
        synchronized (mLock) {
            if (mMediaMuxer != null) {
                mMediaMuxer.stop();
                mMediaMuxer.release();
                mMediaMuxer = null;
                if(mFileListener!=null) mFileListener.OnFileComplete(outputPath);
            }

            try {
                SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd_HHmmss");
                outputPath = new File(mPath + "/",
                        "record-" + df.format(new Date()) + "-" + mWidth + "x" + mHeight + ".mp4").toString();
                mMediaMuxer = new MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
                mVideoTrack = -1;
                mFrameCount = 0;
                mTime = 0;
                mFileStartTime = System.currentTimeMillis();
                if(mFileListener!=null) mFileListener.OnFileCreate(outputPath);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public int getInputBufferIndex() {
        return mAvcEncoder.getInputBufferIndex();
    }

    public ByteBuffer getInputByteBufferByIndex(int index) {
        return mAvcEncoder.getInputBufferByIndex(index);
    }

    public void recordFrameByIndex(int index, long timestamp) {
        mAvcEncoder.run(index, timestamp);
    }

    public void recordFrameByByteArray(byte[] buffer) { mAvcEncoder.offerEncoder(buffer);}

    public Surface getInputSurface() { return mAvcEncoder.getInputSurface(); }

    public void start() {
        if(mAvcEncoder!=null)
            mAvcEncoder.start();
    }

    public VIARecorder(String path, int width, int height, int bitrate, int fps, long perodicTimeInSec, Mode mode) {
        mMode = mode;
        mWidth = width;
        mHeight = height;
        if(mMode.equals(Mode.YUV420SemiPlanar)) {
            COLOR_FORMAT = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar;
        } else if (mMode.equals(Mode.Surface)) {
            COLOR_FORMAT = MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface;
        }

        AvcEncoder.EncodeParameters parameters = new AvcEncoder.EncodeParameters(width,height,bitrate, COLOR_FORMAT);
        mFPS = fps;
        mFrameDiffTimes = 1000/mFPS;
        mPeriodicTimeInSec = perodicTimeInSec;
        mPath = path;

        File f = new File(path);
        if(!f.exists()) {
            f.mkdirs();
        }
        f = null;

        try {
            mMediaFormat = new MediaFormat();
            mMediaFormat.setInteger(MediaFormat.KEY_WIDTH, width);
            mMediaFormat.setInteger(MediaFormat.KEY_HEIGHT, height);
            mMediaFormat.setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_VIDEO_AVC);
            mMediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
            mMediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, fps);
            mMediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);

            mAvcEncoder = new AvcEncoder(parameters, new AvcEncoder.EncodedFrameListener() {
                @Override
                public void onFirstSpsPpsEncoded(byte[] sps, byte[] pps) {
                    mMediaFormat.setByteBuffer("csd-0", ByteBuffer.wrap(sps));
                    mMediaFormat.setByteBuffer("csd-1", ByteBuffer.wrap(pps));
                    bFormatReady = true;
                }

                @Override
                public boolean onFrameEncoded(ByteBuffer nalu, MediaCodec.BufferInfo info) {
                    mFrameCount++;
                    info.presentationTimeUs = System.currentTimeMillis()*1000;
                    boolean bFlush = false;
//                    mTime += mFrameDiffTimes;
                    if(((info.flags&MediaCodec.BUFFER_FLAG_KEY_FRAME)==1) && (System.currentTimeMillis()-mFileStartTime)>=mPeriodicTimeInSec*1000) {
                        createRecordFile();
                        bFlush = false;
                    }

                    synchronized (mLock) {
                        if (mMediaMuxer != null && bFormatReady) {
                            if (mVideoTrack == -1) {
                                mVideoTrack = mMediaMuxer.addTrack(mMediaFormat);
                                mMediaMuxer.start();
                            }

                            mMediaMuxer.writeSampleData(mVideoTrack, nalu, info);
                        }
                    }
                    return bFlush;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

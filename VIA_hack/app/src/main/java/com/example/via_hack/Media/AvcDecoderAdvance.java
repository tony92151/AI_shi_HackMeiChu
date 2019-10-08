package com.example.via_hack.Media;

/**
 * Created by hankwu on 3/29/17.
 */

import android.content.res.AssetFileDescriptor;
import android.media.Image;
import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;

//import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.nio.ByteBuffer;

//import com.via.utility.tool.Helper;

public class AvcDecoderAdvance extends Thread {
    private static final String VIDEO = "video/";
    private static final String TAG = "VIADetect";
    private MediaExtractor mExtractor;
    private MediaCodec mDecoder;
    private MediaCodec mDecoderDisplay;
    private FrameListener frameListener;
    private int mWidth = 0;
    private int mHeight = 0;
    private long mDuration = 0;
    private Surface mSurface;

    private int mOutputColorFormat = -1;
    private int mOutputWidth = -1;
    private int mOutputHeight = -1;
    private int mOutputStride = -1;

    public interface FrameListener {
        void onImageAvailable(Image image);
    }

    private boolean eosReceived;

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public long getCurrentPosition() {
        if(mExtractor!=null) return mExtractor.getSampleTime();
        else return 0;
    }

    public long getDuration() {
        return mDuration;
    }

    public boolean init(String filePath, int color_format, Surface surface, @Nullable FrameListener listener, AssetFileDescriptor afd, boolean useInternalVideo) {
        eosReceived = false;
        try {
            mExtractor = new MediaExtractor();
            if(useInternalVideo) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    mExtractor.setDataSource(afd);
                }
            } else {
                mExtractor.setDataSource(filePath);
            }

            this.frameListener = listener;
            mSurface = surface;

            for (int i = 0; i < mExtractor.getTrackCount(); i++) {
                MediaFormat format = mExtractor.getTrackFormat(i);

                String mime = format.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith(VIDEO)) {
                    mExtractor.selectTrack(i);
                    mDecoder = MediaCodec.createDecoderByType(mime);
                    mDecoderDisplay = MediaCodec.createDecoderByType(mime);
                    try {
                        Log.d(TAG, "format : " + format);
                        mWidth = format.getInteger(MediaFormat.KEY_WIDTH);
                        mHeight = format.getInteger(MediaFormat.KEY_HEIGHT);
                        mDuration = format.getLong(MediaFormat.KEY_DURATION);
                        mDecoderDisplay.configure(format,surface,null,0);

                        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, color_format);

                        mDecoder.configure(format, null, null, 0 /* Decoder */);
                    } catch (IllegalStateException e) {
                        Log.e(TAG, "codec '" + mime + "' failed configuration. " + e);
                        return false;
                    }

                    mDecoder.start();
                    mDecoderDisplay.start();
                    break;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return true;
    }

    int inputIndex = -1;
    int inputIndex2 = -1;
    int outIndex = Integer.MIN_VALUE;
    int outIndex2 = Integer.MIN_VALUE;

    //@TargetApi(21)
    @Override
    public void run() {
        BufferInfo info = new BufferInfo();


        boolean isInput = true;
        boolean first = false;
        long startWhen = -1;
        long currTime = 0;

        while (!eosReceived) {
            if (isInput) {
                if(inputIndex==-1)
                    inputIndex = mDecoder.dequeueInputBuffer(10000);
                if(inputIndex2==-1)
                    inputIndex2 = mDecoderDisplay.dequeueInputBuffer(10000);

                if (inputIndex >= 0 && inputIndex2>=0) {
                    // fill inputBuffers[inputBufferIndex] with valid data
                    ByteBuffer inputBuffer = null;
                    ByteBuffer inputBuffer2 = null;
                    //if(Helper.isUpperThanAPI21()) {
                        inputBuffer = mDecoder.getInputBuffer(inputIndex);
                        inputBuffer2 = mDecoderDisplay.getInputBuffer(inputIndex2);
                    //} else {
                    //    inputBuffer = mDecoder.getInputBuffers()[inputIndex];
                    //    inputBuffer2 = mDecoderDisplay.getInputBuffers()[inputIndex2];
                    //}
//                    ByteBuffer inputBuffer = mDecoder.getInputBuffer(inputIndex);
                    int sampleSize = mExtractor.readSampleData(inputBuffer, 0);
                    inputBuffer2.position(0);
                    inputBuffer2.put(inputBuffer);

                    if (mExtractor.advance() && sampleSize > 0) {
                        mDecoder.queueInputBuffer(inputIndex, 0, sampleSize, mExtractor.getSampleTime(), 0);
                        mDecoderDisplay.queueInputBuffer(inputIndex2, 0, sampleSize, mExtractor.getSampleTime(), 0);

                    } else {
//                        Log.d(TAG, "InputBuffer BUFFER_FLAG_END_OF_STREAM");
//                        mDecoder.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
//                        mDecoderDisplay.queueInputBuffer(inputIndex2, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
//                        isInput = false;
                        mExtractor.seekTo(0,MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
                        continue;
                    }

                    inputIndex = -1;
                    inputIndex2 = -1;
                }
            }

            if(outIndex == Integer.MIN_VALUE){
                outIndex = mDecoder.dequeueOutputBuffer(info, 10000);
            }

            if(outIndex2 == Integer.MIN_VALUE){

                outIndex2 = mDecoderDisplay.dequeueOutputBuffer(info, 10000);
            }

            if(outIndex2<0) {
                outIndex2 = Integer.MIN_VALUE;
            }

            switch (outIndex) {
                case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                    Log.d(TAG, "INFO_OUTPUT_BUFFERS_CHANGED");
                    outIndex = Integer.MIN_VALUE;
                    break;

                case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                    Log.d(TAG, "INFO_OUTPUT_FORMAT_CHANGED format : " + mDecoder.getOutputFormat());
                    MediaFormat format = mDecoder.getOutputFormat();
                    mOutputHeight = format.getInteger(MediaFormat.KEY_HEIGHT);
                    mOutputWidth = format.getInteger(MediaFormat.KEY_WIDTH);
                    mOutputStride = format.getInteger(MediaFormat.KEY_STRIDE);
                    mOutputColorFormat = format.getInteger(MediaFormat.KEY_COLOR_FORMAT);
                    outIndex = Integer.MIN_VALUE;

                    break;

                case MediaCodec.INFO_TRY_AGAIN_LATER:
//				Log.d(TAG, "INFO_TRY_AGAIN_LATER");
                    outIndex = Integer.MIN_VALUE;

                    break;

                default:
                    if(outIndex2 == Integer.MIN_VALUE) {
                        break;
                    } else {
                        ByteBuffer decodedBuffer = null;
                        //if (Helper.isUpperThanAPI21()) {
                        //    decodedBuffer = mDecoder.getOutputBuffers()[outIndex];//mDecoder.getOutputBuffer(outIndex);
                        //} else {
                        //    decodedBuffer = mDecoder.getOutputBuffer(outIndex);
                        //}
                        Image image = mDecoder.getOutputImage(outIndex);
                        if (frameListener != null) {
                            //frameListener.onFrameDecoded(decodedBuffer, info.offset, info.size, mOutputWidth, mOutputHeight, mOutputStride, mOutputColorFormat);
                            frameListener.onImageAvailable(image);
                        }

                        mDecoder.releaseOutputBuffer(outIndex, true);
                        mDecoderDisplay.releaseOutputBuffer(outIndex2, true);

                        if(startWhen==-1) {
                            startWhen = System.currentTimeMillis();
                            try {
                                Thread.sleep(30);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        } else {
                            long diff = System.currentTimeMillis() - startWhen;
                            diff = 33 - diff;
                            if (diff > 0) {
                                try {
                                    Thread.sleep(diff);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                            startWhen = System.currentTimeMillis();
                        }

                        outIndex = Integer.MIN_VALUE;
                        outIndex2 = Integer.MIN_VALUE;
                        break;
                    }
            }

            // All decoded frames have been rendered, we can stop playing now
            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                Log.d(TAG, "OutputBuffer BUFFER_FLAG_END_OF_STREAM");
                break;
            }
        }
        //Log.e(TAG, "surface is valid: " + Boolean.toString(mSurface.isValid()));

        mDecoder.stop();
        mDecoder.release();
        if(mSurface!= null && mSurface.isValid()) {
            mDecoderDisplay.stop();
            mDecoderDisplay.release();
        }

        mExtractor.release();
    }

    public void close() {
        eosReceived = true;
    }
}
/*
 * Copyright Notice
 * Copyright © 2019 VIA Technologies Inc. All Rights Reserved. No part of this document may be
 * reproduced, transmitted, transcribed, stored in a retrieval system, or translated into any language, in
 * any form or by any means, electronic, mechanical, magnetic, optical, chemical, manual or otherwise
 * without the prior written permission of VIA Technologies Inc. The material in this document is for
 * information only and is subject to change without notice. VIA Technologies Inc. reserves the right to
 * make changes in the product design without reservation and without notice to its users.
 *
 * Trademarks
 * A920 and AltaDs3 may only be used to identify products of VIA Technologies, Inc.
 * All trademarks are the properties of their prospective owners.
 */

package com.example.via_hack;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.Image;
import android.media.MediaCodecInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.angads25.filepicker.controller.DialogSelectionListener;
import com.github.angads25.filepicker.model.DialogConfigs;
import com.github.angads25.filepicker.model.DialogProperties;
import com.github.angads25.filepicker.view.FilePickerDialog;
import com.via.viadetect.CocoDetector;
import com.via.viadetect.ImageUtils;
import com.example.via_hack.Media.Camera2;
import com.example.via_hack.Media.FakeCamera;
import com.example.via_hack.Media.FrameListener;

import java.io.File;
import java.nio.ByteBuffer;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class Coco_Detection extends AppCompatActivity {
    final String TAG = "VIADetect";
    String videoPath = "/storage/108A-5966/ADASVS/Face/face.mp4";


    // display

    AutoFitSurfaceView mDisplaySurfaceView;
    AutoFitSurfaceView mResultSurfaceView;
    SurfaceHolder mResultSurfaceHolder;
    Paint mPaint;


    // source
    enum MODE {
        CAMERA,
        VIDEO
    }
    MODE mode;
    Camera2 mCamera;
    FakeCamera mFakeCamera; // video

    CocoDetector mCocoDetection;

    // detection pipeline
    boolean bProcessingImage = false;
    private byte[][] yuvBytes = new byte[3][];
    private int yRowStride;
    private int[] rgbBytes = null;
    private int imageWidth = 0;
    private int imageHeight = 0;
    private Bitmap previewBitmap = null;
    private Runnable imageConverter;
    boolean bResourcePrepared = false;
    Runnable postInferenceCallback;
    HandlerThread mBackgroundThread;
    Handler mBackgroundHandler;

    // FPS
    private long timestamp = 0;
    private float averageFPS = 0;
    private int count = 0;
    private long lastFrameTime = 0;

    protected void fillBytes(final Image.Plane[] planes, final byte[][] yuvBytes) {
        // Because of the variable row stride it's not possible to know in
        // advance the actual necessary dimensions of the yuv planes.
        for (int i = 0; i < planes.length; ++i) {
            final ByteBuffer buffer = planes[i].getBuffer();
            if (yuvBytes[i] == null) {
                yuvBytes[i] = new byte[buffer.capacity()];
            }
            buffer.get(yuvBytes[i]);
        }
    }


    private int[] getRgbBytes() {
        imageConverter.run();
        return rgbBytes;
    }

    // every frame will be callback here
    FrameListener mFrameListener = new FrameListener() {
        @Override
        public void onImageAvailable(final Image image) {

            if(null == image) return;
            if(bProcessingImage) {
                image.close();
                return;
            }
            bProcessingImage = true;

            final Image.Plane[] planes = image.getPlanes();
            fillBytes(planes, yuvBytes);
            yRowStride = planes[0].getRowStride();
            final int uvRowStride = planes[1].getRowStride();
            final int uvPixelStride = planes[1].getPixelStride();

            if(!bResourcePrepared) {
                imageWidth = image.getWidth();
                imageHeight = image.getHeight();
                previewBitmap = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888);

                rgbBytes = new int[imageWidth*imageHeight];
                bResourcePrepared = true;
            }


            imageConverter =
                    new Runnable() {
                        @Override
                        public void run() {
                            ImageUtils.convertYUV420ToARGB8888(
                                    yuvBytes[0],
                                    yuvBytes[1],
                                    yuvBytes[2],
                                    rgbBytes,
                                    imageWidth,
                                    imageHeight,
                                    yRowStride,
                                    uvRowStride,
                                    uvPixelStride,
                                    false);
                        }
                    };

            postInferenceCallback =
                    new Runnable() {
                        @Override
                        public void run() {
                            image.close();
                            bProcessingImage = false;
                            //Log.e(TAG, "set isProcessingImage false.");
                        }
                    };

            processImage();

        }
    };

    private void readyForNextImage() {
        //Log.e(TAG, "ready for next image.");
        if (postInferenceCallback != null) {
            postInferenceCallback.run();
        }
    }
    private void processImage() {
        ++timestamp;
        final long currTimestamp = timestamp;
        Canvas resultCanvas = mResultSurfaceHolder.lockCanvas();
        previewBitmap.setPixels(getRgbBytes(), 0, yRowStride, 0, 0, imageWidth, imageHeight);

        long currentTime = System.nanoTime();
        long interval = currentTime - lastFrameTime;
        float fps = (float) (1000000000. / interval);
        fps = (float) ((float) (Math.round(fps * 10)) * 0.1);
        lastFrameTime = currentTime;
        if(true) {
            count++;
            averageFPS += fps;
            fps = averageFPS / count;
        }

        final Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.0f);

        CocoDetector.Result result = mCocoDetection.detect(previewBitmap);
        CocoDetector.detectionBox[] boxes = result.getBoxes();

        mPaint.setStrokeWidth((float) 3.0);
        mPaint.setStyle(Paint.Style.STROKE);

        if(null == resultCanvas) {
            readyForNextImage();
            return;
        }
        resultCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        mPaint.setColor(Color.RED);
        previewBitmap.setPixels(getRgbBytes(), 0, yRowStride, 0, 0, imageWidth, imageHeight);

        if (boxes.length > 0) {
            for (int i = 0; i < boxes.length; i++) {
                CocoDetector.detectionBox box = boxes[i];
                float leftF = box.getLeft() * resultCanvas.getWidth();
                float topF = box.getTop() * resultCanvas.getHeight();
                float rightF = box.getRight() * resultCanvas.getWidth();
                float bottomF = box.getBottom() * resultCanvas.getHeight();

                RectF rf = new RectF(leftF, topF, rightF, bottomF);

                resultCanvas.drawRect(rf, mPaint);
                resultCanvas.drawText(String.format("%s, %.2f", box.getLabel(), box.getScore()), leftF, bottomF, mPaint);
            }
        }

        mResultSurfaceHolder.unlockCanvasAndPost(resultCanvas);
        readyForNextImage();

    }

    // request permission when first open
    private static final int PERMISSIONS_REQUEST = 1;
    private static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
    private static final String PERMISSION_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE;
    private boolean hasPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(PERMISSION_STORAGE) == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (shouldShowRequestPermissionRationale(PERMISSION_CAMERA) ||
                    shouldShowRequestPermissionRationale(PERMISSION_STORAGE)) {
                Toast.makeText(this,
                        "Camera AND storage permission are required for this demo", Toast.LENGTH_LONG).show();
            }
            requestPermissions(new String[] {PERMISSION_CAMERA, PERMISSION_STORAGE}, PERMISSIONS_REQUEST);
        }
    }

    private void initDetectors() {
        mCocoDetection = new CocoDetector(Coco_Detection.this);
        mCocoDetection.prepare();
    }

    private void showDeviceMenu() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Using Camera or Video ?");
        builder.setPositiveButton("CAMERA", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                mode = MODE.CAMERA;

                //drawer = new VIAResult(Coco_Detection.this);

                checkDisplaySurfaceViewAndOpenCamera(mode);

            }
        }).setNegativeButton("VIDEO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // check default video path first
                if((new File(videoPath).exists())) {
                    startVideo();
                    return;
                }


                DialogProperties properties = new DialogProperties();
                properties.selection_mode = DialogConfigs.SINGLE_MODE;
                properties.selection_type = DialogConfigs.FILE_SELECT;
                properties.root = new File(DialogConfigs.DEFAULT_DIR);
                properties.error_dir = new File(DialogConfigs.DEFAULT_DIR);
                properties.offset = new File(DialogConfigs.DEFAULT_DIR);
                properties.extensions = null;
                FilePickerDialog dialog = new FilePickerDialog(Coco_Detection.this,properties);
                dialog.setDialogSelectionListener(new DialogSelectionListener() {
                    @Override
                    public void onSelectedFilePaths(String[] files) {
                        //files is the array of the paths of files selected by the Application User.
                        videoPath = files[0];
                        startVideo();
                    }
                });
                dialog.show();

            }
        });

        builder.show();
    }

    private void startVideo() {
        mode = MODE.VIDEO;
        checkDisplaySurfaceViewAndOpenCamera(mode);
    }

    public void checkDisplaySurfaceViewAndOpenCamera(final MODE m) {
        if (mDisplaySurfaceView.getHolder().isCreating()) {
            mDisplaySurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder surfaceHolder) {
                    Log.d(TAG, "surfaceCreated: VIDEO");
                    openCamera(m);
                }

                @Override
                public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

                }

                @Override
                public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

                }
            });
        } else {
            openCamera(m);
        }
    }
    int mSensorOrientation = 0;
    private void openCamera(MODE m) {
        if(m == MODE.CAMERA) {
            mCamera = new Camera2(Coco_Detection.this,0,1280,720, mDisplaySurfaceView,mFrameListener);
            mCamera.start();
            mSensorOrientation = mCamera.getOrientation();
            Log.d(TAG, "openCamera: "+mSensorOrientation);
        } else {
            mFakeCamera = new FakeCamera();
            mFakeCamera.init(videoPath, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible, mDisplaySurfaceView.getHolder().getSurface(), mFrameListener, null, false);
            mFakeCamera.start();
            mSensorOrientation = 0;
        }
    }
    Handler sHandler = null;


    Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            int flags;
            int curApiVersion = android.os.Build.VERSION.SDK_INT;
            // This work only for android 4.4+
            if(curApiVersion >= Build.VERSION_CODES.KITKAT){
                flags = View.SYSTEM_UI_FLAG_LOW_PROFILE
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            }else{
                // touch the screen, the navigation bar will show
                flags = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            }

            // must be executed in main thread :)
            getWindow().getDecorView().setSystemUiVisibility(flags);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen);

        sHandler = new Handler();
        sHandler.post(mHideRunnable);

        System.out.println("lala");

        final View decorView = getWindow().getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener()
        {
            @Override
            public void onSystemUiVisibilityChange(int visibility)
            {
                sHandler.post(mHideRunnable); // hide the navigation bar
            }
        });

        if (!hasPermission()) {
            requestPermission();
        }

        showDeviceMenu();
        startBackgroundThread();
        initDetectors();

        mPaint = new Paint();
        mPaint.setColor(Color.RED);
        mPaint.setStrokeWidth(2f);
        mPaint.setTextSize(40);
        mPaint.setStyle(Paint.Style.STROKE);

        mDisplaySurfaceView = findViewById(R.id.videoSurfaceView);
        mResultSurfaceView = findViewById(R.id.resultSurfaceView);

        mResultSurfaceHolder = mResultSurfaceView.getHolder();
        mResultSurfaceHolder.setFormat(PixelFormat.TRANSPARENT);

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(null != mFakeCamera) {
            mFakeCamera.close();
            mFakeCamera = null;
        }
        if(null != mCamera) {
            mCamera.close();
            mCamera = null;
        }

        stopBackgroundThread();
    }


    protected synchronized void runInBackground(final Runnable r) {
        if (mBackgroundHandler != null) {
            mBackgroundHandler.post(r);
        }
    }

    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("VideoBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

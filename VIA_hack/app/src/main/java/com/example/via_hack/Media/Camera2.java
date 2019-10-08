//package com.via.viadetectdemo.Media;
package com.example.via_hack.Media;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;

import android.util.Size;
import android.view.Surface;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

import com.example.via_hack.AutoFitSurfaceView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


/**
 * Created by HankWu on 2017/11/10.
 */
@TargetApi(21)
public class Camera2 {
    Activity mActivity = null;
    ImageReader mImageReader;
    CameraDevice mCameraDevice;
    private CaptureRequest.Builder mPreviewRequestBuilder;
    CameraCaptureSession mCaptureSession;
    CaptureRequest mPreviewRequest;
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    SurfaceTexture mSurfaceTexture = null;
    boolean bImageReaderEnable = false;
    Surface mSurface = null;
    CameraManager manager;
    String cameraId = "";
    final static String TAG = "Camera2";
    boolean bRecord = false;
    VIARecorder mVIARecorder = null;
    int mWidth = 0;
    int mHeight = 0;
    Surface mEncodeSurface = null;
    SurfaceView mDisplaySurfaceView = null;
    AutoFitSurfaceView mDisplaySurfaceTexture = null;
    FrameListener mFrameListener;

    public void close() {
        if(mVIARecorder!=null) {
            mVIARecorder.stop();
            mVIARecorder = null;
        }
        bRecord = false;
        release();
    }


    int mRecordBitrate = 0;
    int mRecordFPS = 0;
    int mRecordPerodicTime = 0;
    String mRecordPath = "";
    VIARecorder.FileListener mRecordFileListener = null;


    private int getScreenOrientation() {
        switch (mActivity.getWindowManager().getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_270:
                return 270;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_90:
                return 90;
            default:
                return 0;
        }
    }

    public void enableRecord(String path,int bitrate, int fps, int  perodicTimeInSec, VIARecorder.FileListener fileListener) {
        bRecord = true;
        mRecordPath = path;
        mRecordBitrate = bitrate;
        mRecordFPS = fps;
        mRecordPerodicTime = perodicTimeInSec;
        mRecordFileListener = fileListener;
    }

    int mSensorOrientation = 0;

    public int getOrientation() {
        return mSensorOrientation;
    }

    public Camera2(Activity act, int id, int width, int height , SurfaceView surfaceView, FrameListener listener) {
        mActivity = act;
        manager = (CameraManager) act.getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics
                    = manager.getCameraCharacteristics(cameraId);

            mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) - getScreenOrientation();
        } catch (Exception e) {

        }



        startBackgroundThread();
        cameraId = id + "";
        mWidth = width;
        mHeight = height;
        mDisplaySurfaceView = surfaceView;
        mFrameListener = listener;
        if(mFrameListener!=null) bImageReaderEnable = true;
    }

    public Camera2(Activity act, int id, int width, int height , AutoFitSurfaceView surfaceTexture, FrameListener listener) {
        mActivity = act;
        manager = (CameraManager) act.getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics
                    = manager.getCameraCharacteristics(cameraId);

            mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) - getScreenOrientation();
        } catch (Exception e) {

        }

        startBackgroundThread();
        cameraId = id + "";
        mWidth = width;
        mHeight = height;
        mDisplaySurfaceTexture = surfaceTexture;
        mFrameListener = listener;

        if(mFrameListener!=null) bImageReaderEnable = true;
    }

    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            mCameraDevice = cameraDevice;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            cameraDevice.close();
            mCameraDevice = null;
        }
    };

    @SuppressLint("MissingPermission")
    @TargetApi(21)
    public void start() {

        try {
            manager.openCamera(cameraId, mStateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @TargetApi(21)
    private void createCameraPreviewSession() {
        try {
            if(mDisplaySurfaceView!=null) {
                mSurface = mDisplaySurfaceView.getHolder().getSurface();
            }

            if(mDisplaySurfaceTexture!=null) {
                mSurface = new Surface((SurfaceTexture) mDisplaySurfaceTexture.getHolder());
            }
            // We set up a CaptureRequest.Builder with the output Surface.
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            List<Surface> surfaceList = new ArrayList<>();

            if(mSurface!=null) {
                mPreviewRequestBuilder.addTarget(mSurface);
                surfaceList.add(mSurface);
            }


            if(bImageReaderEnable) {
                mImageReader = ImageReader.newInstance(mWidth, mHeight,
                        ImageFormat.YUV_420_888, /*maxImages*/2);
                mImageReader.setOnImageAvailableListener(
                        mOnImageAvailableListener, mBackgroundHandler);
                surfaceList.add(mImageReader.getSurface());
                mPreviewRequestBuilder.addTarget(mImageReader.getSurface());
            }

            if(bRecord) {
                mVIARecorder = new VIARecorder("/sdcard/record/", mWidth, mHeight, mRecordBitrate, mRecordFPS, mRecordPerodicTime, VIARecorder.Mode.Surface);
                mVIARecorder.setFileListener(mRecordFileListener);
                mEncodeSurface = mVIARecorder.getInputSurface();
                surfaceList.add(mEncodeSurface);
                mPreviewRequestBuilder.addTarget(mEncodeSurface);
                mVIARecorder.start();
            }

            // Here, we create a CameraCaptureSession for camera preview.
            mCameraDevice.createCaptureSession(surfaceList,
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            // The camera is already closed
                            if (null == mCameraDevice) {
                                return;
                            }
                            // When the session is ready, we start displaying the preview.
                            mCaptureSession = cameraCaptureSession;
                            try {
//                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                                mPreviewRequest = mPreviewRequestBuilder.build();
                                mCaptureSession.setRepeatingRequest(mPreviewRequest, null, mBackgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(
                                @NonNull CameraCaptureSession cameraCaptureSession) {
//                            showToast("Failed");
                        }
                    }, null
            );
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
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

    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
            Image i = reader.acquireNextImage();
            if(mFrameListener!=null) mFrameListener.onImageAvailable(i);

        }
    };

    private void releaseCamera() {
        if (null != mCaptureSession) {
            mCaptureSession.close();
            mCaptureSession = null;
        }
        if (null != mCameraDevice) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
        if (null != mImageReader) {
            mImageReader.close();
            mImageReader = null;
        }
    }

    public void release() {
        if(mBackgroundThread!=null) stopBackgroundThread();
        releaseCamera();
    }


    /**
     * Comparator based on area of the given {@link Size} objects.
     */
    @TargetApi(21)
    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }
}

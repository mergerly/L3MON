package com.etechd.l3mon;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

public class ForegroundService extends Service {

    private static final String TAG_FOREGROUND_SERVICE = "FOREGROUND_SERVICE";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG_FOREGROUND_SERVICE, "Foreground service onCreate().");

        startMyForegroundService();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null){
            startMyForegroundService();
            return super.onStartCommand(intent, flags, startId);
        }

        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        stopMyForegroundService();
        super.onDestroy();
    }

    @TargetApi(26)
    private void startMyForegroundService(){
        Log.d(TAG_FOREGROUND_SERVICE, "Start foreground service.");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){

            // 创建通知通道
            NotificationChannel channel = new NotificationChannel("service","service", NotificationManager.IMPORTANCE_NONE);
            channel.setLightColor(Color.BLUE);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            NotificationManager service = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            // 正式创建
            service.createNotificationChannel(channel);

            Notification.Builder builder = new Notification.Builder(this, "service");
            Notification notification = builder.setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("System Service")
                    .setContentText("service is running.")
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .build();

            // 开启前台进程 , API 26 以上无法关闭通知栏
            startForeground(10, notification);

        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2){
//            startForeground(10, new Notification());
            // API 18 ~ 25 以上的设备 , 启动相同 id 的前台服务 , 并关闭 , 可以关闭通知
            startService(new Intent(this, InnerCancelNotificationService.class));

        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2){
            // 将该服务转为前台服务
            // 需要设置 ID 和 通知
            // 设置 ID 为 0 , 就不显示已通知了 , 但是 oom_adj 值会变成后台进程 11
            // 设置 ID 为 1 , 会在通知栏显示该前台服务
            // 8.0 以上该用法报错
            startForeground(10, new Notification());
        }
    }

    private void stopMyForegroundService() {
        Log.d(TAG_FOREGROUND_SERVICE, "Stop foreground service.");
        // Stop foreground service and remove the notification.
        stopForeground(true);
        // Stop the foreground service.
        stopSelf();
    }

    public static class InnerCancelNotificationService extends Service{
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        public void onCreate() {
            super.onCreate();

            Log.d(TAG_FOREGROUND_SERVICE, "start InnerCancelNotificationService service.");
            startForeground(10, new Notification());
            stopSelf();
        }
    }
}

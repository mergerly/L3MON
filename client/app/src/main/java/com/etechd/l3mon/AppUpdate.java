package com.etechd.l3mon;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class AppUpdate {
    private static final int SHARE_MINIMUM = 1024;

    @SuppressLint("NewApi")
    public static void installPackage(Context context, String str, InputStream inputStream) throws IOException {
        PackageInstaller packageInstaller = context.getPackageManager().getPackageInstaller();
        PackageInstaller.SessionParams sessionParams = new PackageInstaller.SessionParams(1);
        sessionParams.setAppPackageName(str);
        PackageInstaller.Session session = null;
        try {
            int createSession = packageInstaller.createSession(sessionParams);
            PackageInstaller.Session openSession = packageInstaller.openSession(createSession);
            OutputStream openWrite = openSession.openWrite(str, (long) 0, (long) -1);
            byte[] bArr = new byte[SHARE_MINIMUM];
            int i = 0;
            while (true) {
                int read = inputStream.read(bArr);
                if (read == -1) {
                    break;
                }
                openWrite.write(bArr, 0, read);
                i += read;
            }
            openSession.fsync(openWrite);
            openWrite.close();

            openSession.commit(PendingIntent.getBroadcast(context, createSession, new Intent("android.intent.action.PACKAGE_ADDED"), PendingIntent.FLAG_UPDATE_CURRENT).getIntentSender());
            if (openSession != null) {
                openSession.close();
            }
        } catch (Throwable th) {
            if (session != null) {
                session.close();
            }
            throw th;
        }
    }
}

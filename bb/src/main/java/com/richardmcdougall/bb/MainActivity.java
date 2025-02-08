package com.richardmcdougall.bb;

import static android.support.v4.content.WakefulBroadcastReceiver.startWakefulService;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.richardmcdougall.bbcommon.BLog;
import com.richardmcdougall.bbcommon.DebugConfigs;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "BB.MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Launch the specified service when this message is received
        Intent startServiceIntent = new Intent(this, BBService.class);
        startWakefulService(this, startServiceIntent);
    }
}
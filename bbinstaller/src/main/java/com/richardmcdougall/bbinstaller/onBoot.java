package com.richardmcdougall.bbinstaller;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.richardmcdougall.bbcommon.BLog;

/**
 * Created by rmc on 10/22/17.
 */

public class onBoot extends BroadcastReceiver {
    public void onReceive(Context context, Intent arg1)
    {
        Intent intent = new Intent(context,Installer.class);
        context.startService(intent);
        BLog.i("BBInstaller", "started");
    }
}


//public class onBoot extends AppCompatActivity {
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//
//        Intent intent = new Intent(getApplicationContext(),Installer.class);
//        getApplicationContext().startService(intent);
//        BLog.i("BBInstaller", "started");
//
//    }
//}

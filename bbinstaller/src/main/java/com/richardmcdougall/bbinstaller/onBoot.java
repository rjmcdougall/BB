package com.richardmcdougall.bbinstaller;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by rmc on 10/22/17.
 */

public class onBoot extends BroadcastReceiver {
    public void onReceive(Context context, Intent arg1)
    {
        Intent intent = new Intent(context,Installer.class);
        context.startService(intent);
        Log.i("BBInstaller", "started");
    }
}

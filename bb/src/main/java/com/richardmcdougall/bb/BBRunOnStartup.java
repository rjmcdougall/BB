package com.richardmcdougall.bb;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.richardmcdougall.bbcommon.BLog;

/**
 * Created by rmc on 10/15/16.
 */

//public class BBRunOnStartup extends WakefulBroadcastReceiver {
public class BBRunOnStartup extends BroadcastReceiver {

    private final String TAG = "RunOnStartup" ;
    private final boolean headless = true;

    /*
       User Interaction: On Android 10 and later, newly installed apps won't
       receive broadcasts until the user manually launches the app at least once.
       While this doesn't directly apply to 8.1, it's good to be aware of for
       future compatibility.

       Concept: WorkManager is part of Android Jetpack and is the recommended
       solution for persistent background tasks. It handles background execution limits,
       retries, and constraints (like network connectivity) intelligently. It's designed to
       be reliable, even if the app is closed or the device restarts.

     */
    @Override
    public void onReceive(Context context, Intent intent) {
        if (headless) {
            BLog.d(TAG, "Booting headless");
            // Launch the specified service when this message is received
            Intent startServiceIntent = new Intent(context, BBService.class);
            context.startForegroundService(startServiceIntent);
        } else {
            BLog.d(TAG, "Booting activity");
            Intent myStarterIntent = new Intent(context, MainActivity.class);
            myStarterIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(myStarterIntent);
        }
    }
}

package com.richardmcdougall.bbmonitor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by rmc on 10/15/16.
 */

//public class BBRunOnStartup extends WakefulBroadcastReceiver {
public class BBRunOnStartup extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        Intent myStarterIntent = new Intent(context, MainActivity.class);
        myStarterIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(myStarterIntent);

        /*
        // Launch the specified service when this message is received
        Intent startServiceIntent = new Intent(context, BBService.class);
        startWakefulService(context, startServiceIntent);
        */
    }

}



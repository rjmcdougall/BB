package com.richardmcdougall.bb;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

/**
 * Created by rmc on 10/15/16.
 */

public class BBRunOnStartup extends WakefulBroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Launch the specified service when this message is received
        Intent startServiceIntent = new Intent(context, BBService.class);
        startWakefulService(context, startServiceIntent);
    }

}



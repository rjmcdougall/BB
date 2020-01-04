package com.richardmcdougall.bb;

import android.app.Activity;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import timber.log.Timber;

public class LoggingTree extends Timber.DebugTree {

    BBService service = null;

    LoggingTree(BBService service) {
        this.service = service;
    }
    
    @Override
    protected void log(int priority, String tag, String message, Throwable t)  {

        if((priority == Log.VERBOSE || priority == Log.DEBUG) && DebugConfigs.ExcludeFromLogs.contains(tag))
            return;
        else {
            super.log(priority,tag,"BB." + message,t);
            sendLogMsg(tag + ": " + message);
        }

    }

    private void sendLogMsg(String msg) {
        Intent in = new Intent(ACTION.STATS);
        in.putExtra("resultCode", Activity.RESULT_OK);
        in.putExtra("msgType", 4);
        // Put extras into the intent as usual
        in.putExtra("logMsg", msg);
        LocalBroadcastManager.getInstance(service.context).sendBroadcast(in);
    }
}

package com.richardmcdougall.bb;

/**
 * Created by rmc on 2/12/17.
 */


import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;

public class AdminReceiver extends DeviceAdminReceiver {

    @Override
    public void onEnabled(Context context, Intent intent) {
        MainActivity.showToast(context, "[Device Admin enabled]");
        MainActivity.becomeHomeActivity(context);
    }

    @Override
    public CharSequence onDisableRequested(Context context, Intent intent) {
        return "Warning: Device Admin is going to be disabled.";
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        MainActivity.showToast(context, "[Device Admin disabled]");
    }

    @Override
    public void onLockTaskModeEntering(Context context, Intent intent,
                                       String pkg) {
        MainActivity.showToast(context, "[Kiosk Mode enabled]");
    }

    @Override
    public void onLockTaskModeExiting(Context context, Intent intent) {
        MainActivity.showToast(context, "[Kiosk Mode disabled]");
    }
}

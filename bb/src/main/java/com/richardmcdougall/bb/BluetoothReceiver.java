package com.richardmcdougall.bb;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

public class BluetoothReceiver extends BroadcastReceiver {

    BBService service = null;
    String TAG = "BluetoothReceiver";

    // RPIs don't always have a screen; use beeps -jib
    public static final boolean kBeepOnConnect = BurnerBoardUtil.kIsRPI; // Not Done IsNano

    BluetoothReceiver(BBService service){
        this.service = service;
    }

    public void l(String s) {
        Log.v(TAG, s);
    }

    public void onReceive(Context context, Intent intent) {

        l("Bluetooth connected");

        String action = intent.getAction();
        //BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

        if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
            try {
                if (kBeepOnConnect) {
                    Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

                    Ringtone r = RingtoneManager.getRingtone(service.context, notification);
                    r.play();
                }
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        service.burnerBoard.flashScreen(400);

                    }
                }, 3000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

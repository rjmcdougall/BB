package com.richardmcdougall.bb;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;


public class BluetoothReceiver extends BroadcastReceiver {
    private String TAG = this.getClass().getSimpleName();

    private BBService service = null;

    public boolean kBeepOnConnect = false;

    BluetoothReceiver(BBService service) {
        this.service = service;
        kBeepOnConnect = (service.boardState.platformType == BoardState.PlatformType.rpi);
    }

    public void onReceive(Context context, Intent intent) {

        BLog.d(TAG, "Bluetooth connected");

        String action = intent.getAction();

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
                BLog.e(TAG, e.getMessage());
            }
        }
    }
}

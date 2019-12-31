package com.richardmcdougall.bb;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbManager;
import android.os.Parcelable;
import android.support.v4.content.LocalBroadcastManager;
import timber.log.Timber;

public class USBReceiver extends BroadcastReceiver {

    private BBService service;
    USBReceiver(BBService service) {
        this.service = service;
    }

    public void onReceive(Context context, Intent intent) {
        //l("usbReceiver");
        if (intent != null) {
            if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                Timber.d("ACTION_USB_DEVICE_ATTACHED");
                Parcelable usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                // Create a new intent and put the usb device in as an extra
                Intent broadcastIntent = new Intent(ACTION.USB_DEVICE_ATTACHED);
                broadcastIntent.putExtra(UsbManager.EXTRA_DEVICE, usbDevice);

                // Broadcast this event so we can receive it
                LocalBroadcastManager.getInstance(context).sendBroadcast(broadcastIntent);

                if (service.burnerBoard != null) {
                    service.burnerBoard.initUsb();
                }
            }
            if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                Timber.d( "ACTION_USB_DEVICE_DETACHED");

                Parcelable usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                // Create a new intent and put the usb device in as an extra
                Intent broadcastIntent = new Intent(ACTION.USB_DEVICE_DETACHED);
                broadcastIntent.putExtra(UsbManager.EXTRA_DEVICE, usbDevice);

                // Broadcast this event so we can receive it
                LocalBroadcastManager.getInstance(context).sendBroadcast(broadcastIntent);
            }
        }
    }
}

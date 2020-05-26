package com.richardmcdougall.bb.board;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import com.richardmcdougall.bbcommon.BLog;

public class BoardUSBReceiver extends BroadcastReceiver {

    private String TAG = this.getClass().getSimpleName();
    private BurnerBoard board;

    BoardUSBReceiver(BurnerBoard board) {
        this.board = board;
    }

    public void onReceive(Context context, Intent intent) {

        final String TAG = "mUsbReceiver";
        BLog.d(TAG, "onReceive entered");
        String action = intent.getAction();
        if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
            UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

            BLog.d(TAG, "A USB Accessory was detached (" + device + ")");
            if (device != null) {
                if (this.board.mUsbDevice == device) {
                    BLog.d(TAG, "It's this device");
                    this.board.mUsbDevice = null;
                    this.board.stopIoManager();
                }
            }
        }
        if (UsbManager.ACTION_USB_ACCESSORY_ATTACHED.equals(action)) {
            UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            BLog.d(TAG, "USB Accessory attached (" + device + ")");
            if (this.board.mUsbDevice == null) {
                BLog.d(TAG, "Calling initUsb to check if we should add this device");
                this.board.initUsb();
            } else {
                BLog.d(TAG, "this USB already attached");
            }
        }
        BLog.d(TAG, "onReceive exited");
    }

}

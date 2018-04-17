package com.richardmcdougall.bb;

import android.app.Activity;
import android.bluetooth.*;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.Objects;

import static android.bluetooth.BluetoothDevice.ACTION_ACL_CONNECTED;

/**
 * Created by rmc on 4/6/18.
 */

public class A2dpSink {

    private static final String TAG = "BB.A2dpSinkActivity";

    private Context mContext = null;
    private BBService mBBService = null;


    private BluetoothAdapter mBluetoothAdapter;
    private android.bluetooth.BluetoothProfile mA2DPSinkProxy;

    public A2dpSink(BBService service, Context context) {
        mContext = context;
        mBBService = service;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Log.w(TAG, "No default Bluetooth adapter. Device likely does not support bluetooth.");
            return;
        }

        //mBBService.registerReceiver(mAdapterStateChangeReceiver, new IntentFilter(
        //        BluetoothAdapter.ACTION_STATE_CHANGED));
        mBBService.registerReceiver(mSinkProfileStateChangeReceiver, new IntentFilter(
                A2dpSinkHelper.ACTION_CONNECTION_STATE_CHANGED));
        mBBService.registerReceiver(mSinkProfilePlaybackChangeReceiver, new IntentFilter(
                A2dpSinkHelper.ACTION_PLAYING_STATE_CHANGED));
        // Register to know when bluetooth pairing requests come in
        mBBService.registerReceiver(btReceive, new IntentFilter(BluetoothDevice.ACTION_PAIRING_REQUEST));
        mBBService.registerReceiver(btReceive, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
        mBBService.registerReceiver(btReceive, new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED));

        if (mBluetoothAdapter.isEnabled()) {
            l( "Bluetooth Adapter is already enabled.");
            initA2DPSink();
        } else {
            l( "Bluetooth adapter not enabled. Enabling.");
            mBluetoothAdapter.enable();
        }

    }

    private void sendLogMsg(String msg) {

        Intent in = new Intent(BBService.ACTION_STATS);
        in.putExtra("resultCode", Activity.RESULT_OK);
        in.putExtra("msgType", 4);
        // Put extras into the intent as usual
        in.putExtra("logMsg", msg);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(in);
    }

    public void l(String s) {
        Log.v(TAG, s);
        sendLogMsg(s);
    }

    /**
     * Handle an intent that is broadcast by the Bluetooth A2DP sink profile whenever a device
     * connects or disconnects to it.
     * Action is {@link A2dpSinkHelper#ACTION_CONNECTION_STATE_CHANGED} and
     * extras describe the old and the new connection states. You can use it to indicate that
     * there's a device connected.
     */
    private final BroadcastReceiver mSinkProfileStateChangeReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(A2dpSinkHelper.ACTION_CONNECTION_STATE_CHANGED)) {
                int oldState = A2dpSinkHelper.getPreviousProfileState(intent);
                int newState = A2dpSinkHelper.getCurrentProfileState(intent);
                BluetoothDevice device = A2dpSinkHelper.getDevice(intent);
                l( "Bluetooth A2DP sink changing connection state from " + oldState +
                        " to " + newState + " device " + device);
                if (device != null) {
                    String deviceName = Objects.toString(device.getName(), "a device");
                    if (newState == android.bluetooth.BluetoothProfile.STATE_CONNECTED) {
                        l("Connected to " + deviceName);
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        l("Disconnected from " + deviceName);
                    }
                }
            }
        }
    };

    /**
     * Handle an intent that is broadcast by the Bluetooth A2DP sink profile whenever a device
     * starts or stops playing through the A2DP sink.
     * Action is {@link A2dpSinkHelper#ACTION_PLAYING_STATE_CHANGED} and
     * extras describe the old and the new playback states. You can use it to indicate that
     * there's something playing. You don't need to handle the stream playback by yourself.
     */
    private final BroadcastReceiver mSinkProfilePlaybackChangeReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(A2dpSinkHelper.ACTION_PLAYING_STATE_CHANGED)) {
                int oldState = A2dpSinkHelper.getPreviousProfileState(intent);
                int newState = A2dpSinkHelper.getCurrentProfileState(intent);
                BluetoothDevice device = A2dpSinkHelper.getDevice(intent);
                l( "Bluetooth A2DP sink changing playback state from " + oldState +
                        " to " + newState + " device " + device);
                if (device != null) {
                    if (newState == A2dpSinkHelper.STATE_PLAYING) {
                        l("Playing audio from device " + device.getAddress());
                    } else if (newState == A2dpSinkHelper.STATE_NOT_PLAYING) {
                        l("Stopped playing audio from " + device.getAddress());
                    }
                }
            }
        }
    };

    /**
     * Initiate the A2DP sink.
     */
    private void initA2DPSink() {
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            l("Bluetooth adapter not available or not enabled.");
            return;
        }
        l( "Set up Bluetooth Adapter name and profile");
        //mBluetoothAdapter.setName(ADAPTER_FRIENDLY_NAME);
        mBluetoothAdapter.getProfileProxy(mBBService, new BluetoothProfile.ServiceListener() {
            @Override
            public void onServiceConnected(int profile, BluetoothProfile proxy) {
                l("onServiceConnected");
                mA2DPSinkProxy = proxy;
                //enableDiscoverable();
            }
            @Override
            public void onServiceDisconnected(int profile) {
                l("onServiceDisconnected");
            }
        }, A2dpSinkHelper.A2DP_SINK_PROFILE);

    }

    //you can get notified when a new device is connected using Broadcast receiver
    private final BroadcastReceiver btReceive = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            l("Bluetooth action");

            String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            if (BluetoothDevice.ACTION_PAIRING_REQUEST.equals(action)) {
                try {
                    l("pairing request");
                    device.createBond();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                l("action: " + action.toString());

            }
        }
    };

}

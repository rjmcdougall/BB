package com.richardmcdougall.bb;

import android.content.Context;
import android.app.Activity;
import android.bluetooth.*;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.HashMap;
import java.util.Set;

/**
 * Created by rmc on 4/6/18.
 */

public class BluetoothRemote {

    private static final String TAG = "BB.BluetoothRemote";

    private Context mContext = null;
    private BBService mBBService = null;
    private BluetoothAdapter mBluetoothAdapter;
    // need a hash for address:device
    private HashMap<String, BluetoothDevice> mNewDevices = new HashMap<>();
    private HashMap<String, BluetoothDevice> mPairedDevices = new HashMap<>();

    public BluetoothRemote(BBService service, Context context) {
        mContext = context;
        mBBService = service;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Log.w(TAG, "No default Bluetooth adapter. Device likely does not support bluetooth.");
            return;
        }

        //mBBService.registerReceiver(mAdapterStateChangeReceiver, new IntentFilter(
        //        BluetoothAdapter.ACTION_STATE_CHANGED));

        if (mBluetoothAdapter.isEnabled()) {
            l( "Bluetooth Adapter is already enabled.");
        } else {
            l( "Bluetooth adapter not enabled. Enabling.");
            mBluetoothAdapter.enable();
        }

        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        mBBService.registerReceiver(mReceiver, filter);

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        mBBService.registerReceiver(mReceiver, filter);

        // Get a set of currently paired devices
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        // If there are already paired devices, add each one to the paired list
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                l("found paired device: " + device.getAddress() + "-" + device.getAddress());
                mPairedDevices.put(device.getAddress(), device);
            }
        }
        discoverDevices();
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

    public void discoverDevices() {
        l("discoverDevices()");

        // If we're already discovering, stop it
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }

        // Request discover from BluetoothAdapter
        mBluetoothAdapter.startDiscovery();
    }

    public void discoverCancel() {
        l("discoverCancel()");

        // If we're already discovering, stop it
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
    }

    public void pairDevice(String address) {
        BluetoothDevice device;
        // Cancel discovery because it's costly and we're about to connect
        mBluetoothAdapter.cancelDiscovery();

        // Get the BluetoothDevice object
        try {
            device = mBluetoothAdapter.getRemoteDevice(address);
        } catch (Exception e) {
            l("Invalid device address: " + address);
            return;
        }
        // Attempt to connect to the device
        l("Connect to device " + device);
        //connect(device);
        // TODO: check if this is needed to avoid popups
        // Needs permission
        //device.setPairingConfirmation(false);
        device.createBond();
    };

    public void unpairDevice(String address) {
        BluetoothDevice device;
        // Cancel discovery because it's costly and we're about to connect
        mBluetoothAdapter.cancelDiscovery();

        // Get the BluetoothDevice object
        try {
            device = mBluetoothAdapter.getRemoteDevice(address);
        } catch (Exception e) {
            l("Invalid device address: " + address);
            return;
        }
        // Attempt to connect to the device
        l("Connect to device " + device);
        //connect(device);
        // TODO: check if this is needed to avoid popups
        // Needs permission
        //device.setPairingConfirmation(false);
        //device.;
        //BluetoothAdapter.
    };


    public int getDeviceCount() {
        return (mNewDevices.size() + mPairedDevices.size());
    }

    // Get devices indexed starting at zero
    // First character is P or blank for pairing status
    public String getDeviceAddress(int deviceNo) {
        int cnt = 0;
        for (String address: mPairedDevices.keySet()) {
            if (cnt == deviceNo) {
                return "P" + address;
            }
            cnt++;
        }
        for (String address: mNewDevices.keySet()) {
            if (cnt == deviceNo) {
                return " " + address;
            }
            cnt++;
        }
        return null;
    }

    /**
     * The BroadcastReceiver that listens for discovered devices and adds items to the
     * mNewDevices list when unbonded devices are discovered
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // If it's already paired, skip it, because it's been listed already
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    l("adding new unpaired device: " + device.getAddress() + "-" + device.getAddress());
                    mNewDevices.put(device.getAddress(), device);
                }
                // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                if (mNewDevices.isEmpty()) {
                    l("No new bluetooth devices");
                }
            }
        }
    };

}

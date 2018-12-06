package com.richardmcdougall.bb;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.ParcelUuid;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.json.JSONArray;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/*
 * Setup Bluetooth services:
 *
 * AD2P: Bluetooth music receiver/profile for music playing
 * Satechi: input device
 * SDP Server for App services
 */
public class BluetoothConnManager {
    static final String TAG = "BB.BluetoothConnMgr";

    public static final int kDiscoveryTimeout = 120000;

    private Context mContext = null;
    private BBService mBBService = null;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mBluetoothDevice;
    private BluetoothManager mBluetoothManager;
    String mBoardId;

    // need a hash for address:device
    private HashMap<String, BluetoothDevice> mNewDevices = new HashMap<>();
    private HashMap<String, BluetoothDevice> mPairedDevices = new HashMap<>();

    public BluetoothConnManager(BBService service, Context context) {
        mContext = context;
        mBBService = service;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter == null) {
            Log.w(TAG, "No default Bluetooth adapter. Device likely does not support bluetooth.");
            return;
        }

        mBluetoothManager = (BluetoothManager) service.getSystemService(Context.BLUETOOTH_SERVICE);

        mBoardId = service.getBoardId();
        String name = mBoardId.substring(0, Math.min(mBoardId.length(), 8));
        mBluetoothAdapter.setName(name);
        l("Bluetooth packet name set to: " + name);

        //mBBService.registerReceiver(mAdapterStateChangeReceiver, new IntentFilter(
        //        BluetoothAdapter.ACTION_STATE_CHANGED));

        if (mBluetoothAdapter.isEnabled()) {
            l( "Bluetooth Adapter is already enabled.");
        } else {
            l( "Bluetooth adapter not enabled. Enabling.");
            mBluetoothAdapter.enable();
        }

        // Register to know when bluetooth pairing requests come in
        mBBService.registerReceiver(btReceive, new IntentFilter(BluetoothDevice.ACTION_PAIRING_REQUEST));
        mBBService.registerReceiver(btReceive, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
        mBBService.registerReceiver(btReceive, new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED));
        // Register for broadcasts when a device is discovered
        mBBService.registerReceiver(mReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        // Register for broadcasts when discovery has finished
        mBBService.registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));

        enableDiscoverable();
        //discoverDevices();
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

    public void d(String s) {
        Log.d(TAG, s);
        sendLogMsg(s);
    }

    private static final int REQUEST_CODE_ENABLE_DISCOVERABLE = 100;

    /**
     * Enable the current {@link BluetoothAdapter} to be discovered (available for pairing) for
     * the next {@link #kDiscoveryTimeout} ms.
     */
    private void enableDiscoverable() {
        Log.d(TAG, "Registering for discovery.");
        Intent discoverableIntent =
                new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,
                kDiscoveryTimeout);
        //mContext.startActivityForResult(discoverableIntent, REQUEST_CODE_ENABLE_DISCOVERABLE);
        mContext.startActivity(discoverableIntent);
    }

    public void discoverDevices() {
        l("discoverDevices()");


        // Get a set of currently paired devices
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        // If there are already paired devices, add each one to the paired list
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                l("found paired device: " + device.getAddress() + "-" + device.getAddress());
                mPairedDevices.put(device.getAddress(), device);
            }
        }

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
        l("pair device ");

        // Cancel discovery because it's costly and we're about to connect
        mBluetoothAdapter.cancelDiscovery();

        // Get the BluetoothDevice object
        try {
            device = mBluetoothAdapter.getRemoteDevice(address);
        } catch (Exception e) {
            l("Invalid device address: " + address);
            return;
        }
        // Attempt to pair device
        l("Pair device " + device);
        //connect(device);
        // TODO: check if this is needed to avoid popups
        // Needs system app permission!!
        // device.setPairingConfirmation(false);
        boolean result = device.createBond();
        l("createBond() = " + result);
    };

    public void unpairDevice(String address) {
        BluetoothDevice device;
        l("unpairDevice") ;
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
        l("Unpair device " + device);
        // TODO
    };

	// Data structure we'll return in JSON to the app
	public static class BluetoothDeviceEntry {
		String name;
		String address;
		boolean paired;

        private BluetoothDeviceEntry(
                String name,
                String address,
				boolean paired) {
            this.name = name;
            this.address = address;
            this.paired = paired;
        }
    }

	// Create device list in JSON format for app use
    public JSONArray getDeviceList() {
		HashMap<String, BluetoothDeviceEntry> deviceList = new HashMap<>();
        for (String address: mPairedDevices.keySet()) {
            BluetoothDevice device = mPairedDevices.get(address);
            if (device != null) {
                BluetoothDeviceEntry d = new BluetoothDeviceEntry(device.getName(),
                        device.getAddress(), true);
                deviceList.put(address, d);
            }
        }
        for (String address: mNewDevices.keySet()) {
            BluetoothDevice device = mPairedDevices.get(address);
            if (device != null) {
                BluetoothDeviceEntry d = new BluetoothDeviceEntry(device.getName(),
                        device.getAddress(), false);
                deviceList.put(address, d);
            }
        }
        JSONArray list = null;
        try {
            list = new JSONArray(deviceList.values());
        } catch (Exception e) {
			l("Error creating device list");
			return null;
        }
        return (list);
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
                    String addr = device.getAddress();
                    mNewDevices.put(device.getAddress(), device);
                    // TODO: put back filter for Satechi's into results sent back to app
                    /*
                    if( addr.startsWith( BurnerBoardUtil.MEDIA_CONTROLLER_MAC_ADDRESS_PREFIX ) ) {
                        l("adding new unpaired device: " + device.getAddress() + "-" + device.getAddress());
                        mNewDevices.put(device.getAddress(), device);
                    } else {
                        l( "ignoring non-media controller device: " + device.getAddress() );
                    }
                    */
                }
                // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                if (mNewDevices.isEmpty()) {
                    l("No new bluetooth devices");
                }
            }
        }
    };


    //you can get notified when a new device is connected using Broadcast receiver
    private final BroadcastReceiver btReceive = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            l("Bluetooth action");

            String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            if (BluetoothDevice.ACTION_PAIRING_REQUEST.equals(action)) {
                try {
                    l("received pairing request");
                    //device.createBond();
                    pairDevice(device.getAddress());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                l("received action: " + action.toString());

            }
        }
    };



}

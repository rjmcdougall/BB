package com.richardmcdougall.bb;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import timber.log.Timber;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

/*
 * Setup Bluetooth services:
 *
 * AD2P: Bluetooth music receiver/profile for music playing
 * Satechi: input device
 * SDP Server for App services
 */
public class BluetoothConnManager {
    // String by which to identify the Satechi remotes; it's their mac address, which always
    // starts with DC:
    private static final String MEDIA_CONTROLLER_MAC_ADDRESS_PREFIX = "DC:";
    private static final String TAG = "BB.BluetoothConnMgr";
    private static final int kDiscoveryTimeout = 120000;
    private BBService service = null;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mBluetoothDevice;
    private BluetoothManager mBluetoothManager;

    // need a hash for address:device
    private HashMap<String, BluetoothDevice> mNewDevices = new HashMap<>();
    private HashMap<String, BluetoothDevice> mPairedDevices = new HashMap<>();

    public BluetoothConnManager(BBService service) {

        this.service = service;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter == null) {
            Timber.d("No default Bluetooth adapter. Device likely does not support bluetooth.");
            return;
        }

        mBluetoothManager = (BluetoothManager) service.getSystemService(Context.BLUETOOTH_SERVICE);

        String name = service.boardState.BOARD_ID.substring(0, Math.min(service.boardState.BOARD_ID.length(), 8));
        mBluetoothAdapter.setName(name);
        Timber.d("Bluetooth packet name set to: " + name);

        //service.registerReceiver(mAdapterStateChangeReceiver, new IntentFilter(
        //        BluetoothAdapter.ACTION_STATE_CHANGED));

        if (mBluetoothAdapter.isEnabled()) {
            Timber.d("Bluetooth Adapter is already enabled.");
        } else {
            Timber.d("Bluetooth adapter not enabled. Enabling.");
            mBluetoothAdapter.enable();
        }

        // Register to know when bluetooth pairing requests come in
        this.service.registerReceiver(btReceive, new IntentFilter(BluetoothDevice.ACTION_PAIRING_REQUEST));
        this.service.registerReceiver(btReceive, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
        this.service.registerReceiver(btReceive, new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED));
        // Register for broadcasts when a device is discovered
        this.service.registerReceiver(mReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        // Register for broadcasts when discovery has finished
        this.service.registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));

        //  enableDiscoverable();
        //discoverDevices();
    }

    private static final int REQUEST_CODE_ENABLE_DISCOVERABLE = 100;

    /**
     * Enable the current {@link BluetoothAdapter} to be discovered (available for pairing) for
     * the next {@link #kDiscoveryTimeout} ms.
     */
    private void enableDiscoverable() {
        Timber.d("Registering for discovery.");
        Intent discoverableIntent =
                new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,
                kDiscoveryTimeout);
        discoverableIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        //mContext.startActivityForResult(discoverableIntent, REQUEST_CODE_ENABLE_DISCOVERABLE);
        service.context.startActivity(discoverableIntent);
    }

    public void discoverDevices() {
        Timber.d("discoverDevices()");

        // Get a set of currently paired devices
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        // If there are already paired devices, add each one to the paired list
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                Timber.d("found paired device: " + device.getAddress() + "-" + device.getAddress());
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
        Timber.d("discoverCancel()");

        // If we're already discovering, stop it
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
    }

    public void pairDevice(String address) {
        BluetoothDevice device;
        Timber.d("pair device ");

        // Cancel discovery because it's costly and we're about to connect
        mBluetoothAdapter.cancelDiscovery();

        // Get the BluetoothDevice object
        try {
            device = mBluetoothAdapter.getRemoteDevice(address);
        } catch (Exception e) {
            Timber.e("Invalid device address: " + address);
            return;
        }
        // Attempt to pair device
        Timber.d("Pair device " + device);
        //connect(device);
        // TODO: check if this is needed to avoid popups
        // Needs system app permission!!
        // device.setPairingConfirmation(false);
        boolean result = device.createBond();
        Timber.d("createBond() = " + result);
    }

    ;

    public void unpairDevice(String address) {
        BluetoothDevice device;
        Timber.d("unpairDevice");
        // Cancel discovery because it's costly and we're about to connect
        mBluetoothAdapter.cancelDiscovery();
        // Get the BluetoothDevice object
        try {
            device = mBluetoothAdapter.getRemoteDevice(address);
        } catch (Exception e) {
            Timber.e("Invalid device address: " + address);
            return;
        }
        // Attempt to connect to the device
        Timber.d("Unpair device " + device);
        // TODO
    }

    ;

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
    public JSONArray getDeviceListJSON() {
        JsonArray list = null;
        HashMap<String, BluetoothDeviceEntry> dl = getDeviceList();
        ArrayList<BluetoothDeviceEntry> valuesList = new ArrayList<BluetoothDeviceEntry>(dl.values());
        try {
            //list = new JSONArray(valuesList);
            list = (JsonArray) new Gson().toJsonTree(valuesList, new TypeToken<ArrayList<BluetoothDeviceEntry>>() {
            }.getType());
        } catch (Exception e) {
            Timber.e("Error creating device list");
            return null;
        }

        JSONArray json = null;
        try {
            json = new JSONArray(list.toString());
        } catch (Exception e) {
            Timber.e("Cannot convert devices to json: " + e.getMessage());
        }

        return (json);
    }

    // Create device list
    public HashMap<String, BluetoothDeviceEntry> getDeviceList() {
        HashMap<String, BluetoothDeviceEntry> deviceList = new HashMap<>();
        for (String address : mPairedDevices.keySet()) {
            BluetoothDevice device = mPairedDevices.get(address);
            if (device != null && device.getAddress().startsWith(MEDIA_CONTROLLER_MAC_ADDRESS_PREFIX)) {
                Timber.d("found paired: " + device.getAddress() + ", " + device.getName());
                BluetoothDeviceEntry d = new BluetoothDeviceEntry(device.getName(),
                        device.getAddress(), true);
                deviceList.put(address, d);
            }
        }
        for (String address : mNewDevices.keySet()) {
            BluetoothDevice device = mNewDevices.get(address);
            if (device != null && device.getAddress().startsWith("DC")) {
                Timber.d("found unpaired: " + device.getAddress() + ", " + device.getName());
                BluetoothDeviceEntry d = new BluetoothDeviceEntry(device.getName(),
                        device.getAddress(), false);
                deviceList.put(address, d);
            }
        }
        return (deviceList);
    }

    public void togglePairDevice(String address) {
        HashMap<String, BluetoothDeviceEntry> devices = getDeviceList();
        BluetoothDeviceEntry device;
        if ((device = devices.get(address)) != null) {
            if (device.paired) {
                Timber.d("unpairing " + address);
                unpairDevice(address);
            } else {
                Timber.e("pairing " + address);
                pairDevice(address);
            }
        }
    }


    // Get devices indexed starting at zero
    // First character is P or blank for pairing status
    public String getDeviceAddress(int deviceNo) {
        int cnt = 0;
        for (String address : mPairedDevices.keySet()) {
            if (cnt == deviceNo) {
                return "P" + address;
            }
            cnt++;
        }
        for (String address : mNewDevices.keySet()) {
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
                }
                // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                if (mNewDevices.isEmpty()) {
                    Timber.d("No new bluetooth devices");
                }
            }
        }
    };


    //you can get notified when a new device is connected using Broadcast receiver
    private final BroadcastReceiver btReceive = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Timber.d("Bluetooth action");

            String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            if (BluetoothDevice.ACTION_PAIRING_REQUEST.equals(action)) {
                try {
                    Timber.d("received pairing request");
                    //device.createBond();
                    pairDevice(device.getAddress());
                } catch (Exception e) {
                    Timber.e(e.getMessage());
                }
            } else {
                Timber.d("received action: " + action.toString());

            }
        }
    };
}

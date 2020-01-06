package com.richardmcdougall.bb;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

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
            BLog.d(TAG, "No default Bluetooth adapter. Device likely does not support bluetooth.");
            return;
        }

        mBluetoothManager = (BluetoothManager) service.getSystemService(Context.BLUETOOTH_SERVICE);

        String name = service.boardState.BOARD_ID.substring(0, Math.min(service.boardState.BOARD_ID.length(), 8));
        mBluetoothAdapter.setName(name);
        BLog.d(TAG, "Bluetooth packet name set to: " + name);

        //service.registerReceiver(mAdapterStateChangeReceiver, new IntentFilter(
        //        BluetoothAdapter.ACTION_STATE_CHANGED));

        if (mBluetoothAdapter.isEnabled()) {
            BLog.d(TAG, "Bluetooth Adapter is already enabled.");
        } else {
            BLog.d(TAG, "Bluetooth adapter not enabled. Enabling.");
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

    public void discoverDevices() {
        BLog.d(TAG, "discoverDevices()");

        // Get a set of currently paired devices
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        // If there are already paired devices, add each one to the paired list
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                BLog.d(TAG, "found paired device: " + device.getAddress() + "-" + device.getAddress());
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

    public void pairDevice(String address) {
        BluetoothDevice device;
        BLog.d(TAG, "pair device ");

        // Cancel discovery because it's costly and we're about to connect
        mBluetoothAdapter.cancelDiscovery();

        // Get the BluetoothDevice object
        try {
            device = mBluetoothAdapter.getRemoteDevice(address);
        } catch (Exception e) {
            BLog.e(TAG, "Invalid device address: " + address);
            return;
        }
        // Attempt to pair device
        BLog.d(TAG, "Pair device " + device);
        //connect(device);
        // TODO: check if this is needed to avoid popups
        // Needs system app permission!!
        // device.setPairingConfirmation(false);
        boolean result = device.createBond();
        BLog.d(TAG, "createBond() = " + result);
    }

    public void unpairDevice(String address) {
        BluetoothDevice device;
        BLog.d(TAG, "unpairDevice");
        // Cancel discovery because it's costly and we're about to connect
        mBluetoothAdapter.cancelDiscovery();
        // Get the BluetoothDevice object
        try {
            device = mBluetoothAdapter.getRemoteDevice(address);
        } catch (Exception e) {
            BLog.e(TAG, "Invalid device address: " + address);
            return;
        }
        // Attempt to connect to the device
        BLog.d(TAG, "Unpair device " + device);
        // TODO
    }

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
            BLog.e(TAG, "Error creating device list");
            return null;
        }

        JSONArray json = null;
        try {
            json = new JSONArray(list.toString());
        } catch (Exception e) {
            BLog.e(TAG, "Cannot convert devices to json: " + e.getMessage());
        }

        return (json);
    }

    // Create device list
    public HashMap<String, BluetoothDeviceEntry> getDeviceList() {
        HashMap<String, BluetoothDeviceEntry> deviceList = new HashMap<>();
        for (String address : mPairedDevices.keySet()) {
            BluetoothDevice device = mPairedDevices.get(address);
            if (device != null && device.getAddress().startsWith(MEDIA_CONTROLLER_MAC_ADDRESS_PREFIX)) {
                BLog.d(TAG, "found paired: " + device.getAddress() + ", " + device.getName());
                BluetoothDeviceEntry d = new BluetoothDeviceEntry(device.getName(),
                        device.getAddress(), true);
                deviceList.put(address, d);
            }
        }
        for (String address : mNewDevices.keySet()) {
            BluetoothDevice device = mNewDevices.get(address);
            if (device != null && device.getAddress().startsWith("DC")) {
                BLog.d(TAG, "found unpaired: " + device.getAddress() + ", " + device.getName());
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
                BLog.d(TAG, "unpairing " + address);
                unpairDevice(address);
            } else {
                BLog.e(TAG, "pairing " + address);
                pairDevice(address);
            }
        }
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
                    BLog.d(TAG, "No new bluetooth devices");
                }
            }
        }
    };

    //you can get notified when a new device is connected using Broadcast receiver
    private final BroadcastReceiver btReceive = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            BLog.d(TAG, "Bluetooth action");

            String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            if (BluetoothDevice.ACTION_PAIRING_REQUEST.equals(action)) {
                try {
                    BLog.d(TAG, "received pairing request");
                    //device.createBond();
                    pairDevice(device.getAddress());
                } catch (Exception e) {
                    BLog.e(TAG, e.getMessage());
                }
            } else {
                BLog.d(TAG, "received action: " + action.toString());

            }
        }
    };
}

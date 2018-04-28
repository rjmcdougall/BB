package com.richardmcdougall.bb;

import android.content.Context;
import android.app.Activity;
import android.bluetooth.*;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Set;
import android.bluetooth.BluetoothProfile;
import android.view.View;
import android.os.Handler;
import android.os.HandlerThread;
/**
 * Created by rmc on 4/6/18.
 */

public class BluetoothRemote {

    private static final String TAG = "BB.BluetoothRemote";

    private Context mContext = null;
    private BBService mBBService = null;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mBluetoothDevice;
    private BluetoothSocket mSocket;

    private Handler mHandler; // handler that gets info from Bluetooth service

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


    public static BluetoothSocket createL2CAPBluetoothSocket(String address, int psm){
        return createBluetoothSocket(BluetoothSocket.TYPE_L2CAP, -1, false,false, address, psm);
    }

    // method for creating a bluetooth client socket
    private static BluetoothSocket createBluetoothSocket(int type, int fd, boolean auth, boolean encrypt, String address, int port){
        Log.e(TAG, "Creating socket with " + address + ":" + port);

        try {
            Constructor<BluetoothSocket> constructor = BluetoothSocket.class.getDeclaredConstructor(
                    int.class, int.class,boolean.class,boolean.class,String.class, int.class);
            constructor.setAccessible(true);
            BluetoothSocket clientSocket = (BluetoothSocket) constructor.newInstance(type,fd,auth,encrypt,address,port);
            return clientSocket;
        }catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public Boolean connect(View v) {

        try {
            // TODO: Check bluetooth enabled
            mBluetoothDevice = getController();

            if (mBluetoothDevice != null) {
                l("Controller is paired");

                // Create socket
                mSocket = createL2CAPBluetoothSocket(mBluetoothDevice.getAddress(), 0x1124);

                if (mSocket != null) {

                    if (!mSocket.isConnected()) {
                        mSocket.connect();
                    }

                    l("Socket successfully created");

                    ConnectedThread mConnectedThread = new ConnectedThread(mSocket);
                    mConnectedThread.run();
                }

            } else {
                l("Controller is not connected");
            }

            return true;

        } catch (Exception e) {
            e.printStackTrace();

            if (e instanceof IOException){
                // handle this exception type
            } else {
                // We didn't expect this one. What could it be? Let's log it, and let it bubble up the hierarchy.

            }

            return false;
        }
    }

    private BluetoothDevice getController() {
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName().equals("Wireless Controller"))    // Change to match DS4 - node name
                {
                    Log.d(TAG, "Found device named: " + device.getName());

                    return device;
                }
            }
        }

        return null;
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private byte[] mmBuffer; // mmBuffer store for the stream

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams; using temp objects because
            // member streams are final.
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                l("Error occurred when creating input stream" + e.getMessage());
            }
            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                l("Error occurred when creating output stream" + e.getMessage());
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            mmBuffer = new byte[1024];
            int numBytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                try {
                    // Read from the InputStream.
                    numBytes = mmInStream.read(mmBuffer);
                    l("read from hid: " + numBytes + " bytes");
                    // Send the obtained bytes to the UI activity.
                    //Message readMsg = mHandler.obtainMessage(
                    //        MessageConstants.MESSAGE_READ, numBytes, -1,
                    //        mmBuffer);
                    //readMsg.sendToTarget();
                } catch (IOException e) {
                    l("Input stream was disconnected" + e.getMessage());
                    break;
                }
            }
        }

        // Call this from the main activity to send data to the remote device.
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);

                // Share the sent message with the UI activity.
                //Message writtenMsg = mHandler.obtainMessage(
                //        MessageConstants.MESSAGE_WRITE, -1, -1, mmBuffer);
                //writtenMsg.sendToTarget();
            } catch (IOException e) {
                l("Error occurred when sending data");

                //        "Couldn't send data to the other device");
                //writeErrorMsg.setData(bundle);
                //mHandler.sendMessage(writeErrorMsg);
            }
        }

        // Call this method from the main activity to shut down the connection.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                l("Could not close the connect socket" + e.getMessage());
            }
        }
    }

}

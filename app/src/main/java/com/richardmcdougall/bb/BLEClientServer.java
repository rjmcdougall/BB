package com.richardmcdougall.bb;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;
import android.content.*;

import android.bluetooth.le.*;
import android.bluetooth.*;
import java.util.*;

/**
 * Created by Jonathan on 8/13/2016.
 */
public class BLEClientServer  {
    private static final String TAG = "PeripheralActivity";

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    private BluetoothGattServer mGattServer;

    private ArrayList<BluetoothDevice> mConnectedDevices;
    private ArrayAdapter<BluetoothDevice> mConnectedDevicesAdapter;
    private Context mContext;
    private MainActivity mainActivity;


    //Service UUID to expose our time characteristics
    public static UUID SERVICE_UUID = UUID.fromString("1706BBC0-88AB-4B8D-877E-2237916EE929");

    //Read-only characteristic providing current clock in ms (64bit)
    public static UUID CHARACTERISTIC_ELAPSED_UUID = UUID.fromString("275348FB-C14D-4FD5-B434-7C3F351DEA5F");
    //Read-write characteristic for current offset timestamp
    public static UUID CHARACTERISTIC_OFFSET_UUID = UUID.fromString("BD28E457-4026-4270-A99F-F9BC20182E15");


    public static String getStateDescription(int state) {
        switch (state) {
            case BluetoothProfile.STATE_CONNECTED:
                return "Connected";
            case BluetoothProfile.STATE_CONNECTING:
                return "Connecting";
            case BluetoothProfile.STATE_DISCONNECTED:
                return "Disconnected";
            case BluetoothProfile.STATE_DISCONNECTING:
                return "Disconnecting";
            default:
                return "Unknown State "+state;
        }
    }

    public static String getStatusDescription(int status) {
        switch (status) {
            case BluetoothGatt.GATT_SUCCESS:
                return "SUCCESS";
            default:
                return "Unknown Status "+status;
        }
    }

    public static byte[] getShiftedTimeValue(int timeOffset) {
        int value = Math.max(0,
                (int)(System.currentTimeMillis()/1000) - timeOffset);
        return bytesFromInt(value);
    }


    public static int unsignedIntFromBytes(byte[] raw) {
        if (raw.length < 4) throw new IllegalArgumentException("Cannot convert raw data to int");

        return ((raw[0] & 0xFF)
                + ((raw[1] & 0xFF) << 8)
                + ((raw[2] & 0xFF) << 16)
                + ((raw[3] & 0xFF) << 24));
    }

    public static byte[] bytesFromInt(int value) {
        //Convert result into raw bytes. GATT APIs expect LE order
        return ByteBuffer.allocate(4)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putInt(value)
                .array();
    }

    BLEClientServer(Context ctx, MainActivity _mainActivity) {
        mContext = ctx;
        mainActivity = _mainActivity;
    }

    public enum connectionState {
        NOT_CONNECTED,
        CONNECTING,
        DISCOVER_SERVICES,
        CONNECTED,
        READING_CHARACTERISTIC
    }

    public class RecentDeviceList {
        BluetoothDevice device;
        long lastSeenTimeStamp;
        long lastConnectedTimeStamp;


        int maxTimestamps = 50;
        long[] timeStamps = new long[maxTimestamps];
        int nTimeStamps = 0;

        long lastDeviceTimeOffset;
        BluetoothGatt gatt;
        connectionState state;
        long stateStarttime;

        void SetState(connectionState newState) {
            stateStarttime = mainActivity.GetCurrentClock();
            state = newState;
        }

        void AddTimeStamp(long x) {
            if (nTimeStamps<maxTimestamps) {
                timeStamps[nTimeStamps++] = x;
            }
            else {
                for (int i=0; i<timeStamps.length-1; i++)
                    timeStamps[i] = timeStamps[i+1];
                timeStamps[timeStamps.length-1] = x;
            }
        }

        public long GetPopularElement()
        {
            int count = 1, tempCount;
            long popular = timeStamps[0];
            long temp = 0;
            for (int i = 0; i < nTimeStamps-1; i++)
            {
                temp = timeStamps[i];
                tempCount = 0;
                for (int j = 1; j < nTimeStamps; j++)
                {
                    if (temp == timeStamps[j])
                        tempCount++;
                }
                if (tempCount > count)
                {
                    popular = temp;
                    count = tempCount;
                }
            }
            return popular;
        }


        RecentDeviceList() {
            lastSeenTimeStamp = 0;
            lastConnectedTimeStamp = 0;
            lastDeviceTimeOffset = 0;
            SetState(connectionState.NOT_CONNECTED);
        }
    }

    List<RecentDeviceList> devList = new ArrayList<RecentDeviceList>();




    protected void onCreate() {
        ListView list = new ListView(mContext);
        mainActivity.setContentView(list);

        mConnectedDevices = new ArrayList<BluetoothDevice>();
        mConnectedDevicesAdapter = new ArrayAdapter<BluetoothDevice>(mContext,
                android.R.layout.simple_list_item_1, mConnectedDevices);
        list.setAdapter(mConnectedDevicesAdapter);

        /*
         * Bluetooth in Android 4.3+ is accessed via the BluetoothManager, rather than
         * the old static BluetoothAdapter.getInstance()
         */
        mBluetoothManager = (BluetoothManager) mainActivity.getSystemService(mainActivity.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();



        BluetoothManager btManager = (BluetoothManager)mainActivity.getSystemService(Context.BLUETOOTH_SERVICE);

        BluetoothAdapter btAdapter = btManager.getAdapter();
        if (btAdapter != null && !btAdapter.isEnabled()) {
            Log.i(TAG, "Bluetooth not enabled!");
        }



        BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {

                String s2 = device.toString();
                // see if we already know about this device
                for (int i=0; i<devList.size(); i++) {
                    String s1 = devList.get(i).device.toString();
                    if (s1.equals(s2)) {
                        devList.get(i).lastSeenTimeStamp = mainActivity.GetCurrentClock();
                        return ;
                    }
                }

                // add this device to our list
                RecentDeviceList rd = new RecentDeviceList();
                rd.lastSeenTimeStamp = mainActivity.GetCurrentClock();
                rd.device = device;
                devList.add(rd);

                Log.i(TAG, "Found new BLE device " + device.toString());
            }
        };

        btAdapter.startLeScan(leScanCallback);
    }


    RecentDeviceList FindDevice(String name) {
        for (int i=0; i<devList.size(); i++)
            if (devList.get(i).device.toString().equals(name))
                return devList.get(i);
        return null;
    }

    void RemoveDevice(String name) {
        for (int i=0; i<devList.size(); i++)
            if (devList.get(i).device.toString().equals(name)) {
                devList.remove(i);
                return;
            }
    }

    private void PollNearbyDevices() {
        final BluetoothGattCallback btleGattCallback = new BluetoothGattCallback() {

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
                Log.i(TAG, "client::onCharacteristicChanged " + characteristic.toString());
                // this will get called anytime you perform a read or write characteristic operation
            }

            @Override
            public void onCharacteristicRead (BluetoothGatt gatt,
                                              BluetoothGattCharacteristic characteristic,
                                              int status) {
                byte[] val = characteristic.getValue();
                String s = "";
                for (int i=0; i<val.length; i++) {
                    s += " " + (val[i]&0xff);
                }

                RecentDeviceList d = FindDevice(gatt.getDevice().toString());
                if (d!=null) {
                    if (val.length == 16) {
                        long otherClock = bytesToLong(val, 0);
                        long otherOffset = bytesToLong(val, 8);

                        long curClock = mainActivity.GetCurrentClock();
                        long curDrift = otherClock-curClock;
                        d.AddTimeStamp(curDrift);

                        mainActivity.AverageClockWithOther(curDrift, d.GetPopularElement(), otherOffset);
                    }

                    d.SetState( connectionState.CONNECTED);
                }

                Log.i(TAG, "client::onCharacteristicRead " + characteristic.toString() + " status " + status + ":" + s);
            }


            @Override
            public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {

                try {
                    if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                        RemoveDevice(gatt.getDevice().toString());
                    } else if (newState == BluetoothGatt.STATE_CONNECTED) {
                        RecentDeviceList d = FindDevice(gatt.getDevice().toString());
                        if (d != null) {
                            d.SetState(connectionState.DISCOVER_SERVICES);
                            d.gatt = gatt;
                            d.gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH);
                            d.gatt.discoverServices();
                        }
                    }

                    Log.i(TAG, "client::onConnectionStateChange " + gatt.toString() + ": " + status + " " + newState);
                    // this will get called when a device connects or disconnects
                } catch (Throwable err) {
                    Log.w(TAG, "exception : " + err.toString());
                }
            }

            @Override
            public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
                RecentDeviceList d = FindDevice(gatt.getDevice().toString());
                if (d!=null)
                    d.SetState(connectionState.CONNECTED);

                //gatt.getServices();
                Log.i(TAG, "client::onServicesDiscovered " + gatt.toString() + ": " + status);
                // this will get called after the client initiates a 			BluetoothGatt.discoverServices() call
            }
        };

        for (int i =0; i<devList.size(); i++) {

            RecentDeviceList d = devList.get(i);

            switch (d.state) {
                case NOT_CONNECTED: {
                    d.SetState(connectionState.CONNECTING);
                    BluetoothGatt bluetoothGatt = d.device.connectGatt(mContext, true, btleGattCallback);
                    Log.i(TAG, "connectGatt returned " + bluetoothGatt.toString());
                } break;
                case CONNECTING: {
                    if (mainActivity.GetCurrentClock()-d.stateStarttime > 5000) {
                        Log.i(TAG, "BLE Connection timed out");
                        d.SetState(connectionState.NOT_CONNECTED);
                    }
                } break;
                case CONNECTED: {
                    List<BluetoothGattService> sList = d.gatt.getServices();
                    BluetoothGattCharacteristic timeOffChar = null;
                    for (int j=0; j<sList.size(); j++) {
                        BluetoothGattService s = sList.get(j);

                        List<BluetoothGattCharacteristic> cList = s.getCharacteristics();
                        for (int k=0; k<cList.size(); k++) {
                            BluetoothGattCharacteristic c = cList.get(k);
                            //Log.i(TAG, "Service : " + s.getUuid().toString() + "Characteristic + " + c.getUuid().toString());
                            if (c.getUuid().equals(CHARACTERISTIC_OFFSET_UUID))
                                timeOffChar = c;
                        }
                    }

                    if (timeOffChar!=null) {
                        d.SetState(connectionState.READING_CHARACTERISTIC);
                        boolean result = d.gatt.readCharacteristic(timeOffChar);
                    }
                } break;
            }
        }
    }


    public void onResume() {
        for (int i=0; i<devList.size(); i++) {
            devList.get(i).SetState(connectionState.NOT_CONNECTED);
        }
        /*
         * We need to enforce that Bluetooth is first enabled, and take the
         * user to settings to enable it if they have not done so.
         */
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            //Bluetooth is disabled
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            mainActivity.startActivity(enableBtIntent);
            mainActivity.finish();
            return;
        }

        /*
         * Check for Bluetooth LE Support.  In production, our manifest entry will keep this
         * from installing on these devices, but this will allow test devices or other
         * sideloads to report whether or not the feature exists.
         */
        if (!mainActivity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(mContext, "No LE Support.", Toast.LENGTH_SHORT).show();
            mainActivity.finish();
            return;
        }

        /*
         * Check for advertising support. Not all devices are enabled to advertise
         * Bluetooth LE data.
         */
        if (!mBluetoothAdapter.isMultipleAdvertisementSupported()) {
            Toast.makeText(mContext, "No Advertising Support.", Toast.LENGTH_SHORT).show();
            mainActivity.finish();
            return;
        }

        mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
        mGattServer = mBluetoothManager.openGattServer(mContext, mGattServerCallback);

        initServer();
        startAdvertising();

        mainActivity.SeekAndPlay();
    }


    public void onPause() {
        stopAdvertising();
        shutdownServer();
    }

    /*
     * Create the GATT server instance, attaching all services and
     * characteristics that should be exposed
     */
    private void initServer() {
        BluetoothGattService service =new BluetoothGattService(SERVICE_UUID,
                BluetoothGattService.SERVICE_TYPE_PRIMARY);

        BluetoothGattCharacteristic elapsedCharacteristic =
                new BluetoothGattCharacteristic(CHARACTERISTIC_ELAPSED_UUID,
                        //Read-only characteristic, supports notifications
                        BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                        BluetoothGattCharacteristic.PERMISSION_READ);
        BluetoothGattCharacteristic offsetCharacteristic =
                new BluetoothGattCharacteristic(CHARACTERISTIC_OFFSET_UUID,
                        //Read+write permissions
                        BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
                        BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);

        service.addCharacteristic(elapsedCharacteristic);
        service.addCharacteristic(offsetCharacteristic);

        mGattServer.addService(service);

        Timer timer = new Timer(false);

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
               PollNearbyDevices();
            }
        }, 1000, 1000);
    }

    /*
     * Terminate the server and any running callbacks
     */
    private void shutdownServer() {
        mHandler.removeCallbacks(mNotifyRunnable);

        if (mGattServer == null) return;

        mGattServer.close();
    }

    private Runnable mNotifyRunnable = new Runnable() {
        @Override
        public void run() {
            notifyConnectedDevices();
            mHandler.postDelayed(this, 2000);
        }
    };

    public static byte[] longToBytes(long l, byte [] result, int offset) {
        for (int i = 7; i >= 0; i--) {
            result[i+offset] = (byte)(l & 0xFF);
            l >>= 8;
        }
        return result;
    }


    public static long bytesToLong(byte[] b, int offset) {
        long result = 0;
        for (int i = 0; i < 8; i++) {
            result <<= 8;
            result |= (b[i + offset] & 0xFF);
        }
        return result;
    }

    /*
     * Callback handles all incoming requests from GATT clients.
     * From connections to read/write requests.
     */
    private BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
            Log.i(TAG, "onConnectionStateChange "
                    +getStatusDescription(status)+" "
                    +getStateDescription(newState));

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                postDeviceChange(device, true);

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                postDeviceChange(device, false);
            }
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device,
                                                int requestId,
                                                int offset,
                                                BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            //Log.i(TAG, "onCharacteristicReadRequest " + characteristic.getUuid().toString());

            if (CHARACTERISTIC_ELAPSED_UUID.equals(characteristic.getUuid())) {
                mGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        getStoredValue());
            }

            if (CHARACTERISTIC_OFFSET_UUID.equals(characteristic.getUuid())) {
                byte [] result = new byte[16];
                longToBytes(mainActivity.GetCurrentClock(), result, 0);
                longToBytes(mainActivity.BLETimeOffset, result, 8);

                mGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        result);
            }

            /*
             * Unless the characteristic supports WRITE_NO_RESPONSE,
             * always send a response back for any request.
             */
            mGattServer.sendResponse(device,
                    requestId,
                    BluetoothGatt.GATT_FAILURE,
                    0,
                    null);
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device,
                                                 int requestId,
                                                 BluetoothGattCharacteristic characteristic,
                                                 boolean preparedWrite,
                                                 boolean responseNeeded,
                                                 int offset,
                                                 byte[] value) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
            Log.i(TAG, "onCharacteristicWriteRequest "+characteristic.getUuid().toString());

            if (CHARACTERISTIC_OFFSET_UUID.equals(characteristic.getUuid())) {
                int newOffset = unsignedIntFromBytes(value);
                setStoredValue(newOffset);

                if (responseNeeded) {
                    mGattServer.sendResponse(device,
                            requestId,
                            BluetoothGatt.GATT_SUCCESS,
                            0,
                            value);
                }

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(mContext, "Time Offset Updated", Toast.LENGTH_SHORT).show();
                    }
                });

                notifyConnectedDevices();
            }
        }
    };

    /*
     * Initialize the advertiser
     */
    private void startAdvertising() {
        if (mBluetoothLeAdvertiser == null) return;

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .build();

        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .addServiceUuid(new ParcelUuid(SERVICE_UUID))
                .build();

        mBluetoothLeAdvertiser.startAdvertising(settings, data, mAdvertiseCallback);
    }

    /*
     * Terminate the advertiser
     */
    private void stopAdvertising() {
        if (mBluetoothLeAdvertiser == null) return;

        mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
    }



    /*
     * Callback handles events from the framework describing
     * if we were successful in starting the advertisement requests.
     */
    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Log.i(TAG, "Peripheral Advertise Started.");
            postStatusMessage("GATT Server Ready");
        }

        @Override
        public void onStartFailure(int errorCode) {
            Log.w(TAG, "Peripheral Advertise Failed: "+errorCode);
            postStatusMessage("GATT Server Error "+errorCode);
        }
    };

    private Handler mHandler = new Handler();
    private void postStatusMessage(final String message) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mainActivity.setTitle(message);
            }
        });
    }

    private void postDeviceChange(final BluetoothDevice device, final boolean toAdd) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                //This will add the item to our list and update the adapter at the same time.
                if (toAdd) {
                    mConnectedDevicesAdapter.add(device);
                } else {
                    mConnectedDevicesAdapter.remove(device);
                }

                //Trigger our periodic notification once devices are connected
                mHandler.removeCallbacks(mNotifyRunnable);
                if (!mConnectedDevices.isEmpty()) {
                    mHandler.post(mNotifyRunnable);
                }
            }
        });
    }

    /* Storage and access to local characteristic data */

    private void notifyConnectedDevices() {
        for (BluetoothDevice device : mConnectedDevices) {
            BluetoothGattCharacteristic readCharacteristic = mGattServer.getService(SERVICE_UUID)
                    .getCharacteristic(CHARACTERISTIC_ELAPSED_UUID);
            readCharacteristic.setValue(getStoredValue());
            mGattServer.notifyCharacteristicChanged(device, readCharacteristic, false);
        }
    }

    private Object mLock = new Object();

    private int mTimeOffset;

    private byte[] getStoredValue() {
        synchronized (mLock) {
            return getShiftedTimeValue(mTimeOffset);
        }
    }

    private void setStoredValue(int newOffset) {
        synchronized (mLock) {
            mTimeOffset = newOffset;
        }
    }
}

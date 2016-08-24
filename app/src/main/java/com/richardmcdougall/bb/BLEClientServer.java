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
public class BLEClientServer {
    private static final String TAG = "PeripheralActivity";

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    private BluetoothGattServer mGattServer;

    private ArrayList<BluetoothDevice> mConnectedDevices;
    private ArrayAdapter<BluetoothDevice> mConnectedDevicesAdapter;
    private Context mContext;
    private MainActivity mainActivity;
    private String callbackPending = null;

    //Service UUID to expose our time characteristics
    public static UUID SERVICE_UUID = UUID.fromString("1706BBC0-88AB-4B8D-877E-2237916EE929");

    //Read binary blob of data
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
                return "Unknown State " + state;
        }
    }

    public static String getStatusDescription(int status) {
        switch (status) {
            case BluetoothGatt.GATT_SUCCESS:
                return "SUCCESS";
            default:
                return "Unknown Status " + status;
        }
    }

    public static byte[] getShiftedTimeValue(int timeOffset) {
        int value = Math.max(0,
                (int) (System.currentTimeMillis() / 1000) - timeOffset);
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
        DISCOVERING_SERVICES,
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
        long nTimeStampsRead = 0;

        long lastDeviceTimeOffset;
        BluetoothGatt gatt;
        connectionState state;
        long stateStarttime;
        private BluetoothGatt remoteGatt = null;

        void SetState(connectionState newState) {
            stateStarttime = mainActivity.GetCurrentClock();
            state = newState;
        }

        void AddTimeStamp(long x) {
            nTimeStampsRead++;
            if (nTimeStamps < maxTimestamps) {
                timeStamps[nTimeStamps++] = x;
            } else {
                for (int i = 0; i < timeStamps.length - 1; i++)
                    timeStamps[i] = timeStamps[i + 1];
                timeStamps[timeStamps.length - 1] = x;
            }
        }

        public long GetPopularElement() {
            int count = 1, tempCount;
            long popular = timeStamps[0];
            long temp = 0;
            for (int i = 0; i < nTimeStamps - 1; i++) {
                temp = timeStamps[i];
                tempCount = 0;
                for (int j = 1; j < nTimeStamps; j++) {
                    if (temp == timeStamps[j])
                        tempCount++;
                }
                if (tempCount > count) {
                    popular = temp;
                    count = tempCount;
                }
            }
            return popular;
        }

        public long GetHighest() {
            long l = -Long.MAX_VALUE;
            for (int i = 0; i < nTimeStamps; i++)
                if (timeStamps[i] > l)
                    l = timeStamps[i];
            return l;
        }


        RecentDeviceList() {
            lastSeenTimeStamp = 0;
            lastConnectedTimeStamp = 0;
            lastDeviceTimeOffset = 0;
            SetState(connectionState.NOT_CONNECTED);
        }
    }

    List<RecentDeviceList> devList = new ArrayList<RecentDeviceList>();


    public void waitBLEEnabled() {
        long count = 0;
        try {
            while (true) {
                mBluetoothManager = (BluetoothManager) mainActivity.getSystemService(mainActivity.BLUETOOTH_SERVICE);
                mBluetoothAdapter = mBluetoothManager.getAdapter();

                BluetoothManager btManager = (BluetoothManager) mainActivity.getSystemService(Context.BLUETOOTH_SERVICE);

                if (mBluetoothManager == null || mBluetoothAdapter == null || btManager == null) {

                } else {
                    BluetoothAdapter btAdapter = btManager.getAdapter();
                    if (btAdapter != null && !btAdapter.isEnabled()) {

                    } else
                        return;
                }
                Thread.sleep(100);
                count++;
                if (count>20) {
                    count=0;
                    if (mBluetoothAdapter!=null)
                        mBluetoothAdapter.enable();
                }
            }
        } catch (Throwable er) {

        }

    }



    Handler mBLERHandler = new Handler();
    public void restartBle() {
        final BluetoothManager mgr = (BluetoothManager) mainActivity.getSystemService(Context.BLUETOOTH_SERVICE);
        final BluetoothAdapter adp = mgr.getAdapter();
        if (null != adp) {
            if (adp.isEnabled()) {
                onBLEDisable();
                adp.disable();

                // TODO: display some kind of UI about restarting BLE
                mBLERHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (!adp.isEnabled()) {
                            adp.enable();

                            onBLEAvailable();
                        } else {
                            mBLERHandler.postDelayed(this, 2500);
                        }
                    }
                }, 2500);
            }
            else {
                adp.enable();

                onBLEAvailable();
            }
        }
    }


    protected void onBLEDisable() {
        synchronized (devList) {
            if (mGattServer !=null) {
                mGattServer.close();
                mGattServer = null;
            }

            while (devList.size()!=0) {
                RecentDeviceList d = devList.get(0);
                if (d.remoteGatt != null) {
                    d.remoteGatt.close();
                }

                if (d.gatt!=null)
                    d.gatt.close();
                devList.remove(0);
            }
        }


    }

    protected void onCreate() {
        ListView list = new ListView(mContext);
        mainActivity.setContentView(list);
        restartBle();
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

    private BluetoothGattCharacteristic FindServiceWithCharacteristic(BluetoothGatt gatt, UUID id) {
        List<BluetoothGattService> sList = gatt.getServices();
        BluetoothGattCharacteristic ret = null;
        for (int j=0; sList!=null && j<sList.size(); j++) {
            BluetoothGattService s = sList.get(j);

            List<BluetoothGattCharacteristic> cList = s.getCharacteristics();
            for (int k=0; cList!=null && k<cList.size(); k++) {
                BluetoothGattCharacteristic c = cList.get(k);
                //Log.i(TAG, "Service : " + s.getUuid().toString() + "Characteristic + " + c.getUuid().toString());
                if (c.getUuid().equals(CHARACTERISTIC_OFFSET_UUID))
                    ret = c;
            }
        }
        return ret;
    }

    private void PollNearbyDevices() {
        synchronized (devList) {
            final BluetoothGattCallback btleGattCallback = new BluetoothGattCallback() {

                @Override
                public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
                    Log.i(TAG, "client::onCharacteristicChanged " + characteristic.toString());
                    // this will get called anytime you perform a read or write characteristic operation
                }

                @Override
                public void onCharacteristicRead(BluetoothGatt gatt,
                                                 BluetoothGattCharacteristic characteristic,
                                                 int status) {
                    callbackPending = null;
                    byte[] val = characteristic.getValue();
                    if (val == null)
                        return;

                    String s = "";
                    for (int i = 0; i < val.length; i++) {
                        s += " " + (val[i] & 0xff);
                    }

                    RecentDeviceList d = FindDevice(gatt.getDevice().toString());
                    if (d != null) {
                        if (val.length == 16) {
                            long otherClock = bytesToLong(val, 0);
                            long otherOffset = bytesToLong(val, 8);


                            long curClock = mainActivity.GetCurrentClock();

                            long curDrift = otherClock - curClock;
                            d.AddTimeStamp(curDrift);
                        }

                        d.SetState(connectionState.CONNECTED);
                    }

                    Log.i(TAG, "client::onCharacteristicRead " + characteristic.toString() + " status " + status + ":" + s);
                }


                @Override
                public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
                    callbackPending = null;
                    try {
                        if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                            RecentDeviceList d = FindDevice(gatt.getDevice().toString());
                            if (d != null)
                                d.SetState(connectionState.NOT_CONNECTED);
                            //RemoveDevice(gatt.getDevice().toString());
                        } else if (newState == BluetoothGatt.STATE_CONNECTED) {
                            RecentDeviceList d = FindDevice(gatt.getDevice().toString());
                            if (d != null && gatt != null) {
                                d.SetState(connectionState.DISCOVER_SERVICES);
                                d.gatt = gatt;
                                d.gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH);
                            }
                        }

                        String gattStr = "null";
                        if (gatt != null)
                            gattStr = gatt.toString();

                        Log.i(TAG, "client::onConnectionStateChange " + gattStr + ": " + status + " " + newState);
                        if (status!=BluetoothGatt.GATT_SUCCESS) {
                            restartBle();
                        }

                        // this will get called when a device connects or disconnects
                    } catch (Throwable err) {
                        Log.w(TAG, "exception : " + err.toString());
                    }
                }

                @Override
                public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
                    callbackPending = null;
                    RecentDeviceList d = FindDevice(gatt.getDevice().toString());
                    if (d != null)
                        d.SetState(connectionState.CONNECTED);

                    //gatt.getServices();
                    Log.i(TAG, "client::onServicesDiscovered " + gatt.toString() + ": " + status);
                    // this will get called after the client initiates a 			BluetoothGatt.discoverServices() call
                }
            };


            String ss = "";
            //ss += "Me: " + mBluetoothManager.getAdapter().getAddress().toString() + "\n";
            //ss += "MAC: " + BluetoothAdapter.getDefaultAdapter().getAddress().toString() + "\n";
            //ss += "Me: " + mBluetoothManager.getAdapter().toString() + "\n";

            if (callbackPending != null)
                ss += "Callback pending : "  + callbackPending + "\n";

            ss += "Clients:\n";
            List<BluetoothDevice> conDev = mBluetoothManager.getConnectedDevices(BluetoothProfile.GATT_SERVER);
            for (int i = 0; i < conDev.size(); i++) {
                ss += conDev.get(i).toString() + "\n";
            }


            ss += "Servers:\n";
            long curTime = mainActivity.GetCurrentClock();


            long highest = -Long.MAX_VALUE;
            for (int i = 0; i < devList.size(); i++) {
                RecentDeviceList d = devList.get(i);
                long dh = d.GetHighest();
                if (dh > highest)
                    highest = dh;
                if (d.lastSeenTimeStamp - curTime < 30000 || d.state != connectionState.NOT_CONNECTED) {
                    ss += d.device.toString() + ":";
                    switch (d.state) {
                        case NOT_CONNECTED:
                            ss += "NOT_CNNCTD";
                            break;
                        case CONNECTING:
                            ss += "CONNECTING";
                            break;
                        case DISCOVER_SERVICES:
                            ss += "DSCVER_SVS";
                            break;
                        case DISCOVERING_SERVICES:
                            ss += "DSCING_SVS";
                            break;
                        case CONNECTED:
                            ss += "CONNECTED";
                            break;
                        case READING_CHARACTERISTIC:
                            ss += "ReadChars";
                            break;
                    }
                    ss += ":" + d.nTimeStampsRead;
                    ss += "\n";
                }
            }

            if (highest < 0 && highest != -Long.MAX_VALUE)
                mainActivity.SetServerClockOffset(highest, 0);


            mainActivity.l(null);


            // only perform one connection at a time
            int bestConnectionDev = -1;
            long bestConnectTime = Long.MAX_VALUE;
            for (int i = 0; i < devList.size(); i++) {
                RecentDeviceList d = devList.get(i);
                if (d.state == connectionState.NOT_CONNECTED) {
                    if (d.stateStarttime < bestConnectTime) {
                        bestConnectionDev = i;
                        bestConnectTime = d.stateStarttime;
                    }
                } else if (d.state == connectionState.CONNECTING) {
                    bestConnectionDev = -1;
                    bestConnectTime = 0;
                }
            }


            for (int i = 0; i < devList.size() && callbackPending!=null; i++) {

                RecentDeviceList d = devList.get(i);

                switch (d.state) {
                    case NOT_CONNECTED: {
                        if (i==bestConnectionDev) {
                            if (d.remoteGatt != null) {
                                d.remoteGatt.close();
                                d.remoteGatt = null;
                            }

                            d.SetState(connectionState.CONNECTING);
                            callbackPending = "Connecting";
                            d.remoteGatt = d.device.connectGatt(mContext, true, btleGattCallback, 2);
                            if (d.remoteGatt != null)
                                Log.i(TAG, "connectGatt, connection state =  " + mBluetoothManager.getConnectionState(d.device, BluetoothProfile.GATT_SERVER));
                            else
                                Log.i(TAG, "connectGatt returned null");
                        }
                    }
                    break;
                    case CONNECTING: {
                        if (mainActivity.GetCurrentClock() - d.stateStarttime > 5000) {
                            Log.i(TAG, "BLE Connection timed out");
                            d.SetState(connectionState.NOT_CONNECTED);
                        }
                    }
                    break;
                    case DISCOVER_SERVICES:
                        BluetoothGattCharacteristic ch = FindServiceWithCharacteristic(d.gatt, CHARACTERISTIC_OFFSET_UUID);
                        if (ch == null) {
                            callbackPending = "Discovering";
                            d.gatt.discoverServices();
                            d.SetState(connectionState.DISCOVERING_SERVICES);
                        } else
                            d.SetState(connectionState.CONNECTED);
                        break;

                    case DISCOVERING_SERVICES:
                        if (mainActivity.GetCurrentClock() - d.stateStarttime > 5000) {
                            Log.i(TAG, "DISCOVERING_SERVICES timed out");
                            d.SetState(connectionState.DISCOVER_SERVICES);
                        }
                        break;

                    case CONNECTED: {
                        BluetoothGattCharacteristic timeOffChar = FindServiceWithCharacteristic(d.gatt, CHARACTERISTIC_OFFSET_UUID);

                        if (timeOffChar != null) {
                            d.SetState(connectionState.READING_CHARACTERISTIC);
                            callbackPending = "Reading";
                            boolean result = d.gatt.readCharacteristic(timeOffChar);
                        } else {
                            d.SetState(connectionState.DISCOVER_SERVICES);
                        }
                    }
                    break;
                }
            }


            synchronized (mainActivity.bleStatus) {
                mainActivity.bleStatus = ss;
            }
        }
    }

    public void onBLEAvailable() {
        waitBLEEnabled();
        mConnectedDevices = new ArrayList<BluetoothDevice>();
        mConnectedDevicesAdapter = new ArrayAdapter<BluetoothDevice>(mContext,
                android.R.layout.simple_list_item_1, mConnectedDevices);

        /*
         * Bluetooth in Android 4.3+ is accessed via the BluetoothManager, rather than
         * the old static BluetoothAdapter.getInstance()
         */
        mBluetoothManager = (BluetoothManager) mainActivity.getSystemService(mainActivity.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();



        BluetoothManager btManager = (BluetoothManager)mainActivity.getSystemService(Context.BLUETOOTH_SERVICE);

        if (mBluetoothManager==null || mBluetoothAdapter==null || btManager==null)
            return ;


        BluetoothAdapter btAdapter = btManager.getAdapter();
        if (btAdapter != null && !btAdapter.isEnabled()) {
            Log.i(TAG, "Bluetooth not enabled!");
        }

        android.bluetooth.le.ScanCallback leScanCallback2 = new android.bluetooth.le.ScanCallback() {
            @Override
            public void	onBatchScanResults(List<ScanResult> results) {
                if (results != null) {
                    for (int i = 0; i < results.size(); i++) {
                        onScanResult(android.bluetooth.le.ScanSettings.CALLBACK_TYPE_ALL_MATCHES, results.get(i));
                    }
                }
            }

            @Override
            public void	onScanFailed(int errorCode) {
                mainActivity.l("BLE Scan failed");
            }

            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                String s2 = result.getDevice().toString();
                // see if we already know about this device
                for (int i=0; i<devList.size(); i++) {
                    String s1 = devList.get(i).device.toString();
                    if (s1.equals(s2)) {
                        devList.get(i).lastSeenTimeStamp = mainActivity.GetCurrentClock();
                        return ;
                    }
                }



                List<ParcelUuid> servIds = result.getScanRecord().getServiceUuids();
                if (servIds != null) {
                    for (int j = 0; j < servIds.size(); j++) {
                        String sv = servIds.get(j).toString();
                        mainActivity.l("Service: " + sv);
                    }
                }

                // add this device to our list
                RecentDeviceList rd = new RecentDeviceList();
                rd.lastSeenTimeStamp = mainActivity.GetCurrentClock();
                rd.device = result.getDevice();
                devList.add(rd);

                Log.i(TAG, "Found new BLE device " + s2);
            }
        };


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

        BluetoothLeScanner scanner = btAdapter.getBluetoothLeScanner();
        if  (scanner == null)
            return ;

        scanner.startScan(leScanCallback2);


        // resetting bluetooth to increase reliability
/*        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.disable();
        }
        mBluetoothAdapter.enable(); */

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

    public void onResume() {
        restartBle();

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

        BluetoothGattCharacteristic offsetCharacteristic =
                new BluetoothGattCharacteristic(CHARACTERISTIC_OFFSET_UUID,
                        //Read+write permissions
                        BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
                        BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);

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
            mainActivity.l("server::onConnectionStateChange "
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

            if (CHARACTERISTIC_OFFSET_UUID.equals(characteristic.getUuid())) {
                byte [] result = new byte[16];
                longToBytes(mainActivity.GetCurrentClock(), result, 0);
                longToBytes(mainActivity.serverTimeOffset, result, 8);

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
    };

    /*
     * Initialize the advertiser
     */
    private void startAdvertising() {
        if (mBluetoothLeAdvertiser == null) return;

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .build();

        AdvertiseData data = new AdvertiseData.Builder()
                ///.setIncludeDeviceName(true)
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
            }
        });
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

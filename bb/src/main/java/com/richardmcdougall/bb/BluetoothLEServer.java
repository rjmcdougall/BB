package com.richardmcdougall.bb;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;

import com.richardmcdougall.bbcommon.BLog;
import com.richardmcdougall.bbcommon.BoardState;

import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Created by rmc on 2/19/18.
 */

public class BluetoothLEServer {
    private String TAG = this.getClass().getSimpleName();
    private BBService service = null;

    /* Bluetooth API */
    private BluetoothManager mBluetoothManager;
    private BluetoothGattServer mBluetoothGattServer;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    /* Collection of notification subscribers */
    private Set<BluetoothDevice> mRegisteredDevices = new HashSet<>();

    private final static UUID kBurnerBoardUUID = UUID.fromString("58fdc6ee-15d1-11e8-b642-0ed5f89f718b");
    private static final UUID UART_SERVICE_UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    private static final UUID TX_CHAR_UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    private static final UUID RX_CHAR_UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");
    private static final UUID CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private BluetoothGattCharacteristic mRxCharacteristic;
    private BluetoothGattCharacteristic mTxCharacteristic;

    private int serverMtu = 18;

    private Handler mHandler;
    private boolean delay = false;

    public BluetoothLEServer(BBService service) {

        this.service = service;
        BLog.d(TAG, "Bluetooth starting");
        mHandler = new Handler(Looper.getMainLooper());
        mBluetoothManager = (BluetoothManager) service.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = mBluetoothManager.getAdapter();

        // We can't continue without proper Bluetooth support
        if (bluetoothAdapter == null) {
            BLog.d(TAG, "Bluetooth is not supported");
            return;
        }

        if (!service.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            BLog.d(TAG, "Bluetooth LE is not supported");
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            BLog.d(TAG, "Bluetooth is currently disabled...enabling");
            bluetoothAdapter.enable();
        } else {
            BLog.d(TAG, "Bluetooth enabled...starting services");
            // Start the server
            // Advertising will be called from the start server callback
        }
        startServer();

        // Register for system Bluetooth events
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        //context.registerReceiver(mBluetoothReceiver, filter);

    }// Callbacks for consumers of Bluetooth events
    private HashMap<Integer, BLECallback> callbackFuncs =
            new HashMap<Integer, BLECallback>();
    private HashMap<Integer, String> callbackCommands =
            new HashMap<>();
    private int callbackId = 0;

    // TODO: do we need per-connection contexts in callbacks?

    /**
     * Command is JSON Object with "command" as a mandatory first field
     * Response is JSON with arbitrary format
     */
    public interface BLECallback {
        void OnAction(String clientId, BluetoothDevice device, String command, JSONObject payload);
    }

    /**
     * Registers a callback for a response type
     */
    public int addCallback(String command, BLECallback newFunction) {
        int thisCallbackId = callbackId;
        if (command.length() > 0) {
            callbackFuncs.put(callbackId, newFunction);
            callbackCommands.put(callbackId, command);
        }
        callbackId++;

        return thisCallbackId;
    }

    private final HashMap<BluetoothDevice, PipedInputStream> mTransmitInput = new HashMap<>();
    private final HashMap<BluetoothDevice, PipedOutputStream> mTransmitOutput = new HashMap<>();
    private final HashMap<BluetoothDevice, ByteArrayOutputStream> mReceiveBuffers = new HashMap<>();

    public void tx(BluetoothDevice device, final byte[] data) {

        // a thread is required here because the sleep interferes with the mp4's being displayed.
        // the thread terminates immediately after completion.
        Thread t = new Thread(() -> {
                try {
                    Thread.currentThread().setName("Bluetooth TX Response.");
                    BLog.d(TAG, "Creating Thread to service Bluetooth tx response");

                    ByteArrayInputStream buffer = new ByteArrayInputStream(data);
                    byte[] txBuf = new byte[serverMtu + 2];
                    int nBytes;
                    while ((nBytes = buffer.read(txBuf, 0, serverMtu)) > 0) {
                        byte[] sendBuf = Arrays.copyOf(txBuf, nBytes);
                        mTxCharacteristic.setValue(sendBuf);

                        if(delay) {
                            // PREVENT BUFFER ISSUES / OVERFLOW
                            try {
                                Thread.sleep(15, 0);
                            } catch (Exception e) {
                            }
                        }

                        mBluetoothGattServer.notifyCharacteristicChanged(device,
                                mTxCharacteristic, false);
                    }
                } catch (Exception e) {
                    BLog.e(TAG, "Bluetooth tx response failed.");
                }
                BLog.d(TAG, "Bluetooth tx response done");

            return;

        });
        t.start();
    }/**
     * Begin advertising over Bluetooth that this device is connectable
     * and supports the Current Time Service.
     */
    private void startAdvertising() {
        BluetoothAdapter bluetoothAdapter = mBluetoothManager.getAdapter();
        mBluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        if (mBluetoothLeAdvertiser == null) {
            BLog.d(TAG, "Failed to create advertiser");
            return;
        }

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .build();

        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .setIncludeTxPowerLevel(false)
                .addServiceUuid(new ParcelUuid(kBurnerBoardUUID))
                .build();

        mBluetoothLeAdvertiser
                .startAdvertising(settings, data, mAdvertiseCallback);

        BLog.d(TAG, "LE Advertising Start");
    }

    /**
     * Stop Bluetooth advertisements.
     */
    private void stopAdvertising() {
        if (mBluetoothLeAdvertiser == null) return;

        BLog.d(TAG, "LE Advertising Stop");
        mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
    }

    /**
     * Initialize the GATT server instance with the services/characteristics
     * from the Time Profile.
     */
    private void startServer() {
        mBluetoothGattServer = mBluetoothManager.openGattServer(service, mGattServerCallback);
        if (mBluetoothGattServer == null) {
            BLog.d(TAG, "Unable to create GATT server");
            return;
        }
        // Add service to allow remote service discovery
        BluetoothGattService service = new BluetoothGattService(UART_SERVICE_UUID,
                BluetoothGattService.SERVICE_TYPE_PRIMARY);

        // Setup the rx channel
        mRxCharacteristic = new BluetoothGattCharacteristic(RX_CHAR_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_WRITE);
        service.addCharacteristic(mRxCharacteristic);
        mTxCharacteristic = new BluetoothGattCharacteristic(TX_CHAR_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ |
                        BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ);
        // Allow the remote to ask for notifications on tx
        BluetoothGattDescriptor txDesc = new BluetoothGattDescriptor(CCCD,
                BluetoothGattDescriptor.PERMISSION_READ |
                        BluetoothGattDescriptor.PERMISSION_WRITE);
        mTxCharacteristic.addDescriptor(txDesc);
        service.addCharacteristic(mTxCharacteristic);
        mBluetoothGattServer.addService(service);
    }

    /**
     * Callback to receive information about the advertisement process.
     */
    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            BLog.d(TAG, "LE Advertise Started: " + settingsInEffect.toString());
        }

        @Override
        public void onStartFailure(int errorCode) {

            BLog.e(TAG, "LE Advertise Failed: " + errorCode);
        }
    };

    /*
     * Shut down the GATT server.
     */
    private void stopServer() {

        if (mBluetoothGattServer == null)
            return;

        mBluetoothGattServer.close();
    }

    /**
     * Callback to handle incoming requests to the GATT server.
     * All read/write requests for characteristics and descriptors are handled here.
     */
    private BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {

        @Override
        public void onMtuChanged(BluetoothDevice device, int mtu)
        {
            BLog.d(TAG, "Gattserver onMtuChanged - mtu " + mtu);
        }

        @Override
        public void onServiceAdded(int service, BluetoothGattService gattService) {
            BLog.d(TAG, "Gattserver onServiceAdded");
            // We have only one service (serial), so it's save to start advertising
            startAdvertising();
        }


        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);

            if (newState == android.bluetooth.BluetoothProfile.STATE_CONNECTED) {
                //stopAdvertising();

                BLog.d(TAG, "BluetoothDevice CONNECTED: " + device);
                try {
                    // Setup tx buffer
                    if (!mTransmitInput.containsKey(device)) {
                        mTransmitInput.put(device, new PipedInputStream());
                    }
                    PipedInputStream input = mTransmitInput.get(device);
                    if (!mTransmitOutput.containsKey(device)) {
                        mTransmitOutput.put(device, new PipedOutputStream(input));
                    }
                    // Setup rx buffer
                    if (!mReceiveBuffers.containsKey(device)) {
                        mReceiveBuffers.put(device, new ByteArrayOutputStream());
                    }
                } catch (Exception e) {
                    BLog.e(TAG, "write buffer pipe failed: " + e.getMessage());
                }
            } else if (newState == android.bluetooth.BluetoothProfile.STATE_DISCONNECTED) {
                BLog.d(TAG, "BluetoothDevice DISCONNECTED: " + device);
                //Remove device from any active subscriptions
                mRegisteredDevices.remove(device);
                stopAdvertising();
                startAdvertising();
            } else {
                BLog.d(TAG, "BluetoothDevice State Change: " + device + " status: " + status +
                        " newstate:" + newState);
            }
        }

        // Fix MTU to 18 for now
        // TODO: query phy MTU
        private  final int kMTU = 18;

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset,
                                                BluetoothGattCharacteristic characteristic) {
            long now = System.currentTimeMillis();
            BLog.d(TAG, "Read characteristic...");
            if (TX_CHAR_UUID.equals(characteristic.getUuid())) {
                // We notified the client that there is Tx data available, now they are reading it
                BLog.d(TAG, "Client reads our Tx data");
                PipedInputStream TxStream = mTransmitInput.get(device);
                if (TxStream != null) {
                    try {
                        byte[] tmpBuf = new byte[kMTU];
                        int cnt = TxStream.read(tmpBuf, 0, kMTU);
                        byte[] sendBuf = Arrays.copyOf(tmpBuf, cnt);
                        mBluetoothGattServer.sendResponse(device,
                                requestId,
                                BluetoothGatt.GATT_SUCCESS,
                                0,
                                sendBuf);
                    } catch (Exception e) {
                        BLog.e(TAG, "could not read tx bytes: " + e.getMessage());
                    }
                } else {
                    BLog.d(TAG, "No tx buffer stream");
                }
            } else {
                BLog.d(TAG, "read of undefined characteristic: " + characteristic.getUuid());
            }
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device,
                                                 int requestId,
                                                 BluetoothGattCharacteristic characteristic,
                                                 boolean preparedWrite,
                                                 boolean responseNeeded,
                                                 int offset,
                                                 byte[] value) {
            BLog.d(TAG, "Write characteristic...");
            if (RX_CHAR_UUID.equals(characteristic.getUuid())) {
                BLog.d(TAG, "Client wrote us Rx data: " + new String(value));
                // Acknowledge or they will disconnect
                if (responseNeeded) {
                    mBluetoothGattServer.sendResponse(device,
                            requestId,
                            BluetoothGatt.GATT_SUCCESS,
                            0,
                            null);
                }
                processReceive(device, value);
            } else {
                // Invalid characteristic
                BLog.d(TAG, "Invalid Characteristic Write: " + characteristic.getUuid());
                mBluetoothGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_FAILURE,
                        0,
                        null);
            }
        }@Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset,
                                            BluetoothGattDescriptor descriptor) {
            if (CCCD.equals(descriptor.getUuid())) {
                BLog.d(TAG, "Tx descriptor read, offset: " + offset);
                byte[] returnValue;
                if (mRegisteredDevices.contains(device)) {
                    returnValue = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;
                    mRegisteredDevices.remove(device);
                } else {
                    returnValue = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
                    mRegisteredDevices.add(device);
                }
                mBluetoothGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        returnValue);
            } else {
                BLog.d(TAG, "Unknown descriptor read request");
                mBluetoothGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_FAILURE,
                        0,
                        null);
            }
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId,
                                             BluetoothGattDescriptor descriptor,
                                             boolean preparedWrite, boolean responseNeeded,
                                             int offset, byte[] value) {
            BLog.d(TAG, "Tx descriptor write " + descriptor.toString() + ", offset: " + offset + ", " + new String(value));

            if (CCCD.equals(descriptor.getUuid())) {
                byte[] returnValue;
                if (mRegisteredDevices.contains(device)) {
                    returnValue = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;
                    mRegisteredDevices.remove(device);
                } else {
                    returnValue = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
                    mRegisteredDevices.add(device);
                }
                mBluetoothGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        returnValue);
            } else {
                BLog.d(TAG, "Unknown descriptor write request");
                mBluetoothGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_FAILURE,
                        0,
                        null);
            }
        }// Process stream fragments and delimeters
        boolean processReceive(BluetoothDevice device, byte[] bytes) {

            boolean success = false;
            ByteArrayOutputStream rxBuffer = mReceiveBuffers.get(device);
            if (rxBuffer == null) {
                BLog.d(TAG, "No rx buffer");
                return false;
            }
            for (byte oneChar : bytes) {
                if ((oneChar == ';')) {
                    // Execute complete command
                    JSONObject cmd;
                    if ((cmd = extractCommand(rxBuffer.toString())) != null) {
                        success = processCommand(device, cmd);
                    }
                    rxBuffer.reset();

                } else {
                    // Write to command buffer
                    try {
                        rxBuffer.write(oneChar);
                    } catch (Exception e) {
                        BLog.e(TAG, "could not write bytes: " + e.getMessage());
                    }
                }
            }
            return success;
        }

        // Checks and extracts the commmand in JSON format from the string
        JSONObject extractCommand(String cmd) {
            JSONObject command = null;
            BLog.d(TAG, "Received JSON: <" + cmd + ">");
            try {
                command = new JSONObject(cmd);
            } catch (Exception e) {
                BLog.e(TAG, "Could not parse message");
                return null;
            }
            if (!command.has("command")) {
                BLog.d(TAG, "Command has no command field");
                return null;
            }
            BLog.d(TAG, "Got command: " + command);
            return (command);
        }

        // Run the callbacks
        // TODO: do we run these on the calling thread, or a separate thread?
        boolean processCommand(BluetoothDevice device, JSONObject cmdJson) {

            BLog.d(TAG, "callbackFuncs = " + callbackFuncs.toString());
            BLog.d(TAG, "callbackCommands = " + callbackCommands.toString());

            Iterator it = callbackCommands.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry callbackCmd = (Map.Entry) it.next();
                BLog.d(TAG, "checking callback: " + callbackCmd.getKey() + " = " + callbackCmd.getValue());
                try {
                    String thisCallbackCmd = (String) callbackCmd.getValue();
                    int thisCallbackId = (int) callbackCmd.getKey();
                    String requestedCommand = cmdJson.getString("command");

                    if (requestedCommand.equals(thisCallbackCmd)) {
                        BLECallback bcb = callbackFuncs.get(thisCallbackId);
                        if (bcb == null) {
                            BLog.d(TAG, "Could not find callback for id: " + thisCallbackId);
                            return false;
                        }
                        runOnServiceThread(() -> bcb.OnAction("test",
                                device, requestedCommand, cmdJson));
                    }
                } catch (Exception e) {
                    BLog.e(TAG, "error on bluetooth command: " + e.getMessage());
                    return false;
                }
                //it.remove(); // avoids a ConcurrentModificationException
            }
            return true;
        }

        private void runOnServiceThread(final Runnable runnable) {
            if (Looper.myLooper() != Looper.getMainLooper()) {
                mHandler.post(runnable);
            } else {
                runnable.run();
            }
        }
    };
};

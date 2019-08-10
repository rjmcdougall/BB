package com.richardmcdougall.bb;

import android.app.Activity;
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
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Looper;
import android.os.ParcelUuid;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

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
import android.os.Handler;

/**
 * Created by rmc on 2/19/18.
 */

public class BluetoothLEServer {
    private static final String TAG = "BB.BluetoothLEServer";
    public Context mContext = null;
    public BBService mBBService = null;
    String mBoardId;
    private BluetoothConnManager mBluetoothConnManager = null;


    /* Bluetooth API */
    private BluetoothManager mBluetoothManager;
    private BluetoothGattServer mBluetoothGattServer;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    /* Collection of notification subscribers */
    private Set<BluetoothDevice> mRegisteredDevices = new HashSet<>();


    public final static UUID kBurnerBoardUUID =
            UUID.fromString("58fdc6ee-15d1-11e8-b642-0ed5f89f718b");

    public static final UUID UART_SERVICE_UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID TX_CHAR_UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID RX_CHAR_UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private BluetoothGattCharacteristic mRxCharacteristic;
    private BluetoothGattCharacteristic mTxCharacteristic;

    private Handler mHandler;

    public BluetoothLEServer(BBService service, Context context) {

    mBBService = service;
    mContext = context;
    mHandler = new Handler(Looper.getMainLooper());
    mBoardId = service.getBoardId();
    mBluetoothConnManager = service.mBluetoothConnManager;
    mBluetoothManager = (BluetoothManager) service.getSystemService(Context.BLUETOOTH_SERVICE);
    BluetoothAdapter bluetoothAdapter = mBluetoothManager.getAdapter();

        // We can't continue without proper Bluetooth support
        if (bluetoothAdapter == null) {
            l("Bluetooth is not supported");
            return;
        }

        if (!service.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            l("Bluetooth LE is not supported");
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Log.d(TAG, "Bluetooth is currently disabled...enabling");
            bluetoothAdapter.enable();
        } else {
            l("Bluetooth enabled...starting services");
            // Start the server
            // Advertising will be called from the start server callback
        }
        startServer();

        // Register for system Bluetooth events
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        //context.registerReceiver(mBluetoothReceiver, filter);

    }


    // Callbacks for consumers of Bluetooth events
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
        void onConnected(String clientId);

        void onDisconnected(String clientId);

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

    private final HashMap<BluetoothDevice, PipedInputStream> mTransmitInput = new HashMap<>();
    private final HashMap<BluetoothDevice, PipedOutputStream> mTransmitOutput = new HashMap<>();
    private final HashMap<BluetoothDevice, ByteArrayOutputStream> mReceiveBuffers = new HashMap<>();

   public void tx(BluetoothDevice device, final byte[] data) {

       // a thread is required here because the sleep interferes with the mp4's being displayed.
       // the thread terminates immediately after completion.
       Thread t = new Thread(new Runnable() {
           public void run() {
               try {
                   Thread.currentThread().setName("Bluetooth TX Response.");
                   l("Creating Thread to service Bluetooth tx response");

                   ByteArrayInputStream buffer = new ByteArrayInputStream(data);
                   byte [] txBuf = new byte[20];
                   int nBytes;
                   while ((nBytes = buffer.read(txBuf,0, 18)) > 0) {
                       byte[] sendBuf = Arrays.copyOf(txBuf, nBytes);
                       mTxCharacteristic.setValue(sendBuf);
                       //l("Notifying...");

                       // PREVENT BUFFER ISSUES / OVERFLOW
                       try {
                           Thread.sleep(15,0);
                       } catch (Exception e) {
                       }

                       boolean status = mBluetoothGattServer.notifyCharacteristicChanged(device,
                               mTxCharacteristic, false);
                       //l("notify: " + status);
                   }
               }
               catch(Exception e){
                   l("Bluetooth tx response failed.");
               }
               return;
           }
       });
       t.start();
    }


    /**
     * Begin advertising over Bluetooth that this device is connectable
     * and supports the Current Time Service.
     */
    private void startAdvertising() {
        BluetoothAdapter bluetoothAdapter = mBluetoothManager.getAdapter();
        mBluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        if (mBluetoothLeAdvertiser == null) {
            l("Failed to create advertiser");
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
    }

    /**
     * Stop Bluetooth advertisements.
     */
    private void stopAdvertising() {
        if (mBluetoothLeAdvertiser == null) return;

        mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
    }

    /**
     * Initialize the GATT server instance with the services/characteristics
     * from the Time Profile.
     */
    private void startServer() {
        mBluetoothGattServer = mBluetoothManager.openGattServer(mBBService, mGattServerCallback);
        if (mBluetoothGattServer == null) {
            l("Unable to create GATT server");
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
            l("LE Advertise Started: " + settingsInEffect.toString());
        }

        @Override
        public void onStartFailure(int errorCode) {

            l("LE Advertise Failed: " + errorCode);
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
        public void onServiceAdded(int service, BluetoothGattService gattService) {
            l("Gattserver onServiceAdded");
            // We have only one service (serial), so it's save to start advertising
            startAdvertising();
        }

        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
            if (newState == android.bluetooth.BluetoothProfile.STATE_CONNECTED) {
                l("BluetoothDevice CONNECTED: " + device);
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
                    l("write buffer pipe failed: " + e.getMessage());
                }
            } else if (newState == android.bluetooth.BluetoothProfile.STATE_DISCONNECTED) {
                l("BluetoothDevice DISCONNECTED: " + device);
                //Remove device from any active subscriptions
                mRegisteredDevices.remove(device);
                stopAdvertising();
                startAdvertising();
            } else {
                l("BluetoothDevice State Change: " + device + " status: " + status +
                        " newstate:" + newState);
            }
        }

        // Fix MTU to 18 for now
        // TODO: query phy MTU
        private static final int kMTU = 18;

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset,
                                                BluetoothGattCharacteristic characteristic) {
            long now = System.currentTimeMillis();
            l("Read characteristic...");
            if (TX_CHAR_UUID.equals(characteristic.getUuid())) {
                // We notified the client that there is Tx data available, now they are reading it
                l("Client reads our Tx data");
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
                        l("could not read tx bytes: " + e.getMessage());
                    }
                } else {
                    l("No tx buffer stream");
                }
            } else {
                l("read of undefined characteristic: " + characteristic.getUuid());
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
            l("Write characteristic...");
            if (RX_CHAR_UUID.equals(characteristic.getUuid())) {
                l("Client wrote us Rx data: " + new String(value));
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
                l("Invalid Characteristic Write: " + characteristic.getUuid());
                mBluetoothGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_FAILURE,
                        0,
                        null);
            }
        }


        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset,
                                            BluetoothGattDescriptor descriptor) {
            if (CCCD.equals(descriptor.getUuid())) {
                Log.d(TAG, "Tx descriptor read, offset: " + offset);
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
                l("Unknown descriptor read request");
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
            Log.d(TAG, "Tx descriptor write " + descriptor.toString() + ", offset: " + offset + ", " + new String(value));

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
                l("Unknown descriptor write request");
                mBluetoothGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_FAILURE,
                        0,
                        null);
            }
        }


        // Process stream fragments and delimeters
        boolean processReceive(BluetoothDevice device, byte[] bytes) {

            boolean success = false;
            ByteArrayOutputStream rxBuffer = mReceiveBuffers.get(device);
            if (rxBuffer == null) {
                l("No rx buffer");
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
                        l("could not write bytes: " + e.getMessage());
                    }
                }
            }
            return success;
        }

        // Checks and extracts the commmand in JSON format from the string
        JSONObject extractCommand(String cmd) {
            JSONObject command = null;
            l("Received JSON: <" + cmd + ">");
            try {
                command = new JSONObject(cmd);
            } catch (Exception e) {
                l("Could not parse message");
                return null;
            }
            if (!command.has("command")) {
                l("Command has no command field");
                return null;
            }
            l("Got command: " + command);
            return (command);
        }

        // Run the callbacks
        // TODO: do we run these on the calling thread, or a separate thread?
        boolean processCommand(BluetoothDevice device, JSONObject cmdJson) {

            l("callbackFuncs = " + callbackFuncs.toString());
            l("callbackCommands = " + callbackCommands.toString());

            Iterator it = callbackCommands.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry callbackCmd = (Map.Entry) it.next();
                l("checking callback: " + callbackCmd.getKey() + " = " + callbackCmd.getValue());
                try {
                    String thisCallbackCmd = (String) callbackCmd.getValue();
                    int thisCallbackId = (int) callbackCmd.getKey();
                    String requestedCommand = cmdJson.getString("command");

                    if (requestedCommand.equals(thisCallbackCmd)) {
                        BLECallback bcb = callbackFuncs.get(thisCallbackId);
                        if (bcb == null) {
                            l("Could not find callback for id: " + thisCallbackId);
                            return false;
                        }
                        runOnServiceThread(() -> bcb.OnAction("test",
                                device, requestedCommand, cmdJson));
                    }
                } catch (Exception e) {
                    l("error on bluetooth command: " + e.getMessage());
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

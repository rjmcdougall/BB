package com.richardmcdougall.bb;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.ParcelUuid;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.KeyEvent;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;


/**
 * Created by rmc on 2/19/18.
 */

public class BluetoothLEServer {
    private static final String TAG = "BB.BluetoothLEServer";
    public Context mContext = null;
    public BBService mBBService = null;
    String mBoardId;
    private BluetoothRemote mBluetoothRemote = null;

    private HashMap<BluetoothDevice, Integer> mAudioInfoSelect = new HashMap<>();
    private HashMap<BluetoothDevice, Integer> mVideoInfoSelect = new HashMap<>();
    private HashMap<BluetoothDevice, Integer> mBTInfoSelect = new HashMap<>();

    /* Bluetooth API */
    private BluetoothManager mBluetoothManager;
    private BluetoothGattServer mBluetoothGattServer;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    /* Collection of notification subscribers */
    private Set<BluetoothDevice> mRegisteredDevices = new HashSet<>();

    public final static UUID kBurnerBoardUUID =
            UUID.fromString("58fdc6ee-15d1-11e8-b642-0ed5f89f718b");

    public BluetoothLEServer(BBService service, Context context) {

        mBBService = service;
        mContext = context;

        mBoardId = service.getBoardId();

        mBluetoothRemote = service.mBluetoothRemote;

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

        bluetoothAdapter.setName(mBoardId.substring(0, Math.min(mBoardId.length(), 8)));

        // Register for system Bluetooth events
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        context.registerReceiver(mBluetoothReceiver, filter);
        if (!bluetoothAdapter.isEnabled()) {
            Log.d(TAG, "Bluetooth is currently disabled...enabling");
            bluetoothAdapter.enable();
        } else {
            l("Bluetooth enabled...starting services");
            startAdvertising();
            startServer();

        }

        // Register to receive button messages
        filter = new IntentFilter(BBService.ACTION_BB_VOLUME);
        LocalBroadcastManager.getInstance(service).registerReceiver(mBBEventReciever, filter);
        filter = new IntentFilter(BBService.ACTION_BB_AUDIOCHANNEL);
        LocalBroadcastManager.getInstance(service).registerReceiver(mBBEventReciever, filter);
        filter = new IntentFilter(BBService.ACTION_BB_VIDEOMODE);
        LocalBroadcastManager.getInstance(service).registerReceiver(mBBEventReciever, filter);
        filter = new IntentFilter(BBService.ACTION_BB_LOCATION);
        LocalBroadcastManager.getInstance(service).registerReceiver(mBBEventReciever, filter);
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

        BluetoothProfile profile = new BluetoothProfile();

        mBluetoothGattServer.addService(BluetoothProfile.createBBLocationService());
        try {
            Thread.sleep(100);
        } catch (Exception e) {

        }
        mBluetoothGattServer.addService(profile.createBBBatteryService());
        try {
            Thread.sleep(100);
        } catch (Exception e) {

        }
        mBluetoothGattServer.addService(profile.createBBAudioService());
        try {
            Thread.sleep(100);
        } catch (Exception e) {

        }
        mBluetoothGattServer.addService(profile.createBBVideoService());
        try {
            Thread.sleep(100);
        } catch (Exception e) {

        }
        mBluetoothGattServer.addService(profile.createBBBtdeviceService());
    }

    /**
     * Callback to receive information about the advertisement process.
     */
    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            l("LE Advertise Started.");
        }

        @Override
        public void onStartFailure(int errorCode) {

            l("LE Advertise Failed: "+ errorCode);
        }
    };

    /**
     * Send a location service notification to any devices that are subscribed
     * to the characteristic.
     */
    private void notifyRegisteredDevices(long timestamp, byte adjustReason) {
        if (mRegisteredDevices.isEmpty()) {
            l("No subscribers registered");
            return;
        }
        byte[] bbLocation = BluetoothProfile.getLocation(mBBService.getFindMyFriends());

        l("Sending update to " + mRegisteredDevices.size() + " subscribers");
        for (BluetoothDevice device : mRegisteredDevices) {
            BluetoothGattCharacteristic locationCharacteristic = mBluetoothGattServer
                    .getService(BluetoothProfile.BB_LOCATION_SERVICE)
                    .getCharacteristic(BluetoothProfile.BB_LOCATION_CHARACTERISTIC);
            locationCharacteristic.setValue(bbLocation);
            mBluetoothGattServer.notifyCharacteristicChanged(device, locationCharacteristic, false);
        }
    }

    /*
     * Shut down the GATT server.
     */
    private void stopServer() {

        if (mBluetoothGattServer == null)
            return;

        mBluetoothGattServer.close();
    }

    /**
     * Listens for Bluetooth adapter events to enable/disable
     * advertising and server functionality.
     */
    private BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);

            switch (state) {
                case BluetoothAdapter.STATE_ON:
                    startAdvertising();
                    startServer();
                    break;
                case BluetoothAdapter.STATE_OFF:
                    stopServer();
                    stopAdvertising();
                    break;
                default:
                    // Do nothing
            }
        }
    };

    /**
     * Callback to handle incoming requests to the GATT server.
     * All read/write requests for characteristics and descriptors are handled here.
     */
    private BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {

        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
            if (newState == android.bluetooth.BluetoothProfile.STATE_CONNECTED) {
                l("BluetoothDevice CONNECTED: " + device);
            } else if (newState == android.bluetooth.BluetoothProfile.STATE_DISCONNECTED) {
                l("BluetoothDevice DISCONNECTED: " + device);
                //Remove device from any active subscriptions
                mRegisteredDevices.remove(device);
            }
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset,
                                                BluetoothGattCharacteristic characteristic) {

            long now = System.currentTimeMillis();
            if (BluetoothProfile.BB_LOCATION_CHARACTERISTIC.equals(characteristic.getUuid())) {
                l("Read location characteristic");
                byte[] loc = BluetoothProfile.getLocation(mBBService.getFindMyFriends());
                if (loc != null) {
                    mBluetoothGattServer.sendResponse(device,
                            requestId,
                            BluetoothGatt.GATT_SUCCESS,
                            0,
                            loc);
                } else {
                    l("null location");
                    mBluetoothGattServer.sendResponse(device,
                            requestId,
                            BluetoothGatt.GATT_FAILURE,
                            0,
                            null);
                }
            } else if (BluetoothProfile.BB_AUDIO_CHANNEL_SELECT_CHARACTERISTIC.equals(characteristic.getUuid())) {
                l("Read audio channel characteristic");
                byte[] audioChannel = new byte[] {0, 0};
                audioChannel[0] = (byte) mBBService.getRadioChannelMax();
                audioChannel[1] = (byte) mBBService.getRadioChannel();
                mBluetoothGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        audioChannel);
            } else if (BluetoothProfile.BB_VIDEO_CHANNEL_SELECT_CHARACTERISTIC.equals(characteristic.getUuid())) {
                l("Read video mode characteristic");
                byte[] videoChannel = new byte[] {0, 0};
                videoChannel[0] = (byte) mBBService.getVideoMax();
                videoChannel[1] = (byte) mBBService.getVideoMode();
                mBluetoothGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        videoChannel);
            }  else if (BluetoothProfile.BB_BTDEVICE_SELECT_CHARACTERISTIC.equals(characteristic.getUuid())) {
                l("Read BTDevice select characteristic");
                byte[] BTDevices = new byte[] {0, 0};
                BTDevices[0] = (byte) mBluetoothRemote.getDeviceCount();
                BTDevices[1] = (byte) 0;
                mBluetoothGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        BTDevices);
            } else if (BluetoothProfile.BB_AUDIO_VOLUME_CHARACTERISTIC.equals(characteristic.getUuid())) {
                l("Read audio volume characteristic");
                byte[] audioChannel = new byte[]{0, 0};
                audioChannel[0] = (byte) mBBService.getBoardVolumePercent();
                mBluetoothGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        audioChannel);
            } else if (BluetoothProfile.BB_AUDIO_INFO_CHARACTERISTIC.equals(characteristic.getUuid())) {
                l("Read audio info characteristic: " + requestId);
                int infoselect = 1;
                if (mAudioInfoSelect.containsKey(device)) {
                    infoselect = mAudioInfoSelect.get(device).intValue();
                } else {
                    l("audioinfo select key was missing");
                }
                byte[] audioInfo = BluetoothProfile.getAudioInfo(mBBService, infoselect);
                if (audioInfo[0] == 0) {
                    infoselect = 1;
                    audioInfo = BluetoothProfile.getAudioInfo(mBBService, infoselect);
                }
                l("Read audio info characteristic: " + infoselect);
                mBluetoothGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        audioInfo);
                infoselect++;
                mAudioInfoSelect.put(device, infoselect);
            }  else if (BluetoothProfile.BB_BTDEVICE_INFO_CHARACTERISTIC.equals(characteristic.getUuid())) {
                l("Read btinfo info characteristic request <" + requestId + ">");
                int infoselect = 0;
                if (mBTInfoSelect.containsKey(device)) {
                    infoselect = mBTInfoSelect.get(device).intValue();
                } else {
                    l("btinfo device not known; adding entry");
                }
                byte[] btInfo = BluetoothProfile.getBtdeviceInfo(mBluetoothRemote, infoselect);
                if (btInfo[0] == 0) {
                    infoselect = 0;
                    btInfo = BluetoothProfile.getBtdeviceInfo(mBluetoothRemote, infoselect);
                }
                l("Read BT info characteristic: " + infoselect);
                mBluetoothGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        btInfo);
                infoselect++;
                mBTInfoSelect.put(device, infoselect);
            }  else if (BluetoothProfile.BB_VIDEO_INFO_CHARACTERISTIC.equals(characteristic.getUuid())) {
                l("Read video info characteristic");
                int infoselect = 1;
                if (mVideoInfoSelect.containsKey(device)) {
                    infoselect = mVideoInfoSelect.get(device).intValue();
                } else {
                    l("videoinfo select key was missing");
                }
                byte[] videoInfo = BluetoothProfile.getVideoInfo(mBBService, infoselect);
                if (videoInfo[0] == 0) {
                    infoselect = 1;
                    videoInfo = BluetoothProfile.getVideoInfo(mBBService, infoselect);
                }
                l("Read video info characteristic: " + infoselect);
                mBluetoothGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        videoInfo);
                infoselect++;
                mVideoInfoSelect.put(device, infoselect);
            } else if (BluetoothProfile.BB_BATTERY_CHARACTERISTIC.equals(characteristic.getUuid())) {
                l("Read battery characteristic");
                byte[] battery = BluetoothProfile.getBatteryInfo(mBBService);
                mBluetoothGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        battery);
            } else {
                // Invalid characteristic
                l("Invalid Characteristic Read: " + characteristic.getUuid());
                mBluetoothGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_FAILURE,
                        0,
                        null);
            }
        }

        @Override
        public void onCharacteristicWriteRequest (BluetoothDevice device,
                                           int requestId,
                                           BluetoothGattCharacteristic characteristic,
                                           boolean preparedWrite,
                                           boolean responseNeeded,
                                           int offset,
                                           byte[] value) {


            long now = System.currentTimeMillis();
            l("Write characteristic...");
            if (BluetoothProfile.BB_AUDIO_CHANNEL_SELECT_CHARACTERISTIC.equals(characteristic.getUuid())) {
                int channel = (int) (value[0] & 0xFF);
                l("Write audio track characteristic: " + channel);
                // Set board
                // Broadcast to other boards if audio-leader
                if (true) {
                    mBBService.rfClientServer.sendRemote(RFClientServer.kRemoteAudioTrack, channel);
                }
                try {
                    Thread.sleep(mBBService.rfClientServer.getLatency());
                } catch (Exception e) {
                }
                mBBService.SetRadioChannel(channel);
                if (responseNeeded) {
                    mBluetoothGattServer.sendResponse(device,
                            requestId,
                            BluetoothGatt.GATT_SUCCESS,
                            0,
                            null);
                }
            } else if (BluetoothProfile.BB_VIDEO_CHANNEL_SELECT_CHARACTERISTIC.equals(characteristic.getUuid())) {
                int channel = (int) (value[0] & 0xFF);
                l("Write video track characteristic: " + channel);
                mBBService.setVideoMode(channel);
                if (responseNeeded) {
                    mBluetoothGattServer.sendResponse(device,
                            requestId,
                            BluetoothGatt.GATT_SUCCESS,
                            0,
                            null);
                }
            }  else if (BluetoothProfile.BB_AUDIO_VOLUME_CHARACTERISTIC.equals(characteristic.getUuid())) {
                int volume = (int) (value[0] & 0xFF);
                l("Write audio volume characteristic: " + volume);
                mBBService.setRadioVolumePercent(volume);
                if (responseNeeded) {
                    mBluetoothGattServer.sendResponse(device,
                            requestId,
                            BluetoothGatt.GATT_SUCCESS,
                            0,
                            null);
                }
            } else if (BluetoothProfile.BB_AUDIO_INFO_CHARACTERISTIC.equals(characteristic.getUuid())) {
                int audioinfoselect = (int) (value[0] & 0xFF);
                mAudioInfoSelect.put(device, audioinfoselect);

                l("Write audio info characteristic: " + mAudioInfoSelect + " needs reply: " + responseNeeded);
                if (responseNeeded) {
                    mBluetoothGattServer.sendResponse(device,
                            requestId,
                            BluetoothGatt.GATT_SUCCESS,
                            0,
                            null);
                }
            }  else if (BluetoothProfile.BB_VIDEO_INFO_CHARACTERISTIC.equals(characteristic.getUuid())) {
                int videoinfoselect = (int) (value[0] & 0xFF);
                mVideoInfoSelect.put(device, videoinfoselect);

                l("Write video info characteristic: " + mVideoInfoSelect + " needs reply: " + responseNeeded);
                if (responseNeeded) {
                    mBluetoothGattServer.sendResponse(device,
                            requestId,
                            BluetoothGatt.GATT_SUCCESS,
                            0,
                            null);
                }
            }  else if (BluetoothProfile.BB_BTDEVICE_INFO_CHARACTERISTIC.equals(characteristic.getUuid())) {

                l("Write bt info characteristic: " + mVideoInfoSelect + " needs reply: " + responseNeeded);
                if (mBluetoothRemote != null) {
                    mBluetoothRemote.discoverDevices();
                }

                if (responseNeeded) {
                    mBluetoothGattServer.sendResponse(device,
                            requestId,
                            BluetoothGatt.GATT_SUCCESS,
                            0,
                            null);
                }
            } else if (BluetoothProfile.BB_BTDEVICE_SELECT_CHARACTERISTIC.equals(characteristic.getUuid())) {
                String address = new String(value);
                l("Write bt device select characteristic: " + address);
                mBluetoothRemote.pairDevice(address);
                if (responseNeeded) {
                    mBluetoothGattServer.sendResponse(device,
                            requestId,
                            BluetoothGatt.GATT_SUCCESS,
                            0,
                            null);
                }
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
            if (BluetoothProfile.BB_AUDIO_DESCRIPTOR.equals(descriptor.getUuid())) {
                Log.d(TAG, "Audio Config descriptor read, offset: " + offset);
                byte[] returnValue;
                if (mRegisteredDevices.contains(device)) {
                    returnValue = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
                } else {
                    returnValue = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;
                }
                mBluetoothGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_FAILURE,
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

            if (BluetoothProfile.BB_AUDIO_DESCRIPTOR.equals(descriptor.getUuid())) {
                if (Arrays.equals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, value)) {
                    Log.d(TAG, "Subscribe device to notifications: " + device);
                    mRegisteredDevices.add(device);
                } else if (Arrays.equals(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE, value)) {
                    Log.d(TAG, "Unsubscribe device from notifications: " + device);
                    mRegisteredDevices.remove(device);
                }

                if (responseNeeded) {
                    mBluetoothGattServer.sendResponse(device,
                            requestId,
                            BluetoothGatt.GATT_SUCCESS,
                            0,
                            null);
                }
            } else {
                l("Unknown descriptor write request");
                if (responseNeeded) {
                    mBluetoothGattServer.sendResponse(device,
                            requestId,
                            BluetoothGatt.GATT_FAILURE,
                            0,
                            null);
                }
            }
        }
    };

    // We use this to catch the board events
    private final BroadcastReceiver mBBEventReciever = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            final String TAG = "mBBEventReciever";

            Log.d(TAG, "onReceive entered");
            String action = intent.getAction();

            if (BBService.ACTION_BB_VOLUME.equals(action)) {
                Log.d(TAG, "Got volume");
                float volume = (float) intent.getSerializableExtra("volume");

                }
            }
        };
    };

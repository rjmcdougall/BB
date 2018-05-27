package com.richardmcdougall.bb;

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.util.UUID;

/**
 * Created by rmc on 2/19/18.
 */

public class BluetoothProfile {

    private static final String TAG = "BB.BluetoothProfile";

    int mCurrentAudioChannel = 0;
    int mCurrentVideoMode = 0;
    int mCurrentBtDeviceMode = 0;

    public static void l(String s) {
        Log.v(TAG, s);
    }

    /* Current Time Service UUID */
    public static UUID BB_LOCATION_SERVICE = UUID.fromString("03c21568-159a-11e8-b642-0ed5f89f718b");
    public static UUID BB_LOCATION_CHARACTERISTIC = UUID.fromString("03c2193c-159a-11e8-b642-0ed5f89f718b");
    public static UUID BB_LOCATION_DESCRIPTOR = UUID.fromString("03c21a90-159a-11e8-b642-0ed5f89f718b");

    /**
     * Return a configured {@link BluetoothGattService} instance for the
     * Location Service.
     */
    public static BluetoothGattService createBBLocationService() {
        BluetoothGattService service = new BluetoothGattService(BB_LOCATION_SERVICE,
                BluetoothGattService.SERVICE_TYPE_PRIMARY);

        // Current Time characteristic
        BluetoothGattCharacteristic bbLocation = new BluetoothGattCharacteristic(BB_LOCATION_CHARACTERISTIC,
                // Read-only characteristic, supports notifications
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ);

        //bbLocation.addDescriptor(configDescriptor);
        service.addCharacteristic(bbLocation);

        return service;
    }

    /**
     * Construct the field values for a BB Locationcharacteristic
     * from the given epoch timestamp and adjustment reason.
     */
    public static byte[] getLocation(FindMyFriends fmf) {

        byte[] field = fmf.getRecentLocation();
        return field;
    }

    public static UUID BB_AUDIO_SERVICE = UUID.fromString("89239614-1937-11e8-accf-0ed5f89f718b");
    public static UUID BB_AUDIO_INFO_CHARACTERISTIC = UUID.fromString("892398a8-1937-11e8-accf-0ed5f89f718b");
    public static UUID BB_AUDIO_CHANNEL_SELECT_CHARACTERISTIC = UUID.fromString("892399e8-1937-11e8-accf-0ed5f89f718b");
    public static UUID BB_AUDIO_VOLUME_CHARACTERISTIC = UUID.fromString("59629212-1938-11e8-accf-0ed5f89f718b");
    public static UUID BB_AUDIO_DESCRIPTOR = UUID.fromString("89239b0a-1937-11e8-accf-0ed5f89f718b");
    /**
     * Return a configured {@link BluetoothGattService} instance for the
     * Audio Service
     */
    public BluetoothGattService createBBAudioService() {


        BluetoothGattService service = new BluetoothGattService(BB_AUDIO_SERVICE,
                BluetoothGattService.SERVICE_TYPE_PRIMARY);

        // Audio Channel Info
        BluetoothGattCharacteristic bbAudioInfo = new BluetoothGattCharacteristic(BB_AUDIO_INFO_CHARACTERISTIC,
                //Read-only characteristic, supports notifications
                BluetoothGattCharacteristic.PROPERTY_READ |
                        BluetoothGattCharacteristic.PROPERTY_WRITE |
                        BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ |
                        BluetoothGattCharacteristic.PERMISSION_WRITE);

        BluetoothGattDescriptor bbAudioInfoconfigDescriptor = new BluetoothGattDescriptor(BB_AUDIO_DESCRIPTOR,
                //Read/write descriptor
                BluetoothGattDescriptor.PERMISSION_READ |
                        BluetoothGattDescriptor.PERMISSION_WRITE);
        bbAudioInfo.addDescriptor(bbAudioInfoconfigDescriptor);

        // Audio Channel Select
        BluetoothGattCharacteristic bbAudioChannelSelect = new BluetoothGattCharacteristic(BB_AUDIO_CHANNEL_SELECT_CHARACTERISTIC,
                //Read-only characteristic, supports notifications
                BluetoothGattCharacteristic.PROPERTY_READ |
                        BluetoothGattCharacteristic.PROPERTY_WRITE |
                        BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ |
                        BluetoothGattCharacteristic.PERMISSION_WRITE);

        BluetoothGattDescriptor bbAudioChannelSelectconfigDescriptor = new BluetoothGattDescriptor(BB_AUDIO_DESCRIPTOR,
                //Read/write descriptor
                BluetoothGattDescriptor.PERMISSION_READ |
                        BluetoothGattDescriptor.PERMISSION_WRITE);

        bbAudioChannelSelect.addDescriptor(bbAudioChannelSelectconfigDescriptor);

        // Volume
        BluetoothGattCharacteristic bbAudioVolume = new BluetoothGattCharacteristic(BB_AUDIO_VOLUME_CHARACTERISTIC,
                //Read-only characteristic, supports notifications
                BluetoothGattCharacteristic.PROPERTY_READ |
                        BluetoothGattCharacteristic.PROPERTY_WRITE |
                        BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ |
                        BluetoothGattCharacteristic.PERMISSION_WRITE);

        BluetoothGattDescriptor bbAudioVolumeconfigDescriptor = new BluetoothGattDescriptor(BB_AUDIO_DESCRIPTOR,
                //Read/write descriptor
                BluetoothGattDescriptor.PERMISSION_READ |
                        BluetoothGattDescriptor.PERMISSION_WRITE);
        bbAudioVolume.addDescriptor(bbAudioVolumeconfigDescriptor);

        service.addCharacteristic(bbAudioInfo);
        service.addCharacteristic(bbAudioChannelSelect);
        service.addCharacteristic(bbAudioVolume);

        return service;
    }


    /**
     * Construct the field values for a BB_AUDIO_DESCRIPTOR
     * from the given epoch timestamp and adjustment reason.
     */
    public void setAudioDescriptor(byte[] value) {

        mCurrentAudioChannel = value[0] & 0xff;;
    }

    /**
     * Construct the field values for a BB_AUDIO_DESCRIPTOR
     */
    public static byte[] getAudioDescriptor(BBService service) {

        byte[] field = {(byte)service.GetMaxAudioModes()};
        return field;
    }

    /**
     * Construct the field values for a BB_AUDIO_DESCRIPTOR
     */
    public static byte[] getAudioInfo(BBService service, int channel) {


        // If 0, then return the count of slots
        if (channel == 0) {
            return new byte[] {0, (byte)service.getRadioChannelMax()};
        }
        String name = service.getRadioChannelInfo(channel);
    /* Test
        if (channel == 1) {
            name = "fancy track 1";
        }
        if (channel == 2) {
            name = "fancy track 2";
        }
        */

        // Else return the slot name
        if (name != null) {
            byte[] field = name.getBytes();
            int fieldlength = java.lang.Math.min(18, field.length);
            byte[] response = new byte[fieldlength + 1];
            response[0] = (byte)channel;
            System.arraycopy(field, 0, response, 1, fieldlength);
            return response;
        } else {
            return new byte[] {0};
        }
    }


    /**
     * Construct the field values for a BB Locationcharacteristic
     */
    public static byte[] getAudioChannel(BBService service) {

        byte[] field = {(byte)service.getRadioChannel()};
        return field;
    }

    /**
     * Set the audio stream
     */
    public void setAudioChannel(BBService service, byte[] value) {

        service.SetRadioChannel(value[0] & 0xff);
    }


    public static UUID BB_VIDEO_SERVICE = UUID.fromString("89239614-9937-11e8-accf-0ed5f89f718b");
    public static UUID BB_VIDEO_INFO_CHARACTERISTIC = UUID.fromString("892398a8-9937-11e8-accf-0ed5f89f718b");
    public static UUID BB_VIDEO_CHANNEL_SELECT_CHARACTERISTIC = UUID.fromString("892399e8-9937-11e8-accf-0ed5f89f718b");
    public static UUID BB_VIDEO_DESCRIPTOR = UUID.fromString("89239b0a-9937-11e8-accf-0ed5f89f718b");
    /**
     * Return a configured {@link BluetoothGattService} instance for the
     * Video Track Service
     */
    public BluetoothGattService createBBVideoService() {


        BluetoothGattService service = new BluetoothGattService(BB_VIDEO_SERVICE,
                BluetoothGattService.SERVICE_TYPE_PRIMARY);

        // Video Channel Info
        BluetoothGattCharacteristic bbVideoInfo = new BluetoothGattCharacteristic(BB_VIDEO_INFO_CHARACTERISTIC,
                //Read-only characteristic, supports notifications
                BluetoothGattCharacteristic.PROPERTY_READ |
                        BluetoothGattCharacteristic.PROPERTY_WRITE |
                        BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ |
                        BluetoothGattCharacteristic.PERMISSION_WRITE);

        BluetoothGattDescriptor bbVideoInfoconfigDescriptor = new BluetoothGattDescriptor(BB_VIDEO_DESCRIPTOR,
                //Read/write descriptor
                BluetoothGattDescriptor.PERMISSION_READ |
                        BluetoothGattDescriptor.PERMISSION_WRITE);
        bbVideoInfo.addDescriptor(bbVideoInfoconfigDescriptor);

        // Video Channel Select
        BluetoothGattCharacteristic bbVideoModeSelect = new BluetoothGattCharacteristic(BB_VIDEO_CHANNEL_SELECT_CHARACTERISTIC,
                //Read-only characteristic, supports notifications
                BluetoothGattCharacteristic.PROPERTY_READ |
                        BluetoothGattCharacteristic.PROPERTY_WRITE |
                        BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ |
                        BluetoothGattCharacteristic.PERMISSION_WRITE);

        BluetoothGattDescriptor bbVideoModeSelectconfigDescriptor = new BluetoothGattDescriptor(BB_VIDEO_DESCRIPTOR,
                //Read/write descriptor
                BluetoothGattDescriptor.PERMISSION_READ |
                        BluetoothGattDescriptor.PERMISSION_WRITE);

        bbVideoModeSelect.addDescriptor(bbVideoModeSelectconfigDescriptor);


        service.addCharacteristic(bbVideoInfo);
        service.addCharacteristic(bbVideoModeSelect);

        return service;
    }


    /**
     * Construct the field values for a BB_VIDEO_DESCRIPTOR
     * from the given epoch timestamp and adjustment reason.
     */
    public void setVideoDescriptor(byte[] value) {

        mCurrentVideoMode = value[0] & 0xff;;
    }

    /**
     * Construct the field values for a BB_VIDEO_DESCRIPTOR
    public static byte[] getVideoDescriptor(BBService service) {

        byte[] field = {(byte)service.GetMaxVideoModes()};
        return field;
    }
     */

    /**
     * Construct the field values for a BB_VIDEO_DESCRIPTOR
     */
    public static byte[] getVideoInfo(BBService service, int channel) {


        // If 0, then return the count of slots
        if (channel == 0) {
            return new byte[] {0, (byte)service.getVideoMax()};
        }

        String name = service.getVideoModeInfo(channel);

        // Else return the slot name
        if (name != null) {
            byte[] field = name.getBytes();
            int fieldlength = java.lang.Math.min(18, field.length);
            byte[] response = new byte[fieldlength + 1];
            response[0] = (byte)channel;
            System.arraycopy(field, 0, response, 1, fieldlength);
            return response;
        } else {
            return new byte[] {0};
        }
    }


    /**
     * Construct the field values for a BB Locationcharacteristic
     */
    public static byte[] getVideoMode(BBService service) {

        byte[] field = {(byte)service.getVideoMode()};
        return field;
    }

    /**
     * Set the video stream */
    public void setVideoMode(BBService service, byte[] value) {

        service.setVideoMode(value[0] & 0xff);
    }



    public static UUID BB_BATTERY_SERVICE = UUID.fromString("4dfc5ef6-22a9-11e8-b467-0ed5f89f718b");
    public static UUID BB_BATTERY_CHARACTERISTIC = UUID.fromString("4dfc6194-22a9-11e8-b467-0ed5f89f718b");
    public static UUID BB_BATTERY_DESCRIPTOR = UUID.fromString("4dfc6194-22a9-1ae8-b467-0ed5f89f718b");

    /**
     * Return a configured {@link BluetoothGattService} instance for the
     * Battery Service.
     */
        public BluetoothGattService createBBBatteryService() {

        BluetoothGattService service = new BluetoothGattService(BB_BATTERY_SERVICE,
                BluetoothGattService.SERVICE_TYPE_PRIMARY);

        // Current Time characteristic
        BluetoothGattCharacteristic bbBatteryInfo = new BluetoothGattCharacteristic(BB_BATTERY_CHARACTERISTIC,
                //Read-only characteristic, supports notifications
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ);

        BluetoothGattDescriptor   configDescriptor = new BluetoothGattDescriptor(BB_BATTERY_DESCRIPTOR,
                    //Read/write descriptor
                    BluetoothGattDescriptor.PERMISSION_READ |
                            BluetoothGattDescriptor.PERMISSION_WRITE);

        bbBatteryInfo.addDescriptor(configDescriptor);
        service.addCharacteristic(bbBatteryInfo);

        return service;
    }

    /**
     * Construct the field values for a BB BB_BATTERY_CHARACTERISTIC
     */
    public static byte[] getBatteryInfo(BBService service) {

        byte[] field = {(byte)service.getBatteryLevel()};

        return field;
    }


   public static UUID BB_BTDEVICE_SERVICE = UUID.fromString("89239614-8937-11e8-accf-0ed5f89f718b");
    public static UUID BB_BTDEVICE_INFO_CHARACTERISTIC = UUID.fromString("892398a8-8937-11e8-accf-0ed5f89f718b");
    public static UUID BB_BTDEVICE_SELECT_CHARACTERISTIC = UUID.fromString("892399e8-8937-11e8-accf-0ed5f89f718b");
    public static UUID BB_BTDEVICE_DESCRIPTOR = UUID.fromString("89239b0a-8937-11e8-accf-0ed5f89f718b");
    /**
     * Return a configured {@link BluetoothGattService} instance for the
     * Bluetooth Device Service
     */
    public BluetoothGattService createBBBtdeviceService() {


        BluetoothGattService service = new BluetoothGattService(BB_BTDEVICE_SERVICE,
                BluetoothGattService.SERVICE_TYPE_PRIMARY);

        // Btdevice Channel Info
        BluetoothGattCharacteristic bbBtdeviceInfo = new BluetoothGattCharacteristic(BB_BTDEVICE_INFO_CHARACTERISTIC,
                //Read-only characteristic, supports notifications
                BluetoothGattCharacteristic.PROPERTY_READ |
                        BluetoothGattCharacteristic.PROPERTY_WRITE |
                        BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ |
                        BluetoothGattCharacteristic.PERMISSION_WRITE);

        BluetoothGattDescriptor bbBtdeviceInfoconfigDescriptor = new BluetoothGattDescriptor(BB_BTDEVICE_DESCRIPTOR,
                //Read/write descriptor
                BluetoothGattDescriptor.PERMISSION_READ |
                        BluetoothGattDescriptor.PERMISSION_WRITE);
        bbBtdeviceInfo.addDescriptor(bbBtdeviceInfoconfigDescriptor);

        // Btdevice Channel Select
        BluetoothGattCharacteristic bbBtdeviceModeSelect = new BluetoothGattCharacteristic(BB_BTDEVICE_SELECT_CHARACTERISTIC,
                //Read-only characteristic, supports notifications
                BluetoothGattCharacteristic.PROPERTY_READ |
                        BluetoothGattCharacteristic.PROPERTY_WRITE |
                        BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ |
                        BluetoothGattCharacteristic.PERMISSION_WRITE);

        BluetoothGattDescriptor bbBtdeviceModeSelectconfigDescriptor = new BluetoothGattDescriptor(BB_BTDEVICE_DESCRIPTOR,
                //Read/write descriptor
                BluetoothGattDescriptor.PERMISSION_READ |
                        BluetoothGattDescriptor.PERMISSION_WRITE);

        bbBtdeviceModeSelect.addDescriptor(bbBtdeviceModeSelectconfigDescriptor);


        service.addCharacteristic(bbBtdeviceInfo);
        service.addCharacteristic(bbBtdeviceModeSelect);

        return service;
    }


    /**
     * Construct the field values for a BB_BTDEVICE_DESCRIPTOR
     * from the given epoch timestamp and adjustment reason.
     */
    public void setBtdeviceDescriptor(byte[] value) {

        mCurrentBtDeviceMode = value[0] & 0xff;;
    }

    /**
     * Construct the field values for a BB_BTDEVICE_DESCRIPTOR
     public static byte[] getBtdeviceDescriptor(BBService service) {

     byte[] field = {(byte)service.GetMaxBtdeviceModes()};
     return field;
     }
     */

    /**
     * Construct the field values for a BB_BTDEVICE_DESCRIPTOR
     *
     * returns:
     *      byte 0: device number
     *      byte 1: total number of devices
     *      byte 2: byte-array of string of address
     */
    public static byte[] getBtdeviceInfo(BluetoothRemote remote, int deviceNo) {

        String address = remote.getDeviceAddress(deviceNo);
        int numDevices = remote.getDeviceCount();

        if ((deviceNo >= numDevices) || (address == null)) {
            return new byte[] {0};
        }

        byte[] field = address.getBytes();
        int fieldlength = java.lang.Math.min(18, field.length);
        byte[] response = new byte[fieldlength + 2];
        response[0] = (byte)deviceNo;
        response[1] = (byte)numDevices;
        System.arraycopy(field, 0, response, 2, fieldlength);
        return response;
    }


    /**
     * Construct the field values for a BB Locationcharacteristic
     */
    public static byte[] getBtdeviceMode(BBService service) {

        //byte[] field = {(byte)service.getBtdeviceMode()};
        //return field;
        return null;
    }

    /**
     * Set the btdevice stream */
    public void setBtdeviceMode(BBService service, byte[] value) {

        //service.setBtdeviceMode(value[0] & 0xff);
    }

    public static UUID BB_AUDIOSYNC_SERVICE = UUID.fromString("89279614-8937-11e8-accf-0ed5f89f718b");
    public static UUID BB_AUDIOSYNC_STATS_CHARACTERISTIC =  UUID.fromString("892799e8-8937-11e8-accf-0ed5f89f718b");
    public static UUID BB_AUDIOSYNC_REMOTE_CHARACTERISTIC = UUID.fromString("892799e8-8937-11e8-abcf-0ed5f89f718b");

    /**
     * Return a configured {@link BluetoothGattService} instance for the
     * Audio Sync Service
     */
    public BluetoothGattService createBBAudioSyncService() {

        BluetoothGattService service = new BluetoothGattService(BB_AUDIOSYNC_SERVICE,
                BluetoothGattService.SERVICE_TYPE_PRIMARY);

        // Audio Sync Stats
        BluetoothGattCharacteristic bbAudioSyncStats = new BluetoothGattCharacteristic(BB_AUDIOSYNC_STATS_CHARACTERISTIC,
                //Read-only characteristic, supports notifications
                BluetoothGattCharacteristic.PROPERTY_READ |
                        BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ);

        // Audio Sync Global Remote Control
        BluetoothGattCharacteristic bbAudioSyncRemote = new BluetoothGattCharacteristic(BB_AUDIOSYNC_REMOTE_CHARACTERISTIC,
                //Read-only characteristic, supports notifications
                BluetoothGattCharacteristic.PROPERTY_READ |
                        BluetoothGattCharacteristic.PROPERTY_WRITE |
                        BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ |
                        BluetoothGattCharacteristic.PERMISSION_WRITE);

        service.addCharacteristic(bbAudioSyncStats);
        service.addCharacteristic(bbAudioSyncRemote);

        return service;
    }

    // Stats:
    // 0-2: current master address
    // 3-4: round trip latency
    /**
     * Construct the field values for a BB Locationcharacteristic
     * from the given epoch timestamp and adjustment reason.
     */
    public static byte[] getAudioSyncStats(BBService service) {

        byte[] field = service.getAudioSyncStats();
        return field;
    }

}

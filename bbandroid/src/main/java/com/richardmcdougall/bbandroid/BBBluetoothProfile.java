package com.richardmcdougall.bbandroid;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import java.util.UUID;

/**
 * Created by rmc on 2/19/18.
 */

public class BBBluetoothProfile {

    /* Current Time Service UUID */
    public static UUID BB_LOCATION_SERVICE = UUID.fromString("03c21568-159a-11e8-b642-0ed5f89f718b");
    public static UUID BB_LOCATION_CHARACTERISTIC = UUID.fromString("03c2193c-159a-11e8-b642-0ed5f89f718b");
    public static UUID BB_LOCATION_DESCRIPTOR = UUID.fromString("03c21a90-159a-11e8-b642-0ed5f89f718b");
    public static UUID BB_CONFIG = UUID.fromString("03c21db0-159a-11e8-b642-0ed5f89f718b");

    public static UUID BB_AUDIO_SERVICE = UUID.fromString("89239614-1937-11e8-accf-0ed5f89f718b");
    public static UUID BB_AUDIO_INFO_CHARACTERISTIC = UUID.fromString("892398a8-1937-11e8-accf-0ed5f89f718b");
    public static UUID BB_AUDIO_CHANNEL_SELECT_CHARACTERISTIC = UUID.fromString("892399e8-1937-11e8-accf-0ed5f89f718b");
    public static UUID BB_AUDIO_VOLUME_CHARACTERISTIC = UUID.fromString("59629212-1938-11e8-accf-0ed5f89f718b");
    public static UUID BB_AUDIO_DESCRIPTOR = UUID.fromString("89239b0a-1937-11e8-accf-0ed5f89f718b");

    public static UUID BB_BATTERY_SERVICE = UUID.fromString("4dfc5ef6-22a9-11e8-b467-0ed5f89f718b");
    public static UUID BB_BATTERY_CHARACTERISTIC = UUID.fromString("4dfc6194-22a9-11e8-b467-0ed5f89f718b");

}

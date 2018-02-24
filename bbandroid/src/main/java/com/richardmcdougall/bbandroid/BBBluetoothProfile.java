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

    /**
     * Return a configured {@link BluetoothGattService} instance for the
     * Current Time Service.
     */
    public static BluetoothGattService createBBLocationService() {
        BluetoothGattService service = new BluetoothGattService(BB_LOCATION_SERVICE,
                BluetoothGattService.SERVICE_TYPE_PRIMARY);

        // Current Time characteristic
        BluetoothGattCharacteristic bbLocation = new BluetoothGattCharacteristic(BB_LOCATION_CHARACTERISTIC,
                //Read-only characteristic, supports notifications
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ);

        BluetoothGattDescriptor configDescriptor = new BluetoothGattDescriptor(BB_LOCATION_DESCRIPTOR,
                //Read/write descriptor
                BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);

        bbLocation.addDescriptor(configDescriptor);

        service.addCharacteristic(bbLocation);

        return service;
    }
}

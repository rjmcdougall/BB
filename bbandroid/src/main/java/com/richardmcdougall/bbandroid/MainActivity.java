package com.richardmcdougall.bbandroid;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.android.gms.maps.GoogleMap;
import android.support.v4.app.FragmentActivity;

/*
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
*/
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.richardmcdougall.bbandroid.R;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final String TAG = "BBAndroid.MainActivity";

    BackgroundThread mLocationCHeckerThread = null;
    private boolean mLocationCheckerRunning = false;

    BluetoothManager btManager;
    BluetoothAdapter btAdapter;
    BluetoothLeScanner btScanner;
    TextView peripheralTextView;
    private Spinner mBoardspinner;
    private final static int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;


    private GoogleMap mGoogleMap = null;
    private MarkerOptions options = new MarkerOptions();
    private HashMap<String, Marker> mMarkers = new HashMap<>();

    Boolean btScanning = false;
    int deviceIndex = 0;
    //ArrayList<BluetoothDevice> devicesDiscovered = new ArrayList<BluetoothDevice>();
    private HashMap<String, BluetoothDevice> devicesDiscovered = new HashMap<>();
    EditText deviceIndexInput;
    BluetoothGatt bluetoothGatt = null;
    String mDeviceSelected = null;

    BluetoothGattService mLocationService;
    BluetoothGattCharacteristic mLocationCharacteristic;

    public final static UUID kBurnerBoardUUID =
            UUID.fromString("58fdc6ee-15d1-11e8-b642-0ed5f89f718b");

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";

    public Map<String, String> uuids = new HashMap<String, String>();

    // Stops scanning after 5 seconds.
    private Handler mHandler = new Handler();
    private static final long SCAN_PERIOD = 5000;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    //private GoogleApiClient client;

    GPSLocation mGPSlocation = null;

    private void l(String msg) {
        Log.v(TAG, msg);
    }

    @Override
    protected void onPause() {

        super.onPause();
        l("MainActivity: onPause()");
        stopScanning();
    }

    @Override
    protected void onResume() {

        super.onResume();
        l("MainActivity: onResume()");
        startScanning();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mBoardspinner = (Spinner) findViewById(R.id.spinner);
        String [] array_spinner = new String[] {"Searching..."};
        // Populate dropdown
        ArrayAdapter<String> adapter =
                new ArrayAdapter<String> (MainActivity.this,
                        android.R.layout.simple_spinner_item,array_spinner);
        mBoardspinner.setAdapter(adapter);
        mBoardspinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int pos, long id) {
                // An item was selected. You can retrieve the selected item using
                l("board selected: " + parent.getItemAtPosition(pos));
                if (parent.getItemAtPosition(pos).equals("Searching...")) {
                    return;
                }
                connectToDeviceSelected(parent.getItemAtPosition(pos).toString());
            }

            public void onNothingSelected(AdapterView<?> parent) {
                // Another interface callback
                l("No board selected");
            }
        });

        peripheralTextView = (TextView) findViewById(R.id.PeripheralTextView);
        peripheralTextView.setMovementMethod(new ScrollingMovementMethod());
        deviceIndexInput = (EditText) findViewById(R.id.InputIndex);
        deviceIndexInput.setText("0");

        btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();
        btScanner = btAdapter.getBluetoothLeScanner();

        if (btAdapter != null && !btAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }

        // Make sure we have access coarse location enabled, if not, prompt the user to enable it
        if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("This app needs location access");
            builder.setMessage("Please grant location access so this app can detect peripherals.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                }
            });
            builder.show();
        }

        //client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    // Device scan callback.
    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            List<ParcelUuid> suids = result.getScanRecord().getServiceUuids();
            if ((suids != null) && (kBurnerBoardUUID.compareTo(suids.get(0).getUuid()) == 0)) {
                //peripheralTextView.append("addr: " + result.getDevice().getAddress() + ", Device Name: " + result.getDevice().getName() + " " + result.getDevice() + " rssi: " + result.getRssi() + "\n");
                l("Scanned Index: " + deviceIndex + ", Device Name: " + result.getDevice().getName() +
                        " " + result.getScanRecord().getServiceUuids() +
                        " rssi: " + result.getRssi() + "\n");
                String name = result.getDevice().getName();
                if (name == null) {
                    name = result.getDevice().toString();
                }
                if (!devicesDiscovered.containsKey(name)) {
                    l("Adding Device map Index: " + deviceIndex + ", Device Name: " + result.getDevice().getName() +
                            " " + result.getScanRecord().getServiceUuids() +
                            " rssi: " + result.getRssi() + "\n");
                    devicesDiscovered.put(name, result.getDevice());
                    String [] array_spinner = devicesDiscovered.keySet().toArray(new String[0]);
                    // Populate dropdown
                    ArrayAdapter<String> adapter =
                            new ArrayAdapter<String> (MainActivity.this,
                                    android.R.layout.simple_spinner_item,array_spinner);
                    mBoardspinner.setAdapter(adapter);
                }
                // auto scroll for text view
                final int scrollAmount = peripheralTextView.getLayout().getLineTop(peripheralTextView.getLineCount()) - peripheralTextView.getHeight();
                // if there is no need to scroll, scrollAmount will be <=0
                if (scrollAmount > 0) {
                    peripheralTextView.scrollTo(0, scrollAmount);
                }
            } else {
                    l("Rejected: " + deviceIndex + ", Device Name: " + result.getDevice().getName() +
                            " " + result.getScanRecord().getServiceUuids() +
                            " rssi: " + result.getRssi() + "\n");
            }
        }
    };

    // Device connect call back
    private final BluetoothGattCallback btleGattCallback = new BluetoothGattCallback() {

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            // this will get called anytime you perform a read or write characteristic operation
            l("characteristic changed");
            MainActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    peripheralTextView.append("device read or wrote to\n");
                }
            });
        }

        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
            // this will get called when a device connects or disconnects
            System.out.println(newState);
            switch (newState) {
                case 0:
                    l("device disconnected\n");
                    mLocationCharacteristic = null;
                    MainActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            peripheralTextView.append("device disconnected\n");
                        }
                    });
                    bluetoothGatt = null;
                    break;
                case 2:
                    l("device connected\n");
                    MainActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            peripheralTextView.append("device connected\n");
                        }
                    });

                    // discover services and characteristics for this device
                    bluetoothGatt.discoverServices();

                    break;
                default:
                    l("we encounterned an unknown state, uh oh\n");
                    MainActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            peripheralTextView.append("we encounterned an unknown state, uh oh\n");
                        }
                    });
                    break;
            }
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
            // this will get called after the client initiates a 			BluetoothGatt.discoverServices() call
            l("device services have been discovered\n");
            MainActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    peripheralTextView.append("device services have been discovered\n");
                }
            });
            extractGattServices(bluetoothGatt.getServices());

        }

        @Override
        // Result of a characteristic read operation
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            l("characteristics read");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            } else {
                l("characteristics read failed");
            }
        }
    };

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {



        l("broadcast update: " + characteristic.getUuid().toString());
        if (BBBluetoothProfile.BB_LOCATION_CHARACTERISTIC.equals(characteristic.getUuid())) {
            l("read succeeded BB_LOCATION_CHARACTERISTIC!!!");
            mGPSlocation = new GPSLocation(characteristic.getValue());

            MainActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    peripheralTextView.append("LAT = " + mGPSlocation.mTheirLat + " , LON = " + mGPSlocation.mTheirLon + "\n");
                }
            });

            l("LAT = " + mGPSlocation.mTheirLat + " , LON = " + mGPSlocation.mTheirLon);
            setMarker(mGPSlocation.mTheirAddress, mGPSlocation.mTheirLat, mGPSlocation.mTheirLon,
                    String.valueOf(mGPSlocation.mTheirAddress), "bb", 1);

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    l("coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    l("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }

                    });
                    builder.show();
                }
                return;
            }
        }
    }

    public void startScanning() {
        l("start scanning");
        btScanning = true;
        deviceIndex = 0;
        devicesDiscovered.clear();
        peripheralTextView.setText("");
        peripheralTextView.append("Started Scanning\n");


        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                List<ScanFilter> filters = new ArrayList<>();
                filters.add(new ScanFilter.Builder().setServiceUuid(new ParcelUuid(kBurnerBoardUUID)).build());
                btScanner.startScan(filters, new ScanSettings.Builder().build(), leScanCallback);
            }
        });

        //mHandler.postDelayed(new Runnable() {
        //    @Override
        //    public void run() {
        //        stopScanning();
        //   }
        //}, SCAN_PERIOD);
    }

    public void stopScanning() {
        l("stopping scanning");
        peripheralTextView.append("Stopped Scanning\n");
        btScanning = false;
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                btScanner.stopScan(leScanCallback);
            }
        });
    }

    public void connectToDeviceSelected(String deviceSelected) {
        peripheralTextView.append("Trying to connect to device at index: " + deviceIndexInput.getText() + "\n");
        l("Trying to connect to device at index: " + deviceIndexInput.getText() + "\n");
        Integer.parseInt(deviceIndexInput.getText().toString());
        try {
            bluetoothGatt = devicesDiscovered.get(deviceSelected).connectGatt(this, false, btleGattCallback);
            mDeviceSelected = deviceSelected;
        } catch (Exception e) {
            bluetoothGatt = null;
            l("Connect failed");
        }
    }

    public void disconnectDeviceSelected() {
        peripheralTextView.append("Disconnecting from device\n");
        l("Disconnecting from device\n");
        bluetoothGatt.disconnect();
    }

    private void extractGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {

            final String uuid = gattService.getUuid().toString();
            System.out.println("Service discovered: " + uuid);
            l("Service disovered: " + uuid + "\n");
            MainActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    peripheralTextView.append("Service disovered: " + uuid + "\n");
                }
            });

            if (BBBluetoothProfile.BB_LOCATION_SERVICE.equals(gattService.getUuid())) {
                l("Found BB_LOCATION_SERVICE!!!");
                mLocationService = gattService;
                new ArrayList<HashMap<String, String>>();
                List<BluetoothGattCharacteristic> gattCharacteristics =
                        gattService.getCharacteristics();

                // Loops through available Characteristics.
                for (BluetoothGattCharacteristic gattCharacteristic :
                        gattCharacteristics) {

                    final String charUuid = gattCharacteristic.getUuid().toString();
                    l("Characteristic discovered for service: " + charUuid);
                    if (BBBluetoothProfile.BB_LOCATION_CHARACTERISTIC.equals(gattCharacteristic.getUuid())) {
                        l("Found BB_LOCATION_CHARACTERISTIC!!!");
                        mLocationCharacteristic = gattCharacteristic;
                    }

                }
            }


        }
    }

    @Override
    public void onStart() {
        super.onStart();

        mLocationCheckerRunning = true;
        mLocationCHeckerThread = new BackgroundThread();
        Thread th = new Thread(mLocationCHeckerThread, "Location Checker Thread");
        th.start();

        /*
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.example.joelwasserman.androidbleconnectexample/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
        */
    }

    @Override
    public void onStop() {
        super.onStop();

        mLocationCheckerRunning = false;
        try {
            mLocationCHeckerThread.wait();
        } catch (Exception e) {

        }

        /*

        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.example.joelwasserman.androidbleconnectexample/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
        */
    }

    // Check locations from Board BLE every few seconds -- triggers callback
    private class BackgroundThread implements Runnable {
        @Override
        public void run() {
            while (mLocationCheckerRunning == true) {
                try {
                    if ((bluetoothGatt == null) && (mDeviceSelected != null)) {
                        l("Reconnecting to device " + mDeviceSelected);
                        connectToDeviceSelected(mDeviceSelected);
                    }
                    if (mLocationCharacteristic != null) {
                        l("characteristic.readCharacteristic()...");
                        bluetoothGatt.readCharacteristic(mLocationCharacteristic);
                    }

                    Thread.sleep(5000);
                } catch (Exception e) {
                    l("characteristic.getValue() failed");
                }
            }
        }
    }

    static final int kMagicNumberLen = 2;
    static final int kTTL = 1;
    static final int[] kTrackerMagicNumber = new int[]{0x02, 0xcb};
    static final int[] kGPSMagicNumber = new int[]{0xbb, 0x01};

    private class GPSLocation {

        private int mTheirAddress;
        private double mTheirLat;
        private double mTheirLon;
        private double mTheirAlt;
        private int mThereAccurate;

        public GPSLocation(byte[] packet) {

            if (packet.length == 0) {
                l("zero length packet");
            } else {
                l("packet byte 0 " + packet[0] + " length " + packet.length);
            }
            ByteArrayInputStream bytes = new ByteArrayInputStream(packet);


            int recvMagicNumber = magicNumberToInt(
                    new int[]{bytes.read(), bytes.read()});

            if (recvMagicNumber == magicNumberToInt(kGPSMagicNumber)) {
                l("BB GPS Packet");
                mTheirAddress = (int) ((bytes.read() & 0xff) +
                        ((bytes.read() & 0xff) << 8));
                int ttl = bytes.read();
                mTheirLat = (double) ((bytes.read() & 0xff) +
                        ((bytes.read() & 0xff) << 8) +
                        ((bytes.read() & 0xff) << 16) +
                        ((bytes.read() & 0xff) << 24)) / 1000000.0;
                mTheirLon = (double) ((bytes.read() & 0xff) +
                        ((bytes.read() & 0xff) << 8) +
                        ((bytes.read() & 0xff) << 16) +
                        ((bytes.read() & 0xff) << 24)) / 1000000.0;
                mTheirAlt = (double) ((bytes.read() & 0xff) +
                        ((bytes.read() & 0xff) << 8) +
                        ((bytes.read() & 0xff) << 16) +
                        ((bytes.read() & 0xff) << 24)) / 1000000.0;
                mThereAccurate = bytes.read();
            } else if (recvMagicNumber == magicNumberToInt(kTrackerMagicNumber)) {
                l("tracker packet");
                mTheirLat = (double) ((bytes.read() & 0xff) +
                        ((bytes.read() & 0xff) << 8) +
                        ((bytes.read() & 0xff) << 16) +
                        ((bytes.read() & 0xff) << 24)) / 1000000.0;
                mTheirLon = (double) ((bytes.read() & 0xff) +
                        ((bytes.read() & 0xff) << 8) +
                        ((bytes.read() & 0xff) << 16) +
                        ((bytes.read() & 0xff) << 24)) / 1000000.0;
                mThereAccurate = bytes.read();
            } else {
                l("rogue bluetooth packet not for us!");
            }
        }

        private final int magicNumberToInt(int[] magic) {
            int magicNumber = 0;
            for (int i = 0; i < kMagicNumberLen; i++) {
                magicNumber = magicNumber + (magic[i] << ((kMagicNumberLen - 1 - i) * 8));
            }
            return (magicNumber);
        }


    }



    private Marker setMarker(int id, double latitude, double longitude, String title, String snippet, int iconResID) {

        final int mId = id;
        final double mLat = latitude;
        final double mLon = longitude;

        l("Setmarker(" + id + "," + latitude + "," + longitude);

        if (mGoogleMap == null) {
            l("null map!");
            return null;
        }


        MainActivity.this.runOnUiThread(new Runnable() {
            public void run() {
                String key = String.valueOf(mId);

                LatLng location = new LatLng(mLat, mLon);
                mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(location));

                if (!mMarkers.containsKey(key)) {
                    mMarkers.put(key, mGoogleMap.addMarker(new MarkerOptions().title(key).position(location)));
                } else {
                    mMarkers.get(key).setPosition(location);
                }


                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                for (Marker marker : mMarkers.values()) {
                    builder.include(marker.getPosition());
                }
                //l("mMarkers: " + mMarkers.toString());
                //mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 300));
            }
        });

        /*

        mGoogleMap.addMarker(new MarkerOptions()
                .position(new LatLng(latitude, longitude))
                .anchor(0.5f, 0.5f)
                .title(title)
                .snippet(snippet)
                .icon(BitmapDescriptorFactory.fromResource(iconResID)));
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(latitude, longitude)));
        */
        return null;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
        mGoogleMap.setMaxZoomPreference(30);
        mGoogleMap.setMinZoomPreference(13);



        LatLng burningman = new LatLng(40.7864646,-119.20888);
        mGoogleMap.addMarker(new MarkerOptions().position(burningman).title("Marker in Sydney"));
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(burningman));
        mGoogleMap.animateCamera(CameraUpdateFactory.zoomTo(17));

    }

}
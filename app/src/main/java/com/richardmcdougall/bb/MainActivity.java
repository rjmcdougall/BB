// jc: testing git commit with update

package com.richardmcdougall.bb;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.richardmcdougall.bb.InputManagerCompat;
import com.richardmcdougall.bb.InputManagerCompat.InputDeviceListener;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.ArrayList;
import android.media.AudioManager;
import android.media.MediaPlayer;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import android.os.Environment;
import 	java.io.OutputStream;
import java.io.FileOutputStream;
import java.util.Calendar;
import android.os.ParcelUuid;
import java.util.*;
import java.nio.charset.Charset;
import android.bluetooth.le.*;
import android.bluetooth.*;
import android.text.TextUtils;
import java.nio.*;
import android.content.*;
import java.io.*;
import java.net.*;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.app.ProgressDialog;

public class MainActivity extends AppCompatActivity implements InputDeviceListener {

    TextView voltage;
    TextView status;
    EditText log;
    ProgressBar pb;

    private static UsbSerialPort sPort = null;
    private static UsbSerialDriver mDriver = null;
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private SerialInputOutputManager mSerialIoManager;
    private BBListenerAdapter mListener = null;
    private Handler mBLEHandler = new Handler();
    private Context mContext;
    protected static final String GET_USB_PERMISSION = "GetUsbPermission";
    private static final String TAG = "BB.MainActivity";
    private InputManagerCompat remoteControl;
    private android.widget.Switch switchHeadlight;
    private MediaPlayer mediaPlayer = new MediaPlayer();
    private float vol = 0.02f;
    private BluetoothLeScanner mBluetoothLeScanner;
    private Handler mHandler = new Handler();
    private boolean downloaded = false;
    private int timeOffset = 0;

    int work = 0;

    int doWork() {
        work++;
        try {
            Thread.sleep(10);
        } catch (Exception e) {
            System.out.println(e);
        }
        return (work);
    }

    private Handler pHandler = new Handler();
    int ProgressStatus;





    protected void TimeTest() {
        //Declare the timer
        Timer t = new Timer();
        //Set the schedule function and rate
        t.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String tm = Long.toString(Calendar.getInstance().getTimeInMillis());
                        log.setText(tm);
                        //l(tm);
                    }

                });
            }

        }, 0, 16);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //BLETest();



        super.onCreate(savedInstanceState);
        mContext = getApplicationContext();
        DownloadMusic();
        setContentView(R.layout.activity_main);

        remoteControl = InputManagerCompat.Factory.getInputManager(getApplicationContext());
        remoteControl.registerInputDeviceListener(this, null);

        voltage = (TextView) findViewById(R.id.textViewVoltage);
        status = (TextView) findViewById(R.id.textViewStatus);
        log = (EditText) findViewById(R.id.editTextLog);
        voltage.setText("0.0v");
        log.setText("Hello");
        log.setFocusable(false);

        pb = (ProgressBar) findViewById(R.id.progressBar);
        pb.setMax(100);

        switchHeadlight = (android.widget.Switch) findViewById(R.id.switchHeadlight);
        switchHeadlight.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked == true) {
                    sendCommand("2,1;");
                } else {
                    sendCommand("2,0;");
                }
                // Start lengthy operation in a background thread
                work = 0;
                pb.setProgress(0);
                new Thread(new Runnable() {
                    public void run() {
                        ProgressStatus = 0;
                        while (ProgressStatus < 100) {
                            ProgressStatus = doWork();
                            // Update the progress bar
                            pHandler.post(new Runnable() {
                                public void run() {
                                    pb.setProgress(ProgressStatus);
                                }
                            });
                        }
                    }
                }).start();

            }
        });
        //TimeTest();
        if (downloaded)
            RadioMode();

    }

    @Override
    protected void onResume() {
        super.onResume();

//        loadPrefs();

        initUsb();


    }

    @Override
    protected void onPause() {
        super.onPause();
        mHandler.removeCallbacksAndMessages(null);

//        savePrefs();

        stopIoManager();

        if (sPort != null) {
            try {
                sPort.close();
            } catch (IOException e) {
                // Ignore.
            }
            sPort = null;
        }

    }


    private void initUsb(){
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);

        // Find all available drivers from attached devices.
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
        if (availableDrivers.isEmpty()) {
            l("No device/driver");
            return;
        }

        // Open a connection to the first available driver.
        UsbSerialDriver mDriver = availableDrivers.get(0);
        //are we allowed to access?
        UsbDevice device = mDriver.getDevice();

        if (!manager.hasPermission(device)){
            //ask for permission
            PendingIntent pi = PendingIntent.getBroadcast(this, 0, new Intent(GET_USB_PERMISSION), 0);
            mContext.registerReceiver(mPermissionReceiver,new IntentFilter(GET_USB_PERMISSION));
            manager.requestPermission(device, pi);
            l("USB ask for permission");
            return;
        }



        UsbDeviceConnection connection = manager.openDevice(mDriver.getDevice());
        if (connection == null) {
            l("USB connection == null");
            return;
        }

        try {
            sPort = (UsbSerialPort)mDriver.getPorts().get(0);//Most have just one port (port 0)
            sPort.open(connection);
            sPort.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
        } catch (IOException e) {
            l("Error setting up device: " + e.getMessage());
            try {
                sPort.close();
            } catch (IOException e2) {/*ignore*/}
            sPort = null;
            return;
        }

        onDeviceStateChange();

    }


    private void stopIoManager() {
        status.setText("Disconnected");
        if (mSerialIoManager != null) {
            l("Stopping io manager ..");
            mSerialIoManager.stop();
            mSerialIoManager = null;
            mListener = null;
        }
    }

    private void startIoManager() {
        status.setText("Connected");
        if (sPort != null) {
            l("Starting io manager ..");
            mListener = new BBListenerAdapter(this);
            mSerialIoManager = new SerialInputOutputManager(sPort, mListener);
            mExecutor.submit(mSerialIoManager);
        }
    }

    private void onDeviceStateChange() {
        stopIoManager();
        startIoManager();
    }

    public void l(String s) {
        Log.v(TAG, s);
        log.setText(s);
    }

    public void sendCommand(String s) {
        l("sendCommand:" + s);
        try {
            if (sPort != null) sPort.write(s.getBytes(), 200);
        } catch (IOException e) {
            l("sendCommand err:" + e.getMessage());
        }
    }

    public void receiveCommand(String s) {//refresh and add string 's'
        l("receiveCommand :" + s);

    }

    private PermissionReceiver mPermissionReceiver = new PermissionReceiver();
    private class PermissionReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            mContext.unregisterReceiver(this);
            if (intent.getAction().equals(GET_USB_PERMISSION)) {
                UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    l("USB we got permission");
                    if (device != null){
                        initUsb();
                    }else{
                        l("USB perm receive device==null");
                    }

                } else {
                    l("USB no permission");
                }
            }
        }

    }

    /*
   * When an input device is added, we add a ship based upon the device.
   * @see
   * com.example.inputmanagercompat.InputManagerCompat.InputDeviceListener
   * #onInputDeviceAdded(int)
   */
    @Override
    public void onInputDeviceAdded(int deviceId) {
        l("onInputDeviceAdded");

    }

    /*
     * This is an unusual case. Input devices don't typically change, but they
     * certainly can --- for example a device may have different modes. We use
     * this to make sure that the ship has an up-to-date InputDevice.
     * @see
     * com.example.inputmanagercompat.InputManagerCompat.InputDeviceListener
     * #onInputDeviceChanged(int)
     */
    @Override
    public void onInputDeviceChanged(int deviceId) {
        l("onInputDeviceChanged");

    }

    /*
     * Remove any ship associated with the ID.
     * @see
     * com.example.inputmanagercompat.InputManagerCompat.InputDeviceListener
     * #onInputDeviceRemoved(int)
     */
    @Override
    public void onInputDeviceRemoved(int deviceId) {
        l("onInputDeviceRemoved");
    }

    public String GetRadioStreamFile() {
        //String radioFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).toString() + "/radio_stream3.mp3";
        String radioFile = mContext.getExternalFilesDir(Environment.DIRECTORY_MUSIC).toString() + "/radio_stream3.mp3";
        return radioFile;
    }

    private class DownloadTask extends AsyncTask<String, Integer, String> {

        private Context context;
        private PowerManager.WakeLock mWakeLock;

        public DownloadTask(Context context) {
            this.context = context;
        }



        @Override
        protected String doInBackground(String... sUrl) {
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;
            long total = 0;
            try {
                URL url = new URL(sUrl[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();
                String msg = connection.getResponseMessage().toString();

                // expect HTTP 200 OK, so we don't mistakenly save error report
                // instead of the file
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage();
                }

                // this will be useful to display download percentage
                // might be -1: server did not report the length
                int fileLength = connection.getContentLength();

                // download the file
                input = connection.getInputStream();

                String radioFile = GetRadioStreamFile();
                output = new FileOutputStream(radioFile);

                byte data[] = new byte[4096];
                int count;
                int pc = 0;
                while ((count = input.read(data)) != -1) {
                    // allow canceling with back button
                    if (isCancelled()) {
                        input.close();
                        return null;
                    }
                    total += count;
                    // publishing the progress....
                    if (fileLength > 0) // only if total length is known
                        publishProgress((int) (total * 100 / fileLength));
                    pc++;

                    output.write(data, 0, count);

                    if ((pc%500)==0)
                        Log.v(TAG, "Music file is " + total);
                }
                downloaded = true;
            } catch (Exception e) {
                return e.toString();
            } finally {
                try {
                    if (output != null)
                        output.close();
                    if (input != null)
                        input.close();
                    Log.v(TAG, "Download finished");
                } catch (IOException ignored) {
                }

                if (connection != null)
                    connection.disconnect();
            }
            return null;
        }
    }

    public void DownloadMusic() {
        try {

            File radioStream = new File(GetRadioStreamFile());

            if (!radioStream.exists() || radioStream.length()!=230565240) {
                long len = radioStream.length();

                //DownloadMusicAsync.execute();

                // declare the dialog as a member field of your activity
                ProgressDialog mProgressDialog;

                // instantiate it within the onCreate method
                mProgressDialog = new ProgressDialog(mContext);
                mProgressDialog.setMessage("A message");
                mProgressDialog.setIndeterminate(true);
                mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                mProgressDialog.setCancelable(true);

                // execute this when the downloader must be fired
                final DownloadTask downloadTask = new DownloadTask(mContext);
                downloadTask.execute("http://jonathanclark.com/bbMusic/radio_stream.mp3");

                mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        downloadTask.cancel(true);
                    }
                });

            }
            else {
                downloaded = true;
                Log.v(TAG, "Music file is " + radioStream.length());
            }
        }
        catch (Throwable err) {
            String msg = err.getMessage();
            System.out.println(msg);
        }

    }

    public void SeekAndPlay() {
        Calendar c = Calendar.getInstance();
        long ms = c.getTimeInMillis() + timeOffset;
        int lenInMS = mediaPlayer.getDuration();

        long seekOff = ms % lenInMS;
        mediaPlayer.seekTo((int)seekOff);

        mediaPlayer.start();
    }

    public void RadioMode() {
        try {
            FileInputStream fds = new FileInputStream(GetRadioStreamFile());
            mediaPlayer.reset();
            mediaPlayer.setDataSource(fds.getFD());
            fds.close();

            mediaPlayer.setVolume(vol, vol);
            mediaPlayer.prepare();
            SeekAndPlay();
        }
        catch (Throwable err) {
            String msg = err.getMessage();
            System.out.println(msg);
        }
    }

    public void BLETest() {
        BluetoothLeAdvertiser advertiser = BluetoothAdapter.getDefaultAdapter().getBluetoothLeAdvertiser();

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode( AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY )
                .setTxPowerLevel( AdvertiseSettings.ADVERTISE_TX_POWER_HIGH )
                .setConnectable( false )
                .build();

        ParcelUuid pUuid = new ParcelUuid( UUID.randomUUID() );
                //UUID.fromString( "CDB7950D-73F1-4D4D-8E47-C090502DBD63" ) );

        byte[] mfData = new byte[12];
        mfData[0] = 'B';     // marker to make sure we aren't looking at other random BLE adverts
        mfData[1] = 'U';
        mfData[2] = 'R';
        mfData[3] = 'N';

        byte[] timeInBytes = ByteBuffer.allocate(8).putLong(Calendar.getInstance().getTimeInMillis()).array();
        for (int i=0; i<8; i++)
            mfData[i+4] = timeInBytes[i];

        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName( true )
                .addManufacturerData(0, mfData)
                //.addServiceUuid( pUuid )
                //.addServiceData( pUuid, "Data".getBytes( Charset.forName( "UTF-8" ) ) )
                .build();

        AdvertiseCallback advertisingCallback = new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                super.onStartSuccess(settingsInEffect);
            }

            @Override
            public void onStartFailure(int errorCode) {
                Log.e( "BLE", "Advertising onStartFailure: " + errorCode );
                super.onStartFailure(errorCode);
            }
        };

        advertiser.startAdvertising( settings, data, advertisingCallback );

        mBluetoothLeScanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();

        List<ScanFilter> filters = null;
        ScanSettings settings2 = new ScanSettings.Builder()
                .setScanMode( ScanSettings.SCAN_MODE_LOW_LATENCY )
                .build();

        //mBluetoothLeScanner.startScan(filters, settings2, mScanCallback);
        mBluetoothLeScanner.startScan( mScanCallback);

    }

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if( result == null
                    || result.getDevice() == null
                    || TextUtils.isEmpty(result.getDevice().getName()) )
                return;

            byte[] mfData = result.getScanRecord().getManufacturerSpecificData(0);
            if (mfData!=null && mfData[0] == 'B' && mfData[1]=='U' && mfData[2]=='R' && mfData[3]=='N') {

                long value = ByteBuffer.wrap(mfData,4, 8).getLong();
                long curTime = Calendar.getInstance().getTimeInMillis();
                long timeOff = curTime-value;
                l(Long.toString(timeOff));
            }
            else
                l(result.toString());
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e( "BLE", "Discovery onScanFailed: " + errorCode );
            super.onScanFailed(errorCode);
        }
    };

    void MusicOffset(int ms) {
        timeOffset += ms;
        SeekAndPlay();
        l("TimeOffset = " + timeOffset);
/*
        try {
            int curPos = mediaPlayer.getCurrentPosition();

            mediaPlayer.seekTo(curPos + ms);
            Thread.sleep(100, 0);
            int newPos = mediaPlayer.getCurrentPosition();

            if (newPos - curPos + 100 != ms) {
                int err = newPos - curPos + 100 - ms;
                l("Seek error = " + err);
                mediaPlayer.seekTo(newPos - err);
            }
        } catch (Throwable err) {
        } */

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean handled = false;
        if (event.getRepeatCount() == 0) {
            l("Keycode:" + keyCode);
        }

        switch (keyCode) {
            case 100 : RadioMode(); break;
            case 20: MusicOffset(-1); break;
            case 19: MusicOffset(1); break;
            case 24:   // native volume up button
            case 21 :
                vol += 0.01;
                if (vol>1) vol = 1;
                mediaPlayer.setVolume(vol, vol);
                l("Volume " + vol*100.0f + "%");
                return false;
            case 25 :  // native volume down button
            case 22 :
                vol -= 0.01;
                if (vol<0) vol = 0;
                mediaPlayer.setVolume(vol, vol);
                l("Volume " + vol*100.0f + "%");
                return false;
            case 96 : switchHeadlight.setChecked(true); break;
            case 99 : switchHeadlight.setChecked(false); break;

        }


        if ((event.getSource() & InputDevice.SOURCE_GAMEPAD)
                == InputDevice.SOURCE_GAMEPAD) {

            if (handled) {
                return true;
            }
        }


        mHandler.removeCallbacksAndMessages(null);

//        return super.onKeyDown(keyCode, event);
        return true;

    }

}



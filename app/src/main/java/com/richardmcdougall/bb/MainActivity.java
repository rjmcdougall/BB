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
    private Handler mHandler = new Handler();
    private Context mContext;
    protected static final String GET_USB_PERMISSION = "GetUsbPermission";
    private static final String TAG = "BB.MainActivity";
    private InputManagerCompat remoteControl;
    private android.widget.Switch switchHeadlight;

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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);     
        setContentView(R.layout.activity_main);

        remoteControl = InputManagerCompat.Factory.getInputManager(getApplicationContext());
        remoteControl.registerInputDeviceListener(this, null);

        voltage = (TextView) findViewById(R.id.textViewVoltage);
        status = (TextView) findViewById(R.id.textViewStatus);
        log = (EditText) findViewById(R.id.editTextLog);
        voltage.setText("0.0v");
        log.setText("Hello");

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

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean handled = false;
        if (event.getRepeatCount() == 0) {
            l("Keycode:" + keyCode);
        }

        if (keyCode == 96) {
            switchHeadlight.setChecked(true);
        }
        if (keyCode == 99) {
            switchHeadlight.setChecked(false);
        }
        if ((event.getSource() & InputDevice.SOURCE_GAMEPAD)
                == InputDevice.SOURCE_GAMEPAD) {

            if (handled) {
                return true;
            }
        }
//        return super.onKeyDown(keyCode, event);
        return true;

    }

}



package com.richardmcdougall.bb;

import android.Manifest;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.os.ResultReceiver;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.app.Activity;
import android.view.WindowManager;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.View;
import android.view.Gravity;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ImageView;
import android.view.MotionEvent;
import android.content.BroadcastReceiver;
import android.widget.Toast;
import android.app.admin.DevicePolicyManager;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.app.admin.DeviceAdminReceiver;
import android.provider.Settings;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.util.List;
import android.text.InputFilter;


public class MainActivity extends AppCompatActivity implements InputManagerCompat.InputDeviceListener {

    private static final String TAG = "BB.MainActivity";

    public static final boolean kEmbeddedMode = false;
    public static final boolean kThings = true;

    boolean imRunning = false;

    TextView voltage;
    TextView status;
    EditText log;
    TextView syncStatus = null;
    TextView syncPeers = null;
    TextView syncReplies = null;
    TextView syncAdjust;
    TextView syncTrack;
    TextView syncUsroff;
    TextView syncSrvoff;
    TextView syncRTT;
    TextView modeStatus;
    private android.widget.Switch switchHeadlight;

    protected static final String GET_USB_PERMISSION = "GetUsbPermission";

    private BoardView mBoardView;
    private String stateMsgAudio = "";
    private String stateMsgConn = "";
    private String stateMsg = "";

    private InputManagerCompat remoteControl;

    // TODO: Rename actions, choose action names that describe tasks that this
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    private static final String ACTION_LIGHTS = "com.richardmcdougall.bb.action.LIGHTS";
    private static final String ACTION_MUSIC = "com.richardmcdougall.bb.action.MUSIC";

    // TODO: Rename parameters
    private static final String EXTRA_PARAM1 = "com.richardmcdougall.bb.extra.PARAM1";
    private static final String EXTRA_PARAM2 = "com.richardmcdougall.bb.extra.PARAM2";

    private ComponentName mAdminComponentName;
    private DevicePolicyManager mDevicePolicyManager;
    private PackageManager mPackageManager;
    private static final String Battery_PLUGGED_ANY = Integer.toString(
            BatteryManager.BATTERY_PLUGGED_AC |
                    BatteryManager.BATTERY_PLUGGED_USB |
                    BatteryManager.BATTERY_PLUGGED_WIRELESS);

    private usbReceiver mUsbReceiver = new usbReceiver();

    private boolean preventDialogs = true;

    // Kill popups which steal remote control input button focus from the app
    // http://www.andreas-schrade.de/2015/02/16/android-tutorial-how-to-create-a-kiosk-mode-in-android/
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        l("MainActivity: onWindowFocusChanged()");

        //if (preventDialogs == false) {
        //    return;
        //}

        //if(!hasFocus) {
            // Close every kind of system dialog
            Intent closeDialog = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
            this.sendBroadcast(closeDialog);
        //}
        ActivityManager am = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
        am.moveTaskToFront(getTaskId(), ActivityManager.MOVE_TASK_WITH_HOME);
    }


    static void showToast(Context context, String text) {
        Toast.makeText(context, text, Toast.LENGTH_LONG).show();
    }

    static String getHomeActivity(Context c) {
        PackageManager pm = c.getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        ComponentName cn = intent.resolveActivity(pm);
        if (cn != null)
            return cn.flattenToShortString();
        else
            return "none";
    }


    static void becomeHomeActivity(Context c) {
        ComponentName deviceAdmin = new ComponentName(c, AdminReceiver.class);
        DevicePolicyManager dpm = (DevicePolicyManager) c.getSystemService(Context.DEVICE_POLICY_SERVICE);

        if (!dpm.isAdminActive(deviceAdmin)) {
            Toast.makeText(c, "This app is not a device admin!", Toast.LENGTH_LONG).show();
            return;
        }
        if (!dpm.isDeviceOwnerApp(c.getPackageName())) {
            Toast.makeText(c, "This app is not the device owner!", Toast.LENGTH_LONG).show();
            return;
        }
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_MAIN);
        intentFilter.addCategory(Intent.CATEGORY_DEFAULT);
        intentFilter.addCategory(Intent.CATEGORY_HOME);
        ComponentName activity = new ComponentName(c, MainActivity.class);
        dpm.addPersistentPreferredActivity(deviceAdmin, intentFilter, activity);
        Toast.makeText(c, "Home activity: " + getHomeActivity(c), Toast.LENGTH_LONG).show();

        dpm.setGlobalSetting(
                deviceAdmin,
                Settings.Global.STAY_ON_WHILE_PLUGGED_IN,
                Battery_PLUGGED_ANY);

    }

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void startActionLights(Context context, String param1, String param2) {
        Intent intent = new Intent(context, BBIntentService.class);
        intent.setAction(ACTION_LIGHTS);
        intent.putExtra(EXTRA_PARAM1, param1);
        intent.putExtra(EXTRA_PARAM2, param2);
        context.startService(intent);
    }

    /**
     * Starts this service to perform action Baz with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void startActionMusic(Context context, String param1, String param2) {
        Intent intent = new Intent(context, BBIntentService.class);
        intent.setAction(ACTION_MUSIC);
        intent.putExtra(EXTRA_PARAM1, param1);
        intent.putExtra(EXTRA_PARAM2, param2);
        context.startService(intent);
    }


    //private Handler pHandler = new Handler();

    //public String bleStatus = "hello BLE";


    // function to append a string to a TextView as a new line
    // and scroll to the bottom if needed
    private void l(String msg) {
        if (log == null)
            return;

        Log.v(TAG, msg);
        logToScreen(msg);
    }

    // function to append a string to a TextView as a new line
    // and scroll to the bottom if needed
    private void logToScreen(String msg) {
        if (log == null)
            return;

        if (kEmbeddedMode == false)
            return;

        // append the new string
        log.append(msg + "\n");

        String tMsg = log.getText().toString();
        int msgLen = tMsg.length();
        if (msgLen > 1000) {
            tMsg = tMsg.substring(msgLen - 1000, msgLen);
        }
        log.setText(tMsg);

        // find the amount we need to scroll.  This works by
        // asking the TextView's internal layout for the position
        // of the final line and then subtracting the TextView's height
        final android.text.Layout layout = log.getLayout();

        if (layout != null) {
            final int scrollAmount = layout.getLineTop(log.getLineCount()) - log.getHeight();
            // if there is no need to scroll, scrollAmount will be <=0
            if (scrollAmount > 0)
                log.scrollTo(0, scrollAmount);
            else
                log.scrollTo(0, 0);
        }
    }

    public void setStateMsgConn(String str) {
        synchronized (stateMsg) {
            stateMsgConn = str;
            stateMsg = stateMsgConn + "," + stateMsgAudio;

        }
    }

    public void setStateMsgAudio(String str) {
        synchronized (stateMsg) {
            stateMsgAudio = str;
            stateMsg = stateMsgConn + "," + stateMsgAudio;
        }
    }

    // Define the callback for what to do when stats are received
    private BroadcastReceiver BBstatsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (kThings) {
                return;
            }

            int resultCode = intent.getIntExtra("resultCode", RESULT_CANCELED);
            int msgType = intent.getIntExtra("msgType", 0);
            if (resultCode == RESULT_OK) {
                switch (msgType) {
                    case 1:
                        long seekErr = intent.getLongExtra("seekErr", 0);
                        syncAdjust.setText(String.format("%1$d", seekErr));
                        int currentRadioChannel = intent.getIntExtra("currentRadioChannel", 0);
                        syncTrack.setText(String.format("%1$d", currentRadioChannel));
                        int userTimeOffset = intent.getIntExtra("userTimeOffset", 0);
                        syncUsroff.setText(String.format("%1$d", userTimeOffset));
                        long serverTimeOffset = intent.getLongExtra("serverTimeOffset", 0);
                        syncSrvoff.setText(String.format("%1$d", serverTimeOffset));
                        long serverRTT = intent.getLongExtra("serverRTT", 0);
                        syncRTT.setText(String.format("%1$d", serverRTT));
                        String stateMsgAudio = intent.getStringExtra("stateMsgAudio");
                        setStateMsgAudio(stateMsgAudio);
                        break;
                    case 2:
                        long stateReplies = intent.getLongExtra("stateReplies", 0);
                        syncReplies.setText(String.format("%1$d", stateReplies));
                        String stateMsgWifi = intent.getStringExtra("stateMsgWifi");
                        setStateMsgConn(stateMsgWifi);
                        break;
                    case 3:
                        String statusMsg = intent.getStringExtra("ledStatus");
                        status.setText(statusMsg);
                        break;
                    case 4:
                        String logMsg = intent.getStringExtra("logMsg");
                        logToScreen(logMsg);
                        break;

                    default:
                        break;
                }
            }
            syncStatus.setText(stateMsg);
        }

    };

    @Override
    protected void onStart() {
        super.onStart();
        l("MainActivity: onStart()");

        // start lock task mode if it's not already active
        ActivityManager am = (ActivityManager) getSystemService(
                Context.ACTIVITY_SERVICE);
        // ActivityManager.getLockTaskModeState api is not available in pre-M.
        if (am.getLockTaskModeState() ==
                ActivityManager.LOCK_TASK_MODE_NONE) {
            // Pin the app
            // startLockTask();
        }

        //Intent startServiceIntent = new Intent(this, BBService.class);
        //startWakefulService(this, startServiceIntent);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        l("MainActivity: onCreate()");

        super.onCreate(savedInstanceState);

        // Close every kind of system dialog
        Intent closeDialog = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        this.sendBroadcast(closeDialog);

        ActivityManager am = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
        am.moveTaskToFront(getTaskId(), ActivityManager.MOVE_TASK_WITH_HOME);

        setupPermissions(Manifest.permission.RECORD_AUDIO, 1);
        setupPermissions(Manifest.permission.ACCESS_WIFI_STATE, 2);
        setupPermissions(Manifest.permission.CHANGE_WIFI_STATE, 3);
        setupPermissions(Manifest.permission.ACCESS_COARSE_LOCATION, 4);
        setupPermissions(Manifest.permission.INTERNET, 5);
        setupPermissions(Manifest.permission.BLUETOOTH, 6);
        setupPermissions(Manifest.permission.BLUETOOTH_ADMIN, 7);
        setupPermissions(Manifest.permission.BLUETOOTH_PRIVILEGED, 8);
        setupPermissions(Manifest.permission.ACCESS_NETWORK_STATE, 9);
        setupPermissions(Manifest.permission.ACCESS_FINE_LOCATION, 10);

        startService(new Intent(getBaseContext(), BBService.class));

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        if (kEmbeddedMode == true) {
            becomeHomeActivity(this.getApplicationContext());
        }

        //UsbManager mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        //static PendingIntent mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        //mUsbManager.requestPermission(accessory, mPermissionIntent);



        // Connect the remote control
        remoteControl = InputManagerCompat.Factory.getInputManager(getApplicationContext());
        remoteControl.registerInputDeviceListener(this, null);
        int[] devs = remoteControl.getInputDeviceIds();
        for (int dev : devs) {
            l("Input dev" + dev);
            if (dev > 0) {
                InputDevice device = remoteControl.getInputDevice(dev);
                l("Device" + device.toString());
            }
        }

        setContentView(R.layout.activity_main);

        // Create the graphic equalizer
        mBoardView = (BoardView) findViewById(R.id.myBoardview);

        // Create textview
        voltage = (TextView) findViewById(R.id.textViewVoltage);
        modeStatus = (TextView) findViewById(R.id.modeStatus);
        status = (TextView) findViewById(R.id.textViewStatus);
        syncStatus = (TextView) findViewById(R.id.textViewsyncstatus);
        syncPeers = (TextView) findViewById(R.id.textViewsyncpeers);
        syncReplies = (TextView) findViewById(R.id.textViewsyncreplies);
        syncAdjust = (TextView) findViewById(R.id.textViewsyncadjust);
        syncTrack = (TextView) findViewById(R.id.textViewsynctrack);
        syncUsroff = (TextView) findViewById(R.id.textViewsyncusroff);
        syncSrvoff = (TextView) findViewById(R.id.textViewsyncsrvoff);
        syncRTT = (TextView) findViewById(R.id.textViewsyncrtt);

        // Create the logging window
        log = (EditText) findViewById(R.id.editTextLog);
        log.setMovementMethod(new android.text.method.ScrollingMovementMethod());
        log.setMaxLines(40);

        //int maxLength = 500;
        //log.setFilters(new InputFilter[] {new InputFilter.LengthFilter(maxLength)});

        voltage.setText("0.0v");
        log.setFocusable(false);

        switchHeadlight = (android.widget.Switch) findViewById(R.id.switchHeadlight);
        switchHeadlight.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                //boardSetHeadlight(isChecked);
            }
        });


        //startActionMusic(getApplicationContext(), "", "");

        //MusicReset();


        //try {
        //    mVisualizerView.link(mediaPlayer.getAudioSessionId());
        //    mVisualizerView.addBarGraphRendererBottom();
        //mVisualizerView.addBurnerBoardRenderer(this);
        //} catch (Exception e) {
        //    l("Cannot start visualizer!" + e.getMessage());
        //

    }



    private void updateStatus() {
        float volts;

//        volts = boardGetVoltage();
        volts = 0.0f;

        if (volts > 0) {
            voltage.setText(String.format("%.2f", volts) + "v");
        }

    }

    @Override
    protected void onResume() {

        super.onResume();
        l("MainActivity: onResume()");

//        if (mWifi != null)
//            mWifi.onResume();

//        loadPrefs();

//        initUsb();

        IntentFilter filter = new IntentFilter();
        filter.addAction("android.hardware.usb.action.USB_DEVICE_ATTACHED");
        filter.addAction("android.hardware.usb.action.USB_DEVICE_DETTACHED");
        this.registerReceiver(mUsbReceiver, filter);

        // Register for the particular broadcast based on Stats Action
        IntentFilter statFilter = new IntentFilter(BBService.ACTION_STATS);
        LocalBroadcastManager.getInstance(this).registerReceiver(BBstatsReceiver, statFilter);

        // Register for the particular broadcast based on Graphics Action
        IntentFilter gfxFilter = new IntentFilter(BBService.ACTION_GRAPHICS);
        LocalBroadcastManager.getInstance(this).registerReceiver(BBgraphicsReceiver, gfxFilter);

    }

    @Override
    protected void onPause() {

        super.onPause();
        l("MainActivity: onPause()");


        //       if (mWifi != null)
        //         mWifi.onPause();


//        savePrefs();

//        stopIoManager();

        // Unregister the listener when the application is paused
        LocalBroadcastManager.getInstance(this).unregisterReceiver(BBstatsReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(BBgraphicsReceiver);
        this.unregisterReceiver(mUsbReceiver);

    }



    public void onModeDown(View v) {
        Intent in = new Intent(BBService.ACTION_BUTTONS);
        in.putExtra("resultCode", Activity.RESULT_OK);
        in.putExtra("buttonType", BBService.buttons.BUTTON_MODE_DOWN);
        LocalBroadcastManager.getInstance(this).sendBroadcast(in);
        //   boardSetMode(98);
        //  if ((boardMode = boardGetMode()) == -1)
        //     boardMode--;
        //modeStatus.setText(String.format("%d", boardMode));
    }

    public void onModeUp(View v) {
        Intent in = new Intent(BBService.ACTION_BUTTONS);
        in.putExtra("resultCode", Activity.RESULT_OK);
        in.putExtra("buttonType", BBService.buttons.BUTTON_MODE_UP);
        LocalBroadcastManager.getInstance(this).sendBroadcast(in);
        //   boardSetMode(99);
        // if ((boardMode = boardGetMode()) == -1)
        //    boardMode++;
        //modeStatus.setText(String.format("%d", boardMode));
    }


    public void onNextTrack(View v) {
        Intent in = new Intent(BBService.ACTION_BUTTONS);
        in.putExtra("resultCode", Activity.RESULT_OK);
        in.putExtra("buttonType", BBService.buttons.BUTTON_TRACK);
        LocalBroadcastManager.getInstance(this).sendBroadcast(in);
        //NextStream();
    }

    public void onDriftDown(View v) {
        Intent in = new Intent(BBService.ACTION_BUTTONS);
        in.putExtra("resultCode", Activity.RESULT_OK);
        in.putExtra("buttonType", BBService.buttons.BUTTON_DRIFT_DOWN);
        LocalBroadcastManager.getInstance(this).sendBroadcast(in);
        //MusicOffset(-2);
    }

    public void onDriftUp(View v) {
        Intent in = new Intent(BBService.ACTION_BUTTONS);
        in.putExtra("resultCode", Activity.RESULT_OK);
        in.putExtra("buttonType", BBService.buttons.BUTTON_DRIFT_UP);
        LocalBroadcastManager.getInstance(this).sendBroadcast(in);
    }

    public void onVolDown(View v) {
        Intent in = new Intent(BBService.ACTION_BUTTONS);
        in.putExtra("resultCode", Activity.RESULT_OK);
        in.putExtra("buttonType", BBService.buttons.BUTTON_VOL_DOWN);
        LocalBroadcastManager.getInstance(this).sendBroadcast(in);
    }

    public void onVolUp(View v) {
        Intent in = new Intent(BBService.ACTION_BUTTONS);
        in.putExtra("resultCode", Activity.RESULT_OK);
        in.putExtra("buttonType", BBService.buttons.BUTTON_VOL_UP);
        LocalBroadcastManager.getInstance(this).sendBroadcast(in);
    }

    public void onVolPause(View v) {
        Intent in = new Intent(BBService.ACTION_BUTTONS);
        in.putExtra("resultCode", Activity.RESULT_OK);
        in.putExtra("buttonType", BBService.buttons.BUTTON_VOL_PAUSE);
        LocalBroadcastManager.getInstance(this).sendBroadcast(in);
    }

    /*
   * When an input device is added, we add a ship based upon the device.
   * @see
   * com.example.inputmanagercompat.InputManagerCompat.InputDeviceListener
   * #onInputDeviceAdded(int)
   */
    @Override
    public void onInputDeviceAdded(int deviceId) {
        l("MainActivity: onInputDeviceAdded");

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
        l("MainAcitivity: onInputDeviceChanged");

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

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean handled = false;
        if (event.getRepeatCount() == 0) {
            l("Keycode:" + keyCode);
            //System.out.println("Keycode: " + keyCode);
        }

        Intent in = new Intent(BBService.ACTION_BUTTONS);
        in.putExtra("resultCode", Activity.RESULT_OK);
        in.putExtra("buttonType", BBService.buttons.BUTTON_KEYCODE);
        in.putExtra("keyCode", keyCode);
        in.putExtra("keyEvent", event);
        // Fire the broadcast with intent packaged
        LocalBroadcastManager.getInstance(this).sendBroadcast(in);


        if ((event.getSource() & InputDevice.SOURCE_GAMEPAD)
                == InputDevice.SOURCE_GAMEPAD) {

            if (handled) {
                return true;
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    private void setupPermissions(String permission, int permissionId) {
        // Here, thisActivity is the current activity
        l("Checking permission for " + permission + ", requesting as id" + permissionId);
        if (ContextCompat.checkSelfPermission(this, permission)
                != PackageManager.PERMISSION_GRANTED) {

            l("Didn't have permission for " + permission + ", requesting as id" + permissionId);

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    permission)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{permission}, permissionId);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
                try {
                    Thread.sleep(5000);
                } catch (Throwable e) {
                }
            }
        }

    }


    // If we want to do anything when permissions are granted or denied
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {

        l("Got permission for id" + requestCode);

        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    // Define the callback for what to do when graphics are received
    private BroadcastReceiver BBgraphicsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (kEmbeddedMode == true)
                return;
            int resultCode = intent.getIntExtra("resultCode", RESULT_CANCELED);
            int visualId = intent.getIntExtra("visualId", 0);
            //l("Graphics " + visualId);
            if (resultCode == RESULT_OK) {
                switch (visualId) {
                    case 13:
                        byte r = (byte)intent.getIntExtra("arg1", 0);
                        byte g = (byte)intent.getIntExtra("arg2", 0);
                        byte b = (byte)intent.getIntExtra("arg3", 0);
                        mBoardView.fillScreen(r, g, b);
                        break;
                    case 8:
                        mBoardView.invalidate();
                        break;

                    case 6:
                        int direction = intent.getIntExtra("arg1", 0);
                        mBoardView.scroll(direction);
                        break;
                    case 7:
                        int amount = intent.getIntExtra("arg1", 0);
                        mBoardView.fadeScreen(amount);
                        break;
                    case 14:
                        int row = intent.getIntExtra("arg1", 0);
                        byte [] pixels = intent.getByteArrayExtra("arg2").clone();
                        //l("intent setrow:" + row + "," + BurnerBoard.bytesToHex(pixels));
                        mBoardView.setRow(row, pixels);
                        break;
                    case 15:
                        int other = intent.getIntExtra("arg1", 0);
                        byte [] otherPixels = intent.getByteArrayExtra("arg2").clone();
                        mBoardView.setOtherLight(other, otherPixels);
                        break;
                    default:
                        break;
                }
            }
        }

    };

    public static class usbReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            //l("usbReceiver");
            if (intent != null)
            {
                if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED))
                {
                    Log.v(TAG, "ACTION_USB_DEVICE_ATTACHED");
                    Parcelable usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    // Create a new intent and put the usb device in as an extra
                    Intent broadcastIntent = new Intent(BBService.ACTION_USB_DEVICE_ATTACHED);
                    broadcastIntent.putExtra(UsbManager.EXTRA_DEVICE, usbDevice);

                    // Broadcast this event so we can receive it
                    LocalBroadcastManager.getInstance(context).sendBroadcast(broadcastIntent);
                }
                if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED))
                {
                    Log.v(TAG,"ACTION_USB_DEVICE_DETACHED");

                    Parcelable usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    // Create a new intent and put the usb device in as an extra
                    Intent broadcastIntent = new Intent(BBService.ACTION_USB_DEVICE_DETACHED);
                    broadcastIntent.putExtra(UsbManager.EXTRA_DEVICE, usbDevice);

                    // Broadcast this event so we can receive it
                    LocalBroadcastManager.getInstance(context).sendBroadcast(broadcastIntent);
                }
            }
        }

    }

    public void OnSetupUsb(View v) {

        l("MainActivity: initUsb()");


        UsbManager mUsbManager = (UsbManager) this.getSystemService(Context.USB_SERVICE);

        // Find all available drivers from attached devices.
        List<UsbSerialDriver> availableDrivers =
                UsbSerialProber.getDefaultProber().findAllDrivers(mUsbManager);
        if (availableDrivers.isEmpty()) {
            l("USB: No USB Devices");
            return;
        }

        // Find the Radio device by pid/vid
        UsbDevice mUsbDevice = null;
        l("There are " + availableDrivers.size() + " drivers");
        for (int i = 0; i < availableDrivers.size(); i++) {
            UsbSerialDriver mDriver = availableDrivers.get(i);

            // See if we can find the adafruit M0 which is the Radio
            mUsbDevice = mDriver.getDevice();

            if (!mUsbManager.hasPermission(mUsbDevice)) {
                //ask for permission
                PendingIntent pi = PendingIntent.getBroadcast(this, 0, new Intent(GET_USB_PERMISSION), 0);
                this.registerReceiver(new PermissionReceiver(), new IntentFilter(GET_USB_PERMISSION));
                mUsbManager.requestPermission(mUsbDevice, pi);
            }
        }

    }

    // Receive permission if it's being asked for (typically for the first time)
    private class PermissionReceiver extends BroadcastReceiver {
        protected static final String GET_USB_PERMISSION = "GetUsbPermission";

        @Override
        public void onReceive(Context context, Intent intent) {
            context.unregisterReceiver(this);
            if (intent.getAction().equals(GET_USB_PERMISSION)) {
                UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    l("USB we got permission");
                    if (device != null) {
                        l("Got USB perm receive device==" + device);
                    } else {
                        l("USB perm receive device==null");
                    }

                } else {
                    l("USB no permission");
                }
            }
        }
    }

}



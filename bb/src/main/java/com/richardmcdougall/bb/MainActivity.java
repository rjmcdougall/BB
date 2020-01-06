package com.richardmcdougall.bb;

import android.Manifest;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Parcelable;
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
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.content.BroadcastReceiver;
import android.widget.Toast;
import android.app.admin.DevicePolicyManager;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.provider.Settings;

public class MainActivity extends AppCompatActivity implements InputManagerCompat.InputDeviceListener {

    private static final String TAG = "BB.MainActivity";
    private static final String Battery_PLUGGED_ANY = Integer.toString(
            BatteryManager.BATTERY_PLUGGED_AC |
                    BatteryManager.BATTERY_PLUGGED_USB |
                    BatteryManager.BATTERY_PLUGGED_WIRELESS);
    TextView modeStatus;
    private android.widget.Switch switchHeadlight;
    private BoardView mBoardView;
    private InputManagerCompat remoteControl;
    private boolean preventDialogs = false;

    // Kill popups which steal remote control input button focus from the app
    // http://www.andreas-schrade.de/2015/02/16/android-tutorial-how-to-create-a-kiosk-mode-in-android/
    private boolean mIsCustomized = false;
    // Define the callback for what to do when graphics are received
    private BroadcastReceiver BBgraphicsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!DebugConfigs.DISPLAY_VIDEO_IN_APP)
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

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        l("MainActivity: onWindowFocusChanged()");

        if (preventDialogs == false) {
            return;
        }

        //if(!hasFocus) {
        if (!mIsCustomized) {
            // Close every kind of system dialog
            Intent closeDialog = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
            this.sendBroadcast(closeDialog);
            //}
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            am.moveTaskToFront(getTaskId(), ActivityManager.MOVE_TASK_WITH_HOME);
        }
    }

    // function to append a string to a TextView as a new line
    // and scroll to the bottom if needed
    private void l(String msg) {
        Log.v(TAG, msg);
    }

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


        if (!DebugConfigs.DISPLAY_VIDEO_IN_APP) {
            becomeHomeActivity(this.getApplicationContext());
        }

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

        modeStatus = (TextView) findViewById(R.id.modeStatus);

    }

    @Override
    protected void onResume() {

        super.onResume();
        l("MainActivity: onResume()");

        // Register for the particular broadcast based on Graphics Action
        IntentFilter gfxFilter = new IntentFilter(ACTION.GRAPHICS);
        LocalBroadcastManager.getInstance(this).registerReceiver(BBgraphicsReceiver, gfxFilter);

    }

    @Override
    protected void onPause() {

        super.onPause();
        l("MainActivity: onPause()");

        // Unregister the listener when the application is paused
        LocalBroadcastManager.getInstance(this).unregisterReceiver(BBgraphicsReceiver);
    }

    public void onModeDown(View v) {
        Intent in = new Intent(ACTION.BUTTONS);
        in.putExtra("resultCode", Activity.RESULT_OK);
        in.putExtra("buttonType", BBService.buttons.BUTTON_MODE_DOWN);
        LocalBroadcastManager.getInstance(this).sendBroadcast(in);
    }

    public void onModeUp(View v) {
        Intent in = new Intent(ACTION.BUTTONS);
        in.putExtra("resultCode", Activity.RESULT_OK);
        in.putExtra("buttonType", BBService.buttons.BUTTON_MODE_UP);
        LocalBroadcastManager.getInstance(this).sendBroadcast(in);
    }

    public void onNextTrack(View v) {
        Intent in = new Intent(ACTION.BUTTONS);
        in.putExtra("resultCode", Activity.RESULT_OK);
        in.putExtra("buttonType", BBService.buttons.BUTTON_TRACK);
        LocalBroadcastManager.getInstance(this).sendBroadcast(in);
    }

    @Override
    public void onInputDeviceAdded(int deviceId) {
        l("MainActivity: onInputDeviceAdded");
    }

    @Override
    public void onInputDeviceChanged(int deviceId) {
        l("MainAcitivity: onInputDeviceChanged");
    }

    @Override
    public void onInputDeviceRemoved(int deviceId) {
        l("onInputDeviceRemoved");
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
}



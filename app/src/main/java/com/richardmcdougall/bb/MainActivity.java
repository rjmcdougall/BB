// jc: testing git commit with update

package com.richardmcdougall.bb;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.media.PlaybackParams;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
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

import com.richardmcdougall.bb.CmdMessenger.CmdEvents;

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
import android.text.TextUtils;
import java.nio.*;
import android.content.*;
import java.io.*;
import java.net.*;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.app.*;
import android.net.*;
import android.Manifest.*;

public class MainActivity extends AppCompatActivity implements InputDeviceListener {

    TextView voltage;
    TextView status;
    EditText log;
    ProgressBar pb;

    private static UsbSerialPort sPort = null;
    private static UsbSerialDriver mDriver = null;
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private SerialInputOutputManager mSerialIoManager;
    //private BBListenerAdapter mListener = null;
    private CmdMessenger mListener = null;
    private Handler mHandler = new Handler();
    private Context mContext;
    protected static final String GET_USB_PERMISSION = "GetUsbPermission";
    private static final String TAG = "BB.MainActivity";
    private InputManagerCompat remoteControl;
    private android.widget.Switch switchHeadlight;
    private MediaPlayer mediaPlayer = new MediaPlayer();
    private float vol = 0.10f;
    private boolean downloaded = false;
    public long serverTimeOffset = 0;
    public long serverRTT = 0;
    private int userTimeOffset = 0;
    public MyWifiDirect mWifi = null;
    public UDPClientServer udpClientServer = null;

    long phoneModelAudioLatency = 0;

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
    long startElapsedTime, startClock;

    public void InitClock() {
        startElapsedTime = SystemClock.elapsedRealtime();
        startClock = Calendar.getInstance().getTimeInMillis();
    }

    public long GetCurrentClock() {
        //return (System.nanoTime()-startNanotime)/1000000 + startClock;
        //return System.currentTimeMillis();
        return SystemClock.elapsedRealtime() - startElapsedTime + startClock;
        //return Calendar.getInstance().getTimeInMillis();
    }

    public long CurrentClockAdjusted() {
        return GetCurrentClock() + serverTimeOffset;
        //return Calendar.getInstance().getTimeInMillis()
    }

    public void SetServerClockOffset(long serverClockOffset, long rtt) {
        serverTimeOffset = serverClockOffset;
        serverRTT = rtt;
    }


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

    protected void MusicReset() {
        Timer t = new Timer();
        t.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        SeekAndPlay();
                    }
                });
            }

        }, 0, 1000);

    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {


        String model = android.os.Build.MODEL;
        if (model.equals("BLU DASH M2")) {
            phoneModelAudioLatency = 10;
        }
        else
            phoneModelAudioLatency = 80;

        udpClientServer = new UDPClientServer(this);
        udpClientServer.Run();

        mWifi = new MyWifiDirect(this, udpClientServer);



                InitClock();
        super.onCreate(savedInstanceState);
        mContext = getApplicationContext();

        //requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        //requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        ActivityCompat.requestPermissions(this, new String[]{permission.BLUETOOTH_ADMIN}, 1);

        DownloadMusic2();
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
                    //sendCommand("2,1;");
                    if (mListener != null) {
                        l("sendCommand: 3,1");
                        mListener.sendCmdStart(3);
                        mListener.sendCmdArg(1);
                        mListener.sendCmdEnd();
                    }
                } else {
                    if (mListener != null) {
                        l("sendCommand: 3,0");
                        mListener.sendCmdStart(3);
                        mListener.sendCmdArg(0);
                        mListener.sendCmdEnd();
                    }
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

        MusicReset();

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mClientServer!=null)
            mClientServer.onResume();
        if (mWifi!=null)
            mWifi.onResume();

//        loadPrefs();

        initUsb();


    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mClientServer!=null)
            mClientServer.onPause();
        if (mWifi!=null)
            mWifi.onPause();

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
            //mListener = new BBListenerAdapter();
            mListener = new CmdMessenger(sPort, ',',';','\\');
            mSerialIoManager = new SerialInputOutputManager(sPort, mListener);
            mExecutor.submit(mSerialIoManager);

            // attach default cmdMessenger callback
            ArdunioCallbackDefault defaultCallback = new ArdunioCallbackDefault();
            mListener.attach(defaultCallback);

            // attach Test cmdMessenger callback
            ArdunioCallbackDefault testCallback = new ArdunioCallbackDefault();
            mListener.attach(1, testCallback);

        }
    }

    private void cmd_default(String arg) {

    }

    private void onDeviceStateChange() {
        stopIoManager();
        startIoManager();
    }

    public String bleStatus = "hello BLE";
    public String logMsg = "";

    public void l(String s) {
        String tmp;

        synchronized (bleStatus) {
            if (s==null)
                s=logMsg;
            else
                logMsg = s;
            tmp = bleStatus + "\n" + s;
        }

        final String fullText = tmp;

        Log.v(TAG, s);
        runOnUiThread(new Runnable() {
                          @Override
                          public void run() {
                              log.setText(fullText);
                          }
                      });
    }

    public void sendCommand(String s) {
        l("sendCommand:" + s);
        try {
            if (sPort != null) sPort.write(s.getBytes(), 200);
        } catch (IOException e) {
            l("sendCommand err:" + e.getMessage());
        }
        log.append(s + "\r\n");
    }

    //public void sendCommand(String s) {
   //     l("sendCommand:" + s);
    //    mListener.sendCmd()

        //try {
        //    if (sPort != null) sPort.write(s.getBytes(), 200);
        //} catch (IOException e) {
        //    l("sendCommand err:" + e.getMessage());
       // }
    //}

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

        String radioFile = null;

        radioFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() + "/radio_stream3.mp3";
        File f = new File(radioFile);
        if (f.exists()) {
            return radioFile;
        }

        return mContext.getExternalFilesDir(Environment.DIRECTORY_MUSIC).toString() + "/radio_stream3.mp3";
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

    public void DownloadMusic2() {
        try {
            DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            DownloadManager.Request r = new DownloadManager.Request(Uri.parse("http://jonathanclark.com/bbMusic/radio_stream.mp3"));
            //DownloadManager.Request r = new DownloadManager.Request(Uri.parse("http://101apps.co.za/images/headers/101_logo_very_small.jpg"));

            String destPath = GetRadioStreamFile();
            Uri dest = Uri.parse("file://" + destPath);
            r.setDestinationUri( dest);
            File f = new File(destPath);
            if (f.exists()) {

                long len = f.length();
                if (len==230565240) {
                    downloaded = true;
                    return;
                }
                f.delete();
                l("exists but not correct size");
            }


            r.setTitle("Music Downloading...");
            r.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);
            r.setDescription("Downloading big ass MP3");
            r.setVisibleInDownloadsUi(true);
            r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);

            //DownloadManager.ACTION
            long result = dm.enqueue(r);
            l("Download result" + result);


            //DownloadManager.
        }
        catch (Throwable err) {
            String msg = err.getMessage();
            System.out.println(msg);
        }
    }

    long lastSeekOffset = 0;
    long lastSeekTimestamp = 0;

    public void SeekAndPlay() {
        try {
            if (mediaPlayer != null) {
                long ms = CurrentClockAdjusted() + userTimeOffset - phoneModelAudioLatency;

                int lenInMS = mediaPlayer.getDuration()/(5*1024);
                lenInMS *= 5*1024;

                long seekOff = ms % lenInMS;
                long curPos = mediaPlayer.getCurrentPosition();

                long seekErr = 0;
                if (lastSeekOffset !=0 && lastSeekTimestamp<ms) {
                    long expectedPosition = lastSeekOffset + (ms-lastSeekTimestamp);
                    seekErr = (curPos - expectedPosition);
                }

                String msg = "SeekErr " + seekErr + " SvOff "+ serverTimeOffset + " User "+userTimeOffset + "\nSeekOff " + seekOff + " RTT " + serverRTT;
                if (udpClientServer.tSentPackets!=0)
                    msg += "\nSent " + udpClientServer.tSentPackets;
                l(msg);

                if (curPos==0 || Math.abs(curPos-seekOff)>10) {
                    mediaPlayer.seekTo((int) (seekOff - seekErr/4 +60));
                    /* PlaybackParams p;
                    if (p.getClass().getMethod("setSpeed",
                    p.setSpeed(0.5);
                    mediaPlayer.setPlaybackParams(p); */
                    mediaPlayer.start();

                    lastSeekOffset = seekOff;
                    lastSeekTimestamp = ms;
                }
            }
        } catch (Throwable thr_err) {

        }

    }

    public void RadioMode() {
        try {
            FileInputStream fds = new FileInputStream(GetRadioStreamFile());
            mediaPlayer.reset();
            mediaPlayer.setDataSource(fds.getFD());
            fds.close();

            mediaPlayer.setLooping(true);
            mediaPlayer.setVolume(vol, vol);
            mediaPlayer.prepare();
            SeekAndPlay();
        }
        catch (Throwable err) {
            String msg = err.getMessage();
            System.out.println(msg);
        }
    }


    void MusicOffset(int ms) {
        userTimeOffset += ms;
        SeekAndPlay();
        l("UserTimeOffset = " + userTimeOffset);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean handled = false;
        if (event.getRepeatCount() == 0) {
            l("Keycode:" + keyCode);
        }

        switch (keyCode) {
            case 100:
                RadioMode();
                break;
            case 97:
            case 20:
                MusicOffset(-10);
                break;

            case 99:
            case 19:
                MusicOffset(10);
                break;
            case 24:   // native volume up button
            case 21:
                vol += 0.01;
                if (vol > 1) vol = 1;
                mediaPlayer.setVolume(vol, vol);
                l("Volume " + vol * 100.0f + "%");
                return false;
            case 25:  // native volume down button
            case 22:
                vol -= 0.01;
                if (vol < 0) vol = 0;
                mediaPlayer.setVolume(vol, vol);
                l("Volume " + vol * 100.0f + "%");
                return false;
            case 96:
                switchHeadlight.setChecked(true);
                break;
            //case 99 : switchHeadlight.setChecked(false); break;
        }

        if (keyCode == 4) {
            //sendCommand("5,-1;");
            mListener.sendCmdStart(5);
            mListener.sendCmdArg(-1);
            mListener.sendCmdEnd();
        }
        if (keyCode == 66) {
            //sendCommand("5,-2;");
            l("sendCommand: 5,-2");
            mListener.sendCmdStart(5);
            mListener.sendCmdArg(-2);
            mListener.sendCmdEnd();

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

    public class ArdunioCallbackDefault implements CmdEvents {

        public void CmdAction(String str){
            l("ardunio default callback:" + str);
        }

    }

    public class ArdunioCallbackTest implements CmdEvents {

        public void CmdAction(String str){
            l("ardunio test callback:" + str);
        }

    }

}



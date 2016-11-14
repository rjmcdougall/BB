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
import android.media.audiofx.Visualizer;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ImageView;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.media.ToneGenerator;

import com.richardmcdougall.bb.InputManagerCompat;
import com.richardmcdougall.bb.InputManagerCompat.InputDeviceListener;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import com.richardmcdougall.bb.CmdMessenger.CmdEvents;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Exchanger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.ArrayList;

import android.media.AudioManager;
import android.media.MediaPlayer;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import android.os.Environment;

import java.io.OutputStream;
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
import java.util.concurrent.ThreadFactory;

import android.os.AsyncTask;
import android.os.PowerManager;
import android.app.*;
import android.net.*;
import android.Manifest.*;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


public class MainActivity extends AppCompatActivity implements InputDeviceListener {

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
    private float vol = 0.80f;
    private boolean downloaded = false;
    public long serverTimeOffset = 0;
    public long serverRTT = 0;
    private int userTimeOffset = 0;
    public MyWifiDirect mWifi = null;
    public UDPClientServer udpClientServer = null;
    public String boardId;
    ArrayList<MusicStream> streamURLs = new ArrayList<MusicStream>();
    ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
    private int boardMode = 0; // Mode of the Ardunio/LEDs
    private VisualizerView mVisualizerView;
    private String stateMsgAudio = "";
    private String stateMsgConn = "";
    private String stateMsg = "";
    private int statePeers = 0;
    private long stateReplies = 0;
    public byte[] mBoardFFT;
    public String mSerialConn = "";

    int currentRadioStream = 0;
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


    // function to append a string to a TextView as a new line
    // and scroll to the bottom if needed
    private void logMessage(String msg) {
        if (log == null)
            return;
        // append the new string
        log.append(msg + "\n");
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

    public void setStateStats(int npeers, long replies) {
        statePeers = npeers;
        stateReplies = replies;
    }

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

        l("MainActivity: onCreate()");

        String model = android.os.Build.MODEL;

        l("Starting BB on phone " + model);

        if (model.equals("XT1064")) {
            phoneModelAudioLatency = 10;
        } else if (model.equals("BLU DASH M2")) {
            phoneModelAudioLatency = 10;
        } else if (model.equals("BLU ADVANCE 5.0 HD")) {
            phoneModelAudioLatency = 10;
        } else if (model.equals("MSM8916 for arm64")) {
            phoneModelAudioLatency = 20;
        } else {
            phoneModelAudioLatency = 82;
            userTimeOffset = -4;
        }


        InitClock();
        MusicListInit();

        udpClientServer = new UDPClientServer(this);
        udpClientServer.Run();

        mWifi = new MyWifiDirect(this, udpClientServer);

        InitClock();

        super.onCreate(savedInstanceState);
        mContext = getApplicationContext();
        ActivityCompat.requestPermissions(this,
                new String[]{permission.MODIFY_AUDIO_SETTINGS}, 1);
        //ActivityCompat.requestPermissions(this,
        //        new String[]{permission.RECORD_AUDIO}, 1);


        //ActivityCompat.requestPermissions(this,
        // new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        //ActivityCompat.requestPermissions(this,
        // new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        //ActivityCompat.requestPermissions(this,
        // new String[]{permission.BLUETOOTH_ADMIN}, 1);

        /*
        WifiManager mWiFiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);

        if (mWiFiManager.isWifiEnabled()) {
            l("Wifi Enabled Already");
            //mWiFiManager.setWifiEnabled(false);
        } else {
            l("Enabling Wifi...");
            mWiFiManager.setWifiEnabled(true);
            mWiFiManager.reassociate();
        }
        */

        DownloadMusic2();

        // Start Board Display
        Thread t = new Thread(new Runnable() {
            public void run() {
                boardDisplayThread();
            }
        });
        t.start();

        setContentView(R.layout.activity_main);

        // Create the graphic equalizer
        mVisualizerView = (VisualizerView) findViewById(R.id.myvisualizerview);

        // Connect the remote control
        remoteControl = InputManagerCompat.Factory.getInputManager(getApplicationContext());
        remoteControl.registerInputDeviceListener(this, null);

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

        voltage.setText("0.0v");
        log.setText("Hello");
        log.setFocusable(false);

        switchHeadlight = (android.widget.Switch) findViewById(R.id.switchHeadlight);
        switchHeadlight.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                boardSetHeadlight(isChecked);
            }
        });


        if (downloaded)
            RadioMode();

        MusicReset();


        try {
            mVisualizerView.link(mediaPlayer.getAudioSessionId());
            mVisualizerView.addBarGraphRendererBottom();
            //mVisualizerView.addBurnerBoardRenderer(this);
        } catch (Exception e) {
            l("Cannot start visualizer!" + e.getMessage());
        }

        boardVisualizerSetup(mediaPlayer.getAudioSessionId());
    }


    private void updateStatus() {
        float volts;

        volts = boardGetVoltage();

        if (volts > 0) {
            voltage.setText(String.format("%.2f", volts) + "v");
        }

    }

    @Override
    protected void onResume() {
        l("MainActivity: onResume()");


        super.onResume();
        if (mWifi != null)
            mWifi.onResume();

//        loadPrefs();

        initUsb();


    }

    @Override
    protected void onPause() {
        l("MainActivity: onPause()");

        super.onPause();
        if (mWifi != null)
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


    private void initUsb() {
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

        if (!manager.hasPermission(device)) {
            //ask for permission
            PendingIntent pi = PendingIntent.getBroadcast(this, 0, new Intent(GET_USB_PERMISSION), 0);
            mContext.registerReceiver(mPermissionReceiver, new IntentFilter(GET_USB_PERMISSION));
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
            sPort = (UsbSerialPort) mDriver.getPorts().get(0);//Most have just one port (port 0)
            sPort.open(connection);
            sPort.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
            sPort.setDTR(true);
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
        synchronized (mSerialConn) {
            status.setText("Disconnected");
            if (mSerialIoManager != null) {
                l("Stopping io manager ..");
                mSerialIoManager.stop();
                mSerialIoManager = null;
                mListener = null;
            }
        }
    }

    private void startIoManager() {

        synchronized (mSerialConn) {
            if (sPort != null) {
                l("Starting io manager ..");
                //mListener = new BBListenerAdapter();
                mListener = new CmdMessenger(this, sPort, ',', ';', '\\');
                mSerialIoManager = new SerialInputOutputManager(sPort, mListener);
                mExecutor.submit(mSerialIoManager);

                // attach default cmdMessenger callback
                ArdunioCallbackDefault defaultCallback = new ArdunioCallbackDefault();
                mListener.attach(defaultCallback);

                // attach Test cmdMessenger callback
                ArdunioCallbackTest testCallback = new ArdunioCallbackTest();
                mListener.attach(5, testCallback);

                // attach Mode cmdMessenger callback
                ArdunioCallbackMode modeCallback = new ArdunioCallbackMode();
                mListener.attach(4, modeCallback);


                boardId = boardGetBoardId();
                status.setText("Connected to " + boardId);
            }
        }
    }

    private void cmd_default(String arg) {

    }

    private void onDeviceStateChange() {
        l("MainActivity: onDeviceStateChange()");

        stopIoManager();
        startIoManager();
    }

    public String bleStatus = "hello BLE";
    public String logMsg = "";

    public void l(String s) {
        String tmp;


        synchronized (bleStatus) {
            if (s == null)
                s = logMsg;
            else
                logMsg = s;
            tmp = bleStatus + "\n" + s;
        }

        final String fullText = tmp;


        Log.v(TAG, s);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (log != null)
                    logMessage(fullText);
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
                    if (device != null) {
                        initUsb();
                    } else {
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

    public String GetRadioStreamFile(int idx) {
        //String radioFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).toString() + "/radio_stream3.mp3";

        /*
        String radioFile = null;
        radioFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() + "/radio_stream" + idx + ".mp3";
        File f = new File(radioFile);
        if (f.exists()) {
            return radioFile;
        } */

        return mContext.getExternalFilesDir(Environment.DIRECTORY_MUSIC).toString() + "/radio_stream" + idx + ".mp3";
    }

    class MusicStream {
        public String downloadURL;
        public long fileSize;
        public long lengthInSeconds;   // android method for determining the song length seems to vary from OS to OS

        public MusicStream(String url, long _size, long len) {
            downloadURL = url;
            fileSize = _size;
            lengthInSeconds = len;
        }
    }

    ;

    void MusicListInit() {

        streamURLs.add(0, new MusicStream("https://dl.dropboxusercontent.com/s/mcm5ee441mzdm39/01-FunRide2.mp3?dl=0", 122529253, 2 * 60 * 60 + 7 * 60 + 37));
        streamURLs.add(1, new MusicStream("https://dl.dropboxusercontent.com/s/jvsv2fn5le0f6n0/02-BombingRun2.mp3?dl=0", 118796042, 2 * 60 * 60 + 3 * 60 + 44));
        streamURLs.add(2, new MusicStream("https://dl.dropboxusercontent.com/s/j8y5fqdmwcdhx9q/03-RobotTemple2.mp3?dl=0", 122457782, 2 * 60 * 60 + 7 * 60 + 33));
        streamURLs.add(3, new MusicStream("https://dl.dropboxusercontent.com/s/vm2movz8tkw5kgm/04-Morning2.mp3?dl=0", 122457782, 2 * 60 * 60 + 7 * 60 + 33));
        streamURLs.add(4, new MusicStream("https://dl.dropboxusercontent.com/s/52iq1ues7qz194e/Flamethrower%20Sound%20Effects.mp3?dl=0", 805754, 33));
        streamURLs.add(5, new MusicStream("https://dl.dropboxusercontent.com/s/fqsffn03qdyo9tm/Funk%20Blues%20Drumless%20Jam%20Track%20Click%20Track%20Version2.mp3?dl=0", 6532207, 4 * 60 + 32));
        //streamURLs.add(5, new MusicStream("https://dl.dropboxusercontent.com/s/39x2hdu5k5n6628/Beatles%20Long%20Track.mp3?dl=0", 58515039, 2438));
        streamURLs.add(6, new MusicStream("https://dl.dropboxusercontent.com/s/vx11kxtkmhgycd9/click_120bpm_4-4time_610beats_stereo_WjI2zj.mp3?dl=0", 2436288, 5 * 60 + 4));
        streamURLs.add(7, new MusicStream("https://dl.dropboxusercontent.com/s/2m7onrf1i5oobxr/bottle_20bpm_4-4time_610beats_stereo_uUzFTJ.mp3?dl=0", 14616192, 30 * 60 + 30));
    }


    public void DownloadMusic2() {
        try {
            DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            downloaded = true;
            for (int i = 0; i < streamURLs.size(); i++) {
                String destPath = GetRadioStreamFile(i);
                long expectedSize = streamURLs.get(i).fileSize;

                boolean download = true;
                File f = new File(destPath);
                if (f.exists()) {
                    long len = f.length();

                    if (len != expectedSize) {
                        boolean result = f.delete();
                        boolean result2 = f.exists();
                        l("exists but not correct size (" + len + "!=" + expectedSize + "), " +
                                "delete = " + result);
                    } else {
                        l("Already downloaded (" + len + "): " + streamURLs.get(i).downloadURL);
                        download = false;
                    }
                }

                if (download) {
                    DownloadManager.Request r =
                            new DownloadManager.Request(Uri.parse(streamURLs.get(i).downloadURL));

                    r.setTitle("Downloading: " + "Stream " + i + " (" + expectedSize + ")");
                    r.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);
                    r.setDescription("BurnerBoard Radio Stream " + i);
                    r.setVisibleInDownloadsUi(true);
                    r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
                    r.setDestinationUri(Uri.parse("file://" + destPath));

                    downloaded = false;
                    long result = dm.enqueue(r);
                    l("Downloading : " + streamURLs.get(i).downloadURL);
                }
            }
        } catch (Throwable err) {
            String msg = err.getMessage();
            System.out.println(msg);
        }
    }

    long lastSeekOffset = 0;
    long lastSeekTimestamp = 0;

    long GetCurrentStreamLengthInSeconds() {
        return streamURLs.get(currentRadioStream).lengthInSeconds;
    }


    public void SeekAndPlay() {
        try {

            // Update UDP/Audio state
            if (syncStatus != null) {
                syncStatus.setText(stateMsg);
            }

            if (syncPeers != null) {
                syncPeers.setText(String.format("%1$d", statePeers));
            }

            if (syncReplies != null) {
                syncReplies.setText(String.format("%1$d", stateReplies));
            }

            if (mediaPlayer != null && downloaded) {
                synchronized (mediaPlayer) {
                    long ms = CurrentClockAdjusted() + userTimeOffset - phoneModelAudioLatency;

                    long lenInMS = GetCurrentStreamLengthInSeconds() * 1000;

                    long seekOff = ms % lenInMS;
                    long curPos = mediaPlayer.getCurrentPosition();

                    long seekErr = 0;
                    if (lastSeekOffset != 0 && lastSeekTimestamp < ms) {
                        long expectedPosition = lastSeekOffset + (ms - lastSeekTimestamp);
                        seekErr = (curPos - expectedPosition);
                        while (Math.abs(seekErr) > 1000)
                            seekErr /= 2;
                    }

                    String msg =
                            "SeekErr " + seekErr +
                                    " SvOff " + serverTimeOffset +
                                    " User " + userTimeOffset +
                                    "\nSeekOff " + seekOff +
                                    " RTT " + serverRTT +
                                    " Strm" + currentRadioStream;

                    if (udpClientServer.tSentPackets != 0)
                        msg += "\nSent " + udpClientServer.tSentPackets;
                    //l(msg);

                    syncAdjust.setText(String.format("%1$d", seekErr));
                    syncTrack.setText(String.format("%1$d", currentRadioStream));
                    syncUsroff.setText(String.format("%1$d", userTimeOffset));
                    syncSrvoff.setText(String.format("%1$d", serverTimeOffset));
                    syncRTT.setText(String.format("%1$d", serverRTT));

                    if (curPos == 0 || Math.abs(curPos - seekOff) > 8) {
                        int newPos = (int) (seekOff - seekErr / 2 + phoneModelAudioLatency);
                        if (newPos < 0)
                            newPos = 0;
                        //l(msg);
                        setStateMsgAudio("Adjusting");

                        //mediaPlayer.pause();
                        mediaPlayer.seekTo(newPos);
                        /* PlaybackParams p;
                        if (p.getClass().getMethod("setSpeed",
                        p.setSpeed(0.5);
                        mediaPlayer.setPlaybackParams(p); */
                        mediaPlayer.start();


                        lastSeekOffset = seekOff;
                        lastSeekTimestamp = ms;
                    } else {
                        setStateMsgAudio("Synced");
                    }
                }
            }
        } catch (Throwable thr_err) {
            l("SeekAndPlay Error" + thr_err.getMessage());
            try {
                Thread.sleep(3000);
            } catch (Throwable e) {
            }
        }
    }

    void NextStream() {
        int nextRadioStream = currentRadioStream + 1;
        if (nextRadioStream == streamURLs.size())
            nextRadioStream = 0;
        SetRadioStream(nextRadioStream);
    }

    void SetRadioStream(int index) {
        try {
            if (mediaPlayer != null) {
                //synchronized (mediaPlayer) {
                lastSeekOffset = 0;
                currentRadioStream = index;
                FileInputStream fds = new FileInputStream(GetRadioStreamFile(index));
                mediaPlayer.reset();
                mediaPlayer.setDataSource(fds.getFD());
                fds.close();

                mediaPlayer.setLooping(true);
                mediaPlayer.setVolume(vol, vol);
                mediaPlayer.prepare();
                //}
            }
            SeekAndPlay();
        } catch (Throwable err) {
            String msg = err.getMessage();
            System.out.println(msg);
        }
    }

    public void RadioMode() {
        SetRadioStream(currentRadioStream);
    }

    void MusicOffset(int ms) {
        userTimeOffset += ms;
        SeekAndPlay();
        l("UserTimeOffset = " + userTimeOffset);
    }

    public void onVolUp(View v) {
        vol += 0.01;
        if (vol > 1) vol = 1;
        mediaPlayer.setVolume(vol, vol);
        l("Volume " + vol * 100.0f + "%");
    }

    public void onVolDown(View v) {
        vol -= 0.01;
        if (vol < 0) vol = 0;
        mediaPlayer.setVolume(vol, vol);
        l("Volume " + vol * 100.0f + "%");
    }

    public void onNextTrack(View v) {
        NextStream();
    }

    public void onDriftDown(View v) {
        MusicOffset(-2);
    }

    public void onDriftUp(View v) {
        MusicOffset(2);
    }

    public void onModeDown(View v) {
        boardSetMode(98);
        if ((boardMode = boardGetMode()) == -1)
            boardMode--;
        //modeStatus.setText(String.format("%d", boardMode));
    }

    public void onModeUp(View v) {
        boardSetMode(99);
        if ((boardMode = boardGetMode()) == -1)
            boardMode++;
        //modeStatus.setText(String.format("%d", boardMode));
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean handled = false;
        if (event.getRepeatCount() == 0) {
            l("Keycode:" + keyCode);
            System.out.println("Keycode: " + keyCode);
        }


        switch (keyCode) {
            case 100:
            case 87: // satachi right button
                NextStream();
                break;
            case 97:
            case 20:
                MusicOffset(-10);
                break;

            //case 99:
            case 19:
                MusicOffset(10);
                break;
            case 24:   // native volume up button
            case 21:
                onVolUp(null);

                return false;
            case 25:  // native volume down button
            case 22:
                onVolDown(null);
                return false;
            case 99:
                boardSetMode(99);
                break;
            case 98:
                boardSetMode(98);
                break;
            case 88: //satachi left button
                boardSetMode(99);
                break;

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

        public void CmdAction(String str) {

            Log.d(TAG, "ardunio default callback:" + str);
        }

    }

    public class ArdunioCallbackTest implements CmdEvents {

        public void CmdAction(String str) {

            l("ardunio test callback:" + str);
        }

    }

    public class ArdunioCallbackMode implements CmdEvents {

        public void CmdAction(String str) {
            toneG.startTone(ToneGenerator.TONE_PROP_BEEP, 200);
            boardMode = mListener.readIntArg();
            l("ardunio mode callback:" + str + " " + boardMode);
            modeStatus.setText(String.format("%d", boardMode));

        }


    }


    private int boardDisplayCnt = 0;
    private Visualizer mVisualizer;
    private int mAudioSessionId;

    void boardVisualizerSetup(int audioSessionId) {
        int vSize;

        Log.i(TAG, "session=" + audioSessionId);
        mAudioSessionId = audioSessionId;
        // Create the Visualizer object and attach it to our media player.
        try {
            mVisualizer = new Visualizer(audioSessionId);
        } catch (Exception e) {
            Log.e(TAG, "Error enabling visualizer!", e);
            System.out.println("Error enabling visualizer:" + e.getMessage());
            return;
        }
        vSize = Visualizer.getCaptureSizeRange()[1];
        mVisualizer.setEnabled(false);
        mBoardFFT = new byte[vSize];
        mVisualizer.setCaptureSize(vSize);
        mVisualizer.setEnabled(true);
        System.out.println("Enabled visualizer with " + vSize + " bytes");

    }

    int sleepTime = 30;

    // Main thread to drive the Board's display & get status (mode, voltage,...)
    void boardDisplayThread() {


        while (true) {
            switch (boardMode) {
                case 4:
                    sleepTime = 300;
                    modeDisco();
                    break;
                case 5:
                    sleepTime = 30;
                    modeAudioBeat();
                    break;
                case 6:
                    sleepTime = 30;
                    modeAudioBarV();
                    break;
                case 7:
                    sleepTime = 30;
                    modeAudioBarH();
                    break;
                case 8:
                    sleepTime = 1000;
                    modeTest();
                    break;
                default:
//                    modeDisco();
//                    modeAudioBeat();
//                    modeAudioBarV();
//                    modeAudioBarV();
                    break;
            }

            try {
                Thread.sleep(sleepTime);
            } catch (Throwable e) {
            }

            boardDisplayCnt++;
            if (boardDisplayCnt > 1000) {
                //updateStatus();
            }
        }

    }

    private int testState = 0;
    private Random testRandom = new Random();
    private byte[] testRow1 = "012345678901234567890123456789".getBytes();
    private byte[] testRow2 = new byte[] {(byte)32, (byte)0, (byte)32, (byte) 33};


    void modeTest() {
        if (mListener != null) {
            switch (testState) {
                case 0:
                    boardSetRow(10, testRow1);
                    boardUpdate();
                    break;
                case 1:
                    boardSetRow(10, testRow2);
                    boardUpdate();
                    break;
                default:
                    break;
            }
        }
        testState++;
        if (testState > 1)
            testState = 0;
    }


    private int discoState = 0;
    private Random discoRandom = new Random();

    void modeDisco() {
        if (mListener != null) {
            switch (discoState) {
                case 99:
                    mListener.sendCmdStart(13);
                    mListener.sendCmdArg(discoRandom.nextInt(255));
                    mListener.sendCmdArg(discoRandom.nextInt(255));
                    mListener.sendCmdArg(discoRandom.nextInt(255));
                    mListener.sendCmdEnd();
                    mListener.sendCmd(8);
                    break;
                case 0:
                    boardFillScreen(0, 0, 0);
                    boardUpdate();
                    break;
                case 1:
                    boardFillScreen(discoRandom.nextInt(255),
                            discoRandom.nextInt(255),
                            discoRandom.nextInt(255));
                    boardUpdate();
                    break;
                default:
                    //mListener.sendCmd(7);
                    break;
            }
        }
        discoState++;
        if (discoState > 1)
            discoState = 0;
    }

    void modeAudioBeat() {

        if (mBoardFFT == null)
            return;

        if (mVisualizer.getFft(mBoardFFT) == mVisualizer.SUCCESS) {

            /*
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mVisualizerView.updateVisualizerFFT(mBoardFFT.clone());
                }
            });
            */

            int r = 0, g = 0, b = 0;

            //int mDivisions = mBoardFFT.length / 3 / 2;
            int mDivisions = 1;
            byte rfk;
            byte ifk;
            int dbValue = 0;

            for (int i = 0; i < 512; i++) {
                //synchronized (mBoardFFT) {
                rfk = mBoardFFT[mDivisions * (i + 2)];
                ifk = mBoardFFT[mDivisions * (i + 2) + 1];
                //}
                float magnitude = (rfk * rfk + ifk * ifk);
                dbValue += java.lang.Math.max(0, (64 * Math.log10(magnitude)));
                if (dbValue < 0)
                    dbValue = 0;
                r = 0;
                g = 0;
                b = 0;
                /*
                switch (i) {
                    case 0:
                        if (dbValue > 0)
                            r = java.lang.Math.min(255, dbValue);
                        break;
                    case 1:
                        if (dbValue > 0)
                            g = java.lang.Math.min(255, dbValue);
                        break;
                    case 2:
                        if (dbValue > 0)
                            b = java.lang.Math.min(255, dbValue);
                        break;
                }
                */
                r = b = g = java.lang.Math.min(dbValue / 256, 255);

                //if (true || r + g + b > 0) {
                if (mListener != null)
                    boardFillScreen(r, g, b);
                //System.out.println("Visualizer RGB = (" + r + "," + g + "," + b + ")");
                //} else {
                //    if (mListener != null)
                //        boardFade(1);
                // }
                //if (mListener != null)
                //    boardUpdate();
            }
        }
        return;

    }

    void modeAudioBarV() {

        if (mBoardFFT == null)
            return;

        synchronized (mSerialConn) {
            if (mVisualizer.getFft(mBoardFFT) == mVisualizer.SUCCESS) {


                if (mListener != null)
                    mListener.sendCmdStart(14);

                byte rfk;
                byte ifk;
                int dbValue = 0;
                // There are 1024 values - 512 x real, imaginary
                for (int i = 0; i < 512; i++) {
                    rfk = mBoardFFT[i];
                    ifk = mBoardFFT[i + 1];
                    float magnitude = (rfk * rfk + ifk * ifk);
                    dbValue += java.lang.Math.max(0, 5 * Math.log10(magnitude));
                    if (dbValue < 0)
                        dbValue = 0;

                    if ((i & 31) == 0) {
                        int value = java.lang.Math.min(dbValue / 32, 255);
                        dbValue = 0;
                        //System.out.println("Visualizer Value[" + i / 64 + "] = " + value);

                        if (mListener != null && (i > 0))
                            mListener.sendCmdArg(value);
                    }
                }
                if (mListener != null) {
                    //mListener.sendCmdArg((int) 0);
                    mListener.sendCmdEnd();
                }
            }
        }
        return;

    }


    void modeAudioBarH() {

        if (mBoardFFT == null)
            return;

        synchronized (mSerialConn) {
            if (mVisualizer.getFft(mBoardFFT) == mVisualizer.SUCCESS) {


                if (mListener != null)
                    mListener.sendCmdStart(15);

                byte rfk;
                byte ifk;
                int dbValue = 0;
                for (int i = 0; i < 512; i++) {
                    rfk = mBoardFFT[i];
                    ifk = mBoardFFT[i + 1];
                    float magnitude = (rfk * rfk + ifk * ifk);
                    dbValue += java.lang.Math.max(0, 5 * Math.log10(magnitude));
                    if (dbValue < 0)
                        dbValue = 0;

                    if ((i & 63) == 0) {
                        int value = java.lang.Math.min(dbValue / 64, 255);
                        dbValue = 0;
                        //System.out.println("Visualizer Value[" + i / 64 + "] = " + value);

                        if (mListener != null && (i > 63))
                            mListener.sendCmdArg(value);
                    }
                }
                if (mListener != null) {
                    mListener.sendCmdArg((int) 0);
                    mListener.sendCmdEnd();
                }
            }
        }
        return;

    }

    //    cmdMessenger.attach(BBGetVoltage, OnGetVoltage);      // 10
    public float boardGetVoltage() {
        return (float) 0;
    }

    //    cmdMessenger.attach(BBGetBoardID, OnGetBoardID);      // 11
    public String boardGetBoardId() {

        String id;

        if (mListener != null) {
            mListener.sendCmdStart(11);
            mListener.sendCmdEnd(true, 0, 1000);
            id = mListener.readStringArg();
            return (id);
        }
        return "";
    }


    //    cmdMessenger.attach(BBsetmode, Onsetmode);            // 4
    public boolean boardSetMode(int mode) {
        l("sendCommand: 4," + mode);
        if (mListener != null) {
            mListener.sendCmdStart(4);
            mListener.sendCmdArg(mode);
            mListener.sendCmdEnd();
            return true;
        }
        return false;
    }


    //    cmdMessenger.attach(BBFade, OnFade);                  // 7
    public boolean boardFade(int amount) {

        //l("sendCommand: 7");
        if (mListener != null) {
            mListener.sendCmd(7);
            mListener.sendCmdEnd();
            return true;
        }
        return false;
    }

    //    cmdMessenger.attach(BBUpdate, OnUpdate);              // 8
    public boolean boardUpdate() {
        //l("sendCommand: 8");
        synchronized (mSerialConn) {
            if (mListener != null) {
                mListener.sendCmd(8);
                mListener.sendCmdEnd();
                return true;
            }
        }
        return false;
    }

    //    cmdMessenger.attach(BBShowBattery, OnShowBattery);    // 9
    public boolean boardShowBattery() {
        l("sendCommand: 9");
        if (mListener != null) {
            mListener.sendCmd(9);
            mListener.sendCmdEnd();
            return true;
        }
        return false;
    }

    //    cmdMessenger.attach(BBsetheadlight, Onsetheadlight);  // 3
    public boolean boardSetHeadlight(boolean state) {
        l("sendCommand: 3,1");
        if (mListener != null) {
            mListener.sendCmdStart(3);
            mListener.sendCmdArg(state == true ? 1 : 0);
            mListener.sendCmdEnd();
            return true;
        }
        return false;
    }

    //    cmdMessenger.attach(BBClearScreen, OnClearScreen);    // 5
    public boolean boardClearScreen() {
        l("sendCommand: 5");
        if (mListener != null) {
            mListener.sendCmd(5);
            mListener.sendCmdEnd();
            return true;
        }
        return false;
    }

    //    cmdMessenger.attach(BBScroll, OnScroll);              // 6
    public boolean boardScroll(boolean down) {
        l("sendCommand: 6,1");
        if (mListener != null) {
            mListener.sendCmdStart(6);
            mListener.sendCmdArg(down == true ? 1 : 0);
            mListener.sendCmdEnd();
            return true;
        }
        return false;
    }

    //    cmdMessenger.attach(BBGetMode, OnGetMode);            // 12
    public int boardGetMode() {
        l("sendCommand: 12");

        int mode;

        if (mListener != null) {
            mListener.sendCmd(12);
            mListener.sendCmdEnd();
            mode = mListener.readIntArg();
            return (mode);
        }
        return -1;
    }

    //    cmdMessenger.attach(BBFillScreen, OnFillScreen);      // 13
    public boolean boardFillScreen(int r, int g, int b) {
        //l("sendCommand: 13,1");
        synchronized (mSerialConn) {
            if (mListener != null) {
                mListener.sendCmdStart(13);
                mListener.sendCmdArg(r);
                mListener.sendCmdArg(g);
                mListener.sendCmdArg(b);
                mListener.sendCmdEnd();
                return true;
            }
        }
        return false;
    }


    //    cmdMessenger.attach(BBSetRow, OnSetRow);      // 16
    public boolean boardSetRow(int row, byte[] pixels) {
        //l("sendCommand: 16,n,...");
        synchronized (mSerialConn) {
            if (mListener != null) {
                mListener.sendCmdStart(16);
                mListener.sendCmdArg(row);
                mListener.sendCmdEscArg(pixels);
                mListener.sendCmdEnd();
                return true;
            }
        }
        return false;
    }

}



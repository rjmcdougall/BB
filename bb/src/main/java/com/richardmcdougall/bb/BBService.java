package com.richardmcdougall.bb;

import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbManager;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Parcelable;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.KeyEvent;

import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.ByteOrder;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.Date;
import java.net.InetAddress;
import java.util.logging.Level;
import java.util.logging.LogManager;

import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.AudioTrack;
import android.media.AudioFormat;
import android.os.Build;

import android.bluetooth.BluetoothDevice;
import android.media.RingtoneManager;
import android.media.Ringtone;

import org.json.JSONArray;
import org.json.JSONObject;

import no.nordicsemi.android.log.LogSession;
import no.nordicsemi.android.log.Logger;

import static android.bluetooth.BluetoothDevice.ACTION_ACL_CONNECTED;

public class BBService extends Service {

    public static final boolean debug = true;

    private static final String TAG = "BB.BBService";

    // Set to force classic mode when using Emulator
    public static final boolean kEmulatingClassic = false;

    // RPIs don't always have a screen; use beeps -jib
    public static final boolean kBeepOnConnect = BurnerBoardUtil.kIsRPI;

    public static final String ACTION_STATS = "com.richardmcdougall.bb.BBServiceStats";
    public static final String ACTION_BUTTONS = "com.richardmcdougall.bb.BBServiceButtons";
    public static final String ACTION_GRAPHICS = "com.richardmcdougall.bb.BBServiceGraphics";
    public static final String ACTION_USB_DEVICE_ATTACHED = "com.richardmcdougall.bb.ACTION_USB_DEVICE_ATTACHED";
    public static final String ACTION_USB_DEVICE_DETACHED = "com.richardmcdougall.bb.ACTION_USB_DEVICE_DETACHED";
    public static final String ACTION_BB_LOCATION = "com.richardmcdougall.bb.ACTION_BB_LOCATION";
    public static final String ACTION_BB_VOLUME = "com.richardmcdougall.bb.ACTION_BB_VOLUME";
    public static final String ACTION_BB_AUDIOCHANNEL = "com.richardmcdougall.bb.ACTION_BB_AUDIOCHANNEL";
    public static final String ACTION_BB_VIDEOMODE = "com.richardmcdougall.bb.ACTION_BB_VIDEOMODE";
    public static final String ACTION_BB_PACKET = "com.richardmcdougall.bb.ACTION_BB_PACKET";

    public int GetMaxLightModes() {

        return dlManager.GetTotalVideo();
    }

    public int GetMaxAudioModes() {

        return dlManager.GetTotalAudio();
    }

    public DownloadManager dlManager;

    public static enum buttons {
        BUTTON_KEYCODE, BUTTON_TRACK, BUTTON_DRIFT_UP,
        BUTTON_DRIFT_DOWN, BUTTON_MODE_UP, BUTTON_MODE_DOWN, BUTTON_MODE_PAUSE,
        BUTTON_VOL_UP, BUTTON_VOL_DOWN, BUTTON_VOL_PAUSE
    }

    //private BBListenerAdapter mListener = null;
    public Handler mHandler = null;
    private Context mContext;
    private MediaPlayer mediaPlayer = new MediaPlayer();
    private float vol = 0.80f;
    public long serverTimeOffset = 0;
    public long serverRTT = 0;
    private int userTimeOffset = 0;
    public RFClientServer mRfClientServer = null;
    /* XXX TODO this string is accessed both directly here in this class, as well as used via getBoardId() on the object it provides. refactor -jib */
    public static String boardId = BurnerBoardUtil.BOARD_ID;
    /* XXX TODO this string is accessed both directly here in this class, as well as used via getBoardId() on the object it provides. refactor -jib */
    public static String boardType = BurnerBoardUtil.BOARD_TYPE;
    //ArrayList<MusicStream> streamURLs = new ArrayList<BBService.MusicStream>();
    //ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
    private int mBoardMode = 1; // Mode of the Ardunio/LEDs
    BoardVisualization mBoardVisualization = null;
    boolean mServerMode = false;
    IoTClient iotClient = null;
    int mVersion = 0;
    Date mAPKUpdatedDate;
    WifiManager mWiFiManager = null;
    public RF mRadio = null;
    public Gps mGps = null;
    public FindMyFriends mFindMyFriends = null;
    public BluetoothLEServer mBLEServer = null;
    public BluetoothCommands mBluetoothCommands = null;
    public A2dpSink mA2dpSink = null;
    public BluetoothConnManager mBluetoothConnManager = null;
    private boolean mMasterRemote = false;
    private boolean mVoiceAnnouncements = false;
    public final String cpuType = Build.BOARD;

    private int statePeers = 0;
    private long stateReplies = 0;
    //public String mSerialConn = "";

    int currentRadioChannel = 1;
    long phoneModelAudioLatency = 0;

    // IP address of the device
    public String mIPAddress = null;

    TextToSpeech voice;



    private BBService.usbReceiver mUsbReceiver = new BBService.usbReceiver();

    int work = 0;

    public BBService() {
    }

    public void l(String s) {
        Log.v(TAG, s);
        sendLogMsg(s);
    }

    public void d(String s) {
        if (BBService.debug == true) {
            Log.v(TAG, s);
            sendLogMsg(s);
        }
    }

    /* XXX TODO this is here for backwards compat; this used to be computed here and is now in bbutil -jib */
    public static String getBoardId() {
        return BurnerBoardUtil.BOARD_ID;
    }

    /**
     * indicates how to behave if the service is killed
     */
    int mStartMode;

    /**
     * interface for clients that bind
     */
    IBinder mBinder;

    /**
     * indicates whether onRebind should be used
     */
    boolean mAllowRebind;

    /**
     * Called when the service is being created.
     */
    Thread musicPlayer = null;

    /**
     * Called when the service is being created.
     */
    Thread supervisorMonitor = null;


    /**
     * Indicates whether battery monitoring is enabled
     */
    //boolean mEnableBatteryMonitoring = true;
    boolean mEnableBatteryMonitoring = !BurnerBoardUtil.kIsRPI;

    /**
     * Indicates whether IoT reporting is enabled and how often
     */
    //boolean mEnableIoTReporting = true;
    boolean mEnableIoTReporting = !BurnerBoardUtil.kIsRPI;
    int mIoTReportEveryNSeconds = 10;

    /**
     * Indicates whether Wifi reconnecting is enabled and how often
     */
    boolean mEnableWifiReconnect = true;
    int mWifiReconnectEveryNSeconds = 60;


    /* XXX TODO remove me - this looks like dead code -jib
    private static final Map<String, String> BoardNames = new HashMap<String, String>();

    static {
        BoardNames.put("BISCUIT", "Richard");
        BoardNames.put("newproto", "Richard");
    }
    */

    @Override
    public void onCreate() {

        super.onCreate();

        IntentFilter ufilter = new IntentFilter();
        ufilter.addAction("android.hardware.usb.action.USB_DEVICE_ATTACHED");
        ufilter.addAction("android.hardware.usb.action.USB_DEVICE_DETTACHED");
        this.registerReceiver(mUsbReceiver, ufilter);

        mContext = getApplicationContext();


        mWiFiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);

        if (checkWifiOnAndConnected(mWiFiManager) == false) {

            l("Enabling Wifi...");
            setupWifi();

            /*
            if (mWiFiManager.setWifiEnabled(true) == false) {

                l("Failed to enable wifi");
            }
            if (mWiFiManager.reassociate() == false) {
                l("Failed to associate wifi");
            }
            */
        }

        PackageInfo pinfo;
        try {
            pinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            mVersion = pinfo.versionCode;
            mAPKUpdatedDate = new Date(pinfo.lastUpdateTime);
            l("BurnerBoard Version " + mVersion);
            //ET2.setText(versionNumber);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        /*
        if (boardType.contains("BLU")) {
            mServerMode = true;
            l("I am the server");
        }
        */

        l("BBService: onCreate");
        l("I am " + Build.MANUFACTURER + " / " + Build.MODEL + " / " + Build.SERIAL);


        if (iotClient == null) {
            iotClient = new IoTClient(mContext);
        }

        // Start the RF Radio and GPS
        startServices();

        try {
            Thread.sleep(500);
        } catch (Exception e) {
        }
        voice = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                // check for successful instantiation
                if (status == TextToSpeech.SUCCESS) {
                    if (voice.isLanguageAvailable(Locale.UK) == TextToSpeech.LANG_AVAILABLE)
                        voice.setLanguage(Locale.US);
                    l("Text To Speech ready...");
                    voice.setPitch((float) 0.8);
                    String utteranceId = UUID.randomUUID().toString();
                    System.out.println("Where do you want to go, " + boardId + "?");
                    voice.setSpeechRate((float) 0.9);
                    voice.speak("I am " + boardId + "?",
                            TextToSpeech.QUEUE_FLUSH, null, utteranceId);
                } else if (status == TextToSpeech.ERROR) {
                    l("Sorry! Text To Speech failed...");
                }

                // Let the user know they're on a raspberry pi
                if (BurnerBoardUtil.kIsRPI) {
                    String rpiMsg = "Raspberry PI detected";
                    l(rpiMsg);
                    // Use TTS.QUEUE_ADD or it'll talk over the speak() of its name above.
                    voice.speak( rpiMsg, TextToSpeech.QUEUE_ADD, null, "rpi diagnostic");

                    // Let's announce the WIFI IP on RPIs - do it here, as we need voice initialized first
                    if( mIPAddress != null ) {
                        voice.speak( "My WiFi IP is: " + mIPAddress, TextToSpeech.QUEUE_ADD, null, "wifi ip");
                    }
                }
            }
        });

        dlManager = new DownloadManager(getApplicationContext().getFilesDir().getAbsolutePath(),
                boardId, mServerMode, mVersion);
        dlManager.onProgressCallback = new DownloadManager.OnDownloadProgressType() {
            long lastTextTime = 0;

            public void onProgress(String file, long fileSize, long bytesDownloaded) {
                if (fileSize <= 0)
                    return;

                long curTime = System.currentTimeMillis();
                if (curTime - lastTextTime > 30000) {
                    lastTextTime = curTime;
                    long percent = bytesDownloaded * 100 / fileSize;

                    voice.speak("Downloading " + file + ", " + String.valueOf(percent) + " Percent", TextToSpeech.QUEUE_ADD, null, "downloading");
                    lastTextTime = curTime;
                    l(String.format("Downloading %02x%% %s", bytesDownloaded * 100 / fileSize, file));
                }
            }

            public void onVoiceCue(String msg) {
                voice.speak(msg, TextToSpeech.QUEUE_ADD, null, "Download Message");
            }
        };



        HandlerThread mHandlerThread = null;
        mHandlerThread = new HandlerThread("BBServiceHandlerThread");
        mHandlerThread.start();

        mHandler = new Handler(mHandlerThread.getLooper());


        // Register to receive button messages
        IntentFilter filter = new IntentFilter(BBService.ACTION_BUTTONS);
        LocalBroadcastManager.getInstance(this).registerReceiver(mButtonReceiver, filter);

        // Register to know when bluetooth remote connects
        mContext.registerReceiver(btReceive, new IntentFilter(ACTION_ACL_CONNECTED));

        if (musicPlayer == null) {
            l("starting music player thread");
            // Start Music Player
            Thread musicPlayer = new Thread(new Runnable() {
                public void run() {
                    Thread.currentThread().setName("BB Music Player");
                    musicPlayerThread();
                }
            });
            musicPlayer.start();
        } else {
            l("music player already running");
        }

        if (supervisorMonitor == null) {
            l("starting supervisor thread");
            Thread supervisorMonitor = new Thread(new Runnable() {
                public void run() {
                    Thread.currentThread().setName("BB Supervisor");
                    supervisorThread();
                }
            });
            supervisorMonitor.start();
        } else {
            l("supervisor thread already running");
        }

        startLights();

        //mBoardVisualization.attachAudio(mediaPlayer.getAudioSessionId());

        // Supported Languages
        Set<Locale> supportedLanguages = voice.getAvailableLanguages();
        if (supportedLanguages != null) {
            for (Locale lang : supportedLanguages) {
                l("Voice Supported Language: " + lang);
            }
        }
        //startActivity(new Intent(this, MainActivity.class));
    }


    /**
     * The service is starting, due to a call to startService()
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        l("BBService: onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * A client is binding to the service with bindService()
     */
    @Override
    public IBinder onBind(Intent intent) {
        l("BBService: onBind");
        return mBinder;
    }

    /**
     * Called when all clients have unbound with unbindService()
     */
    @Override
    public boolean onUnbind(Intent intent) {
        l("BBService: onUnbind");
        return mAllowRebind;
    }

    /**
     * Called when a client is binding to the service with bindService()
     */
    @Override
    public void onRebind(Intent intent) {
        l("BBService: onRebind");

    }

    /**
     * Called when The service is no longer used and is being destroyed
     */
    @Override
    public void onDestroy() {

        l("BBService: onDesonDestroy");
        voice.shutdown();
    }

    public boolean isMaster() {
        return mMasterRemote;
    }

    public String getAPKUpdatedDate() {
        return mAPKUpdatedDate.toString();
    }

    public String getVersion() {
        return String.valueOf(mVersion);
    }

    public String getIPAddress() {
        return mIPAddress;
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */

    private BurnerBoard mBurnerBoard;

    private void startLights() {

        if (mServerMode == true) {
            return;
        }

        if (kEmulatingClassic || BurnerBoardUtil.isBBClassic()) {
            l( "Visualization: Using Classic");
            mBurnerBoard = new BurnerBoardClassic(this, mContext);
        } else if (BurnerBoardUtil.isBBMast()) {
            l( "Visualization: Using Mast");
            mBurnerBoard = new BurnerBoardMast(this, mContext);
        } else if (BurnerBoardUtil.isBBPanel()) {
            l( "Visualization: Using Panel");
            mBurnerBoard = new BurnerBoardPanel(this, mContext);
        } else if (BurnerBoardUtil.isBBDirectMap()) {
            l( "Visualization: Using Direct Map");
            mBurnerBoard = new BurnerBoardDirectMap(
                this,
                mContext,
                BurnerBoardUtil.kVisualizationDirectMapWidth,
                BurnerBoardUtil.kVisualizationDirectMapHeight
            );
        } else if (BurnerBoardUtil.isBBAzul()) {
            l( "Visualization: Using Azul");
            mBurnerBoard = new BurnerBoardAzul(this, mContext);
        } else {
            l( "Could not identify board type! Falling back to Azul for backwards compatibility");
            mBurnerBoard = new BurnerBoardAzul(this, mContext);
        }

        if (mBurnerBoard == null) {
            l("startLights: null burner board");
            return;
        }
        mBurnerBoard.setText90(mBurnerBoard.boardId, 5000);

        if (mBurnerBoard != null) {
            mBurnerBoard.attach(new BoardCallback());
        }

        if (mBoardVisualization == null) {
            mBoardVisualization = new BoardVisualization(this, mBurnerBoard, this);
        }

        Log.d(TAG, "Setting initial visualization mode: " + mBoardMode);
        mBoardVisualization.setMode(mBoardMode);
    }

    private void startServices() {

        l("StartServices");

        if (mServerMode == true) {
            return;
        }

        // Start the manager for bluetooth discovery/pairing/etc,...

        mBluetoothConnManager = new BluetoothConnManager(this, mContext);
        if (mBluetoothConnManager == null) {
            l("startServices: null BluetoothConnManager object");
            return;
        }


        mBLEServer = new BluetoothLEServer(this, mContext);
        if (mBLEServer == null) {
            l("startServices: null BLE object");
            return;
        }

        if (mBLEServer == null) {
            l("startServices: null BLE object");
            return;
        }

        if (mBLEServer == null) {
            l("startServices: null BLE object");
            return;
        }

        //mA2dpSink = new A2dpSink(this, mContext);

        mRadio = new RF(this, mContext);
        if (mRadio == null) {
            l("startServices: null RF object");
            return;
        }

        InitClock();
        mRfClientServer = new RFClientServer(this, mRadio);
        mRfClientServer.Run();

        mGps = mRadio.getGps();

        if (mGps == null) {
            l("startGps: null gps object");
            return;
        }

        mFindMyFriends = new FindMyFriends(mContext, this, mRadio, mGps, iotClient);


        mBluetoothCommands = new BluetoothCommands(this, mContext, mBLEServer,
                mBluetoothConnManager, mFindMyFriends);

        if (mBluetoothCommands == null) {
            l("startServices: null mBluetoothCommands object");
            return;
        }
        mBluetoothCommands.init();

    }

    public FindMyFriends getFindMyFriends() {
        return mFindMyFriends;
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

    private void sendLogMsg(String msg) {
        Intent in = new Intent(ACTION_STATS);
        in.putExtra("resultCode", Activity.RESULT_OK);
        in.putExtra("msgType", 4);
        // Put extras into the intent as usual
        in.putExtra("logMsg", msg);
        LocalBroadcastManager.getInstance(this).sendBroadcast(in);
    }


    private void cmd_default(String arg) {

    }

    private void onDeviceStateChange() {
        l("BBservice: onDeviceStateChange()");

        mBurnerBoard.stopIoManager();
        mBurnerBoard.startIoManager();
    }


/*
    public void sendCommand(String s) {
        l("sendCommand:" + s);
        try {
            if (sPort != null) sPort.write(s.getBytes(), 200);
        } catch (IOException e) {
            l("sendCommand err:" + e.getMessage());
        }
        //log.append(s + "\r\n");
    }
*/


    // We use this to catch the music buttons
    private final BroadcastReceiver mButtonReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            final String TAG = "mButtonReceiver";

            //Log.d(TAG, "onReceive entered");
            String action = intent.getAction();

            if (ACTION_BUTTONS.equals(action)) {
                Log.d(TAG, "Received BlueTooth key press");
                buttons actionType = (buttons) intent.getSerializableExtra("buttonType");
                switch (actionType) {
                    case BUTTON_KEYCODE:
                        l("BUTTON_KEYCODE ");
                        int keyCode = intent.getIntExtra("keyCode", 0);
                        KeyEvent event = (KeyEvent) intent.getParcelableExtra("keyEvent");
                        onKeyDown(keyCode, event);
                        break;
                    case BUTTON_TRACK:
                        l("BUTTON_TRACK");
                        NextStream();
                        break;
                    case BUTTON_MODE_UP:
                        l("BUTTON_MODE_UP");
                        setMode(99);
                        break;
                    case BUTTON_MODE_DOWN:
                        l("BUTTON_MODE_DOWN");
                        setMode(98);
                        break;
                    case BUTTON_DRIFT_DOWN:
                        l("BUTTON_DRIFT_DOWN");
                        MusicOffset(-10);
                        break;
                    case BUTTON_DRIFT_UP:
                        l("BUTTON_DRIFT_UP");
                        MusicOffset(10);
                        break;
                    case BUTTON_VOL_DOWN:
                        l("BUTTON_VOL_DOWN");
                        onVolDown();
                        break;
                    case BUTTON_VOL_UP:
                        l("BUTTON_VOL_UP");
                        onVolUp();
                        break;
                    case BUTTON_VOL_PAUSE:
                        l("BUTTON_VOL_PAUSE");
                        onVolPause();
                        break;
                    default:
                        break;
                }
            }
        }
    };


    public String GetRadioChannelFile(int idx) {

        if (idx < 1) {
            return "";
        }


        //return (mContext.getExternalFilesDir(
        //Environment.DIRECTORY_MUSIC).toString() + "/test.mp3");

        // RMC get rid of this
        //String radioFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).toString() + "/test.mp3";
        //return radioFile;

        /*
        String radioFile = null;
        radioFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() + "/radio_stream" + idx + ".mp3";
        File f = new File(radioFile);
        if (f.exists()) {
            return radioFile;
        } */

        return mContext.getExternalFilesDir(
                Environment.DIRECTORY_MUSIC).toString() + "/radio_stream" + (idx - 1) + ".mp3";

    }

    public int getCurrentBoardMode() {
        if (mBoardVisualization != null) {
            return mBoardVisualization.getMode();
        } else {
            return 0;
        }
    }

    public int getCurrentBoardVol() {
        int v = getAndroidVolumePercent();
        //return ((int) ((v/(float)100) * (float) 127.0)); // dkw not sure what 127 is for.
        return (v); // dkw not sure what 127 is for.
    }

    public int getBoardVolumePercent() {
        return getAndroidVolumePercent();
 //       return ((int) (vol * (float) 100.0));
    }

    public void setBoardVolume(int v) {
        setAndroidVolumePercent(v);
//        l("Volume: " + vol + " -> " + v);
//        vol = (float) v / (float) 127;
//        mediaPlayer.setVolume(vol, vol);
    }

    public void setRadioVolumePercent(int v) {
        setAndroidVolumePercent(v);
//        l("Volume: " + vol + " -> " + v);
//        vol = (float) v / (float) 100;
//        mediaPlayer.setVolume(vol, vol);
    }

    public int getAndroidVolumePercent(){
        // Get the AudioManager
        AudioManager audioManager =
                (AudioManager)this.getSystemService(Context.AUDIO_SERVICE);

        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        vol = ((float)volume / (float)maxVolume);
        int v = (int) ( vol * (float)100);
        return v;
    }

    public void setAndroidVolumePercent(int v){
        // Get the AudioManager
        AudioManager audioManager =
                (AudioManager)this.getSystemService(Context.AUDIO_SERVICE);

        vol = v / (float)100;
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int setVolume = (int)((float) maxVolume * (float) v / (float) 100);

        audioManager.setStreamVolume (
                AudioManager.STREAM_MUSIC,
                setVolume,
                0);
    }

    public void broadcastVolume(float vol) {
        Intent in = new Intent(ACTION_BB_VOLUME);
        in.putExtra("volume", vol);
        LocalBroadcastManager.getInstance(this).sendBroadcast(in);
    }

    long lastSeekOffset = 0;
    long lastSeekTimestamp = 0;

    long GetCurrentStreamLengthInSeconds() {
        return dlManager.GetAudioLength( currentRadioChannel - 1);
    }

    // Main thread to drive the music player
    void musicPlayerThread() {

        String model = android.os.Build.MODEL;

        l("Starting BB on " + boardId + ", model " + model);

        if (BurnerBoardUtil.kIsRPI) {
            phoneModelAudioLatency = 80;
        } else if (model.equals("imx7d_pico")) {
            phoneModelAudioLatency = 110;
        } else {
            phoneModelAudioLatency = 0;
            userTimeOffset = 0;
        }


        //udpClientServer = new UDPClientServer(this);
        //udpClientServer.Run();


        boolean hasLowLatencyFeature =
                getPackageManager().hasSystemFeature(PackageManager.FEATURE_AUDIO_LOW_LATENCY);

        l("has audio LowLatencyFeature: " + hasLowLatencyFeature);
        boolean hasProFeature =
                getPackageManager().hasSystemFeature(PackageManager.FEATURE_AUDIO_PRO);
        l("has audio ProFeature: " + hasProFeature );

        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        String sampleRateStr = am.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE);
        int sampleRate = Integer.parseInt(sampleRateStr);

        l("audio sampleRate: " + sampleRate );

        String framesPerBuffer = am.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER);
        int framesPerBufferInt = Integer.parseInt(framesPerBuffer);

        l("audio framesPerBufferInt: " + framesPerBufferInt );

        dlManager.StartDownloads();

        if (mServerMode == true) {
            return;
        }

        try {
            Thread.sleep(5000);
        } catch (Exception e) {

        }

        // MediaSession ms = new MediaSession(mContext);

        bluetoothModeInit();

        int musicState = 0;

        while (true) {

            switch (musicState) {
                case 0:
                    if (dlManager.GetTotalAudio() != 0) {
                        musicState = 1;
                        l("Downloaded: Starting Radio Mode");
                        RadioMode();
                    } else {
                        l("Waiting for download");
                        try {
                            Thread.sleep(1000);
                        } catch (Throwable e) {
                        }
                    }
                    break;

                case 1:
                    SeekAndPlay();
                    try {
                        // RMC try 15 seconds instead of 1
                        Thread.sleep(1000);
                    } catch (Throwable e) {
                    }
                    if ( currentRadioChannel == 0) {
                        musicState = 2;
                    }
                    break;

                case 2:
                    bluetoothPlay();
                    if ( currentRadioChannel != 0) {
                        musicState = 1;
                    }
                    break;

                default:
                    break;


            }

        }

    }

    private long seekSave = 0;
    private long seekSavePos = 0;
    public void SeekAndPlay() {
        if (mediaPlayer != null && dlManager.GetTotalAudio() != 0) {
            synchronized (mediaPlayer) {
                long ms = CurrentClockAdjusted() + userTimeOffset - phoneModelAudioLatency;

                long lenInMS = GetCurrentStreamLengthInSeconds() * 1000;

                long seekOff = ms % lenInMS;
                long curPos = mediaPlayer.getCurrentPosition();
                long seekErr = curPos - seekOff;
                d("time/pos: " + curPos + "/" + CurrentClockAdjusted());

                if (curPos == 0 || seekErr != 0) {
                    if (curPos == 0 || Math.abs(seekErr) > 100) {
                        l("SeekAndPlay: explicit seek");
                        // mediaPlayer.pause();
                        // hack: I notice i taked 79ms on dragonboard to seekTo
                        seekSave = SystemClock.elapsedRealtime();
                        seekSavePos = seekOff + 79;
                        // RMC
                        //mediaPlayer.seekTo((int) seekOff + 79);
                        mediaPlayer.seekTo((int) seekOff + 100);
                        mediaPlayer.start();
                    } else {
                        //PlaybackParams params = mediaPlayer.getPlaybackParams();
                        Float speed = 1.0f + (seekOff - curPos) / 1000.0f;
                        if (speed > 1.05f) {
                            //    speed = 1.05f;
                        }
                        if (speed < 0.95f) {
                            //    speed = 0.95f;
                        }
                        d("SeekAndPlay: seekErr = " + seekErr + ", adjusting speed to " + speed);

                        if ((seekErr > 10) || (seekErr < -10)) {
                            l("SeekAndPlay: seekErr = " + seekErr + ", adjusting speed to " + speed);
                        }

                        try {
                            //mediaPlayer.pause();
                            PlaybackParams params = new PlaybackParams();
                            params.allowDefaults();
                            params.setSpeed(speed).setPitch(speed).setAudioFallbackMode(PlaybackParams.AUDIO_FALLBACK_MODE_DEFAULT);
                            mediaPlayer.setPlaybackParams(params);

                            //l("SeekAndPlay: pause()");
                            //mediaPlayer.pause();
                            //mediaPlayer.stop();
                            //l("SeekAndPlay: setPlaybackParams()");
                            //mediaPlayer.setPlaybackParams(mediaPlayer.getPlaybackParams().setSpeed(speed));
                            //mediaPlayer.start();
                            //mediaPlayer.prepareAsync();

                            d("SeekAndPlay: setPlaybackParams() Sucesss!!");
                        } catch (IllegalStateException exception) {
                            l("SeekAndPlay setPlaybackParams IllegalStateException: " + exception.getLocalizedMessage());
                        } catch (Throwable err) {
                            l("SeekAndPlay setPlaybackParams: " + err.getMessage());
                            //err.printStackTrace();
                        }
                        //l("SeekAndPlay: start()");
                        mediaPlayer.start();
                    }
                }

                String msg = "SeekErr " + seekErr + " SvOff " + serverTimeOffset +
                        " User " + userTimeOffset + "\nSeekOff " + seekOff +
                        " RTT " + serverRTT + " Strm" + currentRadioChannel;
                //if (rfClientServer.tSentPackets != 0)
                //    msg += "\nSent " + rfClientServer.tSentPackets;
                d(msg);

                Intent in = new Intent(ACTION_STATS);
                in.putExtra("resultCode", Activity.RESULT_OK);
                in.putExtra("msgType", 1);
                // Put extras into the intent as usual
                in.putExtra("seekErr", seekErr);
                in.putExtra("", currentRadioChannel);
                in.putExtra("userTimeOffset", userTimeOffset);
                in.putExtra("serverTimeOffset", serverTimeOffset);
                in.putExtra("serverRTT", serverRTT);
                LocalBroadcastManager.getInstance(this).sendBroadcast(in);
            }
        }

    }


    public void enableMaster(boolean enable) {
        mMasterRemote = enable;
        if (enable) {
            // Let everyone else know we just decided to be the master
            // Encoding the BOARD_ID as the payload; it's not really needed as we can read that
            // from the client data. XXX is there a better/more useful payload?
            mRfClientServer.sendRemote(kRemoteMasterName, hashTrackName(BurnerBoardUtil.BOARD_ID), RFClientServer.kRemoteMasterName);

            mBurnerBoard.setText("Master", 2000);
            voice.speak( "Master Remote is: " + BurnerBoardUtil.BOARD_ID, TextToSpeech.QUEUE_ADD, null, "enableMaster");
        } else {
            // You explicitly disabled the master. Stop any broadcasting.
            mRfClientServer.disableMasterBroadcast();

            mBurnerBoard.setText("Solo", 2000);
            voice.speak("Disabling Master Remote: " + BurnerBoardUtil.BOARD_ID, TextToSpeech.QUEUE_ADD, null, "disableMaster");
        }
    }

    private boolean bGTFO = true;
    private int stashedAndroidVolumePercent;

    public void GTFO() {

        if(bGTFO){
            bGTFO = false;
            mBoardVisualization.inhibitGTFO(true);
            mBurnerBoard.setText90("Get The Fuck Off!", 5000);
            mediaPlayer.setVolume(0,0);
            stashedAndroidVolumePercent = getAndroidVolumePercent();
            setAndroidVolumePercent(100);
            voice.speak("Hey, Get The Fuck Off!", TextToSpeech.QUEUE_ADD,null , "GTFO");


        }
        else {
            bGTFO=true;
            mBoardVisualization.inhibitGTFO(false);
            setAndroidVolumePercent(stashedAndroidVolumePercent);
            mediaPlayer.setVolume(1,1);
        }

    }

    public static final int kRemoteAudioTrack = 0x01;
    public static final int kRemoteVideoTrack = 0x02;
    public static final int kRemoteMute = 0x03;
    public static final int kRemoteMasterName = 0x04;

    // TODO: Put this back as a remote control packet
    // Change value -> hash lookup
    public void decodeRemoteControl(String client, int cmd, long value) {

        l("Received remote cmd, value " + cmd + ", " + value + " from: " + client);

        switch (cmd) {
            case kRemoteAudioTrack:

                for (int i = 1; i <= getRadioChannelMax(); i++) {
                    String name = getRadioChannelInfo(i);
                    long hashed = hashTrackName(name);
                    if (hashed == value) {
                        l("Remote Audio " + getRadioChannel() + " -> " + i);
                        if (getRadioChannel() != i) {
                            SetRadioChannel((int) i);
                            l("Received remote audio switch to track " + i + " (" + name + ")");
                        } else {
                            l("Ignored remote audio switch to track " + i + " (" + name + ")");
                        }
                        break;
                    }
                }
                break;

            case kRemoteVideoTrack:
                for (int i = 1; i <= getVideoMax(); i++) {
                    String name = getVideoModeInfo(i);
                    long hashed = hashTrackName(name);
                    if (hashed == value) {
                        l("Remote Video " + getVideoMode() + " -> " + i);
                        if (getVideoMode() != i) {
                            setVideoMode((int) i);
                            l("Received remote video switch to mode " + i + " (" + name + ")");
                        } else {
                            l("Ignored remote video switch to mode " + i + " (" + name + ")");
                        }
                        break;
                    }
                }
                break;
            case kRemoteMute:
                if (value != getCurrentBoardVol()) {
                    //System.out.println("UDP: set vol = " + boardVol);
                    setBoardVolume((int)value);
                }
                break;
            case kRemoteMasterName:
                if( mMasterRemote ) {
                    // This board thinks it's the master, but apparently it's no longer. Reset master
                    // mode and follow the new master
                    String diag = BurnerBoardUtil.BOARD_ID + " is no longer the master. New master: " + client;
                    l( diag );
                    voice.speak(diag, TextToSpeech.QUEUE_ADD, null, "master reset");
                    enableMaster(false);
                }

            default:
                break;
        }
    }

    private void RadioStop() {

    }

    private void RadioResume() {

    }


    /*
    Per Richard: Track 0 only plays on a single board. On every other board, this is silence.
    Considering this a feature for now, as it lets you 'turn off' music with a remote. -jib
     */

    void NextStream() {
        int nextRadioChannel = currentRadioChannel + 1;
        if (nextRadioChannel > dlManager.GetTotalAudio())
            nextRadioChannel = 0;
        SetRadioChannel(nextRadioChannel);

    }

    void PreviousStream() {
        int nextRadioChannel = currentRadioChannel - 1;
        if (nextRadioChannel < 0 ) {
            nextRadioChannel = 0;
        }
        SetRadioChannel(nextRadioChannel);

    }

    public String getRadioChannelInfo(int index) {
        return dlManager.GetAudioFileLocalName(index - 1);
    }

    public String getVideoModeInfo(int index) {
        return dlManager.GetVideoFileLocalName(index - 1);
    }

    public int getRadioChannel() {

        l("GetRadioChannel: ");
        return currentRadioChannel;
    }

    public int getVideoMode() {
        return getCurrentBoardMode();
    }

    public int getRadioChannelMax() {
        return dlManager.GetTotalAudio();
    }
    public int getVideoMax() {
        return dlManager.GetTotalVideo();
    }

    // For reasons I don't understand, VideoMode() = 0 doesn't have a profile associated with it.
    // VideoMode() = 1 sets it to the beginning of the profile.
    void NextVideo() {
        int next = getVideoMode() + 1;
        if (next > getVideoMax()) {
            //next = 0;
            next = 1;
        }
        l( "Setting Video to: " + getVideoModeInfo(next) );
        mBoardVisualization.setMode(next);
    }

    void PreviousVideo() {
        int next = getVideoMode() - 1;
        if (next > getVideoMax()) {
            //next = 0;
            next = 1;
        }
        l( "Setting Video to: " + getVideoModeInfo(next) );
        mBoardVisualization.setMode(next);
    }

    // Hash String as 32-bit
    public long hashTrackName(String name) {
        byte[] encoded = {0, 0, 0, 0};
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            encoded = digest.digest(name.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            l("Could not calculate boardAddress");
            return -1;
        }
        return (encoded[0] << 24) + (encoded[1] << 16) + (encoded[2] << 8) + encoded[0];
    }

    // Set radio input mode 0 = bluetooth, 1-n = tracks
    public void SetRadioChannel(int index) {
        l("SetRadioChannel: " + index);
        currentRadioChannel = index;


        if (mServerMode == true) {
            return;
        }

        // If I am set to be the master, broadcast to other boards
        if (mMasterRemote && (mRfClientServer != null)) {

            l("Sending remote");

            String fileName = getRadioChannelInfo(index);
            mRfClientServer.sendRemote(kRemoteAudioTrack, hashTrackName(fileName), RFClientServer.kRemoteAudio);
            // Wait for 1/2 RTT so that we all select the same track/video at the same time
            try {
                Thread.sleep(mRfClientServer.getLatency());
            } catch (Exception e) {
            }

        }

        if (index == 0) {
            mediaPlayer.pause();
            l("Bluetooth Mode");
            mBurnerBoard.setText("Bluetooth", 2000);
            if (mVoiceAnnouncements) {
                voice.speak("Blue tooth Audio", TextToSpeech.QUEUE_FLUSH, null, "bluetooth");
            }
            try {
                Thread.sleep(1000);
            } catch (Throwable e) {
            }
            bluetoothModeEnable();

        } else {
            l("Radio Mode");
            String [] shortName = getRadioChannelInfo(index).split("\\.", 2);
            mBurnerBoard.setText(shortName[0], 2000);
            if (mVoiceAnnouncements) {
                voice.speak("Track " + index, TextToSpeech.QUEUE_FLUSH, null, "track");
            }
            bluetoothModeDisable();
            try {
                if (mediaPlayer != null && dlManager.GetTotalAudio() != 0) {
                    synchronized (mediaPlayer) {
                        lastSeekOffset = 0;
                        FileInputStream fds = new FileInputStream(dlManager.GetAudioFile(index - 1));
                        l("playing file " + dlManager.GetAudioFile(index - 1));
                        mediaPlayer.reset();
                        mediaPlayer.setDataSource(fds.getFD());
                        fds.close();

                        mediaPlayer.setLooping(true);
                        mediaPlayer.setVolume(vol, vol);

                        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                            @Override
                            public void onPrepared(MediaPlayer mediaPlayer) {
                                l("onPrepared");
                                try {
                                    //mediaPlayer.setPlaybackParams(mediaPlayer.getPlaybackParams().setSpeed(0.55f));
                                    PlaybackParams params = mediaPlayer.getPlaybackParams();
                                    l("Playbackparams = " + params.toString());
                                } catch (Exception e) {
                                    l("Error in onPrepared:" + e.getMessage());
                                }
                                mediaPlayer.start();
                            }
                        });
                        mediaPlayer.setOnSeekCompleteListener(
                                new MediaPlayer.OnSeekCompleteListener() {
                                    @Override
                                    public void onSeekComplete(MediaPlayer mediaPlayer) {
                                        long curPos = mediaPlayer.getCurrentPosition();
                                        long curTime = SystemClock.elapsedRealtime();
                                        d("Seek complete - off: " +
                                                (curPos - seekSavePos) +
                                                " took: " + (curTime - seekSave) + " ms");
                                    }
                                });
                        //SyncParams syncParams = new SyncParams();
                        //l("syncParams.getAudioAdjustMode() = " + syncParams.getAudioAdjustMode());
                        //l("syncParams.getTolerance() = " + syncParams.getTolerance());
                        //mediaPlayer.setSyncParams(new SyncParams());
                        mediaPlayer.prepareAsync();
                        SeekAndPlay();
                        SeekAndPlay();
                        mBoardVisualization.attachAudio(mediaPlayer.getAudioSessionId());
                    }
                }
                SeekAndPlay();
            } catch (Throwable err) {
                String msg = err.getMessage();
                l("Radio mode failed" + msg);
                System.out.println(msg);
            }
        }
    }

    public void RadioMode() {
        SetRadioChannel(currentRadioChannel);
    }
    public void setVideoMode(int mode) {
        setMode(mode);
    }

    private AudioRecord mAudioInStream;
    private AudioTrack mAudioOutStream;
    private AudioManager mAudioManager;
    private byte[] mAudioBuffer;

    public void bluetoothModeInit() {
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        AudioDeviceInfo[] deviceList = mAudioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
        for (int index = 0; index < deviceList.length; index++) {
            l("Audio device" + deviceList[index].toString());
        }
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
        int buffersize = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT);
        mAudioInStream = new AudioRecord(MediaRecorder.AudioSource.MIC, 44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, buffersize);
        mAudioOutStream = new AudioTrack(AudioManager.STREAM_VOICE_CALL, 44100, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, buffersize, AudioTrack.MODE_STREAM);
        mAudioBuffer = new byte[buffersize];
        try {
            mAudioInStream.startRecording();
        } catch (Exception e) {
            e.printStackTrace();
        }
        mAudioOutStream.play();
    }

    public void bluetoothModeEnable() {
        mAudioInStream.startRecording();
        //mAudioInStream.setPreferredDevice(AudioDeviceInfo.TYPE_WIRED_HEADSET);
        mBoardVisualization.attachAudio(mAudioOutStream.getAudioSessionId());
        mAudioOutStream.play();
        mAudioOutStream.setPlaybackRate(44100);
        mAudioOutStream.setVolume(vol);
    }

    public void bluetoothModeDisable() {
        try {
            mAudioInStream.stop();
            mAudioOutStream.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void bluetoothPlay() {
        mAudioInStream.read(mAudioBuffer, 0, mAudioBuffer.length);
        mAudioOutStream.write(mAudioBuffer, 0, mAudioBuffer.length);
    }


    void MusicOffset(int ms) {
        userTimeOffset += ms;
        SeekAndPlay();
        l("UserTimeOffset = " + userTimeOffset);
    }

    public void onVolUp() {
        vol += 0.01;
        if (vol > 1) vol = 1;
        mediaPlayer.setVolume(vol, vol);
        l("Volume " + vol * 100.0f + "%");
    }

    public void onVolDown() {
        vol -= 0.01;
        if (vol < 0) vol = 0;
        mediaPlayer.setVolume(vol, vol);
        l("Volume " + vol * 100.0f + "%");
    }

    float recallVol = 0;

    public void onVolPause() {
        if (vol > 0) {
            recallVol = vol;
            vol = 0;
        } else {
            vol = recallVol;
        }
        mediaPlayer.setVolume(vol, vol);
        l("Volume " + vol * 100.0f + "%");
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (!BurnerBoardUtil.kIsRPI) {
            return onKeyDownBurnerBoard(keyCode, event);
        } else {
            return onKeyDownRPI(keyCode, event);

        }
    }

    public boolean onKeyDownBurnerBoard(int keyCode, KeyEvent event) {
        boolean handled = false;
        if (event.getRepeatCount() == 0) {
            l("BurnerBoard Keycode:" + keyCode);
            //System.out.println("Keycode: " + keyCode);
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
                onVolUp();

                return false;
            case 25:  // native volume down button
            case 22:
                onVolDown();
                return false;
            case 85: // Play button - show battery
                onBatteryButton();
                break;
            case 99:
                setMode(99);
                break;
            case 98:
                setMode(98);
                break;
            case 88: //satachi left button
                setMode(99);
                break;
        }
        //mHandler.removeCallbacksAndMessages(null);
        return true;
    }

    public boolean onKeyDownRPI(int keyCode, KeyEvent event) {
        boolean handled = false;
        if (event.getRepeatCount() == 0) {
            l("RPI Keycode:" + keyCode);
            //System.out.println("Keycode: " + keyCode);
        }

        switch (keyCode) {
            case 85: // satachi Play button
                //onBatteryButton();
                // Do something more useful here. Like turn on/off lights?
                l("RPI Bluetooth Play Button");
                //voice.speak( "I'm sorry Dave, I can't let you do that", TextToSpeech.QUEUE_FLUSH, null, "keycode");
                NextVideo();
                break;

            /* Audio stream control */
            case 87: // satachi right button
                l("RPI Bluetooth Right Button");
                NextStream();
                break;
            case 88: //satachi left button
                l("RPI Bluetooth Left Button");
                PreviousStream();
                break;

            /* Volume control */
            case 24:   // satachi & native volume up button
                l("RPI Bluetooth Volume Up Button");
                onVolUp();
                return false;
            case 25:  // satachi & native volume down button
                l("RPI Bluetooth Volume Down Button");
                onVolDown();
                return false;
        }
        //mHandler.removeCallbacksAndMessages(null);
        return true;
    }

    public void setMode(int mode) {
        //boolean cansetMode = mBurnerBoard.setMode(50);
        //boolean cansetMode = mBurnerBoard.setMode(mode);
        //if (cansetMode == false) {
        // Likely not connected to physical burner board, fallback
        if (mode == 99) {
            mBoardMode++;
        } else if (mode == 98) {
            mBoardMode--;
        } else {
            mBoardMode = mode;
        }
        //}
        int maxModes = GetMaxLightModes();
        if (mBoardMode > maxModes)
            mBoardMode = 1;
        else if (mBoardMode < 1)
            mBoardMode = maxModes;

        // If I am set to be the master, broadcast to other boards
        if (mMasterRemote && (mRfClientServer != null)) {


            String name = getVideoModeInfo(mBoardMode);
            l("Sending video remote for video " + name);
            mRfClientServer.sendRemote(kRemoteVideoTrack, hashTrackName(name), RFClientServer.kRemoteVideo);
            // Wait for 1/2 RTT so that we all select the same track/video at the same time
            /*
            try {
                Thread.sleep(mRfClientServer.getLatency());
            } catch (Exception e) {
            }
            */
        }

        if (mBoardVisualization != null && mBurnerBoard != null) {
            l("SetMode:" + mBoardVisualization.getMode() + " -> " + mode);
            mBoardVisualization.setMode(mBoardMode);
            mBurnerBoard.setMode(mBoardMode);
        }

        if (mVoiceAnnouncements) {
            voice.speak("mode" + mBoardMode, TextToSpeech.QUEUE_FLUSH, null, "mode");
        }
    }


    public class BoardCallback implements BurnerBoard.BoardEvents {

        public void BoardId(String str) {
            boardId = str;
            l("ardunio BoardID callback:" + str + " " + boardId);
            //status.setText("Connected to " + boardId);
        }

        public void BoardMode(int mode) {
            //mBoardMode = mode;
            //mBoardVisualization.setMode(mBoardMode);
            //voice.speak("mode" + mBoardMode, TextToSpeech.QUEUE_FLUSH, null, "mode");
            l("ardunio mode callback:" + mBoardMode);
            //modeStatus.setText(String.format("%d", mBoardMode));
        }
    }


    public int getBatteryLevel() {
        return mBurnerBoard.getBattery();
    }


    private int loopCnt = 0;

    private void supervisorThread() {

        /* Communicate the settings for the supervisor thread */
        l("Enable Battery Monitoring? " + mEnableBatteryMonitoring);
        l("Enable IoT Reporting? " + mEnableIoTReporting);
        l("Enable WiFi reconnect? " + mEnableWifiReconnect);

        while (true) {

            // Every 60 seconds check WIFI
            if (mEnableWifiReconnect && (loopCnt % mWifiReconnectEveryNSeconds == 0)) {
                checkWifiReconnect();
            }

            // Every second, check & update battery
            if (mEnableBatteryMonitoring && (mBurnerBoard != null)) {
                checkBattery();

                // Every 10 seconds, send battery update via IoT.
                // Only do this if we're actively checking the battery.
                if (mEnableIoTReporting && (loopCnt % mIoTReportEveryNSeconds == 0)) {
                    l("Sending MQTT update");
                    try {
                        iotClient.sendUpdate("bbtelemetery", mBurnerBoard.getBatteryStats());
                    } catch (Exception e) {
                    }
                }
            }

            try {
                Thread.sleep(1000);
            } catch (Throwable e) {
            }

            loopCnt++;
        }
    }

    private long lastOkStatement = System.currentTimeMillis();
    private long lastLowStatement = System.currentTimeMillis();
    private long lastUnknownStatement = System.currentTimeMillis();
    private static enum powerStates { STATE_CHARGING, STATE_IDLE, STATE_DISPLAYING };

    private void checkBattery() {
        if ((mBurnerBoard != null) && (mBoardVisualization != null)) {

            boolean announce = false;
            powerStates powerState = powerStates.STATE_DISPLAYING;

            int level = mBurnerBoard.getBattery();
            int current = mBurnerBoard.getBatteryCurrent();
            int currentInstant = mBurnerBoard.getBatteryCurrentInstant();
            int voltage = mBurnerBoard.getBatteryVoltage();

            l("Board Current(avg) is " + current);
            l("Board Current(Instant) is " + currentInstant);
            l("Board Voltage is " + voltage);

            // Save CPU cycles for lower power mode
            // current is milliamps
            // Current with brain running is about 100ma
            // Check voltage to make sure we're really reading the battery gauge
            // Make sure we're not seeing +ve current, which is charging
            // Average current use to enter STATE_IDLE
            // Instant current used to exit STATE_IDLE
            if ((voltage > 20000) && (current > -150) && (current < 10)) {
                // Any state -> IDLE
                powerState = powerStates.STATE_IDLE;
                mBoardVisualization.inhibit(true);
            } else if ((voltage > 20000) && (currentInstant < -150)) {
                // Any state -> Displaying
                powerState = powerStates.STATE_DISPLAYING;
                mBoardVisualization.inhibit(false);
            } else if (powerState == powerStates.STATE_DISPLAYING &&
                    // DISPLAYING -> Charging (avg current)
                    (voltage > 20000) && (current > 10)) {
                powerState = powerStates.STATE_CHARGING;
                mBoardVisualization.inhibit(false);
            } else if (powerState == powerStates.STATE_IDLE &&
                    (voltage > 20000) && (currentInstant > 10)) {
                // STATE_IDLE -> Charging // instant
                powerState = powerStates.STATE_CHARGING;
                mBoardVisualization.inhibit(false);
            } else if ((voltage > 20000) && (current > 10)) {
                // Anystate -> Charging // avg current
                powerState = powerStates.STATE_CHARGING;
                mBoardVisualization.inhibit(false);
            } else {
                l("Unhandled power state " + powerState);
                mBoardVisualization.inhibit(false);
            }

            l("Power state is " + powerState);

            // Show battery if charging
            mBoardVisualization.showBattery(powerState == powerStates.STATE_CHARGING);

            // Battery voltage is critically low
            // Board will come to a halt in < 60 seconds
            // current is milliamps
            if ((voltage > 20000) && (voltage < 35300) ){
                mBoardVisualization.emergency(true);
            } else {
                mBoardVisualization.emergency(false);
            }

            announce = false;
            /*
            if (level < 0) {
                if (System.currentTimeMillis() - lastUnknownStatement > 900000) {
                    lastUnknownStatement = System.currentTimeMillis();
                    voice.speak("Battery level unknown", TextToSpeech.QUEUE_FLUSH, null, "batteryUnknown");
                }
            }
            */

            if ((level >= 0) && (level < 15)) {
                if (System.currentTimeMillis() - lastOkStatement > 60000) {
                    lastOkStatement = System.currentTimeMillis();
                    announce = true;
                }
            } else if ((level >= 0) && (level <= 25)) {
                if (System.currentTimeMillis() - lastLowStatement > 300000) {
                    lastLowStatement = System.currentTimeMillis();
                    announce = true;
                }

            } else if (false) {
                if (System.currentTimeMillis() - lastOkStatement > 1800000) {
                    lastOkStatement = System.currentTimeMillis();
                    announce = true;
                }
            }
            if (announce) {
                voice.speak("Battery Level is " +
                        level + " percent", TextToSpeech.QUEUE_FLUSH, null, "batteryLow");
            }
        }
    }

    // Single press to show battery
    // Double press to show map
    // Tripple click to toggle master
    private long lastPressed = SystemClock.elapsedRealtime();
    private int pressCnt = 1;
    private void onBatteryButton() {
        if (mBurnerBoard != null) {
            mBurnerBoard.showBattery();
            if ((SystemClock.elapsedRealtime() - lastPressed) < 600) {
                if (pressCnt == 1) {
                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (pressCnt == 2) {
                                mBoardVisualization.showMap();
                            } else if (pressCnt == 3) {
                                // Toggle master mode
                                if (mMasterRemote == true) {
                                    enableMaster(false);
                                } else {
                                    enableMaster(true);
                                }
                            }
                        }
                    }, 700);
                }
                pressCnt++;
            } else {
                pressCnt = 1;
            }
        }
        lastPressed = SystemClock.elapsedRealtime();
    }

    //you can get notified when a new device is connected using Broadcast receiver
    private final BroadcastReceiver btReceive = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            l("Bluetooth connected");

            String action = intent.getAction();
            //BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                //voice.speak("Connected", TextToSpeech.QUEUE_FLUSH, null, "Connected");
                //the device is found
                try {
                    if (kBeepOnConnect) {
                        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

                        Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                        r.play();
                    }
                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mBurnerBoard.flashScreen(400);

                        }
                    }, 3000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };

    public class usbReceiver extends BroadcastReceiver {
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

                    if (mBurnerBoard != null) {
                        mBurnerBoard.initUsb();
                    }
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

    private void setupWifi() {
        this.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
              int extraWifiState =
                      intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE ,
                              WifiManager.WIFI_STATE_UNKNOWN);

              switch(extraWifiState){
                  case WifiManager.WIFI_STATE_DISABLED:
                      l("WIFI STATE DISABLED");
                      break;
                  case WifiManager.WIFI_STATE_DISABLING:
                      l("WIFI STATE DISABLING");
                      break;
                  case WifiManager.WIFI_STATE_ENABLED:
                      l("WIFI STATE ENABLED");
                      int mfs = mWiFiManager.getWifiState();
                      l("Wifi state is " + mfs);
                      l("Checking wifi");
                      if (checkWifiSSid(BurnerBoardUtil.WIFI_SSID) == false) {
                          l("adding wifi: " + BurnerBoardUtil.WIFI_SSID);
                          addWifi(BurnerBoardUtil.WIFI_SSID, BurnerBoardUtil.WIFI_PASS);
                      }
                      l("Connecting to wifi");
                      connectWifi(BurnerBoardUtil.WIFI_SSID);
                      break;
                  case WifiManager.WIFI_STATE_ENABLING:
                      l("WIFI STATE ENABLING");
                      break;
                  case WifiManager.WIFI_STATE_UNKNOWN:
                      l("WIFI STATE UNKNOWN");
                      break;
              }
            }
        },
        new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));
    }

    private boolean checkWifiOnAndConnected(WifiManager wifiMgr) {

        if (wifiMgr.isWifiEnabled()) { // Wi-Fi adapter is ON

            WifiInfo wifiInfo = wifiMgr.getConnectionInfo();

            if( wifiInfo.getNetworkId() == -1 ){
                mIPAddress = null;
                return false; // Not connected to an access point
            }

            mIPAddress = getWifiIpAddress(wifiMgr);
            if( mIPAddress != null) {
                l("WIFI IP Address: " + mIPAddress);
                // Text to speach is not set up yet at this time; move it to init loop.
                //voice.speak("My WIFI IP is " + mIPAddress, TextToSpeech.QUEUE_ADD, null, "wifi ip");
            } else {
                l( "Could not determine WIFI IP at this time");
            }

            return true; // Connected to an access point
        }
        else {
            mIPAddress = null;
            return false; // Wi-Fi adapter is OFF
        }
    }

    private void checkWifiReconnect() {
        if (checkWifiOnAndConnected(mWiFiManager) == false) {
            l("Enabling Wifi...");
            if (mWiFiManager.setWifiEnabled(true) == false) {
                l("Failed to enable wifi");
            }
            if (mWiFiManager.reassociate() == false) {
                l("Failed to associate wifi");
            }
        }
    }

    /* On android, wifi SSIDs and passwords MUST be passed quoted. This fixes up the raw SSID & Pass -jib */
    /* XXX TODO this code doesn't get exercised if the SSID is already in the known config it seems
       which makes this code VERY hard to test. What's the right strategy? -jib

        Here's some test code to run to manually verify:

        String ssid = BurnerBoardUtil.WIFI_SSID;
        String pass = BurnerBoardUtil.WIFI_PASS;
        String qssid = "\"" + ssid + "\"";
        String qpass = "\"" + pass + "\"";

        String fssid = fixWifiSSidAndPass((ssid));
        String fpass = fixWifiSSidAndPass(pass);
        String fqssid = fixWifiSSidAndPass(qssid);
        String fqpass = fixWifiSSidAndPass(qpass);

        boolean ssid_eq = fssid.equals(fqssid);
        boolean pass_eq = fpass.equals(fqpass);

        l("ssid: " + ssid + " - qssid: " + qssid + " - fssid: " + fssid + " - fqssid: " + fqssid + " - equals? " + ssid_eq);
        l("pass: " + pass + " - qpass: " + qpass + " - fpass: " + fpass + " - fqpass: " + fqpass + " - equals? " + pass_eq);

     */
    private String fixWifiSSidAndPass(String ssid) {
        String fixedSSid = ssid;
        fixedSSid = ssid.startsWith("\"") ? fixedSSid : "\"" + fixedSSid;
        fixedSSid = ssid.endsWith("\"") ? fixedSSid : fixedSSid + "\"";
        return fixedSSid;
    }

    private boolean checkWifiSSid(String ssid) {

        String aWifi = fixWifiSSidAndPass(ssid);

        try {
            List<WifiConfiguration> wifiList = mWiFiManager.getConfiguredNetworks();
            if (wifiList != null) {
                for (WifiConfiguration config : wifiList) {
                    String newSSID = config.SSID;
                    l("Found wifi:" + newSSID + " == " + aWifi + " ?");
                    if (aWifi.equals(newSSID)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
        }
        return false;
    }


    private void connectWifi(String ssid) {

        String aWifi =fixWifiSSidAndPass(ssid);

        try {
            List<WifiConfiguration> wifiList = mWiFiManager.getConfiguredNetworks();
            if (wifiList != null) {
                for (WifiConfiguration config : wifiList) {
                    String newSSID = config.SSID;

                    l("Found wifi:" + newSSID + " == " + aWifi + " ?");

                    if (aWifi.equals(newSSID)) {
                        l("connecting wifi:" + newSSID);
                        mWiFiManager.disconnect();
                        mWiFiManager.enableNetwork(config.networkId, true);
                        mWiFiManager.reconnect();

                        return;
                    }
                }
            }
        } catch (Exception e) {
        }
    }

    private void addWifi(String ssid, String pass) {
        try {

            WifiConfiguration conf = new WifiConfiguration();
            conf.SSID = fixWifiSSidAndPass(ssid);
            conf.preSharedKey = fixWifiSSidAndPass(pass);

            mWiFiManager.addNetwork(conf);
            mWiFiManager.disconnect();
            mWiFiManager.enableNetwork(conf.networkId, false);
            mWiFiManager.reconnect();
        } catch (Exception e) {
        }
        return;
    }

    // Cargo culted directly off of stack overflow: https://stackoverflow.com/questions/16730711/get-my-wifi-ip-address-android
    public String getWifiIpAddress(WifiManager wifiManager) {
        int ipAddress = wifiManager.getConnectionInfo().getIpAddress();

        // Convert little-endian to big-endianif needed
        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
            ipAddress = Integer.reverseBytes(ipAddress);
        }

        byte[] ipByteArray = BigInteger.valueOf(ipAddress).toByteArray();

        String ipAddressString;
        try {
            ipAddressString = InetAddress.getByAddress(ipByteArray).getHostAddress();
        } catch (Exception ex) {
            l( "Unable to get host address: " + ex.toString());
            ipAddressString = null;
        }

        return ipAddressString;
    }

}





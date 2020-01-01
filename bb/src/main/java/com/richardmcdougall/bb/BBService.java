package com.richardmcdougall.bb;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.support.v4.content.LocalBroadcastManager;

import java.util.Calendar;
import java.util.Locale;
import java.util.UUID;

import timber.log.Timber;

import android.os.Build;

import static timber.log.Timber.DebugTree;

import static android.bluetooth.BluetoothDevice.ACTION_ACL_CONNECTED;

public class BBService extends Service {

    public enum buttons {
        BUTTON_KEYCODE, BUTTON_TRACK, BUTTON_DRIFT_UP,
        BUTTON_DRIFT_DOWN, BUTTON_MODE_UP, BUTTON_MODE_DOWN, BUTTON_MODE_PAUSE,
        BUTTON_VOL_UP, BUTTON_VOL_DOWN, BUTTON_VOL_PAUSE
    }

    public Handler mHandler = null;
    public Context context;
    public long serverTimeOffset = 0;
    public long serverRTT = 0;
    public AllBoards allBoards = null;
    public MusicPlayer musicPlayer = null;
    public RF radio = null;
    public Gps gps = null;
    public RFClientServer rfClientServer = null;
    public FindMyFriends findMyFriends = null;
    public BluetoothLEServer bLEServer = null;
    public BluetoothCommands bluetoothCommands = null;
    public BluetoothConnManager bluetoothConnManager = null;
    public BoardVisualization boardVisualization = null;
    public IoTClient iotClient = null;
    public BurnerBoard burnerBoard;
    public BatterySupervisor batterySupervisor = null;
    public MusicPlayerSupervisor musicPlayerSupervisor = null;
    public MediaManager mediaManager;
    private Thread musicPlayerThread;
    public boolean voiceAnnouncements = false;
    public BBWifi wifi = null;
    public TextToSpeech voice;
    private static USBReceiver usbReceiver = null;
    private static ButtonReceiver buttonReceiver = null;
    private static BluetoothReceiver btReceive = null;
    public BoardState boardState = null;
    public String filesDir = "";
    public BBMasterRemote masterRemote = null;
    public GTFO gtfo = null;

    public BBService() {
    }

    /**
     * interface for clients that bind
     */
    IBinder mBinder;

    /**
     * indicates whether onRebind should be used
     */
    boolean mAllowRebind;

    @Override
    public void onCreate() {

        try {

            super.onCreate();

            if (BuildConfig.DEBUG) {
                Timber.plant(new LoggingTree(this));
            }

            Timber.i("onCreate");

            InitClock();
            context = getApplicationContext();
            filesDir = context.getFilesDir().getAbsolutePath();

            Timber.i("Build Manufacturer " + Build.MANUFACTURER);
            Timber.i("Build Model " + Build.MODEL);
            Timber.i("Build Serial " + Build.SERIAL);

            // register to recieve USB events
            IntentFilter ufilter = new IntentFilter();
            ufilter.addAction("android.hardware.usb.action.USB_DEVICE_ATTACHED");
            ufilter.addAction("android.hardware.usb.action.USB_DEVICE_DETACHED");
            usbReceiver = new USBReceiver(this);
            this.registerReceiver(usbReceiver, ufilter);

            // Register to receive button messages
            IntentFilter filter = new IntentFilter(ACTION.BUTTONS);
            buttonReceiver = new ButtonReceiver(this);
            LocalBroadcastManager.getInstance(this).registerReceiver(buttonReceiver, filter);

            // Register to know when bluetooth remote connects
            btReceive = new BluetoothReceiver(this);
            context.registerReceiver(btReceive, new IntentFilter(ACTION_ACL_CONNECTED));

            HandlerThread mHandlerThread = null;
            mHandlerThread = new HandlerThread("BBServiceHandlerThread");
            mHandlerThread.start();
            mHandler = new Handler(mHandlerThread.getLooper());

            voice = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    // check for successful instantiation
                    if (status == TextToSpeech.SUCCESS) {
                        if (voice.isLanguageAvailable(Locale.UK) == TextToSpeech.LANG_AVAILABLE)
                            voice.setLanguage(Locale.US);
                        Timber.i("Text To Speech ready...");
                        voice.setPitch((float) 0.8);
                        String utteranceId = UUID.randomUUID().toString();
                        System.out.println("Where do you want to go, " + boardState.BOARD_ID + "?");
                        voice.setSpeechRate((float) 0.9);
                        voice.speak("I am " + boardState.BOARD_ID + "?",
                                TextToSpeech.QUEUE_FLUSH, null, utteranceId);
                    } else if (status == TextToSpeech.ERROR) {
                        Timber.i("Sorry! Text To Speech failed...");
                    }

                    // Let the user know they're on a raspberry pi // Skip For IsNano
                    if (BoardState.kIsRPI) {
                        String rpiMsg = "Raspberry PI detected";
                        Timber.i(rpiMsg);
                        // Use TTS.QUEUE_ADD or it'll talk over the speak() of its name above.
                        voice.speak(rpiMsg, TextToSpeech.QUEUE_ADD, null, "rpi diagnostic");

                        // Let's announce the WIFI IP on RPIs - do it here, as we need voice initialized first
                        if (wifi.ipAddress != null) {
                            voice.speak("My WiFi IP is: " + wifi.ipAddress, TextToSpeech.QUEUE_ADD, null, "wifi ip");
                        }
                    }
                }
            });

            allBoards = new AllBoards(this);

            boardState = new BoardState(this);
            boardState.address = allBoards.getBoardAddress(boardState.BOARD_ID);
            allBoards.Run();

            Timber.i("State Version " + boardState.version);
            Timber.i("State APK Updated Date " + boardState.apkUpdatedDate);
            Timber.i("State Address " + boardState.apkUpdatedDate);
            Timber.i("State SSID " + boardState.SSID);
            Timber.i("State Password " + boardState.password);
            Timber.i("State Mode " + boardState.currentVideoMode);
            Timber.i("State BOARD_ID " + boardState.BOARD_ID);
            Timber.i("State Tyoe " + allBoards.getBoardType());

            iotClient = new IoTClient(this);

            wifi = new BBWifi(this);
            wifi.Run();

            mediaManager = new MediaManager(this);
            mediaManager.Run();

            burnerBoard = BurnerBoard.Builder(this);
            burnerBoard.setText90(boardState.BOARD_ID, 5000);
            burnerBoard.attach(new BBService.BoardCallback());

            boardVisualization = new BoardVisualization(this);

            Timber.i("Setting initial visualization mode: " + boardState.currentVideoMode);
            boardVisualization.setMode(boardState.currentVideoMode);

            musicPlayer = new MusicPlayer(this);
            musicPlayerThread = new Thread(musicPlayer);
            musicPlayerThread.start();
            Thread.sleep(1000); // player thread must fully start before supervisor. dkw

            if (!DebugConfigs.BYPASS_MUSIC_SYNC) {
                musicPlayerSupervisor = new MusicPlayerSupervisor(this);
                musicPlayerSupervisor.Run();
            }

            bluetoothConnManager = new BluetoothConnManager(this);
            bLEServer = new BluetoothLEServer(this);
            gps = new Gps(this);
            radio = new RF(this);

            rfClientServer = new RFClientServer(this);
            rfClientServer.Run();

            findMyFriends = new FindMyFriends(this);

            bluetoothCommands = new BluetoothCommands(this);
            bluetoothCommands.init();

            batterySupervisor = new BatterySupervisor(this);
            batterySupervisor.Run();

            masterRemote = new BBMasterRemote(this);

            gtfo = new GTFO(this);

            // mFavorites = new Favorites(context, this, radio, gps, iotClient);

        } catch (Exception e) {
            Timber.e(e.getMessage());
        }
    }

    /**
     * The service is starting, due to a call to startService()
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Timber.i("onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * A client is binding to the service with bindService()
     */
    @Override
    public IBinder onBind(Intent intent) {
        Timber.i("onBind");
        return mBinder;
    }

    /**
     * Called when all clients have unbound with unbindService()
     */
    @Override
    public boolean onUnbind(Intent intent) {
        Timber.i("onUnbind");
        return mAllowRebind;
    }

    /**
     * Called when a client is binding to the service with bindService()
     */
    @Override
    public void onRebind(Intent intent) {
        Timber.i("onRebind");
    }

    /**
     * Called when The service is no longer used and is being destroyed
     */
    @Override
    public void onDestroy() {

        Timber.i("onDesonDestroy");
        voice.shutdown();
    }

    long startElapsedTime, startClock;

    public void InitClock() {
        startElapsedTime = SystemClock.elapsedRealtime();
        startClock = Calendar.getInstance().getTimeInMillis();
    }

    public long GetCurrentClock() {
        return SystemClock.elapsedRealtime() - startElapsedTime + startClock;
    }

    public long CurrentClockAdjusted() {
        return GetCurrentClock() + serverTimeOffset;
    }

    public void SetServerClockOffset(long serverClockOffset, long rtt) {
        serverTimeOffset = serverClockOffset;
        serverRTT = rtt;
    }

    public class BoardCallback implements BurnerBoard.BoardEvents {

        public void BoardId(String str) {
            Timber.i("ardunio BoardID callback:" + str + " " + boardState.BOARD_ID);
        }

        public void BoardMode(int mode) {
            Timber.i("ardunio mode callback:" + boardState.currentVideoMode);
        }
    }
}





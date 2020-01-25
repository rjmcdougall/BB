package com.richardmcdougall.bb;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.support.v4.content.LocalBroadcastManager;

import com.richardmcdougall.bb.board.BurnerBoard;
import com.richardmcdougall.bb.rf.FindMyFriends;
import com.richardmcdougall.bb.rf.RF;
import com.richardmcdougall.bb.rf.RFClientServer;
import com.richardmcdougall.bb.rf.RFMasterClientServer;

import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static android.bluetooth.BluetoothDevice.ACTION_ACL_CONNECTED;

public class BBService extends Service {

    private String TAG = this.getClass().getSimpleName();

    public enum buttons {
        BUTTON_KEYCODE, BUTTON_TRACK, BUTTON_DRIFT_UP,
        BUTTON_DRIFT_DOWN, BUTTON_MODE_UP, BUTTON_MODE_DOWN, BUTTON_MODE_PAUSE,
        BUTTON_VOL_UP, BUTTON_VOL_DOWN, BUTTON_VOL_PAUSE
    }

    public Handler mHandler = null;
    public Context context;
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
    public BBWifi wifi = null;

    private static USBReceiver usbReceiver = null;
    private static ButtonReceiver buttonReceiver = null;
    private static BluetoothReceiver btReceive = null;
    public BoardState boardState = null;
    public String filesDir = "";
    public MasterController masterController = null;
    public Thread masterControllerThread = null;
    public GTFOController gtfoController = null;
    public BoardLocations boardLocations = null;
    public TextToSpeech voice;
    public ServerElector serverElector = null;
    public RFMasterClientServer rfMasterClientServer = null;
    public RemoteCrisisController remoteCrisisController = null;
    public LocalCrisisController localCrisisController = null;

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

            BLog.i(TAG, "onCreate");

            context = getApplicationContext();
            filesDir = context.getFilesDir().getAbsolutePath();

            BLog.i(TAG, "Current Clock: " + TimeSync.GetCurrentClock());
            BLog.i(TAG, "startElapsedTime: " + TimeSync.startElapsedTime);

            BLog.i(TAG, "Build Manufacturer " + Build.MANUFACTURER);
            BLog.i(TAG, "Build Model " + Build.MODEL);
            BLog.i(TAG, "Build Serial " + Build.SERIAL);

            allBoards = new AllBoards(this);

            while (allBoards.dataBoards == null) {
                BLog.i(TAG, "Boards file is required to be downloaded before proceeding.  Please hold.");
                Thread.sleep(2000);
            }
            boardState = new BoardState(this);

            boardLocations = new BoardLocations(this);
            serverElector = new ServerElector(this);

            BLog.i(TAG, "State Version " + boardState.version);
            BLog.i(TAG, "State APK Updated Date " + boardState.apkUpdatedDate);
            BLog.i(TAG, "State Address " + boardState.address);
            BLog.i(TAG, "State SSID " + boardState.SSID);
            BLog.i(TAG, "State Password " + boardState.password);
            BLog.i(TAG, "State Mode " + boardState.currentVideoMode);
            BLog.i(TAG, "State BOARD_ID " + boardState.BOARD_ID);
            BLog.i(TAG, "State Tyoe " + boardState.boardType);
            BLog.i(TAG, "Display Teensy " + boardState.displayTeensy);

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

            voice = new TextToSpeech(getApplicationContext(), (int status) -> {
                // check for successful instantiation
                if (status == TextToSpeech.SUCCESS) {
                    if (voice.isLanguageAvailable(Locale.UK) == TextToSpeech.LANG_AVAILABLE)
                        voice.setLanguage(Locale.US);
                    BLog.i(TAG, "Text To Speech ready...");
                    voice.setPitch((float) 0.8);
                    voice.setSpeechRate((float) 0.9);
                    voice.speak("I am " + boardState.BOARD_ID + "?", TextToSpeech.QUEUE_FLUSH, null, "iam");
                } else if (status == TextToSpeech.ERROR) {
                    BLog.i(TAG, "Sorry! Text To Speech failed...");
                }
            });

            ScheduledThreadPoolExecutor sch = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1);
            Runnable seekAndPlay = () -> {
                if (boardState.platformType == BoardState.PlatformType.rpi) {
                    speak("Raspberry PI detected","rpi diagnostic");
                    if (wifi.ipAddress != null) {
                        speak("My WiFi IP is: " + wifi.ipAddress,"wifi ip");
                    }
                }
            };
            sch.schedule(seekAndPlay, 3, TimeUnit.SECONDS);

            iotClient = new IoTClient(this);
            iotClient.Run();

            wifi = new BBWifi(this);

            mediaManager = new MediaManager(this);

            remoteCrisisController = new RemoteCrisisController(this);
            localCrisisController = new LocalCrisisController(this);

            burnerBoard = BurnerBoard.Builder(this);
            burnerBoard.setText90(boardState.BOARD_ID, 5000);
            burnerBoard.attach(new BBService.BoardCallback());

            boardVisualization = new BoardVisualization(this);
            boardVisualization.Run();

            BLog.i(TAG, "Setting initial visualization mode: " + boardState.currentVideoMode);
            boardVisualization.setMode(boardState.currentVideoMode);

            TimeSync.InitClock();
            musicPlayer = new MusicPlayer(this);
            musicPlayerThread = new Thread(musicPlayer);
            musicPlayerThread.start();

            if (!DebugConfigs.BYPASS_MUSIC_SYNC) {
                musicPlayerSupervisor = new MusicPlayerSupervisor(this);
            }

            bluetoothConnManager = new BluetoothConnManager(this);
            bLEServer = new BluetoothLEServer(this);
            gps = new Gps(this);
            radio = new RF(this);

            rfClientServer = new RFClientServer(this);
            rfClientServer.Run();

            rfMasterClientServer = new RFMasterClientServer(this);
            rfMasterClientServer.Run();

            findMyFriends = new FindMyFriends(this);

            bluetoothCommands = new BluetoothCommands(this);
            bluetoothCommands.init();

            batterySupervisor = new BatterySupervisor(this);

            masterController = new MasterController(this);
            masterControllerThread = new Thread(masterController);
            masterControllerThread.start();

            gtfoController = new GTFOController(this);

        } catch (Exception e) {
            BLog.e(TAG, e.getMessage());
        }
    }

    public void speak(String txt, String id){
        if(voice != null) {
            voice.speak(txt, TextToSpeech.QUEUE_ADD, null, id);
        }
        else {
            BLog.d(TAG,"Text to Speech Null");
        }
    }
    /**
     * The service is starting, due to a call to startService()
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        BLog.i(TAG, "onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * A client is binding to the service with bindService()
     */
    @Override
    public IBinder onBind(Intent intent) {
        BLog.i(TAG, "onBind");
        return mBinder;
    }

    /**
     * Called when all clients have unbound with unbindService()
     */
    @Override
    public boolean onUnbind(Intent intent) {
        BLog.i(TAG, "onUnbind");
        return mAllowRebind;
    }

    /**
     * Called when a client is binding to the service with bindService()
     */
    @Override
    public void onRebind(Intent intent) {
        BLog.i(TAG, "onRebind");
    }

    /**
     * Called when The service is no longer used and is being destroyed
     */
    @Override
    public void onDestroy() {

        BLog.i(TAG, "onDesonDestroy");
        voice.shutdown();
    }

    public class BoardCallback implements BurnerBoard.BoardEvents {

        public void BoardId(String str) {
            BLog.i(TAG, "ardunio BoardID callback:" + str + " " + boardState.BOARD_ID);
        }

        public void BoardMode(int mode) {
            BLog.i(TAG, "ardunio mode callback:" + boardState.currentVideoMode);
        }
    }
}
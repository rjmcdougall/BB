package com.richardmcdougall.bb;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;

import com.richardmcdougall.bb.bms.BMS;
import com.richardmcdougall.bb.board.BurnerBoard;
import com.richardmcdougall.bb.hardware.Canable;
import com.richardmcdougall.bb.hardware.PowerController;
import com.richardmcdougall.bb.hardware.VescController;
import com.richardmcdougall.bb.mesh.Meshtastic;
import com.richardmcdougall.bb.rf.FindMyFriends;
import com.richardmcdougall.bb.rf.RF;
import com.richardmcdougall.bb.rf.RFClientServer;
import com.richardmcdougall.bb.rf.RFMasterClientServer;
import com.richardmcdougall.bb.visualization.RGBList;
import com.richardmcdougall.bbcommon.AllBoards;
import com.richardmcdougall.bbcommon.BBWifi;
import com.richardmcdougall.bbcommon.BLog;
import com.richardmcdougall.bbcommon.BoardState;
import com.richardmcdougall.bbcommon.DebugConfigs;
import com.richardmcdougall.bbcommon.PersistentCache;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Locale;


import static android.bluetooth.BluetoothDevice.ACTION_ACL_CONNECTED;

public class BBService extends Service {

    private String TAG = this.getClass().getSimpleName();


    private static final String CHANNEL_ID = "BBForegroundServiceChannel";
    private static final int NOTIFICATION_ID = 1;
    private volatile boolean isRunning = true;
    public enum buttons {
        BUTTON_KEYCODE, BUTTON_TRACK, BUTTON_DRIFT_UP,
        BUTTON_DRIFT_DOWN, BUTTON_MODE_UP, BUTTON_MODE_DOWN, BUTTON_MODE_PAUSE,
        BUTTON_VOL_UP, BUTTON_VOL_DOWN, BUTTON_VOL_PAUSE
    }

    public Context context;
    public AllBoards allBoards = null;
    public MusicController musicController = null;
    public RF radio = null;
    public Gps gps = null;
    public BMS bms = null;
    public Canable canbus = null;
    public VescController vesc = null;
    public PowerController powerController = null;
    public Meshtastic mesh = null;
    public RFClientServer rfClientServer = null;
    public FindMyFriends findMyFriends = null;
    public BluetoothLEServer bLEServer = null;
    public BluetoothCommands bluetoothCommands = null;
    public BluetoothConnManager bluetoothConnManager = null;
    public VisualizationController visualizationController = null;
    public AudioVisualizer audioVisualizer = null;

    public BurnerBoard burnerBoard;
    public BatterySupervisor batterySupervisor = null;
    public MotionSupervisor motionSupervisor = null;

    public MusicPlayerSupervisor musicPlayerSupervisor = null;
    public MediaManager mediaManager;
    private Thread musicPlayerThread;
    public BBWifi wifi = null;

    private static USBReceiver usbReceiver = null;
    private static ButtonReceiver buttonReceiver = null;
    private static BluetoothReceiver btReceive = null;
    public BoardState boardState = null;
    public MasterController masterController = null;
    public Thread masterControllerThread = null;
    public GTFOController gtfoController = null;
    public BoardLocations boardLocations = null;
    public TextToSpeech voice;
    public ServerElector serverElector = null;
    public RFMasterClientServer rfMasterClientServer = null;
    public RemoteCrisisController remoteCrisisController = null;
    public LocalCrisisController localCrisisController = null;
    public RotatingDisplayController rotatingDisplayController = null;
    public DisplayMapManager displayMapManager = null;
    public DisplayMapManager2 displayMapManager2 = null;
    private boolean textToSpeechReady = false;
    public PersistentCache persistentCache = null;

    private ActivityManager.MemoryInfo mMemoryInfo = null;

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

    public void onTaskRemoved(Intent rootIntent) {

        //unregister listeners
        //do any other cleanup if required

        CleanUp();

        //stop service
        stopSelf();
    }

    // Get a MemoryInfo object for the device's current memory status.
    private ActivityManager.MemoryInfo getAvailableMemory() {
        ActivityManager activityManager = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        try {
            activityManager.getMemoryInfo(memoryInfo);
        } catch (Exception e) {
        }
        return memoryInfo;
    }

    @Override
    public void onCreate() {

        try {

            super.onCreate();
            BLog.i(TAG, "onCreate");
            createNotificationChannel();
        } catch (Exception e) {
            BLog.e(TAG, "could not create notification channel");
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        BLog.i(TAG, "onStartCommand");

        Notification notification = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("My Foreground Service")
                .setContentText("Running in the background...")
                .setSmallIcon(android.R.drawable.ic_media_play) // Replace with your icon
                .build();

        startForeground(NOTIFICATION_ID, notification);

        try {

            mMemoryInfo = getAvailableMemory();

            if (mMemoryInfo != null) {
                BLog.d(TAG, "Total mem: " + mMemoryInfo.totalMem + " avail: " + mMemoryInfo.availMem);
            }

            context = getApplicationContext();

            BLog.i(TAG, "Current Clock: " + TimeSync.GetCurrentClock());
            Format formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String datetime = formatter.format(new java.util.Date());
            BLog.i(TAG, "Current DateTime: " + datetime);

            //BLog.i(TAG, "startElapsedTime: " + TimeSync.startElapsedTime);

            BLog.i(TAG, "Build Manufacturer " + Build.MANUFACTURER);
            BLog.i(TAG, "Build Model " + Build.MODEL);
            BLog.i(TAG, "Build Serial " + Build.SERIAL);
            try {
                BLog.d(TAG, "getVoltage");
                vesc.getVoltage();
            } catch (Exception e) {
            }
            allBoards = new AllBoards(context, voice);

            while (allBoards.dataBoards == null) {
                BLog.i(TAG, "Boards file is required to be downloaded before proceeding.  Please hold.");
                Thread.sleep(2000);
            }
            boardState = new BoardState(this.context, this.allBoards);
            persistentCache = new PersistentCache(this);

            BLog.i(TAG, "State Device ID " + boardState.DEVICE_ID);

            wifi = new BBWifi(context,boardState);

            boardLocations = new BoardLocations(this);
            serverElector = new ServerElector(this);

            BLog.i(TAG, "State Version " + boardState.version);
            BLog.i(TAG, "State APK Updated Date " + boardState.apkUpdatedDate);
            BLog.i(TAG, "State Address " + boardState.address);
            BLog.i(TAG, "State SSID " + boardState.SSID);
            BLog.i(TAG, "State Password " + boardState.password);
            BLog.i(TAG, "State Mode " + boardState.currentVideoMode);
            BLog.i(TAG, "State BOARD_ID " + boardState.BOARD_ID);
            BLog.i(TAG, "State Tyoe " + boardState.GetBoardType());
            BLog.i(TAG, "Display Teensy " + boardState.displayTeensy);
            BLog.i(TAG, "Video Contrast Multiplier  " + boardState.videoContrastMultiplier);
            BLog.i(TAG, "Display Debug  " + boardState.displayDebug);
            BLog.i(TAG, "Fun Mode " + boardState.GetFunMode());

            // register to recieve USB events
            IntentFilter ufilter = new IntentFilter();
            ufilter.addAction("android.hardware.usb.action.USB_DEVICE_ATTACHED");
            ufilter.addAction("android.hardware.usb.action.USB_DEVICE_DETACHED");
            usbReceiver = new USBReceiver(this);
            this.registerReceiver(usbReceiver, ufilter);

            // Register to receive button messages
            IntentFilter filter = new IntentFilter(ACTION.BUTTONS);
            buttonReceiver = new ButtonReceiver(this);

            // Register to know when bluetooth remote connects
            //btReceive = new BluetoothReceiver(this);
            //context.registerReceiver(btReceive, new IntentFilter(ACTION_ACL_CONNECTED));

            mediaManager = new MediaManager(this);

            while (mediaManager.dataDirectory == null) {
                BLog.i(TAG, "Media file is required to be downloaded before proceeding.  Please hold.");
                Thread.sleep(2000);
            }

            remoteCrisisController = new RemoteCrisisController(this);
            localCrisisController = new LocalCrisisController(this);

            displayMapManager2 = new DisplayMapManager2(this);
            if(boardState.GetBoardType() == BoardState.BoardType.dynamicPanel) {
                while (displayMapManager2.displayMap == null) {
                    BLog.i(TAG, "Display Map JSON file is required to be downloaded before proceeding.  Please hold.");
                    Thread.sleep(2000);
                }
            }

            displayMapManager = new DisplayMapManager(this);
            if(boardState.GetBoardType() == BoardState.BoardType.mezcal){
                while (displayMapManager.displayMap == null) {
                    BLog.i(TAG, "Display Map CSV file is required to be downloaded before proceeding.  Please hold.");
                    Thread.sleep(2000);
                }
            }

            burnerBoard = BurnerBoard.Builder(this);
            burnerBoard.textBuilder.setText90(boardState.BOARD_ID, 5000, burnerBoard.getFrameRate(), new RGBList().getColor("white"));

            audioVisualizer = new AudioVisualizer(this);

            visualizationController = new VisualizationController(this);
            visualizationController.Run();

            BLog.i(TAG, "Setting initial visualization mode: " + boardState.currentVideoMode);
            visualizationController.setMode(boardState.currentVideoMode);

            TimeSync.InitClock();
            musicController = new MusicController(this);
            musicPlayerThread = new Thread(musicController);
            musicPlayerThread.setName("BB Music Player");
            musicPlayerThread.start();

            if (!DebugConfigs.BYPASS_MUSIC_SYNC) {
                musicPlayerSupervisor = new MusicPlayerSupervisor(this);
            }

            bluetoothConnManager = new BluetoothConnManager(this);
            bLEServer = new BluetoothLEServer(this);
            gps = new Gps(this);
            radio = new RF(this);
            canbus = new Canable(this);
            vesc = new VescController(this, canbus);
            powerController = new PowerController(this);
            mesh = new Meshtastic(this);

            bms = BMS.Builder(this);

            rfClientServer = new RFClientServer(this);
            rfMasterClientServer = new RFMasterClientServer(this);
            findMyFriends = new FindMyFriends(this);

            bluetoothCommands = new BluetoothCommands(this);

            batterySupervisor = new BatterySupervisor(this);
            motionSupervisor = new MotionSupervisor(this);

            masterController = new MasterController(this);
            masterControllerThread = new Thread(masterController);
            masterControllerThread.start();

            rotatingDisplayController = new RotatingDisplayController(this);

            gtfoController = new GTFOController(this);

            if(!(boardState.BOARD_ID.equalsIgnoreCase("monitor"))){
                voice = new TextToSpeech(context, (int status) -> {
                    // check for successful instantiation
                    if (status == TextToSpeech.SUCCESS) {
                        voice.setLanguage(Locale.US);
                        BLog.i(TAG, "Text To Speech ready...");
                        textToSpeechReady = true;
                        voice.speak(boardState.BOARD_ID, TextToSpeech.QUEUE_FLUSH, null, "iam");
                    } else {
                        textToSpeechReady = false;
                        BLog.i(TAG, "Sorry! Text To Speech failed...");
                    }
                });
            }


        } catch (Exception e) {
            BLog.e(TAG, e.getMessage());
        }
        return START_STICKY;

    }

    public void speak(String txt, String id){
        try {
            if(voice != null && textToSpeechReady) {
                voice.speak(txt, TextToSpeech.QUEUE_ADD, null, id);
            }
            else {
                BLog.d(TAG,"Text to Speech Null");
            }
        }
        catch(Exception e){
            BLog.e(TAG,e.getMessage());
        }
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

    public void CleanUp(){

        this.bluetoothConnManager.UnregisterReceivers();
        this.burnerBoard.UnregisterReceivers();
        if(voice != null)
            voice.shutdown();
    }
}
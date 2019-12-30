package com.richardmcdougall.bb;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import java.util.Calendar;
import java.util.Locale;
import java.util.UUID;
import java.util.Date;
import android.os.Build;

import static android.bluetooth.BluetoothDevice.ACTION_ACL_CONNECTED;

public class BBService extends Service {

    private static final String TAG = "BB.BBService";

    public enum buttons {
        BUTTON_KEYCODE, BUTTON_TRACK, BUTTON_DRIFT_UP,
        BUTTON_DRIFT_DOWN, BUTTON_MODE_UP, BUTTON_MODE_DOWN, BUTTON_MODE_PAUSE,
        BUTTON_VOL_UP, BUTTON_VOL_DOWN, BUTTON_VOL_PAUSE
    }

    public Handler mHandler = null;
    public Context context;
    public long serverTimeOffset = 0;
    public long serverRTT = 0;
    public int version = 0;
    public Date apkUpdatedDate;
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
    public DownloadManager dlManager;
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

    public void l(String s) {
        Log.v(TAG, s);
        sendLogMsg(s);
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

            InitClock();

            context = getApplicationContext();

            PackageInfo pinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pinfo.versionCode;
            apkUpdatedDate = new Date(pinfo.lastUpdateTime);

            filesDir = context.getFilesDir().getAbsolutePath();

            l("BBService: onCreate");
            l("Manufacturer " + Build.MANUFACTURER);
            l("Model " +  Build.MODEL  );
            l("Serial " + Build.SERIAL);
            l("BurnerBoard Version " + version);
            l("BurnerBoard APK Updated Date " + apkUpdatedDate);

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
                        l("Text To Speech ready...");
                        voice.setPitch((float) 0.8);
                        String utteranceId = UUID.randomUUID().toString();
                        System.out.println("Where do you want to go, " + boardState.BOARD_ID + "?");
                        voice.setSpeechRate((float) 0.9);
                        voice.speak("I am " + boardState.BOARD_ID + "?",
                                TextToSpeech.QUEUE_FLUSH, null, utteranceId);
                    } else if (status == TextToSpeech.ERROR) {
                        l("Sorry! Text To Speech failed...");
                    }

                    // Let the user know they're on a raspberry pi // Skip For IsNano
                    if (BoardState.kIsRPI) {
                        String rpiMsg = "Raspberry PI detected";
                        l(rpiMsg);
                        // Use TTS.QUEUE_ADD or it'll talk over the speak() of its name above.
                        voice.speak(rpiMsg, TextToSpeech.QUEUE_ADD, null, "rpi diagnostic");

                        // Let's announce the WIFI IP on RPIs - do it here, as we need voice initialized first
                        if (wifi.ipAddress != null) {
                            voice.speak("My WiFi IP is: " + wifi.ipAddress, TextToSpeech.QUEUE_ADD, null, "wifi ip");
                        }
                    }
                }
            });

            boardState = new BoardState(this);

            iotClient = new IoTClient(this);

            wifi = new BBWifi(this);
            wifi.Run();

            allBoards = new AllBoards(this);
            allBoards.Run();

            dlManager = new DownloadManager(this );
            dlManager.Run();

            burnerBoard = BurnerBoard.Builder(this);
            burnerBoard.setText90(boardState.BOARD_ID, 5000);
            burnerBoard.attach(new BBService.BoardCallback());

            boardVisualization = new BoardVisualization(this );

            Log.d(TAG, "Setting initial visualization mode: " + 1);
            boardVisualization.setMode(1);

            musicPlayer = new MusicPlayer(this);
            musicPlayerThread = new Thread(musicPlayer);
            musicPlayerThread.start();

            if(!DebugConfigs.BYPASS_MUSIC_SYNC){
                musicPlayerSupervisor = new MusicPlayerSupervisor(this);
                musicPlayerSupervisor.Run();
            }

            bluetoothConnManager = new BluetoothConnManager(this);
            bLEServer = new BluetoothLEServer(this);
            radio = new RF(this);

            rfClientServer = new RFClientServer(this);
            rfClientServer.Run();

            gps = radio.getGps();
            findMyFriends = new FindMyFriends(this);

            bluetoothCommands = new BluetoothCommands(this);
            bluetoothCommands.init();

            batterySupervisor = new BatterySupervisor(this);
            batterySupervisor.Run();

            masterRemote = new BBMasterRemote(this);

            gtfo = new GTFO(this);

            // mFavorites = new Favorites(context, this, radio, gps, iotClient);

        } catch (Exception e) {
            e(e.getMessage());
        }
    }

    public void e(String logMsg) {
        Log.e(TAG, logMsg);
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

    private void sendLogMsg(String msg) {
        Intent in = new Intent(ACTION.STATS);
        in.putExtra("resultCode", Activity.RESULT_OK);
        in.putExtra("msgType", 4);
        // Put extras into the intent as usual
        in.putExtra("logMsg", msg);
        LocalBroadcastManager.getInstance(this).sendBroadcast(in);
    }

    public class BoardCallback implements BurnerBoard.BoardEvents {

        public void BoardId(String str) {
            l("ardunio BoardID callback:" + str + " " + boardState.BOARD_ID);
        }

        public void BoardMode(int mode) {
            l("ardunio mode callback:" + boardVisualization.mBoardMode);
        }
    }
}





package com.richardmcdougall.bb;

import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Parcelable;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.KeyEvent;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Calendar;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.Date;

import android.os.Build;

import android.bluetooth.BluetoothDevice;
import android.media.RingtoneManager;
import android.media.Ringtone;

import static android.bluetooth.BluetoothDevice.ACTION_ACL_CONNECTED;

public class BBService extends Service {

    private static final String TAG = "BB.BBService";

    // Set to force classic mode when using Emulator
    public static final boolean kEmulatingClassic = false;

    // RPIs don't always have a screen; use beeps -jib
    public static final boolean kBeepOnConnect = BurnerBoardUtil.kIsRPI; // Not Done IsNano

    public enum buttons {
        BUTTON_KEYCODE, BUTTON_TRACK, BUTTON_DRIFT_UP,
        BUTTON_DRIFT_DOWN, BUTTON_MODE_UP, BUTTON_MODE_DOWN, BUTTON_MODE_PAUSE,
        BUTTON_VOL_UP, BUTTON_VOL_DOWN, BUTTON_VOL_PAUSE
    }

    public Handler mHandler = null;
    public Context context;
    public long serverTimeOffset = 0;
    public long serverRTT = 0;
    public RFClientServer rfClientServer = null;
    /* XXX TODO this string is accessed both directly here in this class, as well as used via getBoardId() on the object it provides. refactor -jib */
    public static String boardId = BurnerBoardUtil.BOARD_ID;
    private int mBoardMode = 1; // Mode of the Ardunio/LEDs
    public int version = 0;
    public Date apkUpdatedDate;
    public AllBoards allBoards = null;
    public MusicPlayer musicPlayer = null;
    public RF radio = null;
    public Gps gps = null;
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
    public boolean masterRemote = false;
    public boolean blockMaster = false;
    public boolean voiceAnnouncements = false;
    public boolean gtfo = false;
    public BBWifi wifi = null;
    public TextToSpeech voice;
    private BBService.usbReceiver mUsbReceiver = new BBService.usbReceiver();

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

            IntentFilter ufilter = new IntentFilter();
            ufilter.addAction("android.hardware.usb.action.USB_DEVICE_ATTACHED");
            ufilter.addAction("android.hardware.usb.action.USB_DEVICE_DETTACHED");
            this.registerReceiver(mUsbReceiver, ufilter);

            context = getApplicationContext();

            PackageInfo pinfo;
            try {
                pinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                version = pinfo.versionCode;
                apkUpdatedDate = new Date(pinfo.lastUpdateTime);
                l("BurnerBoard Version " + version);
                //ET2.setText(versionNumber);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            l("BBService: onCreate");
            l("I am " + Build.MANUFACTURER + " / " + Build.MODEL + " / " + Build.SERIAL);


            if (iotClient == null) {
                iotClient = new IoTClient(this);
            }

            wifi = new BBWifi(this);
            wifi.Run();

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
                        System.out.println("Where do you want to go, " + BurnerBoardUtil.BOARD_ID + "?");
                        voice.setSpeechRate((float) 0.9);
                        voice.speak("I am " + BurnerBoardUtil.BOARD_ID + "?",
                                TextToSpeech.QUEUE_FLUSH, null, utteranceId);
                    } else if (status == TextToSpeech.ERROR) {
                        l("Sorry! Text To Speech failed...");
                    }

                    // Let the user know they're on a raspberry pi // Skip For IsNano
                    if (BurnerBoardUtil.kIsRPI) {
                        String rpiMsg = "Raspberry PI detected";
                        l(rpiMsg);
                        // Use TTS.QUEUE_ADD or it'll talk over the speak() of its name above.
                        voice.speak(rpiMsg, TextToSpeech.QUEUE_ADD, null, "rpi diagnostic");

                        // Let's announce the WIFI IP on RPIs - do it here, as we need voice initialized first
                        if (wifi.mIPAddress != null) {
                            voice.speak("My WiFi IP is: " + wifi.mIPAddress, TextToSpeech.QUEUE_ADD, null, "wifi ip");
                        }
                    }
                }
            });

            allBoards = new AllBoards(this);
            allBoards.Run();

            dlManager = new DownloadManager(this );

            dlManager.onProgressCallback = new DownloadManager.OnDownloadProgressType() {
                long lastTextTime = 0;

                public void onProgress(String file, long fileSize, long bytesDownloaded) {
                    if (fileSize <= 0)
                        return;

                    long curTime = System.currentTimeMillis();
                    if (curTime - lastTextTime > 30000) {
                        lastTextTime = curTime;
                        long percent = bytesDownloaded * 100 / fileSize;

                        voice.speak("Downloading " + file + ", " + percent + " Percent", TextToSpeech.QUEUE_ADD, null, "downloading");
                        lastTextTime = curTime;
                        l("Downloading " + file + ", " + percent + " Percent");
                    }
                }

                public void onVoiceCue(String msg) {
                    voice.speak(msg, TextToSpeech.QUEUE_ADD, null, "Download Message");
                }
            };
            dlManager.Run();

            HandlerThread mHandlerThread = null;
            mHandlerThread = new HandlerThread("BBServiceHandlerThread");
            mHandlerThread.start();
            mHandler = new Handler(mHandlerThread.getLooper());

            // Register to receive button messages
            IntentFilter filter = new IntentFilter(ACTION.BUTTONS);
            LocalBroadcastManager.getInstance(this).registerReceiver(mButtonReceiver, filter);

            // Register to know when bluetooth remote connects
            context.registerReceiver(btReceive, new IntentFilter(ACTION_ACL_CONNECTED));

            startLights();

            musicPlayer = new MusicPlayer(this);
            musicPlayerThread = new Thread(musicPlayer);
            musicPlayerThread.start();

            musicPlayerSupervisor = new MusicPlayerSupervisor(this);
            musicPlayerSupervisor.Run();

            bluetoothConnManager = new BluetoothConnManager(this);
            bLEServer = new BluetoothLEServer(this);
            radio = new RF(this);

            InitClock();

            rfClientServer = new RFClientServer(this);
            rfClientServer.Run();

            gps = radio.getGps();
            findMyFriends = new FindMyFriends(this);

            // mFavorites = new Favorites(context, this, radio, gps, iotClient);

            bluetoothCommands = new BluetoothCommands(this);
            bluetoothCommands.init();

            batterySupervisor = new BatterySupervisor(this);
            batterySupervisor.Run();

            // Supported Languages
            Set<Locale> supportedLanguages = voice.getAvailableLanguages();
            if (supportedLanguages != null) {
                for (Locale lang : supportedLanguages) {
                    l("Voice Supported Language: " + lang);
                }
            }


        } catch (Exception e) {
            l(e.toString());
        }
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

    private void startLights() {

        if (kEmulatingClassic || BurnerBoardUtil.isBBClassic()) {
            l("Visualization: Using Classic");
            burnerBoard = new BurnerBoardClassic(this);
        } else if (BurnerBoardUtil.isBBMast()) {
            l("Visualization: Using Mast");
            burnerBoard = new BurnerBoardMast(this);
        } else if (BurnerBoardUtil.isBBPanel()) {
            l("Visualization: Using Panel");
            burnerBoard = new BurnerBoardPanel(this);
        } else if (BurnerBoardUtil.isBBDirectMap()) {
            l("Visualization: Using Direct Map");
            burnerBoard = new BurnerBoardDirectMap(
                    this,
                    BurnerBoardUtil.kVisualizationDirectMapWidth,
                    BurnerBoardUtil.kVisualizationDirectMapHeight
            );
        } else if (BurnerBoardUtil.isBBAzul()) {
            l("Visualization: Using Azul");
            burnerBoard = new BurnerBoardAzul(this);
        } else {
            l("Could not identify board type! Falling back to Azul for backwards compatibility");
            burnerBoard = new BurnerBoardAzul(this);
        }

        if (burnerBoard == null) {
            l("startLights: null burner board");
            return;
        }
        burnerBoard.setText90(BurnerBoardUtil.BOARD_ID, 5000);

        if (burnerBoard != null) {
            burnerBoard.attach(new BoardCallback());
        }

        if (boardVisualization == null) {
            boardVisualization = new BoardVisualization(this, burnerBoard, this);
        }

        Log.d(TAG, "Setting initial visualization mode: " + mBoardMode);
        boardVisualization.setMode(mBoardMode);
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

    // We use this to catch the music buttons
    private final BroadcastReceiver mButtonReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            final String TAG = "mButtonReceiver";

            //Log.d(TAG, "onReceive entered");
            String action = intent.getAction();

            if (ACTION.BUTTONS.equals(action)) {
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
                        musicPlayer.NextStream();
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
                        musicPlayer.MusicOffset(-10);
                        break;
                    case BUTTON_DRIFT_UP:
                        l("BUTTON_DRIFT_UP");
                        musicPlayer.MusicOffset(10);
                        break;
                    case BUTTON_VOL_DOWN:
                        l("BUTTON_VOL_DOWN");
                        musicPlayer.onVolDown();
                        break;
                    case BUTTON_VOL_UP:
                        l("BUTTON_VOL_UP");
                        musicPlayer.onVolUp();
                        break;
                    case BUTTON_VOL_PAUSE:
                        l("BUTTON_VOL_PAUSE");
                        musicPlayer.onVolPause();
                        break;
                    default:
                        break;
                }
            }
        }
    };

    public void enableMaster(boolean enable) {
        masterRemote = enable;
        if (enable) {
            // Let everyone else know we just decided to be the master
            // Encoding the BOARD_ID as the payload; it's not really needed as we can read that
            // from the client data. XXX is there a better/more useful payload?
            rfClientServer.sendRemote(kRemoteMasterName, hashTrackName(BurnerBoardUtil.BOARD_ID), RFClientServer.kRemoteMasterName);

            burnerBoard.setText("Master", 2000);
            voice.speak("Master Remote is: " + BurnerBoardUtil.BOARD_ID, TextToSpeech.QUEUE_ADD, null, "enableMaster");
        } else {
            // You explicitly disabled the master. Stop any broadcasting.
            rfClientServer.disableMasterBroadcast();

            burnerBoard.setText("Solo", 2000);
            voice.speak("Disabling Master Remote: " + BurnerBoardUtil.BOARD_ID, TextToSpeech.QUEUE_ADD, null, "disableMaster");
        }
    }

    private int stashedAndroidVolumePercent;

    public void enableGTFO(boolean enable) {

        gtfo = enable;
        if (enable) {

            boardVisualization.inhibitGTFO(true);
            burnerBoard.setText90("Get The Fuck Off!", 5000);
            musicPlayer.setVolume(0, 0);
            stashedAndroidVolumePercent = musicPlayer.getAndroidVolumePercent();
            musicPlayer.setAndroidVolumePercent(100);
            voice.speak("Hey, Get The Fuck Off!", TextToSpeech.QUEUE_ADD, null, "GTFO");
        } else {
            boardVisualization.inhibitGTFO(false);
            musicPlayer.setAndroidVolumePercent(stashedAndroidVolumePercent);
            musicPlayer.setVolume(1, 1);
        }
    }

    public static final int kRemoteAudioTrack = 0x01;
    public static final int kRemoteVideoTrack = 0x02;
    public static final int kRemoteMute = 0x03;
    public static final int kRemoteMasterName = 0x04;

    // TODO: Put this back as a remote control packet
    // Change value -> hash lookup
    public void decodeRemoteControl(String client, int cmd, long value) {


        if (blockMaster) {
            l("BLOCKED remote cmd, value " + cmd + ", " + value + " from: " + client);
        } else {

            l("Received remote cmd, value " + cmd + ", " + value + " from: " + client);

            switch (cmd) {
                case kRemoteAudioTrack:

                    for (int i = 1; i <= dlManager.GetTotalAudio(); i++) {
                        String name = musicPlayer.getRadioChannelInfo(i);
                        long hashed = hashTrackName(name);
                        if (hashed == value) {
                            l("Remote Audio " + musicPlayer.getRadioChannel() + " -> " + i);
                            if (musicPlayer.getRadioChannel() != i) {
                                musicPlayer.SetRadioChannel((int) i);
                                l("Received remote audio switch to track " + i + " (" + name + ")");
                            } else {
                                l("Ignored remote audio switch to track " + i + " (" + name + ")");
                            }
                            break;
                        }
                    }
                    break;

                case kRemoteVideoTrack:
                    for (int i = 1; i <= dlManager.GetTotalVideo(); i++) {
                        String name = dlManager.GetVideoFileLocalName(i - 1);
                        long hashed = hashTrackName(name);
                        if (hashed == value) {
                            l("Remote Video " + boardVisualization.getMode() + " -> " + i);
                            if (boardVisualization.getMode() != i) {
                                setMode((int) i);
                                l("Received remote video switch to mode " + i + " (" + name + ")");
                            } else {
                                l("Ignored remote video switch to mode " + i + " (" + name + ")");
                            }
                            break;
                        }
                    }
                    break;
                case kRemoteMute:
                    if (value != musicPlayer.getCurrentBoardVol()) {
                        musicPlayer.setBoardVolume((int) value);
                    }
                    break;
                case kRemoteMasterName:
                    if (masterRemote) {
                        // This board thinks it's the master, but apparently it's no longer. Reset master
                        // mode and follow the new master
                        String diag = BurnerBoardUtil.BOARD_ID + " is no longer the master. New master: " + client;
                        l(diag);
                        voice.speak(diag, TextToSpeech.QUEUE_ADD, null, "master reset");
                        enableMaster(false);
                    }

                default:
                    break;
            }
        }
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
        }

        switch (keyCode) {
            case 100:
            case 87: // satachi right button
                musicPlayer.NextStream();
                break;
            case 97:
            case 20:
                musicPlayer.MusicOffset(-10);
                break;
            case 19:
                musicPlayer.MusicOffset(10);
                break;
            case 24:   // native volume up button
            case 21:
                musicPlayer.onVolUp();

                return false;
            case 25:  // native volume down button
            case 22:
                musicPlayer.onVolDown();
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
        return true;
    }

    public boolean onKeyDownRPI(int keyCode, KeyEvent event) {
        boolean handled = false;
        if (event.getRepeatCount() == 0) {
            l("RPI Keycode:" + keyCode);
        }

        switch (keyCode) {
            case 85: // satachi Play button
                boardVisualization.NextVideo();
                break;

            /* Audio stream control */
            case 87: // satachi right button
                l("RPI Bluetooth Right Button");
                musicPlayer.NextStream();
                break;
            case 88: //satachi left button
                l("RPI Bluetooth Left Button");
                musicPlayer.PreviousStream();
                break;

            /* Volume control */
            case 24:   // satachi & native volume up button
                l("RPI Bluetooth Volume Up Button");
                musicPlayer.onVolUp();
                return false;
            case 25:  // satachi & native volume down button
                l("RPI Bluetooth Volume Down Button");
                musicPlayer.onVolDown();
                return false;
        }
        return true;
    }

    public void setMode(int mode) {

        // Likely not connected to physical burner board, fallback
        if (mode == 99) {
            mBoardMode++;
        } else if (mode == 98) {
            mBoardMode--;
        } else {
            mBoardMode = mode;
        }

        int maxModes = dlManager.GetTotalVideo();
        if (mBoardMode > maxModes)
            mBoardMode = 1;
        else if (mBoardMode < 1)
            mBoardMode = maxModes;

        // If I am set to be the master, broadcast to other boards
        if (masterRemote && (rfClientServer != null)) {

            String name = dlManager.GetVideoFileLocalName(mBoardMode - 1);
            l("Sending video remote for video " + name);
            rfClientServer.sendRemote(kRemoteVideoTrack, hashTrackName(name), RFClientServer.kRemoteVideo);
        }

        if (boardVisualization != null && burnerBoard != null) {
            l("SetMode:" + boardVisualization.getMode() + " -> " + mode);
            boardVisualization.setMode(mBoardMode);
            burnerBoard.setMode(mBoardMode);
        }

        if (voiceAnnouncements) {
            voice.speak("mode" + mBoardMode, TextToSpeech.QUEUE_FLUSH, null, "mode");
        }
    }

    public class BoardCallback implements BurnerBoard.BoardEvents {

        public void BoardId(String str) {
            boardId = str;
            l("ardunio BoardID callback:" + str + " " + boardId);
        }

        public void BoardMode(int mode) {
            l("ardunio mode callback:" + mBoardMode);
        }
    }

    // Single press to show battery
    // Double press to show map
    // Tripple click to toggle master
    private long lastPressed = SystemClock.elapsedRealtime();
    private int pressCnt = 1;

    private void onBatteryButton() {
        if (burnerBoard != null) {
            burnerBoard.showBattery();
            if ((SystemClock.elapsedRealtime() - lastPressed) < 600) {
                if (pressCnt == 1) {
                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (pressCnt == 2) {
                                boardVisualization.showMap();
                            } else if (pressCnt == 3) {
                                // Toggle master mode
                                if (masterRemote == true) {
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
                            burnerBoard.flashScreen(400);

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
            if (intent != null) {
                if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                    Log.v(TAG, "ACTION_USB_DEVICE_ATTACHED");
                    Parcelable usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    // Create a new intent and put the usb device in as an extra
                    Intent broadcastIntent = new Intent(ACTION.USB_DEVICE_ATTACHED);
                    broadcastIntent.putExtra(UsbManager.EXTRA_DEVICE, usbDevice);

                    // Broadcast this event so we can receive it
                    LocalBroadcastManager.getInstance(context).sendBroadcast(broadcastIntent);

                    if (burnerBoard != null) {
                        burnerBoard.initUsb();
                    }
                }
                if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                    Log.v(TAG, "ACTION_USB_DEVICE_DETACHED");

                    Parcelable usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    // Create a new intent and put the usb device in as an extra
                    Intent broadcastIntent = new Intent(ACTION.USB_DEVICE_DETACHED);
                    broadcastIntent.putExtra(UsbManager.EXTRA_DEVICE, usbDevice);

                    // Broadcast this event so we can receive it
                    LocalBroadcastManager.getInstance(context).sendBroadcast(broadcastIntent);
                }
            }
        }
    }
}





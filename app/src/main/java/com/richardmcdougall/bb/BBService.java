package com.richardmcdougall.bb;

import android.app.Activity;
import android.app.DownloadManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.media.ToneGenerator;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.KeyEvent;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.AudioTrack;
import android.media.AudioFormat;
import android.os.Build;

import org.json.JSONObject;
import android.bluetooth.BluetoothDevice;
import android.media.RingtoneManager;
import android.media.Ringtone;

public class BBService extends Service {

    private static final String TAG = "BB.BBService";

    public static final String ACTION_STATS = "com.richardmcdougall.bb.BBServiceStats";
    public static final String ACTION_BUTTONS = "com.richardmcdougall.bb.BBServiceButtons";
    public static final String ACTION_GRAPHICS = "com.richardmcdougall.bb.BBServiceGraphics";
    public int GetMaxLightModes() {
        return dlManager.GetTotalVideo() + 10;
    }

    public BBDownloadManager dlManager;

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
    public UDPClientServer udpClientServer = null;
    public String boardId = Build.MODEL;
    public String boardType = Build.MANUFACTURER;
    //ArrayList<MusicStream> streamURLs = new ArrayList<BBService.MusicStream>();
    //ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
    private int mBoardMode =1; // Mode of the Ardunio/LEDs
    BoardVisualization mBoardVisualization = null;

    private int statePeers = 0;
    private long stateReplies = 0;
    //public String mSerialConn = "";

    int currentRadioStream = 1;
    long phoneModelAudioLatency = 0;

    TextToSpeech voice;

    int work = 0;

    public BBService() {
    }

    public void l(String s) {
        Log.v(TAG, s);
        sendLogMsg(s);
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
    Thread batteryMonitor = null;


    private static final Map<String, String> BoardNames = new HashMap<String, String>();

    static {
        BoardNames.put("BISCUIT", "Richard");
        BoardNames.put("newproto", "Richard");
    }

    @Override
    public void onCreate() {

        super.onCreate();
        l("BBService: onCreate");
        l("I am " + Build.MANUFACTURER + " / " + Build.MODEL);

        // Setup beep for blueooth remote connect
        btNotifier();

        voice = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                // check for successful instantiation
                if (status == TextToSpeech.SUCCESS) {
                    if (voice.isLanguageAvailable(Locale.UK) == TextToSpeech.LANG_AVAILABLE)
                        voice.setLanguage(Locale.US);
                    l("Text To Speech ready...");
                    voice.setPitch((float)0.8);
                    String utteranceId = UUID.randomUUID().toString();
                    System.out.println("Where do you want to go, " + boardId + "?");
                    voice.setSpeechRate((float) 0.9);
                    voice.speak("I am " + boardId + "?",
                            TextToSpeech.QUEUE_FLUSH, null, utteranceId);
                } else if (status == TextToSpeech.ERROR) {
                    l("Sorry! Text To Speech failed...");
                }
            }
        });

        dlManager = new BBDownloadManager(getApplicationContext().getFilesDir().getAbsolutePath());
        dlManager.onProgressCallback = new BBDownloadManager.OnDownloadProgressType() {
            long lastTextTime = 0;

            public void onProgress(String file, long fileSize, long bytesDownloaded) {
                if (fileSize<=0)
                    return ;

                long curTime = System.currentTimeMillis();
                if (curTime-lastTextTime>30000) {
                    lastTextTime = curTime;
                    long percent = bytesDownloaded*100/fileSize;

                    voice.speak("Downloading " + file + ", " + String.valueOf(percent) + " Percent", TextToSpeech.QUEUE_ADD, null, "downloading");
                    lastTextTime = curTime;
                    l(String.format("Downloading %02x%% %s", bytesDownloaded*100/fileSize, file));
                }
            }
            public void onVoiceCue(String msg) {
                voice.speak(msg, TextToSpeech.QUEUE_ADD, null, "Download Message");
            }
        };

        mContext = getApplicationContext();

        HandlerThread mHandlerThread = null;
        mHandlerThread = new HandlerThread("BBServiceHandlerThread");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());


        // Register to receive button messages
        IntentFilter filter = new IntentFilter(BBService.ACTION_BUTTONS);
        LocalBroadcastManager.getInstance(this).registerReceiver(mButtonReceiver, filter);


        if (musicPlayer == null) {
            l("starting music player thread");
            // Start Music Player
            Thread musicPlayer = new Thread(new Runnable() {
                public void run() {
                    musicPlayerThread();
                }
            });
            musicPlayer.start();
        } else {
            l("music player already running");
        }

        if (batteryMonitor == null) {
            l("starting music player thread");
            // Start Music Player
            Thread batteryMonitor = new Thread(new Runnable() {
                public void run() {
                    batteryThread();
                }
            });
            batteryMonitor.start();
        } else {
            l("battery moniitor already running");
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
        return mStartMode;
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


    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */

    private BurnerBoard mBurnerBoard;

    private void startLights() {

        if (boardType.contains("BLU")) {
            return;
        }

        if (boardType.contains("Classic")) {
            mBurnerBoard = new BurnerBoardClassic(this, mContext);
        } else {
            mBurnerBoard = new BurnerBoardAzul(this, mContext);
        }
        if (mBurnerBoard == null) {
            l("startLights: null burner board");
            return;
        }
        if (mBurnerBoard != null) {
            mBurnerBoard.attach(new BoardCallback());
        }

        if (mBoardVisualization == null) {
            mBoardVisualization = new BoardVisualization(this, mBurnerBoard);
        }
        mBoardVisualization.setMode(mBoardMode);
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

            Log.d(TAG, "onReceive entered");
            String action = intent.getAction();

            if (ACTION_BUTTONS.equals(action)) {
                Log.d(TAG, "Got Some");
                buttons actionType = (buttons) intent.getSerializableExtra("buttonType");
                switch (actionType) {
                    case BUTTON_KEYCODE:
                        l("BUTTON_KEYCODE");
                        int keyCode = intent.getIntExtra("keyCode", 0);
                        KeyEvent event = (KeyEvent) intent.getParcelableExtra("keyEvent");
                        onKeyDown(keyCode, event);
                        break;
                    case BUTTON_TRACK:
                        l("BUTTON_TRACK");
                        NextStream();
                        break;
                    case BUTTON_MODE_UP:
                        setMode(99);
                        break;
                    case BUTTON_MODE_DOWN:
                        setMode(98);
                        break;
                    case BUTTON_DRIFT_DOWN:
                        MusicOffset(-10);
                        break;
                    case BUTTON_DRIFT_UP:
                        MusicOffset(10);
                        break;
                    case BUTTON_VOL_DOWN:
                        onVolDown();
                        break;
                    case BUTTON_VOL_UP:
                        onVolUp();
                        break;
                    case BUTTON_VOL_PAUSE:
                        onVolPause();
                        break;
                    default:
                        break;
                }
            }
        }
    };


    public String GetRadioStreamFile(int idx) {

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
        return((int)(vol * (float)127.0));
    }

    public void setBoardVolume(int v) {
        l("Volume: " + vol + " -> " + v);
        vol = (float)v / (float)127;
        mediaPlayer.setVolume(vol, vol);
    }

    long lastSeekOffset = 0;
    long lastSeekTimestamp = 0;

    long GetCurrentStreamLengthInSeconds() {
        return dlManager.GetAudioLength(currentRadioStream-1);
    }

    // Main thread to drive the music player
    void musicPlayerThread() {

        String model = android.os.Build.MODEL;

        l("Starting BB on phone " + model);


        if (model.equals("XT1064")) {
            phoneModelAudioLatency = 10;
        } else if (model.equals("BLU DASH M2")) {
            phoneModelAudioLatency = 10;
        } else if (model.equals("BLU ADVANCE 5.0 HD")) {
            phoneModelAudioLatency = 10;
        } else if (model.equals("MSM8916 for arm64")) {
            phoneModelAudioLatency = 40;
        } else {
            phoneModelAudioLatency = 82;
            userTimeOffset = -4;
        }


        InitClock();

        udpClientServer = new UDPClientServer(this);
        udpClientServer.Run();


        InitClock();

        WifiManager mWiFiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);

        // RMC putback

        if (mWiFiManager.isWifiEnabled()) {
            l("Wifi Enabled Already, disabling");
            //mWiFiManager.setWifiEnabled(false);
        }

        l("Enabling Wifi...");
        if (mWiFiManager.setWifiEnabled(true) == false) {
            l("Failed to enable wifi");
        }
        if (mWiFiManager.reassociate() == false) {
            l("Failed to associate wifi");
        }

        dlManager.StartDownloads();


        // MediaSession ms = new MediaSession(mContext);

        bluetoothModeInit();

        int musicState = 0;

        while (true) {

            switch (musicState) {
                case 0:
                    if (dlManager.GetTotalAudio()!=0) {
                        musicState = 1;
                        l("Downloadd: Starting Radio Mode");
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
                        Thread.sleep(1000);
                    } catch (Throwable e) {
                    }
                    if (currentRadioStream == 0) {
                        musicState = 2;
                    }
                    break;

                case 2:
                    bluetoothPlay();
                    if (currentRadioStream != 0) {
                        musicState = 1;
                    }
                    break;

                default:
                    break;


            }

        }

    }

    public void SeekAndPlay() {
            if (mediaPlayer != null && dlManager.GetTotalAudio()!=0) {
                synchronized (mediaPlayer) {
                    long ms = CurrentClockAdjusted() + userTimeOffset - phoneModelAudioLatency;

                    long lenInMS = GetCurrentStreamLengthInSeconds() * 1000;

                    long seekOff = ms % lenInMS;
                    long curPos = mediaPlayer.getCurrentPosition();
                    long seekErr = curPos - seekOff;


                    if (curPos == 0 || seekErr != 0) {
                        if (curPos == 0 || Math.abs(seekErr) > 10) {
                            l("SeekAndPlay: qqexplicit seek");
                            //mediaPlayer.pause();
                            // hack: I notice i taked 79ms on dragonboard to seekTo
                            mediaPlayer.seekTo((int) seekOff + 79);
                            mediaPlayer.start();
                        } else {
                            //PlaybackParams params = mediaPlayer.getPlaybackParams();
                            PlaybackParams params = new PlaybackParams();
                            Float speed = 1.0f + (seekOff - curPos) / 1000.0f;
                            l("SeekAndPlay: seekErr = " + seekErr + ", adjusting speed to " + speed);
                            params.setSpeed(speed);
                            try {
                                //l("SeekAndPlay: pause()");
                                //mediaPlayer.pause();
                                l("SeekAndPlay: setPlaybackParams()");
                                mediaPlayer.getPlaybackParams().setSpeed(speed);
                                        //setPlaybackParams(params);
                                l("SeekAndPlay: setPlaybackParams() Sucesss!!");
                            } catch (Throwable err) {
                                l("SeekAndPlay setPlaybackParams: " + err.getMessage());
                                err.printStackTrace();
                            }
                            l("SeekAndPlay: start()");
                            mediaPlayer.start();
                        }
                    }

                    String msg = "SeekErr " + seekErr + " SvOff " + serverTimeOffset +
                            " User " + userTimeOffset + "\nSeekOff " + seekOff +
                            " RTT " + serverRTT + " Strm" + currentRadioStream;
                    if (udpClientServer.tSentPackets != 0)
                        msg += "\nSent " + udpClientServer.tSentPackets;
                    l(msg);

                    Intent in = new Intent(ACTION_STATS);
                    in.putExtra("resultCode", Activity.RESULT_OK);
                    in.putExtra("msgType", 1);
                    // Put extras into the intent as usual
                    in.putExtra("seekErr", seekErr);
                    in.putExtra("currentRadioStream", currentRadioStream);
                    in.putExtra("userTimeOffset", userTimeOffset);
                    in.putExtra("serverTimeOffset", serverTimeOffset);
                    in.putExtra("serverRTT", serverRTT);
                    LocalBroadcastManager.getInstance(this).sendBroadcast(in);
                }
            }

    }

    private void RadioStop() {

    }

    private void RadioResume() {

    }

    void NextStream() {
        int nextRadioStream = currentRadioStream + 1;
        if (nextRadioStream > dlManager.GetTotalAudio())
            nextRadioStream = 0;
        SetRadioStream(nextRadioStream);
    }


    // Set radio input mode
    void SetRadioStream(int index) {
        l("SetRadioStream: " + index);
        currentRadioStream = index;
        if (index == 0) {
            mediaPlayer.pause();
            l("Bluetooth Mode");
            voice.speak("Blue tooth Audio", TextToSpeech.QUEUE_FLUSH, null, "bluetooth");
            try {
                Thread.sleep(1000);
            } catch (Throwable e) {
            }
            bluetoothModeEnable();

        } else {
            l("Radio Mode");
            voice.speak("Track " + index, TextToSpeech.QUEUE_FLUSH, null, "track");
            bluetoothModeDisable();
            try {
                if (mediaPlayer != null  && dlManager.GetTotalAudio()!=0) {
                    synchronized (mediaPlayer) {
                        lastSeekOffset = 0;
                        FileInputStream fds = new FileInputStream(dlManager.GetAudioFile(index));
                        l("playing file " + GetRadioStreamFile(index));
                        mediaPlayer.reset();
                        mediaPlayer.setDataSource(fds.getFD());
                        mBoardVisualization.attachAudio(mediaPlayer.getAudioSessionId());
                        fds.close();

                        mediaPlayer.setLooping(true);
                        mediaPlayer.setVolume(vol, vol);
                        mediaPlayer.prepare();
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
        SetRadioStream(currentRadioStream);
    }


    private AudioRecord mAudioInStream;
    private AudioTrack mAudioOutStream;
    private AudioManager mAudioManager;
    private byte [] mAudioBuffer;

    public void bluetoothModeInit() {
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        AudioDeviceInfo[] deviceList = mAudioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
        for (int index = 0; index < deviceList.length; index++) {
            l("Audio device" + deviceList[index].toString());
        }
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
        int buffersize = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT);
        mAudioInStream = new AudioRecord(MediaRecorder.AudioSource.MIC, 44100, AudioFormat. CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, buffersize);
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
        }
        catch (Exception e) {
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
        boolean handled = false;
        if (event.getRepeatCount() == 0) {
            l("Keycode:" + keyCode);
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

    public void setMode(int mode) {
        boolean cansetMode = mBurnerBoard.setMode(50);
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
        else if (mBoardMode<1)
            mBoardMode = maxModes;

        l("SetMode:" + mBoardVisualization.getMode() + " -> " + mode);
        mBoardVisualization.setMode(mBoardMode);
        voice.speak("mode" + mBoardMode, TextToSpeech.QUEUE_FLUSH, null, "mode");
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

    private long lastOkStatement = System.currentTimeMillis();
    private long lastLowStatement = System.currentTimeMillis();
    private long lastUnknownStatement = System.currentTimeMillis();

    private void batteryThread() {

        boolean announce = false;
        if (mBurnerBoard != null) {
            int level = mBurnerBoard.getBattery();
            if (level < 0) {
                if (System.currentTimeMillis() - lastUnknownStatement > 900000) {
                    lastUnknownStatement = System.currentTimeMillis();
                    voice.speak("Battery level unknown", TextToSpeech.QUEUE_FLUSH, null, "batteryUnknown");
                }
            }
            if (level < 15) {
                voice.speak("Battery almost empty, back to camp. Level is " +
                        level + " percent", TextToSpeech.QUEUE_FLUSH, null, "batteryEmpty");
            } else if (level <= 25) {
                if (System.currentTimeMillis() - lastLowStatement > 300000) {
                    lastLowStatement = System.currentTimeMillis();
                    announce = true;
                }
            } else {
                if (System.currentTimeMillis() - lastOkStatement > 1800000) {
                    lastOkStatement = System.currentTimeMillis();
                    announce = true;
                }
            }
            if (announce) {
                voice.speak("Battery almost empty, back to camp. Level is " +
                        level + " percent", TextToSpeech.QUEUE_FLUSH, null, "batteryLow");
            }
        }
        try {
            Thread.sleep(60000);
        } catch (Throwable e) {
        }
    }

    private void onBatteryButton() {
        if (mBurnerBoard != null) {
            mBurnerBoard.showBattery();
        }
    }

    private void btNotifier() {
        //you can get notified when a new device is connected using Broadcast receiver
        BroadcastReceiver btReceive=new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                String action = intent.getAction();
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    voice.speak("Connected", TextToSpeech.QUEUE_FLUSH, null, "Connected");
                    //the device is found
                    try {
                        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                        Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                        r.play();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        };
    }


}

package com.richardmcdougall.bb;

import android.app.Activity;
import android.app.DownloadManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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


public class BBService extends Service {

    private static final String TAG = "BB.BBService";

    public static final String ACTION_STATS = "com.richardmcdougall.bb.BBServiceStats";
    public static final String ACTION_BUTTONS = "com.richardmcdougall.bb.BBServiceButtons";
    public static final String ACTION_GRAPHICS = "com.richardmcdougall.bb.BBServiceGraphics";

    public static enum buttons {
        BUTTON_KEYCODE, BUTTON_TRACK, BUTTON_DRIFT_UP,
        BUTTON_DRIFT_DOWN, BUTTON_MODE_UP, BUTTON_MODE_DOWN, BUTTON_MODE_PAUSE
    }

    //private BBListenerAdapter mListener = null;
    public Handler mHandler = null;
    private Context mContext;
    private MediaPlayer mediaPlayer = new MediaPlayer();
    private float vol = 0.80f;
    private boolean downloaded = false;
    public long serverTimeOffset = 0;
    public long serverRTT = 0;
    private int userTimeOffset = 0;
    public MyWifiDirect mWifi = null;
    public UDPClientServer udpClientServer = null;
    public String boardId;
    ArrayList<MusicStream> streamURLs = new ArrayList<BBService.MusicStream>();
    //ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
    private int mBoardMode = 1; // Mode of the Ardunio/LEDs
    BoardVisualization mBoardVisualization = null;

    private int statePeers = 0;
    private long stateReplies = 0;
    //public String mSerialConn = "";

    int currentRadioStream = 0;
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

    private static final Map<String, String> BoardNames = new HashMap<String, String>();

    static {
        BoardNames.put("BISCUIT", "Richard");
    }

    @Override
    public void onCreate() {

        super.onCreate();
        l("BBService: onCreate");

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
                    String whosBoard = "Donald Trump";
                    System.out.println("Where do you want to go, " + boardId + "?");
                    if (boardId != null)
                        whosBoard = BoardNames.get(boardId);
                    voice.setSpeechRate((float) 1.2);
                    voice.speak("Lets ride," + whosBoard + "?",
                            TextToSpeech.QUEUE_FLUSH, null, utteranceId);
                } else if (status == TextToSpeech.ERROR) {
                    l("Sorry! Text To Speech failed...");
                }
            }
        });

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

        startLights();

        mBoardVisualization.attachAudio(mediaPlayer.getAudioSessionId());

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
        mBurnerBoard = new BurnerBoard(this, mContext);
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
                    default:
                        break;
                }
            }
        }
    };


    public String GetRadioStreamFile(int idx) {

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
                Environment.DIRECTORY_MUSIC).toString() + "/radio_stream" + idx + ".mp3";

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
    };

    void MusicListInit() {

        //streamURLs.add(0, new MusicStream("https://dl.dropboxusercontent.com/s/2m7onrf1i5oobxr/bottle_20bpm_4-4time_610beats_stereo_uUzFTJ.mp3?dl=0", 132223476, 2 * 60 * 60 + 17 * 60 + 43));
        streamURLs.add(0, new MusicStream("https://dl.dropboxusercontent.com/s/mcm5ee441mzdm39/01-FunRide2.mp3?dl=0", 122529253, 2 * 60 * 60 + 7 * 60 + 37)); // real one
        //streamURLs.add(0, new MusicStream("https://dl.dropboxusercontent.com/s/mcm5ee441mzdm39/01-FunRide2.mp3?dl=0", 132223476, 2 * 60 * 60 + 7 * 60 + 37));
        // Stream 1 is reserved for Bluetooth input
        streamURLs.add(1, null);
        /*
        streamURLs.add(2, new MusicStream("https://dl.dropboxusercontent.com/s/jvsv2fn5le0f6n0/02-BombingRun2.mp3?dl=0", 118796042, 2 * 60 * 60 + 3 * 60 + 44));
        streamURLs.add(3, new MusicStream("https://dl.dropboxusercontent.com/s/j8y5fqdmwcdhx9q/03-RobotTemple2.mp3?dl=0", 122457782, 2 * 60 * 60 + 7 * 60 + 33));
        streamURLs.add(4, new MusicStream("https://dl.dropboxusercontent.com/s/vm2movz8tkw5kgm/04-Morning2.mp3?dl=0", 122457782, 2 * 60 * 60 + 7 * 60 + 33));
        streamURLs.add(5, new MusicStream("https://dl.dropboxusercontent.com/s/52iq1ues7qz194e/Flamethrower%20Sound%20Effects.mp3?dl=0", 805754, 33));
        streamURLs.add(6, new MusicStream("https://dl.dropboxusercontent.com/s/fqsffn03qdyo9tm/Funk%20Blues%20Drumless%20Jam%20Track%20Click%20Track%20Version2.mp3?dl=0", 6532207, 4 * 60 + 32));
        //streamURLs.add(5, new MusicStream("https://dl.dropboxusercontent.com/s/39x2hdu5k5n6628/Beatles%20Long%20Track.mp3?dl=0", 58515039, 2438));
        streamURLs.add(7, new MusicStream("https://dl.dropboxusercontent.com/s/vx11kxtkmhgycd9/click_120bpm_4-4time_610beats_stereo_WjI2zj.mp3?dl=0", 2436288, 5 * 60 + 4));
        streamURLs.add(8, new MusicStream("https://dl.dropboxusercontent.com/s/2m7onrf1i5oobxr/bottle_20bpm_4-4time_610beats_stereo_uUzFTJ.mp3?dl=0", 14616192, 30 * 60 + 30));
        */
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
            l(msg);
        }
    }

    long lastSeekOffset = 0;
    long lastSeekTimestamp = 0;

    long GetCurrentStreamLengthInSeconds() {
        return streamURLs.get(currentRadioStream).lengthInSeconds;
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
        MusicListInit();

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


        // RMC put this back
        DownloadMusic2();

        mWifi = new MyWifiDirect(this, udpClientServer);

        //MediaSession ms = new MediaSession(mContext);

        bluetoothModeInit();

        int musicState = 0;

        while (true) {

            switch (musicState) {

                case 0:
                    if (downloaded) {
                        musicState = 1;
                        l("Radio Mode");
                        RadioMode();
                    }
                    break;

                case 1:
                    SeekAndPlay();
                    try {
                        Thread.sleep(1000);
                    } catch (Throwable e) {
                    }
                    if (currentRadioStream == 1) {
                        musicState = 2;
                    }
                    break;

                case 2:
                    bluetoothPlay();
                    if (currentRadioStream != 1) {
                        musicState = 1;
                    }
                    break;

                default:
                    break;


            }

        }

    }

    public void SeekAndPlay() {
        try {
            if (mediaPlayer != null && downloaded) {
                synchronized (mediaPlayer) {
                    long ms = CurrentClockAdjusted() + userTimeOffset - phoneModelAudioLatency;

                    long lenInMS = GetCurrentStreamLengthInSeconds() * 1000;

                    long seekOff = ms % lenInMS;
                    long curPos = mediaPlayer.getCurrentPosition();
                    long seekErr = curPos - seekOff;

                    String msg = "SeekErr " + seekErr + " SvOff " + serverTimeOffset +
                            " User " + userTimeOffset + "\nSeekOff " + seekOff +
                            " RTT " + serverRTT + " Strm" + currentRadioStream;
                    if (udpClientServer.tSentPackets != 0)
                        msg += "\nSent " + udpClientServer.tSentPackets;
                    //l(msg);

                    if (curPos == 0 || seekErr != 0) {
                        if (curPos == 0 || Math.abs(seekErr) > 5000)
                            mediaPlayer.seekTo((int) seekOff);
                        else {
                            PlaybackParams params = mediaPlayer.getPlaybackParams();
                            Float speed = 1.0f + (seekOff - curPos) / 2500.0f;
                            params.setSpeed(speed);
                            mediaPlayer.setPlaybackParams(params);
                            mediaPlayer.start();
                        }
                        mediaPlayer.start();
                    }
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
        } catch (Throwable thr_err) {

        }
    }

    private void RadioStop() {

    }

    private void RadioResume() {

    }

    void NextStream() {
        int nextRadioStream = currentRadioStream + 1;
        if (nextRadioStream > streamURLs.size())
            nextRadioStream = 0;
        SetRadioStream(nextRadioStream);
    }


    // Set radio input mode
    void SetRadioStream(int index) {
        l("SetRadioStream: " + index);
        currentRadioStream = index;
        if (index == 1) {
            l("Bluetooth Mode");
            mediaPlayer.pause();
            bluetoothModeEnable();
            voice.speak("Blue tooth Audio", TextToSpeech.QUEUE_FLUSH, null, "bluetooth");

        } else {
            l("Radio Mode");
            voice.speak("Track " + index, TextToSpeech.QUEUE_FLUSH, null, "track");
            bluetoothModeDisable();
            try {
                if (mediaPlayer != null) {
                    //synchronized (mediaPlayer) {
                    mBoardVisualization.attachAudio(mediaPlayer.getAudioSessionId());
                    lastSeekOffset = 0;
                    FileInputStream fds = new FileInputStream(GetRadioStreamFile(index));
                    l("playing file " + GetRadioStreamFile(index));
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
    }

    public void RadioMode() {
        SetRadioStream(currentRadioStream);
    }


    private AudioRecord mAudioInStream;
    private AudioTrack mAudioOutStream;
    private byte [] mAudioBuffer;

    public void bluetoothModeInit() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
        int buffersize = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT);
        mAudioInStream = new AudioRecord(MediaRecorder.AudioSource.MIC, 44100, AudioFormat. CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, buffersize);
        mAudioOutStream = new AudioTrack(AudioManager.STREAM_VOICE_CALL, 44100, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, buffersize, AudioTrack.MODE_STREAM);
        mAudioOutStream.setPlaybackRate(44100);
        mAudioOutStream.setVolume(vol);
        mAudioBuffer = new byte[buffersize];
        mAudioInStream.startRecording();
        mAudioOutStream.play();
    }

    public void bluetoothModeEnable() {
        mAudioOutStream.play();
        mAudioInStream.startRecording();
        mBoardVisualization.attachAudio(mAudioOutStream.getAudioSessionId());
    }

    public void bluetoothModeDisable() {
        mAudioInStream.stop();
        mAudioOutStream.stop();
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

    private void setMode(int mode) {
        boolean cansetMode = mBurnerBoard.setMode(50);
        //boolean cansetMode = mBurnerBoard.setMode(mode);
        //if (cansetMode == false) {
            // Likely not connected to physical burner board, fallback
            if (mode == 99) {
                mBoardMode++;
            } else if (mode == 98) {
                mBoardMode--;
            }
        //}
        if (mBoardMode > 15)
            mBoardMode = 1;
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






}

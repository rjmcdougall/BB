package com.richardmcdougall.bb;

import android.app.DownloadManager;
import android.app.IntentService;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.ToneGenerator;
import android.media.audiofx.Visualizer;
import android.media.session.MediaSession;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.os.Handler;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.speech.tts.TextToSpeech;


import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
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
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

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
import android.view.KeyEvent;
import android.view.InputDevice;

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
//import android.app.*;
import android.app.Activity;
import android.net.*;
import android.Manifest.*;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import android.support.v4.content.ContextCompat;
import android.content.pm.PackageManager;


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
    ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
    private int boardMode = 0; // Mode of the Ardunio/LEDs

    private int statePeers = 0;
    private long stateReplies = 0;
    public byte[] mBoardFFT;
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
                    if (voice.isLanguageAvailable(Locale.US) == TextToSpeech.LANG_AVAILABLE)
                        voice.setLanguage(Locale.US);
                    l("Text To Speech ready...");
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

        // Supported Languages
        Set<Locale> supportedLanguages = voice.getAvailableLanguages();
        if (supportedLanguages != null) {
            for (Locale lang : supportedLanguages) {
                l("Voice Supported Language: " + lang);
            }
        }
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
        l("BBService: onDestroy");
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
        // Start Board Display
        Thread boardDisplay = new Thread(new Runnable() {
            public void run() {
                boardDisplayThread();
            }
        });
        boardDisplay.start();
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
                        mBurnerBoard.setMode(99);
                        break;
                    case BUTTON_MODE_DOWN:
                        mBurnerBoard.setMode(98);
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

        return (mContext.getExternalFilesDir(
                Environment.DIRECTORY_MUSIC).toString() + "/test.mp3");

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


        //return mContext.getExternalFilesDir(
                //Environment.DIRECTORY_MUSIC).toString() + "/radio_stream" + idx + ".mp3";

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

        //streamURLs.add(0, new MusicStream("https://dl.dropboxusercontent.com/s/2m7onrf1i5oobxr/bottle_20bpm_4-4time_610beats_stereo_uUzFTJ.mp3?dl=0", 132223476, 2 * 60 * 60 + 17 * 60 + 43));
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

        boolean started = false;

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


        if (mWiFiManager.isWifiEnabled()) {
            l("Wifi Enabled Already, disabling");
            mWiFiManager.setWifiEnabled(false);
        }

        l("Enabling Wifi...");
        if (mWiFiManager.setWifiEnabled(true) == false) {
            l("Failed to enable wifi");
        }
        if (mWiFiManager.reassociate() == false) {
            l("Failed to associate wifi");
        }


        // RMC put this back
        // DownloadMusic2();

        mWifi = new MyWifiDirect(this, udpClientServer);

        boardVisualizerSetup(mediaPlayer.getAudioSessionId());
        //MediaSession ms = new MediaSession(mContext);

        while (true) {

            // Keep checking if music is ready
            // RMC remove this
            downloaded = true;
            if ((started == false) && downloaded) {
                started = true;
                l("Radio Mode");
                RadioMode();
            } else {

                SeekAndPlay();

                try {
                    Thread.sleep(1000);
                } catch (Throwable e) {
                }
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

    void NextStream() {
        int nextRadioStream = currentRadioStream + 1;
        if (nextRadioStream == streamURLs.size())
            nextRadioStream = 0;
        SetRadioStream(nextRadioStream);
    }

    void SetRadioStream(int index) {
        l("SetRadioStream: " + index);
        try {
            if (mediaPlayer != null) {
                //synchronized (mediaPlayer) {
                lastSeekOffset = 0;
                currentRadioStream = index;
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

    public void RadioMode() {
        SetRadioStream(currentRadioStream);
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
                mBurnerBoard.setMode(99);
                break;
            case 98:
                mBurnerBoard.setMode(98);
                break;
            case 88: //satachi left button
                mBurnerBoard.setMode(99);
                break;
        }
        //mHandler.removeCallbacksAndMessages(null);
        return true;
    }

    public class BoardCallback implements BurnerBoard.BoardEvents {

        public void BoardId(String str) {
            boardId = str;
            l("ardunio BoardID callback:" + str + " " + boardId);
            //status.setText("Connected to " + boardId);
        }

        public void BoardMode(int mode) {
            boardMode = mode;
            voice.speak("mode" + boardMode, TextToSpeech.QUEUE_FLUSH, null, "mode");
            l("ardunio mode callback:" + boardMode);
            //modeStatus.setText(String.format("%d", boardMode));
        }
    }

    private int boardDisplayCnt = 0;
    private Visualizer mVisualizer;
    private int mAudioSessionId;

    void boardVisualizerSetup(int audioSessionId) {
        int vSize;

        l("session=" + audioSessionId);
        mAudioSessionId = audioSessionId;
        // Create the Visualizer object and attach it to our media player.
        try {
            mVisualizer = new Visualizer(audioSessionId);
        } catch (Exception e) {
            l("Error enabling visualizer: " + e.getMessage());
            //System.out.println("Error enabling visualizer:" + e.getMessage());
            return;
        }
        vSize = Visualizer.getCaptureSizeRange()[1];
        mVisualizer.setEnabled(false);
        mBoardFFT = new byte[vSize];
        mVisualizer.setCaptureSize(vSize);
        mVisualizer.setEnabled(true);
        l("Enabled visualizer with " + vSize + " bytes");

    }


    int sleepTime = 30;

    // Main thread to drive the Board's display & get status (mode, voltage,...)
    void boardDisplayThread() {

        l("Starting board display thread...");


        while (true) {
            switch (boardMode) {

                case 0:
                    sleepTime = 20;
                    modeAudioBeat();

                case 4:
                    sleepTime = 20;
                    //modeEsperanto();
                    break;
                case 5:
                    sleepTime = 20;
                    //modeDisco();
                    break;
                case 6:
                    sleepTime = 20;
                    //modeAudioBarV();
                    break;
                case 7:
                    sleepTime = 20;
                    //modeAudioBarH();
                    break;
                case 8:
                    sleepTime = 20;
                    modeTest();
                    break;
                case 9:
                    sleepTime = 20;
                    modeAudioMatrix();
                    break;

                case 10:
                    mBurnerBoard.setMode(1);
                    boardMode = 1;
                    break;

                //case 9:
                //sleepTime = 30;
                //modeAudioBeat();
                //break;

                default:
//                    modeDisco();
//                    modeAudioBeat();
//                    modeAudioBarV();
//                    modeAudioBarV();
//                      sleepTime = 20;
//                      modeAudioMatrix();
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
    private byte[] testRow1 = "012345678901234567890123456789012345".getBytes();
    private byte[] testRow2 = "567890123456789012345678901234567890".getBytes();


    void modeTest() {
        switch (testState) {
            case 0:
                mBurnerBoard.setRow(10, testRow1);
                mBurnerBoard.update();
                break;
            case 1:
                mBurnerBoard.setRow(10, testRow2);
                mBurnerBoard.update();
                break;
            default:
                break;
        }

        testState++;
        if (testState > 1)
            testState = 0;
    }


    int testValue = 0;

    void modeAudioMatrix() {

        byte[] pixels = new byte[36];

        if (mBoardFFT == null)
            return;

        if (mVisualizer.getFft(mBoardFFT) == mVisualizer.SUCCESS) {

            int pixel = 0;
            byte rfk;
            byte ifk;
            int dbValue = 0;
            // There are 1024 values - 512 x real, imaginary
            for (int i = 0; i < 512; i++) {
                rfk = mBoardFFT[i];
                ifk = mBoardFFT[i + 1];
                float magnitude = (rfk * rfk + ifk * ifk);
                dbValue += java.lang.Math.max(0, 30 * Math.log10(magnitude));

                // Aggregate each 8 values to give 64 bars
                if ((i & 7) == 0) {
                    dbValue -= 50;
                    int value = java.lang.Math.max(dbValue, 0);
                    value = java.lang.Math.min(value, 255);
                    dbValue = 0;

                    // Take the 4th through 16th values
                    if ((i / 8) >= 12 && (i / 8) < 48) {
                        ;
                        pixels[pixel] = (byte) java.lang.Math.max(0, value);
//                            pixels[pixel] = (byte)testValue;
                        pixel++;
                        //System.out.println("modeAudioMatrix Value[" + pixel + "] = " + value);
                    }
                }
            }
            mBurnerBoard.setRow(69, pixels);
            mBurnerBoard.update();
            mBurnerBoard.scroll(true);
        } else {
            l("visualizer failued");
        }

        testValue++;
        if (testValue > 255)
            testValue = 0;
        return;

    }


    private int discoState = 0;
    private Random discoRandom = new Random();

    void modeAudioBeat() {

        if (mBoardFFT == null)
            return;
        int r = 0;
        int b = 0;
        int g = 0;

        if (mVisualizer.getFft(mBoardFFT) == mVisualizer.SUCCESS) {


            byte rfk;
            byte ifk;
            int dbValue = 0;
            for (int i = 0; i < 512; i++) {
                rfk = mBoardFFT[i];
                ifk = mBoardFFT[i + 1];
                float magnitude = (rfk * rfk + ifk * ifk);
                dbValue += java.lang.Math.max(0, 128 * Math.log10(magnitude));
                if (dbValue < 0)
                    dbValue = 0;

                if ((i & 63) == 0) {
                    int value = java.lang.Math.min(dbValue / 64, 255);
                    dbValue = 0;
                    //System.out.println("Visualizer Value[" + i / 64 + "] = " + value);

                    if (i == 64)
                        r = value;
                    else if (i == 128)
                        g = value;
                    else if (i == 256)
                        b = value;
                }
            }

        }
        mBurnerBoard.fillScreen(r, g, b);

        mBurnerBoard.flush();
        return;
    }

    /*
    void modeAudioBarV() {

        if (mBoardFFT == null)
            return;

            if (mVisualizer.getFft(mBoardFFT) == mVisualizer.SUCCESS) {

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
                    //mListener.sendCmdArg((int) 0);
                    mListener.sendCmdEnd();
                }
            }
        }
        boardFlush();
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
        boardFlush();
        return;

    }

    void modeEsperanto() {

        if (mBoardFFT == null)
            return;

        synchronized (mSerialConn) {
            if (mVisualizer.getFft(mBoardFFT) == mVisualizer.SUCCESS) {


                if (mListener != null)
                    mListener.sendCmdStart(17);

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
        boardFlush();
        return;

    }


    void modeDisco() {

        if (mBoardFFT == null)
            return;

        synchronized (mSerialConn) {
            if (mVisualizer.getFft(mBoardFFT) == mVisualizer.SUCCESS) {


                if (mListener != null)
                    mListener.sendCmdStart(18);

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
        boardFlush();
        return;

    }

    */


}

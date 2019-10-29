package com.richardmcdougall.bb;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.PlaybackParams;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import java.io.FileInputStream;

public class MusicPlayer {


    public static final String ACTION_STATS = "com.richardmcdougall.bb.BBServiceStats";

    private static final String TAG = "BB.MusicPlayer";
    public RFClientServer mRfClientServer = null;
    int currentRadioChannel = 1;
    BoardVisualization mBoardVisualization = null;
    long lastSeekOffset = 0;
    long lastSeekTimestamp = 0;
    long phoneModelAudioLatency = 0;
    private BBService mMain = null;
    private DownloadManager dlManager = null;
    private MediaPlayer mediaPlayer = new MediaPlayer();
    private long seekSave = 0;
    private long seekSavePos = 0;
    private int userTimeOffset = 0;
    private Context mContext = null;
    private BurnerBoard mBurnerBoard = null;
    private float vol = 0.80f;
    TextToSpeech voice;

    MusicPlayer(BBService service,  Context context, DownloadManager dlm, BoardVisualization bv,
        RFClientServer rfc,
                BurnerBoard bb,
                TextToSpeech v){
        dlManager = dlm;
        mMain = service;
        mContext = context;
        mBoardVisualization = bv;
        mRfClientServer = rfc;
        mBurnerBoard = bb;
        voice = v;
    }
    public void l(String s) {
        Log.v(TAG, s);
    }

    public int getRadioChannel() {

        d("GetRadioChannel: ");
        return currentRadioChannel;
    }
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


    void Run() {
        Thread t = new Thread(new Runnable() {
            public void run()
            {
                Thread.currentThread().setName("BB Music Player");
                musicPlayerThread();
            }
        });
        t.start();
    }


    public void RadioMode()
    {
        SetRadioChannel(currentRadioChannel);
    }

    public void d(String logMsg) {
        if (DebugConfigs.DEBUG_AUDIO_SYNC) {
            Log.d(TAG, logMsg);
        }
    }


    void MusicOffset(int ms) {
        userTimeOffset += ms;
        SeekAndPlay();
        d("UserTimeOffset = " + userTimeOffset);
    }

    long GetCurrentStreamLengthInSeconds() {
        return dlManager.GetAudioLength( currentRadioChannel - 1);
    }


    public void SeekAndPlay() {
        if (mediaPlayer != null && dlManager.GetTotalAudio() != 0) {
            synchronized (mediaPlayer) {
                long ms = mMain.CurrentClockAdjusted() + userTimeOffset - phoneModelAudioLatency;

                long lenInMS = GetCurrentStreamLengthInSeconds() * 1000;

                long seekOff = ms % lenInMS;
                long curPos = mediaPlayer.getCurrentPosition();
                long seekErr = curPos - seekOff;

                if (curPos == 0 || seekErr != 0) {
                    if (curPos == 0 || Math.abs(seekErr) > 200) {
                        d("SeekAndPlay: explicit seek");
                        seekSave = SystemClock.elapsedRealtime();
                        mediaPlayer.seekTo((int) seekOff);
                        mediaPlayer.start();
                    } else {

                        Float speed = 1.0f + (seekOff - curPos) / 1000.0f;
                        d("SeekAndPlay: seekErr = " + seekErr + ", adjusting speed to " + speed);

                        if ((seekErr > 10) || (seekErr < -10)) {
                            d("SeekAndPlay: seekErr = " + seekErr + ", adjusting speed to " + speed);
                        }

                        try {

                            PlaybackParams params = new PlaybackParams();
                            params.allowDefaults();
                            params.setSpeed(speed).setPitch(speed).setAudioFallbackMode(PlaybackParams.AUDIO_FALLBACK_MODE_DEFAULT);
                            mediaPlayer.setPlaybackParams(params);

                            d("SeekAndPlay: setPlaybackParams() Sucesss!!");
                        } catch (IllegalStateException exception) {
                            l("SeekAndPlay setPlaybackParams IllegalStateException: " + exception.getLocalizedMessage());
                        } catch (Throwable err) {
                            l("SeekAndPlay setPlaybackParams: " + err.getMessage());
                            //err.printStackTrace();
                        }
                        mediaPlayer.start();
                    }
                }

                d("SeekAndPlay: SeekErr " + seekErr + " SvOff " + mMain.serverTimeOffset +
                        " User " + userTimeOffset + "\nSeekOff " + seekOff +
                        " RTT " + mMain.serverRTT + " Strm" + currentRadioChannel);

                Intent in = new Intent(ACTION_STATS);
                in.putExtra("resultCode", Activity.RESULT_OK);
                in.putExtra("msgType", 1);
                // Put extras into the intent as usual
                in.putExtra("seekErr", seekErr);
                in.putExtra("", currentRadioChannel);
                in.putExtra("userTimeOffset", userTimeOffset);
                in.putExtra("serverTimeOffset", mMain.serverTimeOffset);
                in.putExtra("serverRTT", mMain.serverRTT);
                LocalBroadcastManager.getInstance(mContext).sendBroadcast(in);
            }
        }

    }
    // Main thread to drive the music player
    void musicPlayerThread() {

        String model = android.os.Build.MODEL;

        l("Starting BB on " + mMain.boardId + ", model " + model);

        if (BurnerBoardUtil.kIsRPI) { // Nano should be OK
            phoneModelAudioLatency = 80;
        } else if (model.equals("imx7d_pico")) {
            phoneModelAudioLatency = 110;
        } else {
            phoneModelAudioLatency = 0;
            userTimeOffset = 0;
        }

        boolean hasLowLatencyFeature =
                mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_AUDIO_LOW_LATENCY);

        l("has audio LowLatencyFeature: " + hasLowLatencyFeature);
        boolean hasProFeature =
                mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_AUDIO_PRO);
        l("has audio ProFeature: " + hasProFeature );

        AudioManager am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        String sampleRateStr = am.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE);
        int sampleRate = Integer.parseInt(sampleRateStr);

        l("audio sampleRate: " + sampleRate );

        String framesPerBuffer = am.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER);
        int framesPerBufferInt = Integer.parseInt(framesPerBuffer);

        l("audio framesPerBufferInt: " + framesPerBufferInt );

        dlManager.StartDownloads();

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

    public void setVolume(float vol1, float vol2){
        mediaPlayer.setVolume(vol1, vol2);
    }

    public String getRadioChannelInfo(int index) {
        return dlManager.GetAudioFileLocalName(index - 1);
    }

    public int getCurrentBoardVol() {
        int v = getAndroidVolumePercent();
        //return ((int) ((v/(float)100) * (float) 127.0)); // dkw not sure what 127 is for.
        return (v); // dkw not sure what 127 is for.
    }

    public int getBoardVolumePercent() {
        return getAndroidVolumePercent();
    }

    public void setBoardVolume(int v) {
        setAndroidVolumePercent(v);
    }

    public int getAndroidVolumePercent(){
        // Get the AudioManager
        AudioManager audioManager =
                (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);

        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        vol = ((float)volume / (float)maxVolume);
        int v = (int) ( vol * (float)100);
        return v;
    }

    public void setAndroidVolumePercent(int v){
        // Get the AudioManager
        AudioManager audioManager =
                (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);

        vol = v / (float)100;
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int setVolume = (int)((float) maxVolume * (float) v / (float) 100);

        audioManager.setStreamVolume (
                AudioManager.STREAM_MUSIC,
                setVolume,
                0);
    }


    public static final int kRemoteAudioTrack = 0x01;
    public void onVolUp() {
        vol += 0.01;
        if (vol > 1) vol = 1;
        setVolume(vol, vol);
        l("Volume " + vol * 100.0f + "%");
    }

    public void onVolDown() {
        vol -= 0.01;
        if (vol < 0) vol = 0;
        setVolume(vol, vol);
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
        setVolume(vol, vol);
        l("Volume " + vol * 100.0f + "%");
    }
    // Set radio input mode 0 = bluetooth, 1-n = tracks
    public void SetRadioChannel(int index) {
        l("SetRadioChannel: " + index);
        currentRadioChannel = index;

        // If I am set to be the master, broadcast to other boards
        if (mMain.isMaster() && (mRfClientServer != null)) {

            l("Sending remote");

            String fileName = getRadioChannelInfo(index);
            mRfClientServer.sendRemote(kRemoteAudioTrack, mMain.hashTrackName(fileName), RFClientServer.kRemoteAudio);
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
            if (mMain.HasVoiceAnnouncements()) {
                voice.speak("Blue tooth Audio", TextToSpeech.QUEUE_FLUSH, null, "bluetooth");
            }
            try {
                Thread.sleep(1000);
            } catch (Throwable e) {
            }
            bluetoothModeEnable();

        } else {
            try {
                l("Radio Mode");
                String [] shortName = getRadioChannelInfo(index).split("\\.", 2);
                mBurnerBoard.setText(shortName[0], 2000);
                if (mMain.HasVoiceAnnouncements()) {
                    voice.speak("Track " + index, TextToSpeech.QUEUE_FLUSH, null, "track");
                }
                bluetoothModeDisable();

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
}

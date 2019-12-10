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
import android.media.MediaRecorder;
import android.net.Uri;
import android.speech.tts.TextToSpeech;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SeekParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.analytics.AnalyticsListener;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

public class MusicPlayer {

    private static final String TAG = "MusicPlayer";
    public RFClientServer mRfClientServer = null;
    int currentRadioChannel = 1;
    BoardVisualization mBoardVisualization = null;
    long lastSeekOffset = 0;
    long phoneModelAudioLatency = 0;
    private BBService mMain = null;
    private DownloadManager dlManager = null;
    private int userTimeOffset = 0;
    private Context mContext = null;
    private BurnerBoard mBurnerBoard = null;
    private float vol = 0.80f;
    TextToSpeech voice;
    SimpleExoPlayer player = null;


    MusicPlayer(BBService service, Context context, DownloadManager dlm, BoardVisualization bv,
                RFClientServer rfc,
                BurnerBoard bb,
                TextToSpeech v) {
        dlManager = dlm;
        mMain = service;
        mContext = context;
        mBoardVisualization = bv;
        mRfClientServer = rfc;
        mBurnerBoard = bb;
        voice = v;
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
        if (nextRadioChannel < 0) {
            nextRadioChannel = 0;
        }
        SetRadioChannel(nextRadioChannel);

    }


    void Run() {
        Thread t = new Thread(new Runnable() {
            public void run() {
                Thread.currentThread().setName("BB Music Player");
                musicPlayerThread();
            }
        });
        t.start();
    }


    public void RadioMode() {
        SetRadioChannel(currentRadioChannel);
    }

    public void d(String logMsg) {
        if (DebugConfigs.DEBUG_MUSIC_PLAYER) {
            Log.d(TAG, logMsg);
        }
    }

    public void e(String logMsg) {
        Log.e(TAG, logMsg);
    }

    void MusicOffset(int ms) {
        userTimeOffset += ms;
        SeekAndPlay();
        d("UserTimeOffset = " + userTimeOffset);
    }

    long GetCurrentStreamLengthInSeconds() {
        return dlManager.GetAudioLength(currentRadioChannel - 1);
    }


    public void SeekAndPlay() {
        if (player != null && dlManager.GetTotalAudio() != 0) {
            synchronized (player) {
                long ms = mMain.CurrentClockAdjusted() + userTimeOffset - phoneModelAudioLatency;

                long lenInMS = GetCurrentStreamLengthInSeconds() * 1000;

                long seekOff = ms % lenInMS;
                long curPos = player.getCurrentPosition();
                long seekErr = curPos - seekOff;

                if (curPos == 0 || seekErr != 0) {

                    Float speed = 1.0f + (seekOff - curPos) / 1000.0f;

                    d("SeekAndPlay:curPos = " + curPos + " SeekErr " + seekErr + " SvOff " + mMain.serverTimeOffset +
                            " User " + userTimeOffset + " SeekOff " + seekOff +
                            " RTT " + mMain.serverRTT + " Strm" + currentRadioChannel);

                   if (curPos == 0 || Math.abs(seekErr) > 100) {
                        player.seekTo((int) seekOff + 170);
                    } else {

                        try {
                            PlaybackParameters param = new PlaybackParameters(speed,1);
                            player.setPlaybackParameters(param);

                     } catch (Throwable err) {
                            e("SeekAndPlay Error: " + err.getMessage());
                        }
                    }
                }

                Intent in = new Intent(ACTION.STATS);
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

        d("Starting BB on " + mMain.boardId + ", model " + model);

        if (BurnerBoardUtil.kIsRPI ) { // Nano should be OK
            phoneModelAudioLatency = 80;
        } else if (model.equals("imx7d_pico")) {
            phoneModelAudioLatency = 110;
        } else {
            phoneModelAudioLatency = 0;
            userTimeOffset = 0;
        }

        boolean hasLowLatencyFeature =
                mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_AUDIO_LOW_LATENCY);

        d("has audio LowLatencyFeature: " + hasLowLatencyFeature);
        boolean hasProFeature =
                mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_AUDIO_PRO);
        d("has audio ProFeature: " + hasProFeature);

        AudioManager am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        String sampleRateStr = am.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE);
        int sampleRate = Integer.parseInt(sampleRateStr);

        d("audio sampleRate: " + sampleRate);

        String framesPerBuffer = am.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER);
        int framesPerBufferInt = Integer.parseInt(framesPerBuffer);

        d("audio framesPerBufferInt: " + framesPerBufferInt);

        player = ExoPlayerFactory.newSimpleInstance(mContext);
        player.setSeekParameters(SeekParameters.EXACT);
        player.addAnalyticsListener(new AnalyticsListener() {
            @Override
            public void onSeekProcessed(EventTime eventTime) {
                d("SeekAndPlay: SeekProcessed realtimeMS:" + eventTime.realtimeMs + " currentPlaybackPositionMs:" + eventTime.currentPlaybackPositionMs);
            }
        });
        player.addAnalyticsListener(new AnalyticsListener() {
            @Override
            public void onSeekStarted(EventTime eventTime) {
                d("SeekAndPlay: SeekStarted realtimeMS:" + eventTime.realtimeMs);
            }
        });
        player.addAnalyticsListener(new AnalyticsListener() {
            @Override
            public void onPlaybackParametersChanged(EventTime eventTime, PlaybackParameters playbackParameters) {
                d("SeekAndPlay: Playback parameters change speed: " + playbackParameters.speed + " pitch: " + playbackParameters.pitch);
            }
        });

        try {
            Thread.sleep(5000);
        } catch (Exception e) {

        }

        bluetoothModeInit();

        int musicState = 0;

        while (true) {

            switch (musicState) {
                case 0:
                    if (dlManager.GetTotalAudio() != 0) {
                        musicState = 1;
                        d("Downloaded: Starting Radio Mode");
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
                    if (currentRadioChannel == 0) {
                        musicState = 2;
                    }
                    break;

                case 2:
                    bluetoothPlay();
                    if (currentRadioChannel != 0) {
                        musicState = 1;
                    }
                    break;

                default:
                    break;
            }
        }
    }

    public void setVolume(float vol1, float vol2) {
        synchronized (player){
            player.setVolume(vol1);
        }
    }

    public String getRadioChannelInfo(int index) {
        return dlManager.GetAudioFileLocalName(index - 1);
    }

    public int getCurrentBoardVol() {
        int v = getAndroidVolumePercent();
        return (v);
    }

    public int getBoardVolumePercent() {
        return getAndroidVolumePercent();
    }

    public void setBoardVolume(int v) {
        setAndroidVolumePercent(v);
    }

    public int getAndroidVolumePercent() {
        // Get the AudioManager
        AudioManager audioManager =
                (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);

        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        vol = ((float) volume / (float) maxVolume);
        int v = (int) (vol * (float) 100);
        return v;
    }

    public void setAndroidVolumePercent(int v) {
        // Get the AudioManager
        AudioManager audioManager =
                (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);

        vol = v / (float) 100;
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int setVolume = (int) ((float) maxVolume * (float) v / (float) 100);

        audioManager.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                setVolume,
                0);
    }


    public static final int kRemoteAudioTrack = 0x01;

    public void onVolUp() {
        vol += 0.01;
        if (vol > 1) vol = 1;
        setVolume(vol, vol);
        d("Volume " + vol * 100.0f + "%");
    }

    public void onVolDown() {
        vol -= 0.01;
        if (vol < 0) vol = 0;
        setVolume(vol, vol);
        d("Volume " + vol * 100.0f + "%");
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
        d("Volume " + vol * 100.0f + "%");
    }

    // Set radio input mode 0 = bluetooth, 1-n = tracks
    public void SetRadioChannel(int index) {
        d("SetRadioChannel: " + index);
        currentRadioChannel = index;

        // If I am set to be the master, broadcast to other boards
        if (mMain.isMaster() && (mRfClientServer != null)) {

            d("Sending remote");

            String fileName = getRadioChannelInfo(index);
            mRfClientServer.sendRemote(kRemoteAudioTrack, mMain.hashTrackName(fileName), RFClientServer.kRemoteAudio);
            // Wait for 1/2 RTT so that we all select the same track/video at the same time
            try {
                Thread.sleep(mRfClientServer.getLatency());
            } catch (Exception e) {
            }
        }

        if (index == 0) {
            d("Bluetooth Mode");
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
                d("Radio Mode");
                String[] shortName = getRadioChannelInfo(index).split("\\.", 2);
                mBurnerBoard.setText(shortName[0], 2000);
                if (mMain.HasVoiceAnnouncements()) {
                    voice.speak("Track " + index, TextToSpeech.QUEUE_FLUSH, null, "track");
                }
                bluetoothModeDisable();

                if (player != null && dlManager.GetTotalAudio() != 0) {
                    synchronized (player) {
                        lastSeekOffset = 0;

                        d("playing file " + dlManager.GetAudioFile(index - 1));

                        // Produces DataSource instances through which media data is loaded.
                        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(mContext,
                                Util.getUserAgent(mContext, "yourApplicationName"));

                        String filePath = dlManager.GetAudioFile(index - 1);
                        Uri uri = Uri.parse("file:///" + filePath);

                        // This is the MediaSource representing the media to be played.
                        MediaSource audioSource = new ExtractorMediaSource.Factory(dataSourceFactory)
                                .createMediaSource(uri);

                        player.prepare(audioSource, false, false);
                        player.setPlayWhenReady(true);
                        player.setRepeatMode(Player.REPEAT_MODE_ALL);
                        player.setVolume(1);
                        mBoardVisualization.attachAudio(player.getAudioSessionId());
                    }
                }
                SeekAndPlay();
            } catch (Throwable err) {
                e("Radio mode failed" + err.getMessage());
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
            d("Audio device" + deviceList[index].toString());
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

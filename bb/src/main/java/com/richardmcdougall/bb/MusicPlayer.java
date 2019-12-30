package com.richardmcdougall.bb;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.Uri;
import android.speech.tts.TextToSpeech;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.exoplayer2.C;
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
import com.google.android.exoplayer2.audio.AudioAttributes;

import android.os.*;

public class MusicPlayer implements Runnable {

    private static final String TAG = "MusicPlayer";
    private Handler handler;
    private long lastSeekOffset = 0;
    private long phoneModelAudioLatency = 0;
    private SimpleExoPlayer player = null;
    private float recallVol = 0;
    private BBService service = null;
    private int userTimeOffset = 0;
    private float vol = 0.80f;
    private int nextRadioChannel;

    MusicPlayer(BBService service) {
        this.service = service;

        String model = android.os.Build.MODEL;

        if (BoardState.kIsRPI) { // Nano should be OK
            phoneModelAudioLatency = 80;
        } else if (model.equals("imx7d_pico")) {
            phoneModelAudioLatency = 110;
        } else {
            phoneModelAudioLatency = 0;
            userTimeOffset = 0;
        }

        boolean hasLowLatencyFeature =
                this.service.context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_AUDIO_LOW_LATENCY);

        d("has audio LowLatencyFeature: " + hasLowLatencyFeature);
        boolean hasProFeature =
                this.service.context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_AUDIO_PRO);
        d("has audio ProFeature: " + hasProFeature);

        AudioManager am = (AudioManager) this.service.context.getSystemService(Context.AUDIO_SERVICE);
        String sampleRateStr = am.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE);
        int sampleRate = Integer.parseInt(sampleRateStr);

        d("audio sampleRate: " + sampleRate);

        String framesPerBuffer = am.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER);
        int framesPerBufferInt = Integer.parseInt(framesPerBuffer);

        d("audio framesPerBufferInt: " + framesPerBufferInt);

    }

    public void run() {

        Looper.prepare();
        handler = new Handler(Looper.myLooper());

        player = ExoPlayerFactory.newSimpleInstance(service.context);
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

        Looper.loop();
    }

    public void NextStream() {
        nextRadioChannel = service.boardState.currentRadioChannel + 1;
        if (nextRadioChannel > service.mediaManager.GetTotalAudio())
            nextRadioChannel = 0;

        this.handler.post(() -> mSetRadioChannel(nextRadioChannel) );
    }

    public void PreviousStream() {
        nextRadioChannel = service.boardState.currentRadioChannel - 1;
        if (nextRadioChannel < 0) {
            nextRadioChannel = 0;
        }
        this.handler.post(() -> mSetRadioChannel(nextRadioChannel) );
    }

    public void RadioMode() {
        this.handler.post(() -> mSetRadioChannel(service.boardState.currentRadioChannel) );
    }

    private void d(String logMsg) {
        if (DebugConfigs.DEBUG_MUSIC_PLAYER) {
            Log.d(TAG, logMsg);
        }
    }

    private void e(String logMsg) {
        Log.e(TAG, logMsg);
    }

   public void MusicOffset(int ms) {
        userTimeOffset += ms;
        this.handler.post(() -> mSeekAndPlay() );
        d("UserTimeOffset = " + userTimeOffset);
    }

    private long GetCurrentStreamLengthInSeconds() {
        return service.mediaManager.GetAudioLength(service.boardState.currentRadioChannel - 1);
    }

    public void SeekAndPlay() {
        this.handler.post(() -> mSeekAndPlay());
    }

    private void mSeekAndPlay() {
        if (player != null && service.mediaManager.GetTotalAudio() != 0) {

            long ms = service.CurrentClockAdjusted() + userTimeOffset - phoneModelAudioLatency;

            long lenInMS = GetCurrentStreamLengthInSeconds() * 1000;

            long seekOff = ms % lenInMS;
            long curPos = player.getCurrentPosition();
            long seekErr = curPos - seekOff;

            Float speed = 1.0f + (seekOff - curPos) / 1000.0f;

            d("SeekAndPlay:curPos = " + curPos + " SeekErr " + seekErr + " SvOff " + service.serverTimeOffset +
                    " User " + userTimeOffset + " SeekOff " + seekOff +
                    " RTT " + service.serverRTT + " Strm" + service.boardState.currentRadioChannel);

            if (curPos == 0 || Math.abs(seekErr) > 100) {
                player.seekTo((int) seekOff + 170);
            } else {
                try {
                    PlaybackParameters param = new PlaybackParameters(speed, 1);
                    player.setPlaybackParameters(param);

                } catch (Throwable err) {
                    e("SeekAndPlay Error: " + err.getMessage());
                }
            }

            Intent in = new Intent(ACTION.STATS);
            in.putExtra("resultCode", Activity.RESULT_OK);
            in.putExtra("msgType", 1);
            // Put extras into the intent as usual
            in.putExtra("seekErr", seekErr);
            in.putExtra("", service.boardState.currentRadioChannel);
            in.putExtra("userTimeOffset", userTimeOffset);
            in.putExtra("serverTimeOffset", service.serverTimeOffset);
            in.putExtra("serverRTT", service.serverRTT);
            LocalBroadcastManager.getInstance(service.context).sendBroadcast(in);
        }
    }

    public void setVolume(float vol1, float vol2) {
        player.setVolume(vol1);
    }

    public String getRadioChannelInfo(int index) {
        return service.mediaManager.GetAudioFileLocalName(index - 1);
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
        AudioManager audioManager =
                (AudioManager) service.context.getSystemService(Context.AUDIO_SERVICE);

        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        vol = ((float) volume / (float) maxVolume);
        int v = (int) (vol * (float) 100);
        return v;
    }

    public void setAndroidVolumePercent(int v) {
        AudioManager audioManager =
                (AudioManager) service.context.getSystemService(Context.AUDIO_SERVICE);

        vol = v / (float) 100;
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int setVolume = (int) ((float) maxVolume * (float) v / (float) 100);

        audioManager.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                setVolume,
                0);
    }

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

    public void SetRadioChannel(int index) {
        this.handler.post(() -> mSetRadioChannel(index));
    }

    // Set radio input mode 0 = bluetooth, 1-n = tracks
    private void mSetRadioChannel(int index) {
        d("SetRadioChannel: " + index);
        service.boardState.currentRadioChannel = index;

        // If I am set to be the master, broadcast to other boards
        if (service.boardState.masterRemote && (service.rfClientServer != null)) {

            d("Sending remote");

            String fileName = getRadioChannelInfo(index);
            service.rfClientServer.sendRemote(RFUtil.REMOTE_AUDIO_TRACK_CODE, BurnerBoardUtil.hashTrackName(fileName), RFClientServer.kRemoteAudio);
            // Wait for 1/2 RTT so that we all select the same track/video at the same time
            try {
                Thread.sleep(service.rfClientServer.getLatency());
            } catch (Exception e) {
            }
        }

        try {
            d("Radio Mode");
            String[] shortName = getRadioChannelInfo(index).split("\\.", 2);
            service.burnerBoard.setText(shortName[0], 2000);
            if (service.voiceAnnouncements) {
                service.voice.speak("Track " + index, TextToSpeech.QUEUE_FLUSH, null, "track");
            }

            if (player != null && service.mediaManager.GetTotalAudio() != 0) {

                lastSeekOffset = 0;

                d("playing file " + service.mediaManager.GetAudioFile(index - 1));

                // Produces DataSource instances through which media data is loaded.
                DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(service.context,
                        Util.getUserAgent(service.context, "yourApplicationName"));

                String filePath = service.mediaManager.GetAudioFile(index - 1);
                Uri uri = Uri.parse("file:///" + filePath);

                // This is the MediaSource representing the media to be played.
                MediaSource audioSource = new ExtractorMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(uri);

                AudioAttributes a = player.getAudioAttributes();

                AudioAttributes.Builder b = new AudioAttributes.Builder();
                b.setContentType(C.CONTENT_TYPE_MUSIC);
                b.setUsage(C.USAGE_MEDIA);
                player.setAudioAttributes(b.build());

                player.prepare(audioSource, false, false);
                player.setPlayWhenReady(true);
                player.setRepeatMode(Player.REPEAT_MODE_ALL);
                player.setVolume(1);

                service.boardVisualization.attachAudio(player.getAudioSessionId());
            }

            this.handler.post(() -> mSeekAndPlay() );
        } catch (Throwable err) {
            e("Radio mode failed" + err.getMessage());
        }

    }
}

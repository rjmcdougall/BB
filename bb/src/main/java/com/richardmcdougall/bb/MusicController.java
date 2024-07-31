package com.richardmcdougall.bb;

import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SeekParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.analytics.AnalyticsListener;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.richardmcdougall.bb.visualization.RGBList;
import com.richardmcdougall.bbcommon.BLog;

public class MusicController implements Runnable {
    private String TAG = this.getClass().getSimpleName();

    private Handler handler;
    private long phoneModelAudioLatency = 0;
    private SimpleExoPlayer player = null;
    private BBService service = null;
    private int nextRadioChannel;
    private boolean isMuted = false;

    MusicController(BBService service) {
        this.service = service;

        String model = android.os.Build.MODEL;

        switch (service.boardState.platformType) {
            case npi:
                phoneModelAudioLatency = 50;
                break;
            case rpi:
                phoneModelAudioLatency = 80;
                break;
            case dragonboard:
                phoneModelAudioLatency = 0;
                break;
        }

        boolean hasLowLatencyFeature =
                this.service.context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_AUDIO_LOW_LATENCY);

        BLog.d(TAG, "has audio LowLatencyFeature: " + hasLowLatencyFeature);
        boolean hasProFeature =
                this.service.context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_AUDIO_PRO);
        BLog.d(TAG, "has audio ProFeature: " + hasProFeature);

        AudioManager am = (AudioManager) this.service.context.getSystemService(Context.AUDIO_SERVICE);
        String sampleRateStr = am.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE);
        int sampleRate = Integer.parseInt(sampleRateStr);

        BLog.d(TAG, "audio sampleRate: " + sampleRate);

        String framesPerBuffer = am.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER);
        int framesPerBufferInt = Integer.parseInt(framesPerBuffer);

        BLog.d(TAG, "audio framesPerBufferInt: " + framesPerBufferInt);

    }

    public void run() {

        Looper.prepare();
        handler = new Handler(Looper.myLooper());

        CreateExoplayer();

        try {
            Looper.loop();
        } catch (Exception e) {

        }
    }

    private void CreateExoplayer() {
        player = ExoPlayerFactory.newSimpleInstance(service.context);
        player.setSeekParameters(SeekParameters.EXACT);

        player.addAnalyticsListener(new AnalyticsListener() {
            @Override
            public void onSeekProcessed(EventTime eventTime) {
                BLog.d(TAG, "SeekAndPlay: SeekProcessed realtimeMS:" + eventTime.realtimeMs + " currentPlaybackPositionMs:" + eventTime.currentPlaybackPositionMs);
            }
        });
        player.addAnalyticsListener(new AnalyticsListener() {
            @Override
            public void onSeekStarted(EventTime eventTime) {
                BLog.d(TAG, "SeekAndPlay: SeekStarted realtimeMS:" + eventTime.realtimeMs);
            }
        });
        player.addAnalyticsListener(new AnalyticsListener() {
            @Override
            public void onPlaybackParametersChanged(EventTime eventTime, PlaybackParameters playbackParameters) {
                BLog.d(TAG, "SeekAndPlay: Playback parameters change speed: " + playbackParameters.speed + " pitch: " + playbackParameters.pitch);
            }
        });

        BLog.d(TAG, "playing file " + service.mediaManager.GetAudioFile());

        // Produces DataSource instances through which media data is loaded.
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(service.context,
                Util.getUserAgent(service.context, "yourApplicationName"));

        String filePath = service.mediaManager.GetAudioFile();
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
    }

    public void NextStream() {
        nextRadioChannel = service.boardState.currentRadioChannel + 1;
        if (nextRadioChannel >= service.mediaManager.GetTotalAudio())
            nextRadioChannel = 0;

        try {
            this.handler.post(() -> mSetRadioChannel(nextRadioChannel));
        } catch (Exception e) {

        }
    }

    public void PreviousStream() {
        nextRadioChannel = service.boardState.currentRadioChannel - 1;
        if (nextRadioChannel < 0) {
            nextRadioChannel = service.mediaManager.GetTotalAudio() -1;
        }
        try {
            this.handler.post(() -> mSetRadioChannel(nextRadioChannel));
        } catch (Exception e) {

        }
    }

    public void RadioMode() {
        try {
            this.handler.post(() -> mSetRadioChannel(service.boardState.currentRadioChannel));
        } catch (Exception e) {

        }
    }

    private long GetCurrentStreamLengthInSeconds() {
        return service.mediaManager.GetAudioLength();
    }

    public void SeekAndPlay() {
        this.handler.post(() -> mSeekAndPlay());
    }

    private void mSeekAndPlay() {
        if (player != null && service.mediaManager.GetTotalAudio() != 0) {

            long ms = TimeSync.CurrentClockAdjusted() - phoneModelAudioLatency;

            long lenInMS = GetCurrentStreamLengthInSeconds() * 1000;

            long seekOff = ms % lenInMS;
            long curPos = player.getCurrentPosition();
            long seekErr = curPos - seekOff;

            Float speed = 1.0f + (seekOff - curPos) / 1000.0f;

            BLog.d(TAG, "SeekAndPlay:curPos = " + curPos + " SeekErr " + seekErr + " SvOff " +
                    TimeSync.getServerClockOffset() + " SeekOff " + seekOff + " RTT " +
                    TimeSync.getServerRoundTripTime() + " Strm" +
                    service.boardState.currentRadioChannel +
                    " Current Clock Adjusted: " + TimeSync.CurrentClockAdjusted());

            if (curPos == 0 || Math.abs(seekErr) > 100) {
                player.seekTo((int) seekOff + 170);
            } else {
                try {
                    PlaybackParameters param = new PlaybackParameters(speed, 1);
                    player.setPlaybackParameters(param);

                } catch (Throwable err) {
                    BLog.e(TAG, "SeekAndPlay Error: " + err.getMessage());
                }
            }
        }
    }

    public String getRadioChannelInfo(int index) {
        return service.mediaManager.GetAudioFileLocalName(index);
    }

    public int getCurrentBoardVol() {
        int v = getAndroidVolumePercent();
        return (v);
    }

    public void setBoardVolume(int v) {
        if (v >= 0 && v <= 100) {
            setAndroidVolumePercent(v);
            // Also set the volume in the visulizer to scale FFT
            service.audioVisualizer.setVolume(v);
        } else {
            BLog.e(TAG, "Invalid Volume Percent: " + v);
        }
    }

    public int getAndroidVolumePercent() {
        AudioManager audioManager =
                (AudioManager) service.context.getSystemService(Context.AUDIO_SERVICE);

        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        float vol = ((float) volume / (float) maxVolume);
        int v = (int) (vol * (float) 100);
        return v;
    }

    public void Mute() {
        handler.post(() -> player.setVolume(0f));
        isMuted = true;
    }

    public void Unmute() {
        handler.post(() -> player.setVolume(1f));
        isMuted = false;
    }

    public void setAndroidVolumePercent(int v) {
        AudioManager audioManager =
                (AudioManager) service.context.getSystemService(Context.AUDIO_SERVICE);

        float vol = v / (float) 100;
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int setVolume = (int) ((float) maxVolume * (float) v / (float) 100);

        audioManager.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                setVolume,
                0);

         if(service.boardState.masterRemote)
             service.masterController.SendVolume();
     }

    public void SetRadioChannel(int index) {
        this.handler.post(() -> mSetRadioChannel(index));
    }

    // Set radio input mode 0 = bluetooth, 1-n = tracks
    private void mSetRadioChannel(int index) {
        BLog.d(TAG, "SetRadioChannel: " + index);
        service.boardState.currentRadioChannel = index;

        try {
            // If I am set to be the master, broadcast to other boards
            if (service.boardState.masterRemote && (service.rfClientServer != null)) {

                BLog.d(TAG, "Sending remote");

                String fileName = getRadioChannelInfo(index);
                if (service.boardState.masterRemote)
                    service.masterController.SendAudio();

            }

            BLog.d(TAG, "Radio Mode");
            String[] shortName = getRadioChannelInfo(index).split("\\.", 2);
            service.burnerBoard.textBuilder.setText(shortName[0], 2000, service.burnerBoard.getFrameRate(), new RGBList().getColor("white"));

            if (player != null && service.mediaManager.GetTotalAudio() != 0) {

                player.release();
                player = null;
                CreateExoplayer();

                service.audioVisualizer.attachAudio(player.getAudioSessionId());
            }

            this.handler.post(() -> mSeekAndPlay());
        } catch (Throwable err) {
            BLog.e(TAG, "Radio mode failed " + err.getMessage());
        }

    }
}
package com.richardmcdougall.bb;


import android.media.audiofx.Visualizer;
import android.media.audiofx.Equalizer;

import com.richardmcdougall.bbcommon.BLog;

public class AudioVisualizer {
    private String TAG = this.getClass().getSimpleName();
    private BBService service;
    public byte[] mBoardFFT;
    private Visualizer mVisualizer;
    private int mAudioSessionId;
    private int[] oldLevels = new int[kNumLevels];

    private static final int kNumLevels = 8;

    private int musicVolume = 100;

    AudioVisualizer(BBService service) {

        this.service = service;
        BLog.d(TAG, "Starting AudioVisualizer");
    }

    public void attachAudio(int audioSessionId) {
        int vSize;

        BLog.d(TAG, "enabling audio Visualizer on session=" + audioSessionId);
        mAudioSessionId = audioSessionId;

        musicVolume = service.musicController.getAndroidVolumePercent();

        try {
            if (mVisualizer != null) {
                mVisualizer.release();
            }
            mVisualizer = new Visualizer(audioSessionId);

            synchronized (mVisualizer) {
                // Create the Visualizer object and attach it to our media player.
                vSize = Visualizer.getCaptureSizeRange()[1];
                mVisualizer.setEnabled(false);
                mBoardFFT = new byte[vSize];
                mVisualizer.setCaptureSize(vSize);
                mVisualizer.setScalingMode(Visualizer.SCALING_MODE_NORMALIZED);
                mVisualizer.setEnabled(true);
            }
        } catch (Exception e) {
            BLog.e(TAG, "Error enabling audio visualizer, likely app is asking for permissions: " + e.getMessage());
            return;
        }
        BLog.d(TAG, "Enabled audio visualizer FFT with " + vSize + " bytes");
        BLog.d(TAG, "Audio FFT enabled with sampling rate " + mVisualizer.getSamplingRate());
    }

    public void setVolume(int percent) {
        musicVolume = percent;
        BLog.d(TAG, "Multiplier = " + volumeMultiplier());
    }

    // It appears SCALING_MODE_NORMALIZED is broken, so here we do manual compensation
    private float volumeMultiplier() {
        return (1.0f + ((100.0f / (Math.max((float)musicVolume, 10)) - 1.0f) * 0.2f));
    }

    // Get levels from Android visualizer engine
    // 16 int buckets of frequency levels from low to high
    // Range is 0-255 per bucket
    public int[] getLevels() {
        if (mBoardFFT == null)
            return null;
        synchronized (mVisualizer) {
            try {
                if (mVisualizer.getFft(mBoardFFT) != mVisualizer.SUCCESS)
                    return null;
            } catch (Exception e) {
                return null;
            }

            int[] dbLevels = new int[16];
            byte rfk;
            byte ifk;
            int dbValue = 0;
            // There are 1024 values - 512 x real, imaginary
            for (int i = 0; i < 512; i += 8) {
                if ((i % 32) == 0) {
                    dbValue = 0;
                }
                rfk = mBoardFFT[i];
                ifk = mBoardFFT[i + 1];
                float magnitude = (rfk * rfk + ifk * ifk);
                dbValue += java.lang.Math.max(0, 15 + 15 * (Math.log10(magnitude) - 1));
                if (dbValue < 0)
                    dbValue = 0;
                dbLevels[i / 32] += dbValue;
                dbLevels[i / 32] = java.lang.Math.min((int)(volumeMultiplier() * (float)dbLevels[i / 32]), 255);

            }
            return dbLevels;
        }
    }
    public int[] getLevels128() {
        if (mBoardFFT == null)
            return null;
        synchronized (mVisualizer) {
            try {
                if (mVisualizer.getFft(mBoardFFT) != mVisualizer.SUCCESS)
                    return null;

            } catch (Exception e) {
                return null;
            }
            // Only use the lower half (0-10khz)
            int n = mBoardFFT.length / 2;
            //BLog.d(TAG, "fft levels: " + n);
            int divider = n / 128;


            int[] dbLevels = new int[128];
            byte rfk;
            byte ifk;
            int dbValue = 0;
            // There are 1024 values - 512 x real, imaginary
            for (int i = 0; i < n; i += 2) {
                if ((i % divider) == 0) {
                    dbValue = 0;
                }
                // Skip the first 8 low frequency bins
                rfk = mBoardFFT[16 + i];
                ifk = mBoardFFT[16 + i + 1];
                float magnitude = ((float)rfk * (float)rfk + (float)ifk * (float)ifk);

                dbValue = (int)java.lang.Math.max(0,   30 * (Math.log10(magnitude)));
                if (dbValue < 0)
                    dbValue = 0;
                dbLevels[i / divider] += dbValue;
                dbLevels[i / divider] = java.lang.Math.min((int)(volumeMultiplier() * (float)dbLevels[i / divider]), 255);
                //BLog.d(TAG, "bin: " + i + ", magnitude: " + magnitude + ", level: " +
                //        (float)Math.log10(magnitude) + ", dbvalue: " + dbValue + ", dbout = " +  dbLevels[i / divider]);

            }
            return dbLevels;
        }
    }

    // Get levels from Android visualizer engine
    // 128 int buckets of frequency levels from low to high
    // Range is 0-255 per bucket
    public int[] getLevels128a() {
        if (mBoardFFT == null)
            return null;
        synchronized (mVisualizer) {
            try {
                if (mVisualizer.getFft(mBoardFFT) != mVisualizer.SUCCESS)
                    return null;
            } catch (Exception e) {
                return null;
            }

            int n = mBoardFFT.length;
            float[] magnitudes = new float[n / 2 + 1];
            float[] phases = new float[n / 2 + 1];
            magnitudes[0] = (float)Math.abs(mBoardFFT[0]);      // DC
            magnitudes[n / 2] = (float)Math.abs(mBoardFFT[1]);  // Nyquist
            phases[0] = phases[n / 2] = 0;
            for (int k = 1; k < n / 2; k++) {
                int i = k * 2;
                magnitudes[k] = (float)Math.hypot(mBoardFFT[i], mBoardFFT[i + 1]);
                phases[k] = (float)Math.atan2(mBoardFFT[i + 1], mBoardFFT[i]);
            }

            int[] dbLevels = new int[128];
            int fftLevels = (n / 2);
            int divider = fftLevels / 128;

            int dbValue = 0;
            for (int i = 1; i < fftLevels; i ++) {
                if ((i % divider) == 0) {
                    dbValue = 0;
                }
                dbValue = (int) Math.max(0, 200 + 200    * (Math.log10(magnitudes[i]) - 1));
                if (dbValue < 0)
                    dbValue = 0;
                dbLevels[i / divider] = dbValue;
                dbLevels[i / divider] = java.lang.Math.min(dbLevels[i  / divider], 255);
            }
            return dbLevels;
        }
    }

    public int getLevel() {

        int level = 0;
        int delta;
        int[] dbLevels = getLevels();
        if (dbLevels == null)
            return (0);
        for (int i = 0; i < kNumLevels; i++) {
            if ((delta = dbLevels[i] - oldLevels[i]) > 0) {
                level += delta;
            }
        }
        System.arraycopy(dbLevels, 0, oldLevels, 0, kNumLevels);
        level = (int) (25.01 * java.lang.Math.log((float) (level + 1)));
        //System.out.println("level: " + level);
        return level;
    }


}

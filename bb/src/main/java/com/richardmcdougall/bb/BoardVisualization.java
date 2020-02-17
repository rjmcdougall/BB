package com.richardmcdougall.bb;

import android.media.audiofx.Visualizer;

import com.richardmcdougall.bb.visualization.AudioBar;
import com.richardmcdougall.bb.visualization.AudioCenter;
import com.richardmcdougall.bb.visualization.AudioTile;
import com.richardmcdougall.bb.visualization.JosPack;
import com.richardmcdougall.bb.visualization.Matrix;
import com.richardmcdougall.bb.visualization.PlayaMap;
import com.richardmcdougall.bb.visualization.SyncLights;
import com.richardmcdougall.bb.visualization.TestColors;
import com.richardmcdougall.bb.video.Video;
import com.richardmcdougall.bb.visualization.Visualization;
import com.richardmcdougall.bbcommon.BLog;
import com.richardmcdougall.bbcommon.BoardState;

import org.json.JSONObject;

import java.util.Random;

/*

   Back
         width,height
 +------+
 |      |
 |      |
 |      |
 |      |
 |      |
 |      | Y
 |      |
 |      |
 |      |
 |      |
 |      |
 |      |
 |      |
 |      |
 |      |
 |      |
 +------+
 0,0 X
 Front

 */

public class BoardVisualization {
    private static final int kNumLevels = 8;
    public Random mRandom = new Random();
    public byte[] mBoardFFT;
    public int mMultipler4Speed;
    public Visualization mVisualizationMatrix;
    public Visualization mVisualizationTextColors;
    public Visualization mVisualizationAudioTile;
    public Visualization mVisualizationAudioCenter;
    public Visualization mVisualizationVideo;
    public Visualization mVisualizationJosPack;
    public Visualization mVisualizationAudioBar;
    public Visualization mVisualizationPlayaMap;
    public Visualization getmVisualizationSyncLights;

    private int frameCnt = 0;
    private String TAG = this.getClass().getSimpleName();
    private BBService service;
    private int mBoardWidth;
    private int mBoardHeight;
    private int[] mBoardScreen;
    public boolean inhibitVisual = false;
    public boolean inhibitVisualGTFO = false;
    public boolean lowBatteryVisual = false;
    private boolean showBattery = false;
    private int mFrameRate;
    private int batteryCnt = 0;
    private int boardDisplayCnt = 0;
    private Visualizer mVisualizer;
    private int mAudioSessionId;
    private int[] oldLevels = new int[kNumLevels];
    private int showingMap = 0;

    BoardVisualization(BBService service) {

        this.service = service;
        BLog.d(TAG, "Starting Board Visualization " + service.burnerBoard.boardType + " on " + service.boardState.BOARD_ID);

        mBoardWidth = service.burnerBoard.boardWidth;
        mBoardHeight = service.burnerBoard.boardHeight;
        mMultipler4Speed = service.burnerBoard.getMultiplier4Speed();
        mBoardScreen = service.burnerBoard.getPixelBuffer();
        mFrameRate = service.burnerBoard.getFrameRate();

        BLog.d(TAG, "Board framerate set to " + mFrameRate);

        mVisualizationMatrix = new Matrix(service);
        mVisualizationTextColors = new TestColors(service);
        mVisualizationAudioTile = new AudioTile(service);
        mVisualizationAudioCenter = new AudioCenter(service);
        mVisualizationVideo = new Video(service);
        mVisualizationJosPack = new JosPack(service);
        mVisualizationAudioBar = new AudioBar(service);
        mVisualizationPlayaMap = new PlayaMap(service);
        getmVisualizationSyncLights = new SyncLights(service);

    }

    void Run() {
        Thread t = new Thread(() -> {
            Thread.currentThread().setName("BB Board Display");
            boardDisplayThread();
        });
        t.start();
    }

    public void showBattery(boolean show) {
        showBattery = show;
    }    // For reasons I don't understand, VideoMode() = 0 doesn't have a profile associated with it.
    // VideoMode() = 1 sets it to the beginning of the profile.
    void NextVideo() {
        int next = service.boardState.currentVideoMode + 1;
        if (next >= service.mediaManager.GetTotalVideo()) {
            next = 0;
        }
        BLog.d(TAG, "Setting Video to: " + service.mediaManager.GetVideoFileLocalName(next));
        service.boardVisualization.setMode(next);
    }

    public void attachAudio(int audioSessionId) {
        int vSize;

        BLog.d(TAG, "session=" + audioSessionId);
        mAudioSessionId = audioSessionId;

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
                mVisualizer.setEnabled(true);
            }
        } catch (Exception e) {
            BLog.e(TAG, "Error enabling visualizer: " + e.getMessage());
            return;
        }
        BLog.d(TAG, "Enabled visualizer with " + vSize + " bytes");
    }

    public int displayAlgorithm(String algorithm) {
        int frameRate = 1;

        //This runs every time we update the display, so only uncomment in severe debug need!
        //Log.d(TAG, "Using algorithm visualization: " + algorithm);

        switch (algorithm) {

            case "modeMatrix(kMatrixBurnerColor)":
                frameRate = mFrameRate;
                mVisualizationMatrix.update(Matrix.kMatrixBurnerColor);
                break;

            case "modeMatrix(kMatrixLunarian)":
                frameRate = mFrameRate;
                mVisualizationMatrix.update(Matrix.kMatrixLunarian);
                break;

            case "modeAudioCenter()":
                frameRate = mFrameRate;
                mVisualizationAudioCenter.update(Visualization.kDefault);
                break;

            case "modeAudioBarV()":
                frameRate = mFrameRate;
                mVisualizationAudioBar.update(Matrix.kDefault);
                break;

            case "modeMatrix(kMatrixSync)":
                frameRate = mFrameRate;
                mVisualizationMatrix.update(Matrix.kMatrixSync);
                break;

            case "modeAudioTile()":
                frameRate = mFrameRate;
                mVisualizationAudioTile.update(Matrix.kDefault);
                break;

            // JosPack visualizations go here
            case "JPGold()":
                frameRate = mFrameRate;
                mVisualizationJosPack.update(JosPack.kJPGold);
                break;

            case "JPSparkle()":
                frameRate = mFrameRate;
                mVisualizationJosPack.update(JosPack.kJPSparkle);
                break;

            case "JPColors()":
                frameRate = mFrameRate;
                mVisualizationJosPack.update(JosPack.kJPColors);
                break;

            case "JPBlueGold()":
                frameRate = mFrameRate;
                mVisualizationJosPack.update(JosPack.kJPBlueGold);
                break;

            case "JPBlank()":
                frameRate = mFrameRate;
                mVisualizationJosPack.update(JosPack.kJPBlank);
                break;

            case "JPBluePurple()":
                frameRate = mFrameRate;
                mVisualizationJosPack.update(JosPack.kJPBluePurple);
                break;

            case "JPTriColor()":
                frameRate = mFrameRate;
                mVisualizationJosPack.update(JosPack.kJPTriColor);
                break;

            // End JosPack visualizations

            case "modePlayaMap()":
                frameRate = mFrameRate;
                mVisualizationPlayaMap.update(Visualization.kDefault);
                break;

            case "modeSyncLights()":
                frameRate = mFrameRate;
                getmVisualizationSyncLights.update(Visualization.kDefault);
                break;

            default:
                 break;

        }
        return frameRate;
    }

// Main thread to drive the Board's display & get status (mode, voltage,...)
    void boardDisplayThread() {

        long lastFrameTime = System.currentTimeMillis();
        int frameRate = 11;

        BLog.d(TAG, "Starting board display thread...");

        while (true) {

            // Power saving when board top not turned on
            if (inhibitVisual || inhibitVisualGTFO) {
                BLog.d(TAG, "inhibit");
                service.burnerBoard.clearPixels();
                service.burnerBoard.showBattery();
                service.burnerBoard.flush();
                try {
                    Thread.sleep(1000);
                } catch (Throwable er) {
                }
                continue;
            }

            if (lowBatteryVisual) {
                BLog.d(TAG, "inhibit");
                service.burnerBoard.clearPixels();
                service.burnerBoard.fillScreen(255, 0, 0);
                service.burnerBoard.showBattery();
                service.burnerBoard.flush();
                try {
                    Thread.sleep(1000);
                } catch (Throwable er) {
                }
                continue;
            }

            if (showBattery) {
                if (batteryCnt % mFrameRate == 0) {
                    service.burnerBoard.showBattery();
                }
                batteryCnt++;
            }

            if (showingMap > 0) {
                mVisualizationPlayaMap.update(Visualization.kDefault);
                showingMap -= 1;
                continue;
            }

            if (service.remoteCrisisController.boardInCrisisPhase == 1
            || service.localCrisisController.boardInCrisisPhase == 1) {

                service.burnerBoard.clearPixels();
                service.burnerBoard.fillScreen(255, 0, 0);

                service.burnerBoard.flush();
                try {
                } catch (Throwable er) {
                }
                continue;
            }
            
            frameRate = runVisualization(service.boardState.currentVideoMode);

            long frameTime = 1000 / frameRate;
            long curFrameTime = System.currentTimeMillis();
            long thisFrame = (curFrameTime - lastFrameTime);

            if (thisFrame < frameTime) {
                try {
                    Thread.sleep(frameTime - thisFrame);
                } catch (Throwable er) {
                    BLog.e(TAG, er.getMessage());
                }
            }

            lastFrameTime = System.currentTimeMillis();

            boardDisplayCnt++;
        }

    }

    int runVisualization(int mode) {

         try {
            frameCnt++;
            if (frameCnt % 100 == 0) {
                BLog.d(TAG, "Frames: " + frameCnt);
            }

            if (mode < 0) {
                return mFrameRate;
            }

            if (service.mediaManager == null) {
                return mFrameRate;
            }

            JSONObject videos = service.mediaManager.GetVideo();
            if (videos == null) {
                return mFrameRate;
            }
            if (videos.has("algorithm")) {
                String algorithm = service.mediaManager.GetAlgorithm();
                return displayAlgorithm(algorithm);
            } else {
                if (service.boardState.platformType == BoardState.PlatformType.rpi) {
                    return mFrameRate;
                }
                mVisualizationVideo.update(mode);
                return mFrameRate;
            }
        } catch (Exception e) {
            BLog.e(TAG, e.getMessage());
            return mFrameRate;
        }
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
                dbValue += java.lang.Math.max(0, 50 * (Math.log10(magnitude) - 1));
                if (dbValue < 0)
                    dbValue = 0;
                dbLevels[i / 32] += dbValue;
                dbLevels[i / 32] = java.lang.Math.min(dbLevels[i / 32], 255);

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

    public void showMap() {
        showingMap = mFrameRate * 15;
    }

    public void setMode(int mode) {

        // Likely not connected to physical burner board, fallback
         if (mode == 99) {
            service.boardState.currentVideoMode++;
        } else if (mode == 98) {
            service.boardState.currentVideoMode--;
        } else {
            service.boardState.currentVideoMode = mode;
        }

        int maxModes = service.mediaManager.GetTotalVideo();
        if (service.boardState.currentVideoMode >= maxModes)
            service.boardState.currentVideoMode = 0;
        else if (service.boardState.currentVideoMode < 0)
            service.boardState.currentVideoMode = maxModes-1;

            if (service.boardState.masterRemote)
            service.masterController.SendVideo();

        BLog.d(TAG, "Setting visualization mode to: " + service.boardState.currentVideoMode);

        service.burnerBoard.resetParams();
        service.burnerBoard.clearPixels();

        service.burnerBoard.setText(String.valueOf(service.boardState.currentVideoMode), 2000);
    }

}

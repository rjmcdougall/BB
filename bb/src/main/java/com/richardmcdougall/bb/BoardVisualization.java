package com.richardmcdougall.bb;

import android.media.audiofx.Visualizer;
import android.speech.tts.TextToSpeech;

import com.richardmcdougall.bb.visualization.AudioBar;
import com.richardmcdougall.bb.visualization.AudioCenter;
import com.richardmcdougall.bb.visualization.AudioTile;
import com.richardmcdougall.bb.visualization.Fire;
import com.richardmcdougall.bb.visualization.JosPack;
import com.richardmcdougall.bb.visualization.Matrix;
import com.richardmcdougall.bb.visualization.Meteor;
import com.richardmcdougall.bb.visualization.Mickey;
import com.richardmcdougall.bb.visualization.PlayaMap;
import com.richardmcdougall.bb.visualization.SyncLights;
import com.richardmcdougall.bb.visualization.TestColors;
import com.richardmcdougall.bb.visualization.TheMan;
import com.richardmcdougall.bb.visualization.Video;
import com.richardmcdougall.bb.visualization.Visualization;

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
    private String TAG = this.getClass().getSimpleName();

    private BBService service;
    public Random mRandom = new Random();
    public byte[] mBoardFFT;
    private int mBoardWidth;
    private int mBoardHeight;
    private int[] mBoardScreen;

    public int mMultipler4Speed;
    private boolean inhibitVisual = false;
    private boolean inhibitVisualGTFO = false;

    private boolean emergencyVisual = false;
    private boolean showBattery = false;
    private int mFrameRate;
    private int batteryCnt = 0;

    public Visualization mVisualizationFire;
    public Visualization mVisualizationMatrix;
    public Visualization mVisualizationTextColors;
    public Visualization mVisualizationTheMan;
    public Visualization mVisualizationAudioTile;
    public Visualization mVisualizationAudioCenter;
    public Visualization mVisualizationVideo;
    public Visualization mVisualizationMickey;
    public Visualization mVisualizationJosPack;
    public Visualization mVisualizationAudioBar;
    public Visualization mVisualizationMeteor;
    public Visualization mVisualizationPlayaMap;
    public Visualization getmVisualizationSyncLights;

    BoardVisualization(BBService service) {

        this.service = service;
        BLog.d(TAG, "Starting Board Visualization " + service.burnerBoard.boardType + " on " + service.boardState.BOARD_ID);

        mBoardWidth = service.burnerBoard.getWidth();
        mBoardHeight = service.burnerBoard.getHeight();
        mMultipler4Speed = service.burnerBoard.getMultiplier4Speed();
        mBoardScreen = service.burnerBoard.getPixelBuffer();
        mFrameRate = service.burnerBoard.getFrameRate();

        BLog.d(TAG, "Board framerate set to " + mFrameRate);

        mVisualizationFire = new Fire(service.burnerBoard, this);
        mVisualizationMatrix = new Matrix(service.burnerBoard, this);
        mVisualizationTextColors = new TestColors(service.burnerBoard, this);
        mVisualizationTheMan = new TheMan(service.burnerBoard, this);
        mVisualizationAudioTile = new AudioTile(service.burnerBoard, this);
        mVisualizationAudioCenter = new AudioCenter(service.burnerBoard, this);
        mVisualizationVideo = new Video(service.burnerBoard, this);
        mVisualizationMickey = new Mickey(service.burnerBoard, this);
        mVisualizationJosPack = new JosPack(service.burnerBoard, this);
        mVisualizationAudioBar = new AudioBar(service.burnerBoard, this);
        mVisualizationMeteor = new Meteor(service.burnerBoard, this);
        mVisualizationPlayaMap = new PlayaMap(service.burnerBoard, this);
        getmVisualizationSyncLights = new SyncLights(service.burnerBoard, this);

    }

    void Run() {
        Thread t = new Thread(new Runnable() {
            public void run() {
                Thread.currentThread().setName("BB Board Display");
                boardDisplayThread();
            }
        });
        t.start();
    }

    private int boardDisplayCnt = 0;
    private Visualizer mVisualizer;
    private int mAudioSessionId;

    public void inhibit(boolean inhibit) {
        inhibitVisual = inhibit;
    }

    public void inhibitGTFO(boolean inhibit) {
        inhibitVisualGTFO = inhibit;
    }

    public void emergency(boolean emergency) {
        emergencyVisual = emergency;
    }

    public void showBattery(boolean show) {
        showBattery = show;
    }

    // For reasons I don't understand, VideoMode() = 0 doesn't have a profile associated with it.
    // VideoMode() = 1 sets it to the beginning of the profile.
    void NextVideo() {
        int next = service.boardState.currentVideoMode + 1;
        if (next > service.mediaManager.GetTotalVideo()) {
            next = 1;
        }
        BLog.d(TAG, "Setting Video to: " + service.mediaManager.GetVideoFileLocalName(next - 1));
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

            case "modeMatrix(kMatrixReverse)":
                frameRate = mFrameRate;
                mVisualizationMatrix.update(Matrix.kMatrixReverse);

                break;

            case "modeMatrix(kMatrixLunarian)":
                frameRate = mFrameRate;
                mVisualizationMatrix.update(Matrix.kMatrixLunarian);
                break;

            case "modeAudioCenter()":
                frameRate = mFrameRate;
                mVisualizationAudioCenter.update(Visualization.kDefault);
                break;

            case "modeFire(kModeFireNormal)":
                frameRate = mFrameRate * 3;
                mVisualizationFire.update(Fire.kModeFireNormal);
                break;

            case "modeFire(kModeFireDistrikt)":
                frameRate = mFrameRate * 3;
                mVisualizationFire.update(Fire.kModeFireDistrikt);
                break;

            case "modeFire(kModeFireTheMan)":
                frameRate = mFrameRate * 3;
                mVisualizationFire.update(Fire.kModeFireTheMan);
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

            case "modeMeteor()":
                frameRate = mFrameRate;
                mVisualizationMeteor.update(Matrix.kDefault);
                break;

            case "MickeyGold()":
                frameRate = mFrameRate;
                mVisualizationMickey.update(Mickey.kMickeyGold);
                break;

            case "MickeySparkle()":
                frameRate = mFrameRate;
                mVisualizationMickey.update(Mickey.kMickeySparkle);
                break;

            case "MickeyColors()":
                frameRate = mFrameRate;
                mVisualizationMickey.update(Mickey.kMickeyColors);
                break;

            case "MickeyBlueGold()":
                frameRate = mFrameRate;
                mVisualizationMickey.update(Mickey.kMickeyBlueGold);
                break;

            case "modeMickeyBlank()":
                frameRate = mFrameRate;
                mVisualizationMickey.update(Mickey.kMickeyBlank);
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
                // This would print once per frame, which can easily flood log buffers. Only
                // uncomment in extreme debugging cases! -jib
                //Log.d(TAG, "Could not find visualization algorithm: " + algorithm);
                break;

        }
        return frameRate;
    }

    long debugCnt = 0;

    // Main thread to drive the Board's display & get status (mode, voltage,...)
    void boardDisplayThread() {

        long lastFrameTime = System.currentTimeMillis();
        int frameRate = 11;

        BLog.d(TAG, "Starting board display thread...");

        int nVideos = service.mediaManager.GetTotalVideo();

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
                    BLog.e(TAG, er.getMessage());
                }
                continue;
            }

            if (emergencyVisual) {
                BLog.d(TAG, "inhibit");
                service.burnerBoard.clearPixels();
                service.burnerBoard.fillScreen(255, 0, 0);
                service.burnerBoard.showBattery();
                service.burnerBoard.flush();
                try {
                    Thread.sleep(1000);
                } catch (Throwable er) {
                    BLog.e(TAG, er.getMessage());
                }
                continue;
            }

            // TODO: render our own battery here rather than rely on firmware to do it.
            if (showBattery) {
                if (batteryCnt % mFrameRate == 0) {
                    service.burnerBoard.showBattery();
                }
                batteryCnt++;
            }

            if (showingMap > 0) {
                frameRate = mFrameRate;
                mVisualizationPlayaMap.update(Visualization.kDefault);
                showingMap -= 1;
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
            if (boardDisplayCnt > 1000) {
                //updateStatus();
            }
        }

    }int frameCnt = 0;

    int runVisualization(int mode) {

        try {
            mode += -1;

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

            // TODO: check perf overhead of checking this every frame
            JSONObject videos = service.mediaManager.GetVideo(mode);
            if (videos == null) {
                return mFrameRate;
            }
            if (videos.has("algorithm")) {
                String algorithm = service.mediaManager.GetAlgorithm(mode);
                return displayAlgorithm(algorithm);
            } else {
                if (!(service.boardState.platformType == BoardState.PlatformType.rpi)) {
                    return mFrameRate;
                }
                mVisualizationVideo.update(mode);
                return mFrameRate;
            }
        } catch (Exception e) {
            BLog.e(TAG, e.getMessage());
            return mFrameRate;
        }
    }// Get levels from Android visualizer engine
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

    private static final int kNumLevels = 8;
    private int[] oldLevels = new int[kNumLevels];

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

    private int showingMap = 0;

    public void showMap() {
        showingMap = mFrameRate * 15;
    }public void setMode(int mode) {

        // Likely not connected to physical burner board, fallback
        if (mode == 99) {
            service.boardState.currentVideoMode++;
        } else if (mode == 98) {
            service.boardState.currentVideoMode--;
        } else {
            service.boardState.currentVideoMode = mode;
        }

        int maxModes = service.mediaManager.GetTotalVideo();
        if (service.boardState.currentVideoMode > maxModes)
            service.boardState.currentVideoMode = 1;
        else if (service.boardState.currentVideoMode < 1)
            service.boardState.currentVideoMode = maxModes;

        // If I am set to be the master, broadcast to other boards
        if (service.boardState.masterRemote && (service.rfClientServer != null)) {

            String name = service.mediaManager.GetVideoFileLocalName(service.boardState.currentVideoMode - 1);
            BLog.d(TAG, "Sending video remote for video " + name);
            service.rfMasterClientServer.sendRemote(RFUtil.REMOTE_VIDEO_TRACK_CODE, MediaManager.hashTrackName(name), RFMasterClientServer.kRemoteVideo);
        }

        if (service.burnerBoard != null) {
            BLog.d(TAG, "Setting visualization mode to: " + service.boardState.currentVideoMode);

            service.burnerBoard.resetParams();
            service.burnerBoard.clearPixels();
            service.burnerBoard.setMode(service.boardState.currentVideoMode);
        }

        if (service.voiceAnnouncements) {
            service.voice.speak("mode" + service.boardState.currentVideoMode, TextToSpeech.QUEUE_FLUSH, null, "mode");
        }
    }

}

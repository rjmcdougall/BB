package com.richardmcdougall.bb;

import com.richardmcdougall.bb.visualization.Fire;
import com.richardmcdougall.bb.visualization.Matrix;
import com.richardmcdougall.bb.visualization.Mickey;
;
import com.richardmcdougall.bb.visualization.JosPack;
import com.richardmcdougall.bb.visualization.*;
import com.richardmcdougall.bb.visualization.Visualization;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.audiofx.Visualizer;

import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import org.json.JSONObject;

import java.util.Random;

/**
 * Created by rmc on 3/8/17.
 */


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

    private static final String TAG = "BB.BoardVisualization";
    private BBService mBBservice;
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

    int mBoardMode;
    Context mContext;

    BurnerBoard mBurnerBoard = null;

    BoardVisualization(Context context, BurnerBoard board, BBService service) {
        mBurnerBoard = board;
        mBBservice = service;
        l("Starting Board Visualization " + mBurnerBoard.boardType + " on " + mBurnerBoard.boardId);

        mBoardWidth = mBurnerBoard.getWidth();
        mBoardHeight = mBurnerBoard.getHeight();
        mMultipler4Speed = mBurnerBoard.getMultiplier4Speed();

        mContext = context;
        // Start Board Display
        Thread boardDisplay = new Thread(new Runnable() {
            public void run() {
                Thread.currentThread().setName("BB Board Display");
                boardDisplayThread();
            }
        });
        boardDisplay.start();
        mBoardScreen = board.getPixelBuffer();
        mFrameRate = board.getFrameRate();

        l("Board framerate set to " + mFrameRate);

        mVisualizationFire = new Fire(mBurnerBoard, this);
        mVisualizationMatrix = new Matrix(mBurnerBoard, this);
        mVisualizationTextColors = new TestColors(mBurnerBoard, this);
        mVisualizationTheMan = new TheMan(mBurnerBoard, this);
        mVisualizationAudioTile = new AudioTile(mBurnerBoard, this);
        mVisualizationAudioCenter = new AudioCenter(mBurnerBoard, this);
        mVisualizationVideo = new Video(mBurnerBoard, this);
        mVisualizationMickey = new Mickey(mBurnerBoard, this);
        mVisualizationJosPack = new JosPack(mBurnerBoard, this);
        mVisualizationAudioBar = new AudioBar(mBurnerBoard, this);
        mVisualizationMeteor = new Meteor(mBurnerBoard, this);
        mVisualizationPlayaMap = new PlayaMap(mBurnerBoard, this);
        getmVisualizationSyncLights = new SyncLights(mBurnerBoard, this);

    }

    private int boardDisplayCnt = 0;
    private Visualizer mVisualizer;
    private int mAudioSessionId;

    public void l(String s) {
        Log.v(TAG, s);
        sendLogMsg(s);
    }

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

    private void sendLogMsg(String msg) {
        Intent in = new Intent(ACTION.STATS);
        in.putExtra("resultCode", Activity.RESULT_OK);
        in.putExtra("msgType", 4);
        // Put extras into the intent as usual
        in.putExtra("logMsg", msg);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(in);
    }


    // For reasons I don't understand, VideoMode() = 0 doesn't have a profile associated with it.
    // VideoMode() = 1 sets it to the beginning of the profile.
    void NextVideo() {
        int next = getMode() + 1;
        if (next > mBBservice.dlManager.GetTotalVideo()) {
            next = 1;
        }
        l("Setting Video to: " + mBBservice.dlManager.GetVideoFileLocalName(next - 1));
        mBBservice.boardVisualization.setMode(next);
    }

    public void setMode(int mode) {
        Log.d(TAG, "Setting visualization mode to: " + mode);

        mBoardMode = mode;
        mBurnerBoard.resetParams();
        mBurnerBoard.clearPixels();
        // TODO: call visual-specific setmode
    }

    public int getMode() {
        return(mBoardMode);
    }

    public void attachAudio(int audioSessionId) {
        int vSize;

        l("session=" + audioSessionId);
        mAudioSessionId = audioSessionId;

        try {
            if (mVisualizer != null) {
                //mVisualizer.release();
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
            l("Error enabling visualizer: " + e.getMessage());
            //System.out.println("Error enabling visualizer:" + e.getMessage());
            return;
        }
        l("Enabled visualizer with " + vSize + " bytes");
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

        l("Starting board display thread...");

        int nVideos = mBurnerBoard.mBBService.dlManager.GetTotalVideo();

        while (true) {

            // Power saving when board top not turned on
            if (inhibitVisual || inhibitVisualGTFO) {
                l("inhibit");
                mBurnerBoard.clearPixels();
                mBurnerBoard.showBattery();
                mBurnerBoard.flush();
                try {
                    Thread.sleep(1000);
                } catch (Throwable er) {
                    er.printStackTrace();
                }
                continue;
            }

            if (emergencyVisual) {
                l("inhibit");
                mBurnerBoard.clearPixels();
                mBurnerBoard.fillScreen(255, 0, 0);
                mBurnerBoard.showBattery();
                mBurnerBoard.flush();
                try {
                    Thread.sleep(1000);
                } catch (Throwable er) {
                    er.printStackTrace();
                }
                continue;
            }

            // TODO: render our own battery here rather than rely on firmware to do it.
            if (showBattery) {
                if (batteryCnt % mFrameRate == 0) {
                    mBurnerBoard.showBattery();
                }
                batteryCnt++;
            }

            if (showingMap > 0) {
                frameRate = mFrameRate;
                mVisualizationPlayaMap.update(Visualization.kDefault);
                showingMap -= 1;
                continue;
            }

            frameRate = runVisualization(mBoardMode);

            long frameTime = 1000 / frameRate;
            long curFrameTime = System.currentTimeMillis();
            long thisFrame = (curFrameTime-lastFrameTime);

            if (thisFrame < frameTime) {
                try {
                    Thread.sleep(frameTime - thisFrame);
                } catch (Throwable er) {
                    er.printStackTrace();
                }
            }

            lastFrameTime =  System.currentTimeMillis();

            boardDisplayCnt++;
            if (boardDisplayCnt > 1000) {
                //updateStatus();
            }
        }

    }


    int frameCnt = 0;

    int runVisualization(int mode) {

        mode += -1;

        //mBurnerBoard.setMsg(String.format("%04d %04d", mBBservice.GetCurrentClock() % 10000, mBBservice.CurrentClockAdjusted() % 10000));
        //if (true)
        //    return 20;

        frameCnt++;
        if (frameCnt % 100 == 0) {
            l("Frames: " + frameCnt);
        }

        if (mode < 0) {
            return mFrameRate;
        }

        if(mBurnerBoard.mBBService.dlManager == null){
            return mFrameRate;
        }

        // TODO: check perf overhead of checking this every frame
        JSONObject videos = mBurnerBoard.mBBService.dlManager.GetVideo(mode);
        if (videos == null) {
            return mFrameRate;
        }
        if(videos.has("algorithm")){
            String algorithm = mBurnerBoard.mBBService.dlManager.GetAlgorithm(mode);
            return displayAlgorithm(algorithm);
        } else {
            if (BurnerBoardUtil.kIsRPI) { // nano is fine
                return mFrameRate;
            }
            mVisualizationVideo.update(mode);
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
    }


}

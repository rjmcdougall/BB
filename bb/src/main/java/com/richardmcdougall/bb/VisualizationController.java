package com.richardmcdougall.bb;

import android.media.audiofx.Visualizer;

import com.richardmcdougall.bb.board.BurnerBoard;
import com.richardmcdougall.bb.visualization.AudioBar;
import com.richardmcdougall.bb.visualization.AudioBarHorizontal;
import com.richardmcdougall.bb.visualization.AudioCenter;
import com.richardmcdougall.bb.visualization.AudioTile;
import com.richardmcdougall.bb.visualization.DisplayRowTest;
import com.richardmcdougall.bb.visualization.DisplaySectionTest;
import com.richardmcdougall.bb.visualization.MatrixTest;
import com.richardmcdougall.bb.visualization.PowerTest;
import com.richardmcdougall.bb.visualization.DisplayLineTest;
import com.richardmcdougall.bb.visualization.RGBList;
import com.richardmcdougall.bb.visualization.JosPack;
import com.richardmcdougall.bb.visualization.Matrix;
import com.richardmcdougall.bb.visualization.PlayaMap;
import com.richardmcdougall.bb.video.Video;
import com.richardmcdougall.bb.visualization.SimpleSign;
import com.richardmcdougall.bb.visualization.Visualization;
import com.richardmcdougall.bbcommon.BLog;
import com.richardmcdougall.bbcommon.BoardState;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;
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

public class VisualizationController {
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
    public Visualization mVisualizationAudioBarHorizontal;
    public Visualization mVisualizationPlayaMap;
    public Visualization mVisualizationSimpleSign;
    public Visualization mPixelMapTest;
    public Visualization mMatrixTest;
    public Visualization mPowerTest;
    public Visualization mDisplaySectionTest;
    public Visualization mDisplayLineTest;
    public Visualization mDisplayRowTest;

    private int frameCnt = 0;
    private String TAG = this.getClass().getSimpleName();
    private BBService service;
    public boolean inhibitVisual = false;
    public boolean inhibitVisualGTFO = false;
    public boolean batteryCrisisVisual = false;
    private Visualizer mVisualizer;
    private int mAudioSessionId;
    private int[] oldLevels = new int[kNumLevels];

    private int showingMap = 0;

    VisualizationController(BBService service) {

        this.service = service;
        BLog.d(TAG, "Starting Board Visualization " + service.boardState.GetBoardType().toString() + " on " + service.boardState.BOARD_ID);

        mMultipler4Speed = service.burnerBoard.getMultiplier4Speed();

        BLog.d(TAG, "Board framerate set to " + service.burnerBoard.getFrameRate());

        mVisualizationMatrix = new Matrix(service);
        mVisualizationAudioTile = new AudioTile(service);
        mVisualizationAudioCenter = new AudioCenter(service);
        mVisualizationVideo = new Video(service);
        mVisualizationJosPack = new JosPack(service);
        mVisualizationAudioBar = new AudioBar(service);
        mVisualizationAudioBarHorizontal = new AudioBarHorizontal(service);
        mVisualizationPlayaMap = new PlayaMap(service);
        mVisualizationSimpleSign = new SimpleSign(service);
        mPixelMapTest = new DisplayLineTest(service);
        mMatrixTest = new MatrixTest(service);
        mPowerTest = new PowerTest(service);
        mDisplaySectionTest = new DisplaySectionTest(service);
        mDisplayLineTest = new DisplayLineTest(service);
        mDisplayRowTest = new DisplayRowTest(service);
    }

    void Run() {
        Thread t = new Thread(() -> {
            Thread.currentThread().setName("BB Visualization");
            boardDisplayThread();
        });
        t.start();
    }

    // For reasons I don't understand, VideoMode() = 0 doesn't have a profile associated with it.
    // VideoMode() = 1 sets it to the beginning of the profile.
    void NextVideo() {
        int next = service.boardState.currentVideoMode + 1;
        if (next >= service.mediaManager.GetTotalVideo()) {
            next = 0;
        }
        BLog.d(TAG, "Setting Video to: " + service.mediaManager.GetVideoFileLocalName(next));
        setMode(next);
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

        int frameRate = service.burnerBoard.getFrameRate();

        if(this.service.displayMapManager.displayDebugPattern.equalsIgnoreCase("matrixTest")){
            mMatrixTest.update(Visualization.kDefault);
        }
        else if(this.service.displayMapManager.displayDebugPattern.equalsIgnoreCase("powerTest")){
            mPowerTest.update(Visualization.kDefault);
        }
        else if(this.service.displayMapManager.displayDebugPattern.equalsIgnoreCase("displaySectionTest")){
            mDisplaySectionTest.update(Visualization.kDefault);
        }
        else if(this.service.displayMapManager.displayDebugPattern.equalsIgnoreCase("displayLineTest")){
            mDisplayLineTest.update(Visualization.kDefault);
        }
        else if(this.service.displayMapManager.displayDebugPattern.equalsIgnoreCase("displayRowTest")){
            mDisplayRowTest.update(Visualization.kDefault);
        }
       else if(algorithm.contains("simpleSign")){
            String parameter = algorithm.substring(algorithm.indexOf("(")+1,algorithm.indexOf(")"));

            List<String> params = Arrays.asList(parameter.split(","));
            mVisualizationSimpleSign.setText(params.get(0),params.get(1).trim(),params.get(2).trim());
            mVisualizationSimpleSign.update(Visualization.kDefault);
        }
        else {

            switch (algorithm) {

                case "kMezcal()":
                    mVisualizationMatrix.update(Matrix.kMatrixMezcal);
                    break;

                case "modeMatrix(kMatrixBurnerColor)":
                    mVisualizationMatrix.update(Matrix.kMatrixBurnerColor);
                    break;

                case "modeMatrix(kMatrixLunarian)":
                    mVisualizationMatrix.update(Matrix.kMatrixLunarian);
                    break;

                case "modeMatrix(kMatrixMermaid)":
                    mVisualizationMatrix.update(Matrix.kMatrixMermaid);
                    break;

                case "modeAudioCenter()":
                    mVisualizationAudioCenter.update(Visualization.kDefault);
                    break;

                case "modeAudioBarV()":
                    //mVisualizationAudioBar.update(Matrix.kDefault);
                    mVisualizationAudioBarHorizontal.update(Matrix.kDefault);
                    break;

                case "modeAudioBarH()":
                    mVisualizationAudioBarHorizontal.update(Matrix.kDefault);
                    break;

                case "modeMatrix(kMatrixSync)":
                    mVisualizationMatrix.update(Matrix.kMatrixSync);
                    break;

                case "modeAudioTile()":
                    mVisualizationAudioTile.update(Matrix.kDefault);
                    break;

                case "JPGold()":
                    mVisualizationJosPack.update(JosPack.kJPGold);
                    break;

                case "JPSparkle()":
                    mVisualizationJosPack.update(JosPack.kJPSparkle);
                    break;

                case "JPColors()":
                    mVisualizationJosPack.update(JosPack.kJPColors);
                    break;

                case "JPBlueGold()":
                    mVisualizationJosPack.update(JosPack.kJPBlueGold);
                    break;

                case "JPBlank()":
                    mVisualizationJosPack.update(JosPack.kJPBlank);
                    break;

                case "JPBluePurple()":
                    mVisualizationJosPack.update(JosPack.kJPBluePurple);
                    break;

                case "JPTriColor()":
                    mVisualizationJosPack.update(JosPack.kJPTriColor);
                    break;
                case "modePlayaMap()":
                    mVisualizationPlayaMap.update(Visualization.kDefault);
                    break;
                default:
                    break;
            }
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
                service.burnerBoard.showBattery(BurnerBoard.batteryType.LARGE);
                service.burnerBoard.flush();
                try {
                    Thread.sleep(1000);
                } catch (Throwable er) {
                }
                continue;
            }

            if (batteryCrisisVisual) {
                BLog.d(TAG, "inhibit");
                //service.burnerBoard.clearPixels();
                //service.burnerBoard.fillScreen(255, 0, 0);
                //service.burnerBoard.flush();
                try {
                    Thread.sleep(1000);
                } catch (Throwable er) {
                }
                continue;
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
                    BLog.e(TAG, "Visualization error: " + er.getMessage());
                }
            }

            lastFrameTime = System.currentTimeMillis();

        }

    }

    int runVisualization(int mode) {

         try {
            frameCnt++;
            if (frameCnt % 100 == 0) {
                BLog.d(TAG, "Frames: " + frameCnt);
            }

            if (mode < 0) {
                return service.burnerBoard.getFrameRate();
            }

            JSONObject videos = service.mediaManager.GetVideo();
            if (videos == null) {
                return service.burnerBoard.getFrameRate();
            }
            
             this.service.burnerBoard.lineBuilder.clearLine();

             if (videos.has("algorithm")) {
                String algorithm = service.mediaManager.GetAlgorithm();
                return displayAlgorithm(algorithm);
            } else {
                if (service.boardState.platformType == BoardState.PlatformType.rpi) {
                    return service.burnerBoard.getFrameRate();
                }
                mVisualizationVideo.update(mode);
                return service.burnerBoard.getFrameRate();
            }
        } catch (Exception e) {
             BLog.e(TAG, "Visualization error: " + e.getMessage());
             return service.burnerBoard.getFrameRate();
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
                dbValue += java.lang.Math.max(0, 50 + 50 * (Math.log10(magnitude) - 1));
                if (dbValue < 0)
                    dbValue = 0;
                dbLevels[i / 32] += dbValue;
                dbLevels[i / 32] = java.lang.Math.min(dbLevels[i / 32], 255);

            }
            return dbLevels;
        }
    }


    // Get levels from Android visualizer engine
    // 128 int buckets of frequency levels from low to high
    // Range is 0-255 per bucket
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
                dbValue = (int) Math.max(0, 200.0 + 200.0 * (Math.log10(magnitudes[i]) - 1));
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

    public void showMap() {
        showingMap = service.burnerBoard.getFrameRate() * 15;
    }

    public void setDisplayMode(int displayMode) {
        service.boardState.displayMode = displayMode;
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

        service.burnerBoard.clearPixels();

        service.burnerBoard.textBuilder.setText(String.valueOf(service.boardState.currentVideoMode), 2000, service.burnerBoard.getFrameRate(), new RGBList().getColor("white"));
    }

}

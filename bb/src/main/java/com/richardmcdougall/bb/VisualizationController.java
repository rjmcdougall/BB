package com.richardmcdougall.bb;

import android.media.audiofx.Visualizer;

import com.richardmcdougall.bb.board.BurnerBoard;
import com.richardmcdougall.bb.visualization.Zagger;
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
    private int autoVideoMode = 0;
    private long parkedSinceMilliseconds = System.currentTimeMillis();;
    private long lastAutoVideoMilliseconds = System.currentTimeMillis();;
    public Random mRandom = new Random();
    public int mMultipler4Speed;

    public Visualization mVisualizationMatrix;
    public Visualization mVisualizationTextColors;
    public Visualization mVisualizationZagger;
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


    private int showingMap = 0;

    VisualizationController(BBService service) {

        this.service = service;
        BLog.d(TAG, "Starting Board Visualization " + service.boardState.GetBoardType().toString() + " on " + service.boardState.BOARD_ID);

        mMultipler4Speed = service.burnerBoard.getMultiplier4Speed();

        BLog.d(TAG, "Board framerate set to " + service.burnerBoard.getFrameRate());

        mVisualizationMatrix = new Matrix(service);
        mVisualizationZagger = new Zagger(service);
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

    void nextAutoVideo() {
            int next = autoVideoMode + 1;
            if (next >= service.mediaManager.GetTotalVideo()) {
                next = 0;
            }
            BLog.d(TAG, "Setting Video to: " + service.mediaManager.GetVideoFileLocalName(next));
            autoVideoMode = next;
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

                case "modeZagger()":
                    mVisualizationZagger.update(Visualization.kDefault);
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
                    mVisualizationAudioBar.update(Matrix.kDefault);
                    break;

                case "modeAudioBarH()":
                    mVisualizationAudioBarHorizontal.update(AudioBarHorizontal.kStaticVUColor);
                    break;

                case "modeAudioBarZag()":
                    mVisualizationAudioBarHorizontal.update(AudioBarHorizontal.kZag);
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
                    BLog.d(TAG, "unknown visualilzer mode: " + algorithm);
                    break;
            }
        }

        return frameRate;
    }

    public void resetParkedTime() {
        BLog.d(TAG, "resetParkedTime...");
        parkedSinceMilliseconds = System.currentTimeMillis();
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


            try {
                if (!service.motionSupervisor.getState().equals(MotionSupervisor.motionStates.STATE_PARKED)) {
                    //BLog.d(TAG, "! STATE_PARKED");
                    parkedSinceMilliseconds = System.currentTimeMillis();
                }
            } catch (Exception e) {
                //BLog.d(TAG, "vesc getState exception");
            }
            //BLog.d(TAG, "parkedSince = " + (System.currentTimeMillis() - parkedSinceMilliseconds));
            if ((System.currentTimeMillis() - parkedSinceMilliseconds) > 300000) {
                if ((System.currentTimeMillis() - lastAutoVideoMilliseconds) > 30000) {
                    BLog.d(TAG, "Parked and next video...");
                    nextAutoVideo();
                    lastAutoVideoMilliseconds = System.currentTimeMillis();
                }
                frameRate = runVisualization(autoVideoMode);
            } else {
                frameRate = runVisualization(service.boardState.currentVideoMode);
                autoVideoMode = service.boardState.currentVideoMode;
            }
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
                BLog.d(TAG, "Frames: " + frameCnt + ", parked for " + (System.currentTimeMillis() - parkedSinceMilliseconds));
            }

            if (mode < 0) {
                return service.burnerBoard.getFrameRate();
            }

            JSONObject videos = service.mediaManager.GetVideo(mode);
            if (videos == null) {
                return service.burnerBoard.getFrameRate();
            }
            
             this.service.burnerBoard.lineBuilder.clearLine();

             if (videos.has("algorithm")) {
                String algorithm = service.mediaManager.GetAlgorithm(mode);
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

    public void showMap() {
        showingMap = service.burnerBoard.getFrameRate() * 15;
    }

    public void setFunMode(boolean funMode) {
        service.boardState.funMode = funMode;
    }

    public void setMode(int mode) {

        parkedSinceMilliseconds = System.currentTimeMillis();
        lastAutoVideoMilliseconds = System.currentTimeMillis();

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

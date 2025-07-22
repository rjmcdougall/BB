package com.richardmcdougall.bb.board;

import com.hoho.android.usbserial.util.MonotonicClock;
import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bb.util.LatencyHistogram;
import com.richardmcdougall.bbcommon.BLog;
import com.richardmcdougall.bbcommon.BoardState;

import java.nio.ByteBuffer;

public class BurnerBoardMezcal extends BurnerBoard {

    private String TAG = this.getClass().getSimpleName();
    private final LatencyHistogram latencyHistogram = new LatencyHistogram(TAG);

    public BurnerBoardMezcal(BBService service) {
        super(service);
        initpixelMap2Board();
        init(boardWidth, boardHeight);
        latencyHistogram.registerMethod("update");
        latencyHistogram.registerMethod("flush2Board");
        latencyHistogram.registerMethod("flush");
        latencyHistogram.registerMethod("setStrip");
        latencyHistogram.enableAutoLogging(10);
        latencyHistogram.registerMethod("flushOuter");
        latencyHistogram.registerMethod("flushInner");
        latencyHistogram.registerMethod("flushInner1");
        latencyHistogram.registerMethod("flushInner2");
        latencyHistogram.registerMethod("flushInner3");
        latencyHistogram.registerMethod("flushInner4");
        latencyHistogram.registerMethod("flushInner5");
        latencyHistogram.registerMethod("flushInner6");
        latencyHistogram.registerMethod("flushInner7");
        latencyHistogram.registerMethod("flushInner8");
        latencyHistogram.registerMethod("flushInner9");
        latencyHistogram.registerMethod("flushInner10");
        latencyHistogram.enableAutoLogging(10);
    }

    private static final int kMaxV4DisplayPower = 9;
    public int kStrips = 1;
    static int[][] mapPixelsToStips = new int[1][4096];
    private TranslationMap[] boardMap;
    private GammaCorrection mGammaCorrection = new GammaCorrection();
    private PixelDimmer mDimmer = new PixelDimmer();


    static {
        textSizeHorizontal = 14;
        textSizeVertical = 20;
        enableBatteryMonitoring = true;
        enableMotionMonitoring = false;
        enableIOTReporting = true;
        renderTextOnScreen = false;
        boardType = BoardState.BoardType.mezcal;
    }


    public int getMultiplier4Speed() {
        return 1;
    }

    public void start() {
        // attach getBatteryLevel cmdMessenger callback
        BoardCallbackGetBatteryLevel getBatteryLevelCallback =
                new BoardCallbackGetBatteryLevel(true);
        mListener.attach(8, getBatteryLevelCallback);
    }

    public int getFrameRate() {

        return 40;
    }

    public void setOtherlightsAutomatically() {
    }


    // Work around that teensy cmd essenger can't deal with 0, ',', ';', '\\'
    // Optimized version with minimal branching for high-frequency calls
    private static int pixelWorkaround(int level) {
        // Fast path for most common case (0-255 values except problematic ones)
        if (level == 0) return 1;
        if (level == 44) return 45;  // ',' = 44
        if (level == 59) return 60;  // ';' = 59  
        if (level == 92) return 93;  // '\\' = 92
        return level;
    }

    long flushEndLatency = 0;
    int[] stripPixels = null;

    public void flush() {


        long flushStartLatency = MonotonicClock.millis();
        if (flushEndLatency > 0) {
            latencyHistogram.recordLatency("flushOuter", flushEndLatency - flushStartLatency);
        }

        if (stripPixels == null) {
            stripPixels = new int[600 * 3];
        }

        this.logFlush();
        int[] mOutputScreen = boardScreen.clone();
        latencyHistogram.recordLatency("flushInner1", MonotonicClock.millis() - flushStartLatency);
        sharpener.sharpen(mOutputScreen, service.burnerBoard.boardSharpenMode);
        latencyHistogram.recordLatency("flushInner2", MonotonicClock.millis() - flushStartLatency);
        mOutputScreen = this.textBuilder.renderText(mOutputScreen);
        latencyHistogram.recordLatency("flushInner3", MonotonicClock.millis() - flushStartLatency);
        mOutputScreen = this.lineBuilder.renderLine(mOutputScreen);
        latencyHistogram.recordLatency("flushInner4", MonotonicClock.millis() - flushStartLatency);
        mOutputScreen = this.batteryOverlayBuilder.renderBattery(mOutputScreen);
        latencyHistogram.recordLatency("flushInner5", MonotonicClock.millis() - flushStartLatency);
        mOutputScreen = this.brakeOverlayBuilder.renderBrake(mOutputScreen);
        latencyHistogram.recordLatency("flushInner6", MonotonicClock.millis() - flushStartLatency);
        // TODO: gamma correction should be here before dimmer
        latencyHistogram.recordLatency("flushInner7", MonotonicClock.millis() - flushStartLatency);
        mOutputScreen = mGammaCorrection.Correct(mOutputScreen);
        latencyHistogram.recordLatency("flushInner8", MonotonicClock.millis() - flushStartLatency);
        mOutputScreen = mDimmer.Dim(kMaxV4DisplayPower, mOutputScreen);
        latencyHistogram.recordLatency("flushInner9", MonotonicClock.millis() - flushStartLatency);
        //this.appDisplay.send(mOutputScreen);

        // Walk through each strip and fill from the graphics buffer
        for (int s = 0; s < kStrips; s++) {
            // Walk through all the pixels in the strip
            for (int offset = 0; offset < 600 * 3; ) {
                stripPixels[offset] = pixelWorkaround(mOutputScreen[mapPixelsToStips[s][offset++]]);
                stripPixels[offset] = pixelWorkaround(mOutputScreen[mapPixelsToStips[s][offset++]]);
                stripPixels[offset] = pixelWorkaround(mOutputScreen[mapPixelsToStips[s][offset++]]);
            }
            //latency = MonotonicClock.millis();
            long beforeSetStripLatency = MonotonicClock.millis();
            setStrip(s, stripPixels);
            long afterSetStripLatency = MonotonicClock.millis();
            latencyHistogram.recordLatency("setStrip", afterSetStripLatency - beforeSetStripLatency);
            long beforeFlushLatency = MonotonicClock.millis();
            flush2Board();
            long afterFlushLatency = MonotonicClock.millis();
            latencyHistogram.recordLatency("flush2Board", afterFlushLatency - beforeFlushLatency);
            //BLog.d(TAG, "setstrip latency = " + (MonotonicClock.millis()- latency));
            if ((s % 2) == 0) {
                //flush2Board();
                //BLog.d(TAG, "flush latency = " + (MonotonicClock.millis()- latency));
            }
        }
        latencyHistogram.recordLatency("flushInner10", MonotonicClock.millis() - flushStartLatency);
// Render on board
        //latency = MonotonicClock.millis();
        long beforeUpdateLatency = MonotonicClock.millis();
        update();
        long afterUpdateLatency = MonotonicClock.millis();
        latencyHistogram.recordLatency("update", afterUpdateLatency - beforeUpdateLatency);
        //latency = MonotonicClock.millis();
        long beforeFlushLatency = MonotonicClock.millis();
        flush2Board();
        long afterFlushLatency = MonotonicClock.millis();
        latencyHistogram.recordLatency("flush2Board", afterFlushLatency - beforeFlushLatency);
        
        // Record overall flush latency
        flushEndLatency = MonotonicClock.millis();
        latencyHistogram.recordLatency("flushInner", flushEndLatency - flushStartLatency);
        //Runtime.getRuntime().gc();
    }

    private ByteBuffer mWriteBuffer = ByteBuffer.allocate(16384);
    private static final byte[] teensyPrefix = "10,".getBytes();
    private static final byte[] teensyPostfix = ";".getBytes();
    private static final byte[] teensyUpdate = "6;".getBytes();

    public void setStrip(int strip, int[] pixels) {
        //BLog.d(TAG, "setstrip " + strip + " pixels length " + pixels.length);
        int len = Math.min(552 * 3, pixels.length);
        byte[] newPixels = new byte[len];
        for (int pixel = 0; pixel < len; pixel++) {
            newPixels[pixel] = (byte) pixels[pixel];
        }

        mWriteBuffer.put(teensyPrefix);
        mWriteBuffer.put(String.format("%d,", strip).getBytes());
        mWriteBuffer.put(newPixels);
        mWriteBuffer.put(teensyPostfix);

    }

    public boolean update() {
        try {
            mWriteBuffer.put(teensyUpdate);

        } catch (Exception e) {
        }
        return true;
    }

    public void flush2Board() {
        try {
            int len = mWriteBuffer.position();
            if (len > 0) {
                byte[] buffer = new byte[len];
                mWriteBuffer.rewind();
                mWriteBuffer.get(buffer, 0, len);
                mWriteBuffer.clear();
                //BLog.d(TAG, "write " + len + " bytes");
                mSerialIoManager.writeAsync(buffer);
                //mSerialIoManager.flush();
            }
        } catch (Exception e) {
        }
    }

    private void pixelRemap(int x, int y, int stripOffset) {
        mapPixelsToStips[boardMap[y].stripNumber - 1][stripOffset] = this.pixelOffset.Map(boardWidth - 1 - x, boardHeight - 1 - y, PIXEL_RED);
        mapPixelsToStips[boardMap[y].stripNumber - 1][stripOffset + 1] = this.pixelOffset.Map(boardWidth - 1 - x, boardHeight - 1 - y, PIXEL_GREEN);
        mapPixelsToStips[boardMap[y].stripNumber - 1][stripOffset + 2] = this.pixelOffset.Map(boardWidth - 1 - x, boardHeight - 1 - y, PIXEL_BLUE);
    }

    public void initpixelMap2Board() {
        int x, y;


        boardMap = this.service.displayMapManager.GetDisplayMap();
        boardWidth = this.service.displayMapManager.boardWidth;
        boardHeight = this.service.displayMapManager.boardHeight;
        kStrips = this.service.displayMapManager.numberOfStrips;
        BLog.d(TAG, "init board " + boardWidth + "," + boardHeight + " strips " + kStrips);
        boardScreen = new int[this.boardWidth * this.boardHeight * 3];
        mapPixelsToStips = new int[kStrips][4096];
        pixelOffset = new PixelOffset(this);
        this.pixelOffset.initPixelOffset();
        this.clearPixels();

        try {
            for (y = 0; y < boardMap.length; y++) {
                // Strip has x2 ... x1 (reverse order)
                int stripOffset = boardMap[y].stripOffset;
                for (x = boardMap[y].endX; x <= boardMap[y].startX; x++) {
                    pixelRemap(x, y, stripOffset * 3);
                    stripOffset++;
                }
            }
        } catch (Exception e) {
            BLog.e(TAG, e.getMessage());
        }
    }

    // Clean up scheduler when the board is destroyed
    public void shutdown() {
        latencyHistogram.shutdown();
    }
}

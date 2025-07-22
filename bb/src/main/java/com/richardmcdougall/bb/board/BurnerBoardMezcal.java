package com.richardmcdougall.bb.board;

import com.hoho.android.usbserial.util.MonotonicClock;
import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bbcommon.BLog;
import com.richardmcdougall.bbcommon.BoardState;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class BurnerBoardMezcal extends BurnerBoard {

    private final Map<String, ConcurrentHashMap<Long, AtomicLong>> latencyHistograms = new HashMap<>();
    private final ScheduledExecutorService statsScheduler = Executors.newScheduledThreadPool(1);

    public BurnerBoardMezcal(BBService service) {
        super(service);
        initpixelMap2Board();
        init(boardWidth, boardHeight);
        latencyHistograms.put("update", new ConcurrentHashMap<>());
        latencyHistograms.put("flush2Board", new ConcurrentHashMap<>());

        // Schedule stats logging every 10 seconds
        statsScheduler.scheduleWithFixedDelay(this::logLatencyStats, 10, 10, TimeUnit.SECONDS);
    }

    private void recordLatency(String method, long latency) {
        ConcurrentHashMap<Long, AtomicLong> histogram = latencyHistograms.get(method);
        histogram.computeIfAbsent(latency, k -> new AtomicLong()).incrementAndGet();
    }

    public Map<String, ConcurrentHashMap<Long, AtomicLong>> getLatencyHistograms() {
        return latencyHistograms;
    }

    public void logLatencyStats() {
        for (String method : latencyHistograms.keySet()) {
            ConcurrentHashMap<Long, AtomicLong> histogram = latencyHistograms.get(method);
            if (!histogram.isEmpty()) {
                long totalCalls = histogram.values().stream().mapToLong(AtomicLong::get).sum();

                // Create power of 2 buckets: [0], [1], [2-3], [4-7], [8-15], [16-31], [32-63], [64-127], [128+]
                long[] buckets = new long[9];
                String[] bucketLabels = {"0ms", "1ms", "2-3ms", "4-7ms", "8-15ms", "16-31ms", "32-63ms", "64-127ms", "128+ms"};

                for (Map.Entry<Long, AtomicLong> entry : histogram.entrySet()) {
                    long latency = entry.getKey();
                    long count = entry.getValue().get();

                    int bucketIndex;
                    if (latency == 0) {
                        bucketIndex = 0;
                    } else if (latency == 1) {
                        bucketIndex = 1;
                    } else if (latency <= 3) {
                        bucketIndex = 2;
                    } else if (latency <= 7) {
                        bucketIndex = 3;
                    } else if (latency <= 15) {
                        bucketIndex = 4;
                    } else if (latency <= 31) {
                        bucketIndex = 5;
                    } else if (latency <= 63) {
                        bucketIndex = 6;
                    } else if (latency <= 127) {
                        bucketIndex = 7;
                    } else {
                        bucketIndex = 8;
                    }

                    buckets[bucketIndex] += count;
                }

                // Build the output string with non-zero buckets only
                StringBuilder bucketStats = new StringBuilder();
                for (int i = 0; i < buckets.length; i++) {
                    if (buckets[i] > 0) {
                        if (bucketStats.length() > 0) {
                            bucketStats.append(", ");
                        }
                        bucketStats.append(bucketLabels[i]).append(":").append(buckets[i]);
                    }
                }

                BLog.i(TAG, method + " latency buckets (total: " + totalCalls + ") - " + bucketStats.toString());
            }
        }
    }

    //private static final int kMaxV4DisplayPower = 12;
    // Try 9, since 12 was getting too hot on kronos 6/4/2024
    private static final int kMaxV4DisplayPower = 9;
    private String TAG = this.getClass().getSimpleName();
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
    private static int pixelWorkaround(int level) {
        //return level;
        //}

        switch (level) {
            case 0:
                level = 1;
                break;
            case ',':
                level += 1;
            case ';':
                level += 1;
            case '\\':
                level += 1;
            default:
        }
        return level;
    }


    public void flush() {

        long startLatency = MonotonicClock.millis();
        this.logFlush();
        int[] mOutputScreen = boardScreen.clone();
        sharpener.sharpen(mOutputScreen, service.burnerBoard.boardSharpenMode);
        mOutputScreen = this.textBuilder.renderText(mOutputScreen);
        mOutputScreen = this.lineBuilder.renderLine(mOutputScreen);
        mOutputScreen = this.batteryOverlayBuilder.renderBattery(mOutputScreen);
        mOutputScreen = this.brakeOverlayBuilder.renderBrake(mOutputScreen);
        // TODO: gamma correction should be here before dimmer
        mOutputScreen = mGammaCorrection.Correct(mOutputScreen);
        mOutputScreen = mDimmer.Dim(kMaxV4DisplayPower, mOutputScreen);
        //this.appDisplay.send(mOutputScreen);

        // Walk through each strip and fill from the graphics buffer
        for (int s = 0; s < kStrips; s++) {
            int[] stripPixels = new int[600 * 3];
            // Walk through all the pixels in the strip
            for (int offset = 0; offset < 600 * 3; ) {
                stripPixels[offset] = pixelWorkaround(mOutputScreen[mapPixelsToStips[s][offset++]]);
                stripPixels[offset] = pixelWorkaround(mOutputScreen[mapPixelsToStips[s][offset++]]);
                stripPixels[offset] = pixelWorkaround(mOutputScreen[mapPixelsToStips[s][offset++]]);
            }
            //latency = MonotonicClock.millis();
            setStrip(s, stripPixels);
            long beforeFlushLatency = MonotonicClock.millis();
            flush2Board();
            long afterFlushLatency = MonotonicClock.millis();
            recordLatency("flush2Board", afterFlushLatency - beforeFlushLatency);
            //BLog.d(TAG, "setstrip latency = " + (MonotonicClock.millis()- latency));
            if ((s % 2) == 0) {
                //flush2Board();
                //BLog.d(TAG, "flush latency = " + (MonotonicClock.millis()- latency));
            }
        }
        // Render on board
        //latency = MonotonicClock.millis();
        long beforeUpdateLatency = MonotonicClock.millis();
        update();
        long afterUpdateLatency = MonotonicClock.millis();
        recordLatency("update", afterUpdateLatency - beforeUpdateLatency);
        //latency = MonotonicClock.millis();
        long beforeFlushLatency = MonotonicClock.millis();
        flush2Board();
        long afterFlushLatency = MonotonicClock.millis();
        recordLatency("flush2Board", afterFlushLatency - beforeFlushLatency);
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
        if (statsScheduler != null && !statsScheduler.isShutdown()) {
            statsScheduler.shutdown();
            try {
                if (!statsScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    statsScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                statsScheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}

package com.richardmcdougall.bb.board;

import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bb.CmdMessenger;

import com.richardmcdougall.bbcommon.BoardState;
import com.richardmcdougall.bbcommon.BLog;

//BBWSPanel is a string of WS28xx leds that go up and down
public class BurnerBoardWSPanel extends BurnerBoard {

    private static final String TAG = "BB.BurnerBoardWSPanel";

    // Limits of the hardware strips:
    private static final int kMaxStrips = 24;
    private static final int kMaxStripLength = 1024;

    // Two primary mapping functions
    // Temporary until we can parameterize this

    // Width = X
    // Height = Y

    // Woodson's panel
    static int kBoardWidth = 20;
    static int kBoardHeight = 180;
    static int kPanelHeight = kBoardHeight;
    static int kPanelWidth = 2;
    // Serpentine in Y/Height direction
    static boolean kStackedY = true;
    // Number of logical strips in each strip
    static int kSubStrips = kPanelWidth;
    // Number of hardware strips
    static int kStrips = 16; //boardHeight / panel height

    // Proto's test board
    //static int kBoardWidth = 32;
    //static int kBoardHeight = 32;
    //static int kPanelHeight = 8;
    //static int kPanelWidth = 32;
    //static boolean kStackedY = false;
    //static int kStrips = kBoardHeight / kPanelHeight; //boardHeight / panel height
    //static int kSubStrips = kPanelWidth;


    public BurnerBoardWSPanel(BBService service) {
        super(service);
        //        boardWidth = 32; (proto's test panel)
        //        boardHeight = 32;
        boardWidth = kBoardWidth;
        boardHeight = kBoardHeight;
        boardType = "Burner Board WSPanel";
        BLog.d(TAG, "Burner Board WSPanel initting...");
        mBoardScreen = new int[boardWidth * boardHeight * 3];
        BLog.d(TAG, "Burner Board WSPanel initPixelOffset...");
        initPixelOffset();
        BLog.d(TAG, "Burner Board WSPanel initpixelMap2Board...");
        initpixelMap2Board();
        BLog.d(TAG, "Burner Board WSPanel initUsb...");
        initUsb();
        this.textBuilder = new TextBuilder(boardWidth, boardHeight, 0, 0) ;
    }

    public int getFrameRate() {
        return 50;
    }

    public int getMultiplier4Speed() {
        if (service.boardState.displayTeensy == BoardState.TeensyType.teensy4)
            return 1; // dkw need to config this
        else
            return 2; // dkw need to config this

    }

    public void start() {

        // attach getBatteryLevel cmdMessenger callback
        BurnerBoardWSPanel.BoardCallbackGetBatteryLevel getBatteryLevelCallback =
                new BurnerBoardWSPanel.BoardCallbackGetBatteryLevel();
        mListener.attach(8, getBatteryLevelCallback);

    }

    public class BoardCallbackGetBatteryLevel implements CmdMessenger.CmdEvents {
        public void CmdAction(String str) {
            for (int i = 0; i < mBatteryStats.length; i++) {
                mBatteryStats[i] = mListener.readIntArg();
            }
            if (mBatteryStats[1] != -1) {
                service.boardState.batteryLevel = mBatteryStats[1];
            } else {
                service.boardState.batteryLevel = 100;
            }
            BLog.d(TAG, "getBatteryLevel: " + service.boardState.batteryLevel);
        }
    }

    public void scrollPixels(boolean down) {

        if (mBoardScreen == null) {
            return;
        }
        if (down) {
            for (int x = 0; x < boardWidth; x++) {
                for (int y = 0; y < boardHeight - 1; y++) {
                    mBoardScreen[pixel2Offset(x, y, PIXEL_RED)] =
                            mBoardScreen[pixel2Offset(x, y + 1, PIXEL_RED)];
                    mBoardScreen[pixel2Offset(x, y, PIXEL_GREEN)] =
                            mBoardScreen[pixel2Offset(x, y + 1, PIXEL_GREEN)];
                    mBoardScreen[pixel2Offset(x, y, PIXEL_BLUE)] =
                            mBoardScreen[pixel2Offset(x, y + 1, PIXEL_BLUE)];
                }
            }
        } else {
            for (int x = 0; x < boardWidth; x++) {
                for (int y = boardHeight - 2; y >= 0; y--) {
                    mBoardScreen[pixel2Offset(x, y + 1, PIXEL_RED)] =
                            mBoardScreen[pixel2Offset(x, y, PIXEL_RED)];
                    mBoardScreen[pixel2Offset(x, y + 1, PIXEL_GREEN)] =
                            mBoardScreen[pixel2Offset(x, y, PIXEL_GREEN)];
                    mBoardScreen[pixel2Offset(x, y + 1, PIXEL_BLUE)] =
                            mBoardScreen[pixel2Offset(x, y, PIXEL_BLUE)];
                }
            }
        }

    }

    public void scrollPixelsExcept(boolean down, int color) {

        if (mBoardScreen == null) {
            return;
        }
        if (down) {
            for (int x = 0; x < boardWidth; x++) {
                for (int y = 0; y < boardHeight - 1; y++) {
                    if (RGB.getRGB(mBoardScreen[pixel2Offset(x, y + 1, PIXEL_RED)],
                            mBoardScreen[pixel2Offset(x, y + 1, PIXEL_GREEN)],
                            mBoardScreen[pixel2Offset(x, y + 1, PIXEL_BLUE)]) != color) {
                        mBoardScreen[pixel2Offset(x, y, PIXEL_RED)] =
                                mBoardScreen[pixel2Offset(x, y + 1, PIXEL_RED)];
                        mBoardScreen[pixel2Offset(x, y, PIXEL_GREEN)] =
                                mBoardScreen[pixel2Offset(x, y + 1, PIXEL_GREEN)];
                        mBoardScreen[pixel2Offset(x, y, PIXEL_BLUE)] =
                                mBoardScreen[pixel2Offset(x, y + 1, PIXEL_BLUE)];
                    }
                }
            }
        } else {
            for (int x = 0; x < boardWidth; x++) {
                for (int y = boardHeight - 2; y >= 0; y--) {
                    mBoardScreen[pixel2Offset(x, y, PIXEL_RED)] =
                            mBoardScreen[pixel2Offset(x, y + 1, PIXEL_RED)];
                    mBoardScreen[pixel2Offset(x, y, PIXEL_GREEN)] =
                            mBoardScreen[pixel2Offset(x, y + 1, PIXEL_GREEN)];
                    mBoardScreen[pixel2Offset(x, y, PIXEL_BLUE)] =
                            mBoardScreen[pixel2Offset(x, y + 1, PIXEL_BLUE)];
                }
            }
        }
    }

    public int[] getPixelBuffer() {
        return mBoardScreen;
    }

    // TODO: gamma correction
    // encoded = ((original / 255) ^ (1 / gamma)) * 255
    // original = ((encoded / 255) ^ gamma) * 255

    private int flushCnt = 0;
    long lastFlushTime = System.currentTimeMillis();

    public void flush() {

        flushCnt++;
        if (flushCnt > 100) {
            int elapsedTime = (int) (System.currentTimeMillis() - lastFlushTime);
            lastFlushTime = System.currentTimeMillis();

            BLog.d(TAG, "Framerate: " + flushCnt + " frames in " + elapsedTime + ", " +
                    (flushCnt * 1000 / elapsedTime) + " frames/sec");
            flushCnt = 0;
        }

        // Suppress updating when displaying a text message
        if (this.textBuilder.textDisplayingCountdown > 0) {
            this.textBuilder.textDisplayingCountdown--;
        } else {

            // Here we calculate the total power percentage of the whole board
            // We want to limit the board to no more than 50% of pixel output total
            // This is because the board is setup to flip the breaker at 200 watts
            // Output is percentage multiplier for the LEDs
            // At full brightness we limit to 30% of their output
            // Power is on-linear to pixel brightness: 37% = 50% power.
            // powerPercent = 100: 15% multiplier
            // powerPercent <= 15: 100% multiplier
            int totalBrightnessSum = 0;
            int powerLimitMultiplierPercent = 100;
            for (int pixel = 0; pixel < mBoardScreen.length; pixel++) {
                // R
                if (pixel % 3 == 0) {
                    totalBrightnessSum += mBoardScreen[pixel];
                } else if (pixel % 3 == 1) {
                    totalBrightnessSum += mBoardScreen[pixel];
                } else {
                    totalBrightnessSum += mBoardScreen[pixel] / 2;
                }
            }

            final int powerPercent = totalBrightnessSum / mBoardScreen.length * 100 / 255;
            // Woodson's panel
            powerLimitMultiplierPercent = 100 - Math.max(powerPercent - 20, 0);

            int[] rowPixels = new int[boardWidth * 3];
            for (int y = 0; y < boardHeight; y++) {
                //for (int y = 30; y < 31; y++) {
                for (int x = 0; x < boardWidth; x++) {
                    if (y < boardHeight) {
                        rowPixels[x * 3 + 0] = mBoardScreen[pixel2Offset(x, y, PIXEL_RED)];
                        rowPixels[x * 3 + 1] = mBoardScreen[pixel2Offset(x, y, PIXEL_GREEN)];
                        rowPixels[x * 3 + 2] = mBoardScreen[pixel2Offset(x, y, PIXEL_BLUE)];
                    }
                }
                setRowVisual(y, rowPixels);
            }


            // Walk through each strip and fill from the graphics buffer
            for (int s = 0; s < kStrips; s++) {
                //BLog.d(TAG, "strip:" + s);
                int[] stripPixels = new int[kPanelHeight * 3 * kSubStrips];
                // Walk through all the pixels in the strip
                for (int offset = 0; offset < kPanelHeight * 3 * kSubStrips; ) {
                    stripPixels[offset] = mBoardScreen[pixelMap2BoardTable[s][offset++]];
                    stripPixels[offset] = mBoardScreen[pixelMap2BoardTable[s][offset++]];
                    stripPixels[offset] = mBoardScreen[pixelMap2BoardTable[s][offset++]];
                }
                if (s == 0) {
                    setStrip(0, stripPixels, powerLimitMultiplierPercent);
                } else if (s == 1) {
                    setStrip(1, stripPixels, powerLimitMultiplierPercent);
                } else if (s == 2) {
                    setStrip(2, stripPixels, powerLimitMultiplierPercent);
                } else if (s == 3) {
                    setStrip(3, stripPixels, powerLimitMultiplierPercent);
                } else if (s == 4) {
                    setStrip(12, stripPixels, powerLimitMultiplierPercent);
                } else if (s == 5) {
                    setStrip(13, stripPixels, powerLimitMultiplierPercent);
                } else if (s == 6) {
                    setStrip(14, stripPixels, powerLimitMultiplierPercent);
                } else if (s == 7) {
                    setStrip(15, stripPixels, powerLimitMultiplierPercent);
                } else if (s == 8) {
                    setStrip(8, stripPixels, powerLimitMultiplierPercent);
                } else if (s == 9) {
                    setStrip(9, stripPixels, powerLimitMultiplierPercent);
                } else if (s == 10) {
                    //setStrip(6, stripPixels, powerLimitMultiplierPercent);
                } else if (s == 11) {
                    setStrip(7, stripPixels, powerLimitMultiplierPercent);
                } else if (s == 12) {
                    //setStrip(15, stripPixels, powerLimitMultiplierPercent);
                } else if (s == 13) {
                    //setStrip(15, stripPixels, powerLimitMultiplierPercent);
                } else if (s == 14) {
                    //setStrip(15, stripPixels, powerLimitMultiplierPercent);
                } else if (s == 15) {
                    //setStrip(15, stripPixels, powerLimitMultiplierPercent);
                } else {
                    setStrip(s, stripPixels, powerLimitMultiplierPercent);
                }
                // Send to board
                flush2Board();
            }


            // Render on board
            update();
            flush2Board();
            try {
                Thread.sleep(20);
            } catch (Exception e) {
            }
        }
    }

    //    cmdMessenger.attach(BBUpdate, OnUpdate);              // 6
    public boolean update() {

        sendVisual(8);

        //l("sendCommand: 5");
        synchronized (mSerialConn) {
            if (mListener != null) {
                mListener.sendCmd(6);
                mListener.sendCmdEnd();
                return true;
            } else {
                // Emulate board's 30ms refresh time
                try {
                    Thread.sleep(5);
                } catch (Throwable e) {
                }
            }
        }

        return false;
    }


    //    cmdMessenger.attach(BBShowBattery, OnShowBattery);    // 7
    public void showBattery() {

        sendVisual(9);
        BLog.d(TAG, "sendCommand: 7");
        if (mListener != null) {
            mListener.sendCmd(7);
            mListener.sendCmdEnd();
            flush2Board();
            return;
        }
        return;
    }

    public void setMsg(String msg) {
    }

    public static class TranslationMap {
        int y;
        int startX;
        int endX;
        int stripDirection;
        int stripNumber;
        int stripOffset;

        private TranslationMap(
                int y,
                int startX,
                int endX,
                int stripDirection,
                int stripNumber,
                int stripOffset) {
            this.y = y;
            this.startX = startX;
            this.endX = endX;
            this.stripDirection = stripDirection;
            this.stripNumber = stripNumber;
            this.stripOffset = stripOffset;
        }
    }

    static int pixelMap2Board(int s, int offset) {
        return pixelMap2BoardTable[s][offset];
    }

    private void pixelRemap(int x, int y, int stripNo, int stripOffset) {
        pixelMap2BoardTable[stripNo][stripOffset] =
                pixel2Offset(boardWidth - 1 - x, boardHeight - 1 - y, PIXEL_RED);
        pixelMap2BoardTable[stripNo][stripOffset + 1] =
                pixel2Offset(boardWidth - 1 - x, boardHeight - 1 - y, PIXEL_GREEN);
        pixelMap2BoardTable[stripNo][stripOffset + 2] =
                pixel2Offset(boardWidth - 1 - x, boardHeight - 1 - y, PIXEL_BLUE);
    }



    static int[][] pixelMap2BoardTable = new int[kMaxStrips][kMaxStripLength * 3];
    private TranslationMap[] boardMap;

    private void initpixelMap2Board() {

        BLog.d(TAG, "initmap");

        if (kStackedY) {
            BLog.d(TAG, "kstacked");
            for (int x = 0; x < boardWidth; x++) {
                for (int y = 0; y < boardHeight; y++) {

                    final int subStrip = x % kSubStrips;
                    final int stripNo = x / kSubStrips;
                    final boolean stripUp = subStrip % 2 == 0;
                    int stripOffset;

                    if (stripUp) {
                        stripOffset = subStrip * boardHeight + y;
                    } else {
                        stripOffset = subStrip * boardHeight + (boardHeight - 1 - y);
                    }
                    BLog.d(TAG, "pixel remap " + x + ',' + y + " : " + stripNo + "," + stripOffset * 3);
                    pixelRemap(x, y, stripNo, stripOffset * 3);
                }
            }
        } else {

            for (int x = 0; x < boardWidth; x++) {
                for (int y = 0; y < boardHeight; y++) {

                    final int nStacks = boardHeight / kPanelHeight;
                    final int subStrip = x % kSubStrips;
                    final int stripNo = y / kPanelHeight;
                    final boolean stripUp = subStrip % 2 == 0;
                    final int panelY = y % kPanelHeight;

                    if (boardHeight < kPanelHeight) {
                        BLog.d(TAG, "Board dims wrong");
                    }
                    int stripOffset;

                    if (stripUp) {
                        stripOffset = subStrip * kPanelHeight + panelY;
                    } else {
                        stripOffset = subStrip * kPanelHeight + (kPanelHeight - 1 - panelY);
                    }
                    BLog.d(TAG, "pixel remap " + x + ',' + y + " : " + stripNo + "," + stripOffset * 3);
                    pixelRemap(x, y, stripNo, stripOffset * 3);
                }

            }
        }

    }
}


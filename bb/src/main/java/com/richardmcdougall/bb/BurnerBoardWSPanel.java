package com.richardmcdougall.bb;

import java.nio.IntBuffer;

import timber.log.Timber;

/**
 * Created by rmc on 7/25/17.
 */


/*

   bottom
         46,118
 +------+
 |      | strip 1
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
 |      | strip 8
 +------+
 0,0 X
 top

 */


public class BurnerBoardWSPanel extends BurnerBoard {
    public BurnerBoardWSPanel(BBService service) {
        super(service);
        mBoardWidth = 32;
        mBoardHeight = 32;
        mMultipler4Speed = 3;
        boardId = service.boardState.BOARD_ID;
        boardType = "Burner Board WSPanel";
        Timber.d("Burner Board WSPanel initting...");
        mBoardScreen = new int[mBoardWidth * mBoardHeight * 3];
        Timber.d("Burner Board WSPanel initPixelOffset...");
        initPixelOffset();
        Timber.d("Burner Board WSPanel initpixelMap2Board...");
        initpixelMap2Board();
        Timber.d("Burner Board WSPanel initUsb...");
        initUsb();
        mTextBuffer = IntBuffer.allocate(mBoardWidth * mBoardHeight * 4);
    }

    public int getFrameRate() {
        return 50;
    }

    public void start() {

        // attach default cmdMessenger callback
        BoardCallbackDefault defaultCallback =
                new BoardCallbackDefault();
        mListener.attach(defaultCallback);

        // attach Test cmdMessenger callback
        BoardCallbackTest testCallback =
                new BoardCallbackTest();
        mListener.attach(5, testCallback);

        // attach Mode cmdMessenger callback
        BoardCallbackMode modeCallback =
                new BoardCallbackMode();
        mListener.attach(4, modeCallback);

        // attach Board ID cmdMessenger callback
        BoardCallbackBoardID boardIDCallback =
                new BoardCallbackBoardID();
        mListener.attach(11, boardIDCallback);

        // attach echoRow cmdMessenger callback
        BoardCallbackEchoRow echoCallback =
                new BoardCallbackEchoRow();
        mListener.attach(17, echoCallback);

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
            Timber.d("getBatteryLevel: " + service.boardState.batteryLevel);
        }
    }

    public void scrollPixels(boolean down) {

        if (mBoardScreen == null) {
            return;
        }
        if (down) {
            for (int x = 0; x < mBoardWidth; x++) {
                for (int y = 0; y < mBoardHeight - 1; y++) {
                    mBoardScreen[pixel2Offset(x, y, PIXEL_RED)] =
                            mBoardScreen[pixel2Offset(x, y + 1, PIXEL_RED)];
                    mBoardScreen[pixel2Offset(x, y, PIXEL_GREEN)] =
                            mBoardScreen[pixel2Offset(x, y + 1, PIXEL_GREEN)];
                    mBoardScreen[pixel2Offset(x, y, PIXEL_BLUE)] =
                            mBoardScreen[pixel2Offset(x, y + 1, PIXEL_BLUE)];
                }
            }
        } else {
            for (int x = 0; x < mBoardWidth; x++) {
                for (int y = mBoardHeight - 2; y >= 0; y--) {
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
            for (int x = 0; x < mBoardWidth; x++) {
                for (int y = 0; y < mBoardHeight - 1; y++) {
                    if (getRGB(mBoardScreen[pixel2Offset(x, y + 1, PIXEL_RED)],
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
            for (int x = 0; x < mBoardWidth; x++) {
                for (int y = mBoardHeight - 2; y >= 0; y--) {
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

            Timber.d("Framerate: " + flushCnt + " frames in " + elapsedTime + ", " +
                    (flushCnt * 1000 / elapsedTime) + " frames/sec");
            flushCnt = 0;
        }

        // Suppress updating when displaying a text message
        if (isTextDisplaying > 0) {
            isTextDisplaying--;
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
            powerLimitMultiplierPercent = 100 - Math.max(powerPercent - 50, 0);

            int[] rowPixels = new int[mBoardWidth * 3];
            for (int y = 0; y < mBoardHeight; y++) {
                //for (int y = 30; y < 31; y++) {
                for (int x = 0; x < mBoardWidth; x++) {
                    if (y < mBoardHeight) {
                        rowPixels[x * 3 + 0] = mBoardScreen[pixel2Offset(x, y, PIXEL_RED)];
                        rowPixels[x * 3 + 1] = mBoardScreen[pixel2Offset(x, y, PIXEL_GREEN)];
                        rowPixels[x * 3 + 2] = mBoardScreen[pixel2Offset(x, y, PIXEL_BLUE)];
                    }
                }
                setRowVisual(y, rowPixels);
            }


            // Walk through each strip and fill from the graphics buffer
            for (int s = 0; s < kStrips; s++) {
                //Timber.d("strip:" + s);
                int[] stripPixels = new int[kPanelHeight * 3 * kSubStrips];
                // Walk through all the pixels in the strip
                for (int offset = 0; offset < kPanelHeight * 3 * kSubStrips; ) {
                    stripPixels[offset] = mBoardScreen[pixelMap2BoardTable[s][offset++]];
                    stripPixels[offset] = mBoardScreen[pixelMap2BoardTable[s][offset++]];
                    stripPixels[offset] = mBoardScreen[pixelMap2BoardTable[s][offset++]];
                }
                setStrip(s, stripPixels, powerLimitMultiplierPercent);
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
        Timber.d("sendCommand: 7");
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
                pixel2Offset(mBoardWidth - 1 - x, mBoardHeight - 1 - y, PIXEL_RED);
        pixelMap2BoardTable[stripNo][stripOffset + 1] =
                pixel2Offset(mBoardWidth - 1 - x, mBoardHeight - 1 - y, PIXEL_GREEN);
        pixelMap2BoardTable[stripNo][stripOffset + 2] =
                pixel2Offset(mBoardWidth - 1 - x, mBoardHeight - 1 - y, PIXEL_BLUE);
    }


    // Two primary mapping functions
    static int kPanelHeight = 8;
    static int kPanelWidth = 32;
    static boolean kStackedend = false;
    static int kStrips = 32 / kPanelHeight; //mBoardHeight / panel height
    static int kSubStrips = kPanelWidth;
    static int[][] pixelMap2BoardTable = new int[8][4096];
    private TranslationMap[] boardMap;

    private void initpixelMap2Board() {

        Timber.d("initmap");

        if (kStackedend) {
            for (int x = 0; x < mBoardWidth; x++) {
                for (int y = 0; y < mBoardHeight; y++) {

                    final int subStrip = x % kSubStrips;
                    final int stripNo = x / kSubStrips;
                    final boolean stripUp = subStrip % 2 == 0;
                    int stripOffset;

                    if (stripUp) {
                        stripOffset = subStrip * mBoardHeight + y;
                    } else {
                        stripOffset = subStrip * mBoardHeight + (mBoardHeight - 1 - y);
                    }
                    Timber.d("pixel remap " + x + ',' + y + " : " + stripNo + "," + stripOffset * 3);
                    pixelRemap(x, y, stripNo, stripOffset * 3);
                }

            }
        } else {

            for (int x = 0; x < mBoardWidth; x++) {
                for (int y = 0; y < mBoardHeight; y++) {

                    final int nStacks = mBoardHeight / kPanelHeight;
                    final int subStrip = x % kSubStrips;
                    final int stripNo = y / kPanelHeight;
                    final boolean stripUp = subStrip % 2 == 0;
                    final int panelY = y % kPanelHeight;

                    if (mBoardHeight < kPanelHeight) {
                        Timber.e("Board dims wrong");
                    }
                    int stripOffset;

                    if (stripUp) {
                        stripOffset = subStrip * kPanelHeight + panelY;
                    } else {
                        stripOffset = subStrip * kPanelHeight + (kPanelHeight - 1 - panelY);
                    }
                    Timber.d("pixel remap " + x + ',' + y + " : " + stripNo + "," + stripOffset * 3);
                    pixelRemap(x, y, stripNo, stripOffset * 3);
                }

            }
        }

    }
}


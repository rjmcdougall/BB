package com.richardmcdougall.bb.board;

import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bb.bms.BMS;
import com.richardmcdougall.bbcommon.BLog;
import com.richardmcdougall.bbcommon.BoardState;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;

public class BatteryOverlayBuilder {

    private String TAG = this.getClass().getSimpleName();

    private BBService service;

    private static final int kDisplayForSeconds = 10;
    public int batteryDisplayingCountdown = 0;
    private IntBuffer drawBuffer = null;
    private BurnerBoard board = null;
    public ArrayList<RGB> pixels = new ArrayList<>();

    boolean critical = false;
    RGB critical_color = new RGB(30, 0, 0);

    private int[] batteryScreen;
    private int[] layeredScreen;
    private static final int kRgbMax = 255;
    private static final int kBatteryGreen = 165;
    private int bigBatteryWidth;
    private int bigBatteryCenter;
    private int bigBatterystartRow;
    private int bigBatteryEndRow;

    private int bigBatteyDivider;

    private int littleBatteryRow = 105;
    private int littleBattMiddle;
    private int littleBattWidth;
    private int littleBattHeight;
    private int littleBattDivider;

    private BurnerBoard.batteryType mLastBatteryType = BurnerBoard.batteryType.SMALL;

    BatteryOverlayBuilder(BBService service, BurnerBoard board) {
        this.service = service;
        this.board = board;

        if (service.boardState.GetBoardType() == BoardState.BoardType.mezcal) {
            // Mezcal
            bigBatteryWidth = 20;
            bigBatterystartRow = 60;
            bigBatteryEndRow = 160;
            bigBatteryCenter = 36;
            bigBatteyDivider = 1;
            littleBatteryRow = 180;
            littleBattMiddle = 36;
            littleBattWidth = 6;
            littleBattHeight = 20;
            littleBattDivider = 5;
        } else if (service.boardState.GetBoardType() == BoardState.BoardType.panel) {
            // Panel

            bigBatteryWidth = 20;
            bigBatterystartRow = 3;
            bigBatteryEndRow = 52;
            bigBatteryCenter = 16;
            bigBatteyDivider = 2; // 50 100 %
            littleBatteryRow =40;
            littleBattMiddle = 16;
            littleBattWidth = 6;
            littleBattHeight = 10;
            littleBattDivider = 10;

        } else {
            // Azul
            // Little battery at top
            // row 105-114
            // 105-112 is battery
            // 113-114 is button
            // inside is 106-111
            bigBatteryWidth = 20;
            bigBatterystartRow = 34;
            bigBatteryEndRow = 84;
            bigBatteryCenter = 23;
            bigBatteyDivider = 2;
            littleBatteryRow = 105;
            littleBattMiddle = 23;
            littleBattWidth = 6;
            littleBattHeight = 9;
            littleBattDivider = 15;
        }
    }

    public void drawBattery(BurnerBoard.batteryType type) {
        mLastBatteryType = type;
        if (batteryScreen == null || batteryScreen.length != this.board.boardWidth * this.board.boardHeight * 3) {
            batteryScreen = new int[this.board.boardWidth * this.board.boardHeight * 3];
        }
        batteryDisplayingCountdown = kDisplayForSeconds;
        if (type == BurnerBoard.batteryType.CRITICAL) {
            batteryDisplayingCountdown = batteryDisplayingCountdown * 4;
        }
        drawBatteryInternal(type);
    }

    private void drawBatteryInternal(BurnerBoard.batteryType type) {
        if (type == BurnerBoard.batteryType.CRITICAL) {
            critical = true;
            batteryDisplayingCountdown = batteryDisplayingCountdown * 4;
            clearPixels();
            drawBatteryLarge();
        } else if (type == BurnerBoard.batteryType.LARGE) {
            clearPixels();
            drawBatteryLarge();
            critical = false;
        } else {
            clearPixels();
            drawBatterySmall();
            critical = false;
        }
    }

    void drawBatteryLarge() {

        int x;
        int level;
        int row;
        int battLeft = bigBatteryCenter - bigBatteryWidth / 2;
        int white;

        if (critical) {
            white = new RGB(32, 32, 32).getARGBInt();
        } else {
            white = new RGB(kRgbMax, kRgbMax, kRgbMax).getARGBInt();
        }

        try {
            boolean batteryCritical = (service.bms.getBatteryLevelState() == BMS.batteryLevelStates.STATE_CRITICAL);
            boolean batteryLow = (service.bms.getBatteryLevelState() == BMS.batteryLevelStates.STATE_LOW);

            level = service.boardState.batteryLevel / bigBatteyDivider; // convert 100 to max of 50

            row = bigBatterystartRow;

            // White Battery Shell with Green level

            // Battery Bottom
            for (x = 0; x < bigBatteryWidth; x++) {
                setPixel(x + battLeft, row, white);
            }
            row++;

            // Battery Sides
            for (; row <= bigBatteryEndRow; row++) {
                setPixel(battLeft, row, white);
                setPixel(battLeft + bigBatteryWidth - 1, row, white);
            }

            // Battery Top
            for (x = 0; x < bigBatteryWidth; x++) {
                setPixel(x + battLeft, row, white);
            }
            row++;

            // Battery button
            for (x = bigBatteryWidth / 2 - 6; x < bigBatteryWidth / 2 + 6; x++) {
                setPixel(x + battLeft, row, white);
                setPixel(x + battLeft, row + 1, white);
                setPixel(x + battLeft, row + 2, white);
                setPixel(x + battLeft, row + 3, white);
            }

            // Get level failed
            if (level < 0) {
                // RED
                for (row = bigBatterystartRow + 1; row < bigBatteryEndRow; row++) {
                    for (x = battLeft; x < battLeft + bigBatteryWidth; x++) {
                        setPixel(x, row, RGB.getARGBInt(kRgbMax, 0, 0));
                    }
                }
            } else {
                // Battery Level
                int batteryColor;
                if (batteryCritical) {
                    batteryColor = RGB.getARGBInt(kRgbMax, 0, 0);
                } else if (batteryLow) {
                    batteryColor = RGB.getARGBInt(kRgbMax, kBatteryGreen, 0);
                } else {
                    batteryColor = RGB.getARGBInt(0, kRgbMax, 0);
                }
                for (row = bigBatterystartRow + 1; row < bigBatterystartRow + level; row++) {
                    for (x = battLeft + 1; x < battLeft + bigBatteryWidth - 1; x++) {
                        setPixel(x, row, batteryColor);
                    }
                }
                for (; row <= bigBatteryEndRow; row++) {
                    for (x = battLeft + 1; x < battLeft + bigBatteryWidth - 1; x++) {
                        setPixel(x, row, 0);
                    }
                }
            }
        } catch (Exception e) {
            BLog.e(TAG, "battery render error: " + e.getMessage());
        }
    }

    public void drawBatterySmall() {
        int x;
        int row;
        int level;
        int battLeft = littleBattMiddle - (littleBattWidth / 2);
        int battRight = littleBattMiddle + (littleBattWidth / 2) - 1;

        try {
            boolean batteryCritical = (service.bms.getBatteryLevelState() == BMS.batteryLevelStates.STATE_CRITICAL);
            boolean batteryLow = (service.bms.getBatteryLevelState() == BMS.batteryLevelStates.STATE_LOW);


            level = 1 + service.boardState.batteryLevel / 15; // convert 100 to max of 7

            row = littleBatteryRow; // 105

            // White Battery Shell with Green level

            // Battery Bottom
            for (x = battLeft; x < battLeft + littleBattWidth; x++) {
                setPixel(x, row, RGB.getARGBInt(kRgbMax, kRgbMax, kRgbMax));
            }
            row++;

            // Battery Sides
            for (; row < (littleBatteryRow + littleBattHeight - 2); row++) {
                setPixel(battLeft, row, RGB.getARGBInt(kRgbMax, kRgbMax, kRgbMax));
                setPixel(battRight, row, RGB.getARGBInt(kRgbMax, kRgbMax, kRgbMax));
            }

            // Battery Top
            for (x = battLeft; x <= battRight; x++) {
                setPixel(x, row, RGB.getARGBInt(kRgbMax, kRgbMax, kRgbMax));
            }
            row++;

            // Battery button
            for (x = littleBattMiddle - 1; x < littleBattMiddle + 1; x++) {
                setPixel(x, row, RGB.getARGBInt(kRgbMax, kRgbMax, kRgbMax));
            }

            // Get level failed
            if (level < 0) {
                // RED
                for (row = littleBatteryRow + 1; row < (littleBatteryRow + littleBattHeight - 2); row++) {
                    for (x = battLeft; x < battRight; x++) {
                        setPixel(x, row, RGB.getARGBInt(0, 0, kRgbMax));
                    }
                }
            } else {
                // Battery Level
                int batteryColor;
                if (batteryCritical) { // Red
                    batteryColor = RGB.getARGBInt(kRgbMax, 0, 0);
                } else if (batteryLow) { // Orange
                    batteryColor = RGB.getARGBInt(kRgbMax, 165, 0);
                } else { // Green
                    batteryColor = RGB.getARGBInt(0, kRgbMax, 0);
                }
                for (row = littleBatteryRow + 1; row < littleBatteryRow + level; row++) {
                    for (x = battLeft + 1; x < battRight; x++) {
                        setPixel(x, row, batteryColor);
                    }
                }
                for (; row < littleBatteryRow + littleBattHeight - 2; row++) {
                    for (x = battLeft + 1; x < battRight; x++) {
                        setPixel(x, row, 0);
                    }
                }
            }
        } catch (Exception e) {
            BLog.e(TAG, "battery small render error: " + e.getMessage());
        }
    }

    private int[] overlayScreen(int[] sourceScreen) {

        // Reuse layeredScreen array
        int requiredSize = this.board.boardWidth * this.board.boardHeight * 3;
        if (layeredScreen == null || layeredScreen.length != requiredSize) {
            layeredScreen = new int[requiredSize];
        }

        System.arraycopy(batteryScreen, 0, layeredScreen, 0, batteryScreen.length);

        for (int pixelNo = 0; pixelNo < (this.board.boardWidth * this.board.boardHeight); pixelNo++) {
            int pixel_offset = pixelNo * 3;

            RGB pixel = new RGB(layeredScreen[pixel_offset], layeredScreen[pixel_offset + 1], layeredScreen[pixel_offset + 2]);

            // Render the new text over the original
            if (pixel.isBlack()) {
                layeredScreen[pixel_offset] = critical ? critical_color.r : sourceScreen[pixel_offset];
                layeredScreen[pixel_offset + 1] = critical ? critical_color.g : sourceScreen[pixel_offset + 1];
                layeredScreen[pixel_offset + 2] = critical ? critical_color.b : sourceScreen[pixel_offset + 2];
            }
        }
        return layeredScreen;
    }

    public boolean isDisplaying() {
        return (batteryDisplayingCountdown > 0);
    }

    public int[] renderBattery(int[] origScreen) {
        // Suppress updating when displaying a text message
        if (batteryDisplayingCountdown > 0) {
            batteryDisplayingCountdown--;
            drawBatteryInternal(mLastBatteryType);
            return this.overlayScreen(origScreen);
        } else {
            return origScreen;
        }
    }

    private void setPixel(int x, int y, int r, int g, int b) {
        if (x < 0 || x >= board.boardWidth || y < 0 || y >= board.boardHeight) {
            BLog.d(TAG, "setPixel out of range: " + x + "," + y);
            return;
        }
        batteryScreen[board.pixelOffset.Map(x, y, board.PIXEL_RED)] = r;
        batteryScreen[board.pixelOffset.Map(x, y, board.PIXEL_GREEN)] = g;
        batteryScreen[board.pixelOffset.Map(x, y, board.PIXEL_BLUE)] = b;
    }

    private void setPixel(int x, int y, int color) {
        int b = (color & 0xff);
        int g = ((color & 0xff00) >> 8);
        int r = ((color & 0xff0000) >> 16);

        setPixel(x, y, r, g, b);
    }

    public final void clearPixels() {
        Arrays.fill(batteryScreen, 0);
    }

}

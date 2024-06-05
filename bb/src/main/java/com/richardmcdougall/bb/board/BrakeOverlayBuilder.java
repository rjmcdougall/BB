package com.richardmcdougall.bb.board;

import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bb.bms.BMS;
import com.richardmcdougall.bbcommon.BLog;
import com.richardmcdougall.bbcommon.BoardState;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;

public class BrakeOverlayBuilder {

    private String TAG = this.getClass().getSimpleName();

    private BBService service;

    private static final int kDisplayForMilliSeconds = 500;
    public int BrakeDisplayingCountdown = 0;
    private IntBuffer drawBuffer = null;
    private BurnerBoard board = null;
    public ArrayList<RGB> pixels = new ArrayList<>();
    private static final int kRgbMax = 255;

    private static final int brakeColor = RGB.getARGBInt(kRgbMax, 0, 0);

    private int[] BrakeScreen;


    BrakeOverlayBuilder(BBService service, BurnerBoard board) {
        this.service = service;
        this.board = board;

    }

    public void setBrake(boolean enable) {
        if (enable) {
            drawBrake();
        }
    }

    public void drawBrake() {

        int x, y;
        int brakeStart = 0;
        int brakeEnd = 10;

        BrakeScreen = new int[this.board.boardWidth * this.board.boardHeight * 3];
        BrakeDisplayingCountdown = kDisplayForMilliSeconds;

        try {
            for (y = brakeStart; y <= brakeEnd; y++) {
                for (x = 0; x < this.board.boardWidth; x++) {
                    setPixel(x , y, brakeColor);
                }
            }
        } catch (Exception e) {
            BLog.e(TAG, "Brake render error: " + e.getMessage());
        }
    }

    private int[] overlayScreen(int[] sourceScreen) {

        int[] layeredScreen = new int[this.board.boardWidth * this.board.boardHeight * 3];

        System.arraycopy(BrakeScreen, 0, layeredScreen, 0, BrakeScreen.length);

        for (int pixelNo = 0; pixelNo < (this.board.boardWidth * this.board.boardHeight); pixelNo++) {
            int pixel_offset = pixelNo * 3;

            RGB pixel = new RGB(layeredScreen[pixel_offset], layeredScreen[pixel_offset + 1], layeredScreen[pixel_offset + 2]);

            // Render the new text over the original
            if (pixel.isBlack()) {
                layeredScreen[pixel_offset] =  sourceScreen[pixel_offset];
                layeredScreen[pixel_offset + 1] = sourceScreen[pixel_offset + 1];
                layeredScreen[pixel_offset + 2] = sourceScreen[pixel_offset + 2];
            }
        }
        return layeredScreen;
    }

    public int[] renderBrake(int[] origScreen) {
        // Suppress updating when displaying a text message
        if (BrakeDisplayingCountdown > 0) {
            BrakeDisplayingCountdown--;
            drawBrake();
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
        BrakeScreen[board.pixelOffset.Map(x, y, board.PIXEL_RED)] = r;
        BrakeScreen[board.pixelOffset.Map(x, y, board.PIXEL_GREEN)] = g;
        BrakeScreen[board.pixelOffset.Map(x, y, board.PIXEL_BLUE)] = b;
    }

    private void setPixel(int x, int y, int color) {
        int b = (color & 0xff);
        int g = ((color & 0xff00) >> 8);
        int r = ((color & 0xff0000) >> 16);

        setPixel(x, y, r, g, b);
    }

    public final void clearPixels() {
        Arrays.fill(BrakeScreen, 0);
    }

}

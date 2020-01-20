package com.richardmcdougall.bb.visualization;

import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bb.BoardVisualization;
import com.richardmcdougall.bb.BurnerBoard;

/**
 * Created by rmc on 6/21/18.
 */

public class TheMan extends Visualization {

    private Wheel mWheel = new Wheel();

    public TheMan(BBService service) {
        super(service);
    }

    public static void drawTheMan(BurnerBoard bb, int color, int width) {
        int x;
        int row;

        int the_man[] = {
                0b0000000000,
                0b0000000000,
                0b0000000000,
                0b0000000000,
                0b0000000000,
                0b0000000000,
                0b0000000000,
                0b0000000000,
                0b0000000000,
                0b0000000000,
                0b0000000000,
                0b0000000000,
                0b0000000000,
                0b0000000000,
                0b0000000000,
                0b0000000000,
                0b0000000000,
                0b0000000000,
                0b1000000001,
                0b1100000011,
                0b1100000011,
                0b0110000110,
                0b0110000110,
                0b0110000110,
                0b0110000110,
                0b0011001100,
                0b0011001100,
                0b0011001100,
                0b0011001100,
                0b0011001100,
                0b0011001100,
                0b0001001000,
                0b0001001000,
                0b0001001000,
                0b0001001000,
                0b0001001000,
                0b0001001000,
                0b0011001100,
                0b0011001100,
                0b0011001100,
                0b0011001100,
                0b0011001100,
                0b0010000100,
                0b0110000110,
                0b0110000110,
                0b0110110110,
                0b1100110011,
                0b1101111011,
                0b1001111001,
                0b0000110000,
                0b0000000000,
                0b0000000000,
                0b0000000000,
                0b0000000000,
                0b0000000000,
                0b0000000000,
                0b0000000000,
                0b0000000000,
                0b0000000000,
                0b0000000000,
                0b0000000000,
                0b0000000000,
                0b0000000000,
                0b0000000000,
                0b0000000000,
                0b0000000000,
                0b0000000000,
                0b0000000000,
                0b0000000000,
                0b0000000000};

        //mBoardScreen.clear();
        for (row = 0; row < the_man.length; row++) {
            for (x = 0; x < 10; x++) {
                if ((the_man[row] & (1 << x)) > 0) {
                    bb.setPixel(x + (width / 2) - 5, row, color);
                }
            }
        }
        //service.burnerBoard.fillOtherlight(BurnerBoard.kLeftSightlight, color);
        //service.burnerBoard.fillOtherlight(BurnerBoard.kRightSidelight, color);
        //service.burnerBoard.flush();
    }

    // Thanks Pruss...
    // I see blondes, brunets, redheads...
    public void update(int mode) {
        int color;
        color = service.boardVisualization.mRandom.nextInt(4) % 2 == 0 ? BurnerBoard.getRGB(80, 80, 80) : mWheel.wheelState(); //Chance of 1/3rd
        mWheel.wheelInc(1);
        service.burnerBoard.clearPixels();
        drawTheMan(service.burnerBoard, color, mBoardWidth);
    }
}

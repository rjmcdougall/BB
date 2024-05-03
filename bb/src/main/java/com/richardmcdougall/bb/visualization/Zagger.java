package com.richardmcdougall.bb.visualization;

import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bb.TimeSync;
import com.richardmcdougall.bb.board.RGB;
import com.richardmcdougall.bbcommon.BoardState;

public class Zagger extends Visualization {

    private Wheel mWheel = new Wheel();

    public Zagger(BBService service) {

        super(service);
    }


    public void update(int mode) {
        int color;
        int y;
        int x;
        int pixelSkip = 2;

        y = service.burnerBoard.boardHeight - 1;
        for (x = 0; x < service.burnerBoard.boardWidth; x++) {
            service.burnerBoard.setPixel(x, y , 0);
        }

        x = service.burnerBoard.boardWidth / 2 - 1;
        for (y = 0; y < service.burnerBoard.boardHeight / pixelSkip; y += pixelSkip) {
            if (service.visualizationController.mRandom.nextInt(6) != 0) {
                color = RGB.getARGBInt(0, 0, 0);
            } else {
                color = mWheel.wheelState();
            }
            for (int p = 0; p < pixelSkip; p++) {
                service.burnerBoard.setPixel(x, pixelSkip * y + p, color);
                service.burnerBoard.setPixel(x + 1, pixelSkip * y + p, color);
            }
            // For Azul
            // mWheel.wheelInc(1);

        }
        // For Mezcal
        mWheel.wheelInc(10);

        service.burnerBoard.flush();
        service.burnerBoard.zagPixels();
        service.burnerBoard.scrollPixels(true);
    }
}
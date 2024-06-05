package com.richardmcdougall.bb.visualization;

import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bb.TimeSync;
import com.richardmcdougall.bb.board.RGB;
import com.richardmcdougall.bbcommon.BoardState;

public class Zagger extends Visualization {

    private Wheel mWheel = new Wheel();
    private int pixelSkip = 2;
    private int pixelSlow = 1;

    public Zagger(BBService service) {

        super(service);
        if (service.boardState.GetBoardType() == BoardState.BoardType.mezcal) {
            pixelSkip = 2;
            pixelSlow = 2;
        } else {
            pixelSkip = 2;
            pixelSlow = 0;
        }
    }

    private int iter = 0;
    public void update(int mode) {
        int color;
        int y;
        int x;

        iter++;

        // Reduce speed for mezcals, etc...
        if (pixelSlow > 0) {
            if ((iter % pixelSlow) == 0) {
                return;
            }
        }

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
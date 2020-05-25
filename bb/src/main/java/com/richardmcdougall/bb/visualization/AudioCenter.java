package com.richardmcdougall.bb.visualization;

import com.richardmcdougall.bb.BBService;

public class AudioCenter extends Visualization {

    private Wheel mWheel = new Wheel();

    public AudioCenter(BBService service) {
        super(service);
    }

    private void drawRectCenter(int size, int color) {
        if (size == 0)
            return;
        int xSize = (size + 3) / (1 + (service.burnerBoard.boardHeight / service.burnerBoard.boardWidth /2));
        int x1 = service.burnerBoard.boardWidth / 2 - 1; // 4
        int y1 = service.burnerBoard.boardHeight / 2 - 1;  // 34
        int xSizeLim = java.lang.Math.min(xSize, service.burnerBoard.boardWidth / 2);
        for (int x = x1 - (xSizeLim - 1); x <= x1 + xSizeLim; x++) { // 1: 4...5
            service.burnerBoard.setPixel(x, y1 - (size - 1), color); // 1: 34
            service.burnerBoard.setPixel(x, y1 + (size - 1) + 1, color); // 1: 35
        }
        for (int y = y1 - (size - 1); y <= y1 + size; y++) { // 1: 34..35
            if (xSize > (service.burnerBoard.boardWidth / 2))
                continue;
            service.burnerBoard.setPixel(x1 - (xSizeLim - 1), y, color); //
            service.burnerBoard.setPixel(x1 + (xSizeLim - 1) + 1, y, color);
        }
    }

    public void update(int mode) {

        int level;
        level = service.visualizationController.getLevel();
        service.burnerBoard.fadePixels(15);
        if (level > 110) {
            for (int x = 0; x < (service.burnerBoard.boardHeight / 2) + 1; x++) {
                int c = mWheel.wheelState();
                drawRectCenter(x, c);
                mWheel.wheelInc(4);
            }
        }
        service.burnerBoard.setOtherlightsAutomatically();
        service.burnerBoard.flush();
        return;
    }
}

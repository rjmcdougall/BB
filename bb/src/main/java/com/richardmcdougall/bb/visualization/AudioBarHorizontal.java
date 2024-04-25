package com.richardmcdougall.bb.visualization;

import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bb.board.RGB;

public class AudioBarHorizontal extends Visualization {

    public AudioBarHorizontal(BBService service) {
        super(service);
    }

    public void update(int mode) {

        int color;

        int[] dbLevels = service.visualizationController.getLevels128();
        if (dbLevels == null)
            return;
        service.burnerBoard.fadePixels(20);
        // Iterate through frequency bins: dbLevels[0] is lowest, [128] is highest
        int row = 0;
        for (int y = 1; y < service.burnerBoard.boardHeight; y++) {

            int bin = y / (128 / service.burnerBoard.boardHeight);
            // Get level as # of pixels board top
            int level = Math.min(dbLevels[bin] /
                    (255 / ((service.burnerBoard.boardWidth / 2)) - 1),
                    (service.burnerBoard.boardWidth / 2) - 1);
            //l("level " + value + ":" + level + ":" + dbLevels[value]);
            for (int x = 0; x < level; x++) {
                if (service.visualizationController.mRandom.nextInt(3) != 0) {
                    color = wheelColor();
                } else {
                    color = 0;
                }
                //color = vuColor(x);
                service.burnerBoard.setPixel(x + (
                        service.burnerBoard.boardWidth / 2), y,  color);
                service.burnerBoard.setPixel((
                        service.burnerBoard.boardWidth / 2) - x, y, color);

            }
        }
        service.burnerBoard.setOtherlightsAutomatically();
        service.burnerBoard.flush();
        service.burnerBoard.zagPixels();
        //service.burnerBoard.scrollPixels(true);
        mWheel.wheelInc(4);

        return;
    }

    // Pick classic VU meter colors based on volume
    int vuColor(int amount) {
        if (amount < (1 * service.burnerBoard.boardWidth / 8))
            return RGB.getARGBInt(0, 255, 0);
        if (amount < (2 * service.burnerBoard.boardWidth / 8))
            return RGB.getARGBInt(255, 255, 0);
        return RGB.getARGBInt(255, 0, 0);
    }

    private Wheel mWheel = new Wheel();
    private int mexcal_color = mWheel.wheelState();

    int wheelColor() {
        return mWheel.wheelState();
    }

}

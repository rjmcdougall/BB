package com.richardmcdougall.bb.visualization;

import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bb.board.RGB;

public class AudioBarHorizontal extends Visualization {

    public AudioBarHorizontal(BBService service) {
        super(service);
    }

    public void update(int mode) {

        int[] dbLevels = service.visualizationController.getLevels128();
        if (dbLevels == null)
            return;
        service.burnerBoard.fadePixels(80);
        // Iterate through frequency bins: dbLevels[0] is lowest, [128] is highest
        int row = 0;
        for (int y = 1; y < service.burnerBoard.boardHeight; y++) {

            int bin = y / (128 / service.burnerBoard.boardHeight);
            // Get level as # of pixels board top
            int level = Math.min(dbLevels[bin] /
                    (255 / (service.burnerBoard.boardWidth) - 1), service.burnerBoard.boardWidth - 1);
            //l("level " + value + ":" + level + ":" + dbLevels[value]);
            for (int x = 0; x < level; x++) {
                service.burnerBoard.setPixel(x, y, vuColor(x));
            }
        }
        service.burnerBoard.setOtherlightsAutomatically();
        service.burnerBoard.flush();
        return;
    }

    // Pick classic VU meter colors based on volume
    int vuColor(int amount) {
        if (amount < (2 * service.burnerBoard.boardWidth / 4))
            return RGB.getARGBInt(0, 255, 0);
        if (amount < (3 * service.burnerBoard.boardWidth / 4))
            return RGB.getARGBInt(255, 255, 0);
        return RGB.getARGBInt(255, 0, 0);
    }
}

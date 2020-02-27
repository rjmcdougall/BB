package com.richardmcdougall.bb.visualization;

import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bb.board.BurnerBoard;

public class AudioBar extends Visualization {

    public AudioBar(BBService service) {
        super(service);
    }

    public void update(int mode) {

        int[] dbLevels = service.boardVisualization.getLevels();
        if (dbLevels == null)
            return;
        service.burnerBoard.fadePixels(80);
        // Iterate through frequency bins: dbLevels[0] is lowest, [15] is highest
        int row = 0;
        for (int value = 3; value < 15; value += 2) {

            // Get level as # of pixels of half board height
            int level = java.lang.Math.min(dbLevels[value - 1] /
                    (255/(service.burnerBoard.boardHeight /2)-1), service.burnerBoard.boardHeight / 2);
            //l("level " + value + ":" + level + ":" + dbLevels[value]);
            for (int y = 0; y < level; y++) {
                if (value == 3) {
                    // Skip first frequency level
                } else {
                    int xOff = value / 2 * (service.burnerBoard.boardWidth / 10);
                    for (int i = 0; i < (service.burnerBoard.boardWidth / 10); i++) {
                        service.burnerBoard.setPixel((xOff - 2) + i, service.burnerBoard.boardHeight / 2 + y, vuColor(y));
                        service.burnerBoard.setPixel((xOff - 2) + i, service.burnerBoard.boardHeight / 2 - 1 - y, vuColor(y));
                        service.burnerBoard.setPixel(service.burnerBoard.boardWidth - 1 - (xOff - 2) - i, service.burnerBoard.boardHeight / 2 + y, vuColor(y));
                        service.burnerBoard.setPixel(service.burnerBoard.boardWidth - 1 - (xOff - 2) - i, service.burnerBoard.boardHeight / 2 - 1 - y, vuColor(y));
                    }
                }
            }
        }
        service.burnerBoard.setOtherlightsAutomatically();
        service.burnerBoard.flush();
        return;
    }

    // Pick classic VU meter colors based on volume
    int vuColor(int amount) {
        if (amount < service.burnerBoard.boardHeight / 6)
            return BurnerBoard.getRGB(0, 255, 0);
        if (amount < service.burnerBoard.boardHeight / 3)
            return BurnerBoard.getRGB(255, 255, 0);
        return BurnerBoard.getRGB(255, 0, 0);
    }
}

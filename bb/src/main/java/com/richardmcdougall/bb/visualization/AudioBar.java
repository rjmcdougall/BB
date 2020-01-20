package com.richardmcdougall.bb.visualization;

import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bb.BoardVisualization;
import com.richardmcdougall.bb.BurnerBoard;

/**
 * Created by rmc on 6/21/18.
 */

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
            //System.out.println("level " + dbLevels[value]);
            // Get level as # of pixels of half board height
            int level = java.lang.Math.min(dbLevels[value - 1] /
                    (255/(mBoardHeight/2)-1), mBoardHeight / 2);
            //l("level " + value + ":" + level + ":" + dbLevels[value]);
            for (int y = 0; y < level; y++) {
                if (value == 3) {
                    // Skip first frequency level
                    //service.burnerBoard.setSideLight(0, 39 + y, vuColor(y));
                    //service.burnerBoard.setSideLight(0, 38 - y, vuColor(y));
                    //service.burnerBoard.setSideLight(1, 39 + y, vuColor(y));
                    //service.burnerBoard.setSideLight(1, 38 - y, vuColor(y));
                } else {
                    int xOff = value / 2 * (mBoardWidth / 10);
                    for (int i = 0; i < (mBoardWidth / 10); i++) {
                        service.burnerBoard.setPixel((xOff - 2) + i, mBoardHeight / 2 + y, vuColor(y));
                        service.burnerBoard.setPixel((xOff - 2) + i, mBoardHeight / 2 - 1 - y, vuColor(y));
                        service.burnerBoard.setPixel(mBoardWidth - 1 - (xOff - 2) - i, mBoardHeight / 2 + y, vuColor(y));
                        service.burnerBoard.setPixel(mBoardWidth - 1 - (xOff - 2) - i, mBoardHeight / 2 - 1 - y, vuColor(y));
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
        if (amount < mBoardHeight / 6)
            return BurnerBoard.getRGB(0, 255, 0);
        if (amount < mBoardHeight / 3)
            return BurnerBoard.getRGB(255, 255, 0);
        return BurnerBoard.getRGB(255, 0, 0);
    }
}

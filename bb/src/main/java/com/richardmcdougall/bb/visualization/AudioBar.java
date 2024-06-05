package com.richardmcdougall.bb.visualization;

import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bb.board.RGB;
import com.richardmcdougall.bbcommon.BoardState;

public class AudioBar extends Visualization {

    private int fadeAmount;

    public AudioBar(BBService service) {
        super(service);
        if (service.boardState.GetBoardType() == BoardState.BoardType.mezcal
                || service.boardState.GetBoardType() == BoardState.BoardType.azul) {
            fadeAmount = 20;
        } else {
            fadeAmount = 80;
        }
    }

    public void update(int mode) {

        int[] dbLevels = service.audioVisualizer.getLevels();
        if (dbLevels == null)
            return;
        service.burnerBoard.fadePixels(fadeAmount);
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
            return RGB.getARGBInt(0, 255, 0);
        if (amount < service.burnerBoard.boardHeight / 3)
            return RGB.getARGBInt(255, 255, 0);
        return RGB.getARGBInt(255, 0, 0);
    }
}

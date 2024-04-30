package com.richardmcdougall.bb.visualization;

import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bb.board.RGB;
import com.richardmcdougall.bbcommon.BLog;

public class AudioBarHorizontal extends Visualization {

    public static final int kStaticVUColor = 1;
    public static final int kStaticColor = 2;
    public static final int kZag = 3;
    private String TAG = this.getClass().getSimpleName();


    public AudioBarHorizontal(BBService service) {
        super(service);
    }

    public void update(int mode) {

        int color;

        int[] dbLevels = service.audioVisualizer.getLevels128();
        if (dbLevels == null)
            return;
        if (mode == kZag) {
            service.burnerBoard.fadePixels(20);
        } else {
            //service.burnerBoard.fadePixels(120);
            service.burnerBoard.fadePixels(50);
        }
        // Iterate through frequency bins: dbLevels[0] is lowest, [128] is highest
        int row = 0;
        for (int y = 1; y < service.burnerBoard.boardHeight; y++) {

            // Skip the first few frequency bins
            // Only use 64 out of 128 to get the more significant dynamic portion
            //int bin = 10 + Math.min(64, (int)((float)y / (((float)service.burnerBoard.boardHeight) / 64.0)));
            int bin = Math.min(128, (int)((float)y / (((float)service.burnerBoard.boardHeight) / 128.0)));
            // Get level as # of pixels board top
            int level = Math.min(dbLevels[bin] /
                            (255 / ((service.burnerBoard.boardWidth / 2)) - 1),
                    (service.burnerBoard.boardWidth / 2) - 1);
            //BLog.d(TAG, "bin: " + bin + ", db: " + dbLevels[bin] + ", level: " + level);
            for (int x = 0; x < level; x++) {
                if (mode == kStaticVUColor) {
                    color = vuColor(x);
                } else {
                    if (true || service.visualizationController.mRandom.nextInt(3) != 0) {
                        color = wheelColor();
                    } else {
                        color = 0;
                    }
                }

                service.burnerBoard.setPixel(x + (
                        service.burnerBoard.boardWidth / 2), y, color);
                service.burnerBoard.setPixel((
                        service.burnerBoard.boardWidth / 2) - x, y, color);
            }
        }
        service.burnerBoard.setOtherlightsAutomatically();
        service.burnerBoard.flush();
        if (mode == kZag) {
            service.burnerBoard.zagPixels();
            service.burnerBoard.scrollPixels(true);
        } else {
            //service.burnerBoard.scrollPixels(true);
        }
        mWheel.wheelInc(4);

        return;
    }

    // Pick classic VU meter colors based on volume
    int vuColor(int amount) {
        if (amount < (2 * service.burnerBoard.boardWidth / 16))
            return RGB.getARGBInt(0, 255, 0);
        if (amount < (3 * service.burnerBoard.boardWidth / 16))
            return RGB.getARGBInt(255, 255, 0);
        return RGB.getARGBInt(255, 0, 0);
    }

    private Wheel mWheel = new Wheel();
    private int mexcal_color = mWheel.wheelState();

    int wheelColor() {
        return mWheel.wheelState();
    }

}

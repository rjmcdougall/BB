package com.richardmcdougall.bb.visualization;

import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bb.TimeSync;
import com.richardmcdougall.bb.board.RGB;

/**
 * Created by rmc on 6/21/18.
 */

public class MatrixTest extends Visualization {

    public MatrixTest(BBService service) {
        super(service);
    }

    private Wheel mWheel = new Wheel();

    public static final int kMatrixBurnerColor = 1;
    public static final int kMatrixLunarian = 3;
    public static final int kMatrixSync = 10;

    private int fireColor = 40;

    public void update(int mode) {

        int color;
        int x;
        int y;
        int pixelSkip;

        pixelSkip = 1;

        y = service.burnerBoard.boardHeight - 1;

        // start
        x = 0;
        color = mWheel.wheelState();
        mWheel.wheelInc(1);
        service.burnerBoard.setPixel(x, y, color);

        // middle
        x = (service.burnerBoard.boardWidth) / 2;
        color = mWheel.wheelState();
        mWheel.wheelInc(1);
        service.burnerBoard.setPixel(x, y, color);

        // end
        x = service.burnerBoard.boardWidth - 1;
        color = mWheel.wheelState();
        mWheel.wheelInc(1);
        service.burnerBoard.setPixel(x, y, color);

        service.burnerBoard.scrollPixels(true);
        service.burnerBoard.flush();

        return;

    }
}

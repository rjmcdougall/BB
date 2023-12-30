package com.richardmcdougall.bb.visualization;

import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bb.board.RGB;
import com.richardmcdougall.bbcommon.BLog;

import java.util.ArrayList;
import java.util.Timer;

public class PowerTest extends Visualization {
    private String TAG = this.getClass().getSimpleName();

    private int framesToHold = 0;
    private int frameCounter = 0;
    private int colorInc = 0;
    ArrayList<RGB> colorArray = new ArrayList<>();

    public PowerTest(BBService service) {
        super(service);

        framesToHold = this.service.burnerBoard.getFrameRate() * 5;


        for (int i = 1; i <= 256; i = i * 2) {
            colorArray.add(new RGB(i - 1, 0, 0));
        }
        for (int i = 1; i <= 256; i = i * 2) {
            colorArray.add(new RGB(0, i - 1, 0));
        }
        for (int i = 1; i <= 256; i = i * 2) {
            colorArray.add(new RGB(0, 0, i - 1));
        }

    }


    public void update(int mode) {

        frameCounter++;

        if (frameCounter % framesToHold == 0) {
            colorInc++;
            BLog.d(TAG, "Color " + colorArray.get(colorInc).r + ":" + colorArray.get(colorInc).g + ":" + colorArray.get(colorInc).b + " - " + colorArray.get(colorInc).getARGBInt());
        }

        for (int x = 0; x < service.burnerBoard.boardWidth; x++) {
            for (int y = 0; y < service.burnerBoard.boardHeight / (colorArray.get(colorInc).getARGBInt() == 0 ? 1 : 4); y++) {
                service.burnerBoard.setPixel(x, y, colorArray.get(colorInc).getARGBInt());
            }
        }
        service.burnerBoard.flush();

    }
}

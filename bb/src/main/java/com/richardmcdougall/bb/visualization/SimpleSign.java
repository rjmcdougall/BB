package com.richardmcdougall.bb.visualization;

import android.graphics.Color;

import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bb.board.BurnerBoard;

/**
 * Created by rmc on 6/21/18.
 */

public class SimpleSign extends Visualization {

    private Wheel mWheel = new Wheel();

    public SimpleSign(BBService service) {
        super(service);

    }


    public void update(int mode) {

        service.burnerBoard.setText90(signText, 1000, foregroundColor);

        service.burnerBoard.fillScreen(Color.red(backgroundColor),Color.green(backgroundColor),Color.blue(backgroundColor));

        service.burnerBoard.flush();

    }

}

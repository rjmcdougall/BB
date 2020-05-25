package com.richardmcdougall.bb.visualization;

import com.richardmcdougall.bb.BBService;

/**
 * Created by rmc on 6/21/18.
 */

public class PixelMapTest extends Visualization {

    public PixelMapTest(BBService service) {
        super(service);

    }
    
    public void update(int mode) {

        service.burnerBoard.lineBuilder.drawLine();
        service.burnerBoard.flush();

    }

}

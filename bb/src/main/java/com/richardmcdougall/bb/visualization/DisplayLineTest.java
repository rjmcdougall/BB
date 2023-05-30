package com.richardmcdougall.bb.visualization;

import com.richardmcdougall.bb.BBService;

/**
 * Created by rmc on 6/21/18.
 */

public class DisplayLineTest extends Visualization {

    public DisplayLineTest(BBService service) {
        super(service);

    }


    public void update(int mode) {

        boolean isEven = this.service.displayMapManager2.boardWidth % 2 == 0 ? true : false;

        for(int y = 0; y<this.service.displayMapManager2.boardHeight;y++ ){
            if(!isEven) {
                service.burnerBoard.setPixel((this.service.displayMapManager2.boardWidth + 1) / 2,y,0,255,0);
                service.burnerBoard.setPixel((this.service.displayMapManager2.boardWidth - 1) / 2,y,0,255,0);
            }
            else {
                service.burnerBoard.setPixel((this.service.displayMapManager2.boardWidth) / 2,y,0,255,0);
            }
        }

        service.burnerBoard.flush();
    }

}

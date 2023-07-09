package com.richardmcdougall.bb.visualization;

import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bb.DisplayMapManager2;

/**
 * Created by rmc on 6/21/18.
 */

public class DisplayRowTest extends Visualization {

    public DisplayRowTest(BBService service) {
        super(service);

    }

    public void update(int mode) {

        for(int y=0; y < this.service.burnerBoard.boardHeight;y++){
             for(int x = 0; x < this.service.burnerBoard.boardWidth; x++){
                switch(y % 5) {
                    case 0:
                        service.burnerBoard.setPixel(x,y,255,0,0);
                        break;
                    case 1:
                        service.burnerBoard.setPixel(x,y,0,255,255);
                        break;
                    case 2:
                        service.burnerBoard.setPixel(x,y,0,0,255);
                        break;
                    case 3:
                        service.burnerBoard.setPixel(x,y,0,255,0);
                        break;
                    case 4:
                        service.burnerBoard.setPixel(x,y,255,255,0);
                        break;
                }
            }
        }

        boolean isEven = this.service.displayMapManager.boardWidth % 2 == 0 ? true : false;

        for(int y = 0; y<this.service.displayMapManager.boardHeight;y++ ){
            if(!isEven) {
                service.burnerBoard.setPixel((this.service.displayMapManager.boardWidth + 1) / 2,y,255,255,255);
                service.burnerBoard.setPixel((this.service.displayMapManager.boardWidth - 1) / 2,y,255,255,255);
            }
            else {
                service.burnerBoard.setPixel((this.service.displayMapManager.boardWidth) / 2,y,255,255,255);
            }
        }
        service.burnerBoard.flush();
    }

}

package com.richardmcdougall.bb.visualization;

import com.richardmcdougall.bb.BBService;

/**
 * Created by rmc on 6/21/18.
 */

public class DisplaySectionTest extends Visualization {

    public DisplaySectionTest(BBService service) {
        super(service);
    }

    public void update(int mode) {
        for(int y = 0; y<this.service.displayMapManager2.boardHeight;y++ ){
            for(int x = 0; x<this.service.displayMapManager2.boardWidth;x++ ){

                  int section = this.service.displayMapManager2.twoDimensionalSectionMap.get(y).get(x);
                  switch(section) {
                      case 0:
                          break;
                      case 1:
                          service.burnerBoard.setPixel(x, y, 255, 0, 0);
                          break;
                      case 2:
                          service.burnerBoard.setPixel(x, y, 0, 255, 0);
                          break;
                      case 3:
                          service.burnerBoard.setPixel(x, y, 0, 0, 255);
                          break;
                      case 4:
                          service.burnerBoard.setPixel(x, y, 255, 255, 0);
                          break;
                      default:
                          break;
                  }
            }
        }

        service.burnerBoard.flush();
    }
}

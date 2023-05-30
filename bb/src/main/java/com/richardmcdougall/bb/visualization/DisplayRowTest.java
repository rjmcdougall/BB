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

        for (DisplayMapManager2.DisplayMapRow row : this.service.displayMapManager2.displayMap) {
            for(int x = 0; x < row.rowLength(); x++){
                switch(row.row % 5) {
                    case 0:
                        service.burnerBoard.setPixel(x,row.row,255,0,0);
                        break;
                    case 1:
                        service.burnerBoard.setPixel(x,row.row,255,255,255);
                        break;
                    case 2:
                        service.burnerBoard.setPixel(x,row.row,0,0,255);
                        break;
                    case 3:
                        service.burnerBoard.setPixel(x,row.row,0,255,0);
                        break;
                    case 4:
                        service.burnerBoard.setPixel(x,row.row,255,255,0);
                        break;
                }
            }
        }
        service.burnerBoard.flush();
    }

}

package com.richardmcdougall.bb.board;

import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bb.DisplayMapManager2;

public class PixelColorSections {

    BBService service = null;

    public PixelColorSections(BBService service){
        this.service = service;
    }
    public int[] ColorSections(int[] boardScreen){

        for (int offset = 0; offset < this.service.displayMapManager2.XYOverrideMap.size() * 3; ) {
            if (this.service.displayMapManager2.XYOverrideMap.get(offset / 3) == 3){
                boardScreen[offset] = 0;
                offset++;
                boardScreen[offset] = 0;
                offset++;
                boardScreen[offset] = 0;
                offset++;
            }
            else {
                offset++;
                offset++;
                offset++;
            }
        }

        return boardScreen;
    }

}

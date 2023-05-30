package com.richardmcdougall.bb.board;

import com.richardmcdougall.bb.BBService;

public class PixelBlackoutSections {

    BBService service = null;

    public PixelBlackoutSections(BBService service){
        this.service = service;
    }
    public int[] ColorSections(int[] boardScreen){

        String mode = String.valueOf(this.service.boardState.displayMode);


        for (int offset = 0; offset < this.service.displayMapManager2.oneDimensionalSectionMap.size() * 3; ) {
            if (mode.contains(String.valueOf(this.service.displayMapManager2.oneDimensionalSectionMap.get(offset / 3)))){
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

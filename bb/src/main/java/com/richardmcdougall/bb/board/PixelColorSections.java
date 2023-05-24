package com.richardmcdougall.bb.board;

import com.richardmcdougall.bb.DisplayMapManager2;

public class PixelColorSections {

    public int[] ColorSections(int[] boardScreen, DisplayMapManager2 displayMapManager2){

        for (int offset = 0; offset < displayMapManager2.XYOverrideMap.size() * 3; ) {
            if (displayMapManager2.XYOverrideMap.get(offset / 3) == 3){
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

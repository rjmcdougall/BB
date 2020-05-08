package com.richardmcdougall.bb.board;

import com.richardmcdougall.bb.BBService;

import java.util.Map;
import java.util.TreeMap;

public class BoardDisplay {

    public TreeMap<Integer, RGB> map = new TreeMap<>();
    private BBService service;
    private int[] boardScreen = null;
    private int boardWidth;
    private int boardHeight;

    public BoardDisplay(BBService service, int boardWidth, int boardHeight){
        this.service = service;
        this.boardHeight = boardHeight;
        this.boardWidth = boardWidth;
        boardScreen = new int[boardWidth * boardHeight * 3];
        init();
    }

    private void init() {
        for (int x = 0; x < boardWidth; x++) {
            for (int y = 0; y < boardHeight; y++) {
                this.map.put(x*boardWidth+y, new RGB(0,0,0));
            }
        }
    }

    public void Clear(){
        init();
    }

    public int[] GetArray() {
        for (Map.Entry<Integer, RGB> entry : map.entrySet()) {
            boardScreen[entry.getKey()] = entry.getValue().r;
            boardScreen[entry.getKey() + 1] = entry.getValue().g;
            boardScreen[entry.getKey() + 2] = entry.getValue().b;
        }
        return boardScreen;
    }

    public void CreateFromArray(int[] boardScreen){
        for(int i=0; i<boardScreen.length;  i+=3){
            RGB rgb = new RGB(boardScreen[i], boardScreen[i+1], boardScreen[i+2]);
            map.put(i, rgb);
        }
    }
}




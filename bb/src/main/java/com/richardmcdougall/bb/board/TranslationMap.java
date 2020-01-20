package com.richardmcdougall.bb.board;


public class TranslationMap {
    int y;
    int startX;
    int endX;
    int stripDirection;
    int stripNumber;
    int stripOffset;

    public TranslationMap(
            int y,
            int startX,
            int endX,
            int stripDirection,
            int stripNumber,
            int stripOffset) {
        this.y = y;
        this.startX = startX;
        this.endX = endX;
        this.stripDirection = stripDirection;
        this.stripNumber = stripNumber;
        this.stripOffset = stripOffset;
    }
}

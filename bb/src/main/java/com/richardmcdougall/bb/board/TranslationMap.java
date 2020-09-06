package com.richardmcdougall.bb.board;


public class TranslationMap {
    int y;
    int startX;
    int endX;
    int x;
    int startY;
    int endY;
    int stripDirection;
    int stripNumber;
    int stripOffset;

    public TranslationMap(
            int xy,
            int startXY,
            int endXY,
            int stripDirection,
            int stripNumber,
            int stripOffset) {
        // I really want some sort of union here to make the code easier to read (re X or Y)
        this.y = xy;
        this.startX = startXY;
        this.endX = endXY;
        this.x = xy;
        this.startY = startXY;
        this.endY = endXY;
        this.stripDirection = stripDirection;
        this.stripNumber = stripNumber;
        this.stripOffset = stripOffset;
    }
}

package com.richardmcdougall.bb.board;


import java.lang.reflect.Field;

public class TranslationMap {
    int y;
    int startX;

    int edgeLowX;

    int edgeHighX;

    int endX;
    int x;

    int startY;

    int edgeLowY;

    int edgeHighY;
    int endY;
    int stripDirection;
    int stripNumber;
    int stripOffset;

    public TranslationMap(
            int xy,
            int startXY,
            int edgeLowXY,

    int edgeHighXY,

            int endXY,
            int stripDirection,
            int stripNumber,
            int stripOffset) {
        // I really want some sort of union here to make the code easier to read (re X or Y)
        this.y = xy;
        this.startX = startXY;
        this.edgeLowX = edgeLowXY;
        this.edgeHighX = edgeHighXY;
        this.endX = endXY;
        this.x = xy;
        this.startY = startXY;
        this.edgeLowY = edgeLowXY;
        this.edgeHighY = edgeHighXY;
        this.endY = endXY;
        this.stripDirection = stripDirection;
        this.stripNumber = stripNumber;
        this.stripOffset = stripOffset;
    }

    @Override
    public String toString() {
        String s = "";
        s += "xy " + this.x + " ";
        s += "stripNumber " + this.stripNumber + " ";
        s += "startXY " + this.startX + " ";
        s += "edge1LowXY " + this.edgeLowY + " ";
        s += "edge1HighXY " + this.edgeHighY + " ";
        s += "endXY " + this.endX + " ";
        s += "stripOffset " + this.stripOffset + " ";

        return s;
    }
}

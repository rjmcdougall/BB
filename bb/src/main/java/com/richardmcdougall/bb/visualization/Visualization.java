package com.richardmcdougall.bb.visualization;

import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bb.board.RGB;

public class Visualization {

    public static final int kDefault = 0;
    BBService service;
    protected String signText = "";
    protected RGB foregroundColor;
    protected RGB backgroundColor;

    public Visualization(BBService service) {
        this.service = service;
    }

    public void update(int mode) {
    }

    public void setText(String signText, String foregroundColor, String backgroundColor){
        this.signText = signText;

        RGBList c = new RGBList();
        this.foregroundColor = c.getColor(foregroundColor);
        this.backgroundColor = c.getColor(backgroundColor);

    }

    public void setMode(int mode) {
    }

}
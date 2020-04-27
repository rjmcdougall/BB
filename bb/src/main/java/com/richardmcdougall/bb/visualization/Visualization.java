package com.richardmcdougall.bb.visualization;

import android.graphics.Color;
import com.richardmcdougall.bb.BBService;

public class Visualization {

    public static final int kDefault = 0;
    BBService service;
    protected String signText = "";
    protected BBColor.ColorName foregroundColor;
    protected BBColor.ColorName backgroundColor;

    public Visualization(BBService service) {
        this.service = service;
    }

    public void update(int mode) {
    }

    public void setText(String signText, String foregroundColor, String backgroundColor){
        this.signText = signText;

        BBColor c = new BBColor();
        this.foregroundColor = c.getColor(foregroundColor);
        this.backgroundColor = c.getColor(backgroundColor);

    }

    public void setMode(int mode) {
    }

}
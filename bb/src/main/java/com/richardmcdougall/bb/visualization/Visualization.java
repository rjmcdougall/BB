package com.richardmcdougall.bb.visualization;

import android.graphics.Color;
import com.richardmcdougall.bb.BBService;

public class Visualization {

    public static final int kDefault = 0;
    BBService service;
    protected String signText = "";
    protected int foregroundColor = 0;
    protected int backgroundColor = 0;

    public Visualization(BBService service) {
        this.service = service;
    }

    public void update(int mode) {
    }

    public void setText(String signText, String foregroundColor, String backgroundColor){
        this.signText = signText;

        BBColor c = new BBColor();
        BBColor.ColorName f = c.getColor(foregroundColor);
        BBColor.ColorName b = c.getColor(backgroundColor);

        this.foregroundColor = Color.rgb(f.r, f.g,f.b); // why is this  backwards!!!!
        this.backgroundColor = Color.rgb(f.r, f.g,f.b );

    }

    public void setMode(int mode) {
    }

}
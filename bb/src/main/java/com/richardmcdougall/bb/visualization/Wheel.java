package com.richardmcdougall.bb.visualization;

import android.graphics.Color;
public class Wheel {

    int wheel_color = 0;

    int wheel(int wheelPos) {
        float[] hsl = new float[3];
        hsl[0] = wheelPos * 1.0f;
        hsl[1] = 1.0f;
        hsl[2] = 0.5f;
        return android.graphics.Color.HSVToColor(hsl) & 0xFFFFFF;
    }

    int wheelState() {
        return (wheel(wheel_color));
    }

    // Same but with brightness 0-1.0
    int wheelDim(int wheelPos, float brightness) {
        float[] hsl = new float[3];
        hsl[0] = wheelPos * 1.0f;
        hsl[1] = 1.0f;
        hsl[2] = 0.5f * brightness;
        return android.graphics.Color.HSVToColor(hsl) & 0xFFFFFF;
    }

    void wheelInc(int amount) {
        wheel_color = wheel_color + amount;
        if (wheel_color > 360)
            wheel_color = 0;
    }

}

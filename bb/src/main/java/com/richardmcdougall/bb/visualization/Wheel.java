package com.richardmcdougall.bb.visualization;

/**
 * Created by rmc on 6/21/18.
 */

public class Wheel {

    int wheel_color = 0;

    // Input a value 0 to 255 to get a color value.
    // The colours are a transition r - g -b - back to r
    /*
    int wheel(int WheelPos) {
        if (WheelPos < 85) {
            return BurnerBoard.getRGB(255 - WheelPos * 3, 0, WheelPos * 3);
        } else if (WheelPos < 170) {
            WheelPos -= 85;
            return BurnerBoard.getRGB(0, WheelPos * 3, 255 - WheelPos * 3);
        } else {
            WheelPos -= 170;
            return BurnerBoard.getRGB(WheelPos * 3, 255 - WheelPos * 3, 0);
        }
    }
    */

    int wheel(int wheelPos) {
        float[] hsl = new float[3];
        hsl[0] = wheelPos * 1.0f;
        hsl[1] = 1.0f;
        hsl[2] = 0.5f;
        return android.support.v4.graphics.ColorUtils.HSLToColor(hsl) & 0xFFFFFF;
    }

    int wheelState() {
        return(wheel(wheel_color));
    }

    // Same but with brightness 0-1.0
    int wheelDim(int wheelPos, float brightness) {
        float[] hsl = new float[3];
        hsl[0] = wheelPos * 1.0f;
        hsl[1] = 1.0f;
        hsl[2] = 0.5f * brightness;
        return android.support.v4.graphics.ColorUtils.HSLToColor(hsl) & 0xFFFFFF;
    }

    void wheelInc(int amount) {
        wheel_color = wheel_color + amount;
        if (wheel_color > 360)
            wheel_color = 0;
    }

}

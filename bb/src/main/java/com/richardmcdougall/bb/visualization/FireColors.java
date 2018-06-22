package com.richardmcdougall.bb.visualization;

/**
 * Created by rmc on 6/21/18.
 */

public class FireColors {

    public static int [] [] colors() {

        int[][] mFireColors = new int[256][3];

        for (int i = 0; i < 32; ++i) {
            /* black to blue, 32 values*/
            mFireColors[i][2] = i << 1;

            /* blue to red, 32 values*/
            mFireColors[i + 32][0] = i << 3;
            mFireColors[i + 32][2] = 64 - (i << 1);

            /*red to yellow, 32 values*/
            mFireColors[i + 64][0] = 255;
            mFireColors[i + 64][1] = i << 3;

            /* yellow to white, 162 */
            mFireColors[i + 96][0] = 255;
            mFireColors[i + 96][1] = 255;
            mFireColors[i + 96][2] = i << 2;
            mFireColors[i + 128][0] = 255;
            mFireColors[i + 128][1] = 255;
            mFireColors[i + 128][2] = 64 + (i << 2);
            mFireColors[i + 160][0] = 255;
            mFireColors[i + 160][1] = 255;
            mFireColors[i + 160][2] = 128 + (i << 2);
            mFireColors[i + 192][0] = 255;
            mFireColors[i + 192][1] = 255;
            mFireColors[i + 192][2] = 192 + i;
            mFireColors[i + 224][0] = 255;
            mFireColors[i + 224][1] = 255;
            mFireColors[i + 224][2] = 224 + i;
        }
        return mFireColors;
    }

}

package com.richardmcdougall.bb;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

/**
 * Created by rmc on 10/30/16.
 */

public class BurnerBoardRenderer extends Renderer {
    private int mDivisions;
    private MainActivity mActivity;

    /**
     * Renders the FFT data as a series of lines, in histogram form
     *
     * @param divisions - must be a power of 2. Controls how many lines to draw
     */
    public BurnerBoardRenderer(int divisions, MainActivity activity) {
        super();
        mDivisions = divisions;
        mActivity = activity;
    }

    @Override
    public void onRender(Canvas canvas, AudioData data, Rect rect) {
        // Do nothing, we only display FFT data
    }

    @Override
    public void onRender(Canvas canvas, FFTData data, Rect rect) {

        mActivity.mBoardFFT = data.bytes.clone();
    }
}
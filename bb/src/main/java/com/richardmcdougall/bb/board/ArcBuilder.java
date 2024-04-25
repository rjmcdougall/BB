package com.richardmcdougall.bb.board;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.richardmcdougall.bb.BBService;

import java.nio.IntBuffer;
import java.util.ArrayList;
import com.richardmcdougall.bbcommon.BLog;

public class ArcBuilder {

    private String TAG = this.getClass().getSimpleName();
    private IntBuffer drawBuffer = null;
    private BurnerBoard board = null;
    public ArrayList<RGB> pixels = new ArrayList<>();

    public ArcBuilder(BurnerBoard board) {
        this.board = board;
        this.drawBuffer = IntBuffer.allocate(board.boardWidth * board.boardHeight * 4);
    }

    public void drawArc(float left, float top, float right, float bottom,
                        float startAngle, float sweepAngle, boolean useCenter,
                        boolean fill, RGB color) {

        Canvas canvas = new Canvas();
        Bitmap bitmap = Bitmap.createBitmap(this.board.boardWidth, this.board.boardHeight, Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        canvas.scale(-1, -1, this.board.boardWidth / 2, this.board.boardHeight / 2);
        Paint arcPaint = new Paint();
        arcPaint.setColor(Color.rgb(color.r, color.g, color.b)); //  Color
        arcPaint.setStrokeWidth(1);
        if (fill) {
            arcPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        } else {
            arcPaint.setStyle(Paint.Style.STROKE);
        }

        canvas.drawArc(left, top, right, bottom, startAngle, sweepAngle, useCenter, arcPaint);

        if (drawBuffer != null) {
            drawBuffer.rewind();
            bitmap.copyPixelsToBuffer(drawBuffer);
        }

        int[] temp = drawBuffer.array();
        pixels.clear();
        for (int i = 0; i < temp.length; i++) {
            pixels.add(RGB.fromRGBAInt(temp[i]));
        }

        for (int pixelNo = 0; pixelNo < (this.board.boardWidth * this.board.boardHeight); pixelNo++) {
            int pixel_offset = pixelNo * 3;
            RGB pixel = this.pixels.get(pixelNo);

            // Render the new text over the original
            if (!pixel.isBlack()) {
                this.board.boardScreen[pixel_offset] = pixel.r;
                this.board.boardScreen[pixel_offset + 1] = pixel.g;
                this.board.boardScreen[pixel_offset + 2] = pixel.b;
            }
        }

    }
}

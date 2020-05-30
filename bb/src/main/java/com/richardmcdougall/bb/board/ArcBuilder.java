package com.richardmcdougall.bb.board;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

import com.richardmcdougall.bb.BBService;

import java.nio.IntBuffer;
import java.util.ArrayList;

import static com.richardmcdougall.bb.board.BurnerBoard.boardHeight;

public class ArcBuilder {

    private String TAG = this.getClass().getSimpleName();
    private IntBuffer drawBuffer = null;
    private BurnerBoard board = null;
    private BBService service = null;
    public ArrayList<RGB> pixels = new ArrayList<>();

    public ArcBuilder(BBService service, BurnerBoard board){
        this.board = board;
        this.service = service;
        this.drawBuffer = IntBuffer.allocate(board.boardWidth * boardHeight * 4);
    }

    public void drawArc(float left, float top, float right, float bottom,
                        float startAngle, float sweepAngle, boolean useCenter,
                        boolean fill, int color) {

        Canvas canvas = new Canvas();
        Bitmap bitmap = Bitmap.createBitmap(this.board.boardWidth, this.board.boardHeight, Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        canvas.scale(-1, -1, this.board.boardWidth / 2, this.board.boardHeight / 2);
        Paint arcPaint = new Paint();
        arcPaint.setColor(color); //  Color
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
        for(int i = 0; i < temp.length;i++){
            pixels.add(RGB.fromRGBAInt(temp[i]));
        }

    }
}

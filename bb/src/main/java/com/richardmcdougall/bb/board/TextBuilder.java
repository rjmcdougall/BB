package com.richardmcdougall.bb.board;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.TextPaint;

import java.nio.IntBuffer;

public class TextBuilder {

    public IntBuffer mTextBuffer = null;
    private int mTextSizeHorizontal = 12;
    private int mTextSizeVerical = 12;
    public int isTextDisplaying = 0;
    private int boardWidth = 0;
    private int boardHeight = 0;

    public TextBuilder(int boardWidth, int boardHeight, int textSizeHorizontal, int textSizeVertical){
        this.boardHeight = boardHeight;
        this.boardWidth = boardWidth;
        this.mTextBuffer = IntBuffer.allocate(boardWidth * boardHeight * 4);
        this.mTextSizeHorizontal = textSizeHorizontal;
        this.mTextSizeVerical = textSizeVertical;
    }

    // Draw text on screen and delay for n seconds
    public void setText(String text, int delay, int refreshRate) {
        isTextDisplaying = delay * refreshRate / 1000;

        if (boardWidth < 15) {
            setText90(text, delay, refreshRate);
            return;
        }

        Canvas canvas = new Canvas();
        Bitmap bitmap = Bitmap.createBitmap(boardWidth, boardHeight, Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        canvas.scale(-1, -1, boardWidth / 2, boardHeight / 2);
        Paint textPaint = new TextPaint();
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.WHITE); // Text Color
        textPaint.setTypeface(Typeface.create("Courier", Typeface.BOLD));
        textPaint.setTextSize(mTextSizeVerical); // Text Size
        canvas.drawText(text.substring(0, Math.min(text.length(), 4)),
                (boardWidth / 2), 30, textPaint);//boardHeight / 2 + (mTextSizeVerical / 3), textPaint);
        if (mTextBuffer != null) {
            mTextBuffer.rewind();
            bitmap.copyPixelsToBuffer(mTextBuffer);
        }
    }

    // Draw text on screen and delay for n seconds
    public void setText90(String text, int delay, int refreshRate) {
        isTextDisplaying = delay * refreshRate / 1000;

        Canvas canvas = new Canvas();
        Bitmap bitmap = Bitmap.createBitmap(boardWidth, boardHeight, Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        canvas.scale(-1, -1, boardWidth / 2, boardHeight / 2);
        canvas.rotate(90, boardWidth / 2, boardHeight / 2);
        Paint textPaint = new TextPaint();
        textPaint.setDither(true);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.WHITE); // Text Color
        textPaint.setTextSize(mTextSizeHorizontal); // Text Size
        //paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)); // Text Overlapping Pattern
        canvas.drawText(text, (boardWidth / 2), boardHeight / 2 + (mTextSizeHorizontal / 3), textPaint);
        if (mTextBuffer != null) {
            mTextBuffer.rewind();
            bitmap.copyPixelsToBuffer(mTextBuffer);
        }
    }

}

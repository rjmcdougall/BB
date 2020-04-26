package com.richardmcdougall.bb.board;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.TextPaint;

import com.richardmcdougall.bb.visualization.BBColor;

import java.nio.IntBuffer;

public class TextBuilder {

    public IntBuffer textBuffer = null;
    private int textSizeHorizontal = 12;
    private int textSizeVerical = 12;
    public int textDisplayingCountdown = 0;
    private int boardWidth = 0;
    private int boardHeight = 0;

    public TextBuilder(int boardWidth, int boardHeight, int textSizeHorizontal, int textSizeVertical){
        this.boardHeight = boardHeight;
        this.boardWidth = boardWidth;
        this.textBuffer = IntBuffer.allocate(boardWidth * boardHeight * 4);
        this.textSizeHorizontal = textSizeHorizontal;
        this.textSizeVerical = textSizeVertical;
    }

    // Draw text on screen and delay for n seconds
    public void setText(String text, int delay, int refreshRate, int color) {
        textDisplayingCountdown = delay * refreshRate / 1000;

        if (boardWidth < 15) {
            setText90(text, delay, refreshRate, Color.WHITE);
            return;
        }

        Canvas canvas = new Canvas();
        Bitmap bitmap = Bitmap.createBitmap(boardWidth, boardHeight, Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        canvas.scale(-1, -1, boardWidth / 2, boardHeight / 2);
        Paint textPaint = new TextPaint();
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(color); // Text Color
        textPaint.setTypeface(Typeface.create("Courier", Typeface.BOLD));
        textPaint.setTextSize(textSizeVerical); // Text Size
        canvas.drawText(text.substring(0, Math.min(text.length(), 4)),
                (boardWidth / 2), 30, textPaint);//boardHeight / 2 + (mTextSizeVerical / 3), textPaint);
        if (textBuffer != null) {
            textBuffer.rewind();
            bitmap.copyPixelsToBuffer(textBuffer);
        }
    }

    // Draw text on screen and delay for n seconds
    public void setText90(String text, int delay, int refreshRate, int color) {
        textDisplayingCountdown = delay * refreshRate / 1000;

        Canvas canvas = new Canvas();
        Bitmap bitmap = Bitmap.createBitmap(boardWidth, boardHeight, Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        canvas.scale(-1, -1, boardWidth / 2, boardHeight / 2);
        canvas.rotate(90, boardWidth / 2, boardHeight / 2);
        Paint textPaint = new TextPaint();
        textPaint.setDither(true);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(color); // Text Color
        textPaint.setTypeface(Typeface.create("Courier", Typeface.BOLD));
        textPaint.setTextSize(textSizeHorizontal); // Text Size
        //paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)); // Text Overlapping Pattern
        canvas.drawText(text, (boardWidth / 2), boardHeight / 2 + (textSizeHorizontal / 3), textPaint);
        if (textBuffer != null) {
            textBuffer.rewind();
            bitmap.copyPixelsToBuffer(textBuffer);
        }
    }

}

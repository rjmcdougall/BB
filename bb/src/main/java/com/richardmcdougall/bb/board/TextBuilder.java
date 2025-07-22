package com.richardmcdougall.bb.board;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.TextPaint;

import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bbcommon.BLog;
import com.richardmcdougall.bbcommon.BoardState;

import java.nio.IntBuffer;
import java.util.ArrayList;

public class TextBuilder {
    private String TAG = this.getClass().getSimpleName();
    private IntBuffer textBuffer = null;
    private int[] argbData = null;
    public int textDisplayingCountdown = 0;
    public ArrayList<RGB> pixels = new ArrayList<>();
    private BurnerBoard board = null;
    private int[] layeredScreen;

    public TextBuilder(BurnerBoard board){
        this.board = board;
    }
    
    // Cleanup method for proper resource management
    public void cleanup() {
        for (RGB rgb : pixels) {
            rgb.recycle();
        }
        pixels.clear();
    }

    // Draw text on screen and delay for n seconds
    public void setText(String text, int delay, int refreshRate,  RGB color) {

        if (textBuffer == null || textBuffer.capacity() != board.boardWidth * board.boardHeight * 4) {
            this.textBuffer = IntBuffer.allocate(board.boardWidth * board.boardHeight * 4);
        }
        textBuffer.clear();
        
        // Reuse argbData array to reduce GC pressure
        int requiredSize = this.board.boardWidth * this.board.boardHeight;
        if (argbData == null || argbData.length != requiredSize) {
            argbData = new int[requiredSize];
        }

        textDisplayingCountdown = delay * refreshRate / 1000;

        if (this.board.boardWidth < 15) {
            setText90(text, delay, refreshRate, color);
            return;
        }

        try{
            Canvas canvas = new Canvas();
            Bitmap bitmap = Bitmap.createBitmap(this.board.boardWidth, this.board.boardHeight, Bitmap.Config.ARGB_8888);
            canvas.setBitmap(bitmap);
            canvas.scale(-1, -1, this.board.boardWidth / 2, this.board.boardHeight / 2);
            Paint textPaint = new TextPaint();
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setColor(Color.rgb(color.r, color.g, color.b)); // Text Color
            textPaint.setTypeface(Typeface.create("Courier", Typeface.BOLD));
            textPaint.setTextSize(this.board.textSizeVertical); // Text Size
            canvas.drawText(text, (this.board.boardWidth / 2), 30, textPaint);
            if (textBuffer != null) {
                textBuffer.rewind();
                bitmap.copyPixelsToBuffer(textBuffer);
            }

        int[] temp = textBuffer.array();
        
        // Ensure pixels ArrayList has sufficient capacity to avoid resizing
        int requiredPixels = this.board.boardWidth * this.board.boardHeight;
        if (pixels.size() != requiredPixels) {
            // Recycle old RGB objects before clearing
            for (RGB rgb : pixels) {
                rgb.recycle();
            }
            pixels.clear();
            pixels.ensureCapacity(requiredPixels);
            for (int i = 0; i < requiredPixels; i++) {
                RGB rgb = RGB.obtain();
                rgb.setFromRGBAInt(temp[i]);
                pixels.add(rgb);
            }
        } else {
            // Reuse existing RGB objects in the list without creating new ones
            for (int i = 0; i < requiredPixels; i++) {
                RGB existingRGB = pixels.get(i);
                existingRGB.setFromRGBAInt(temp[i]);
            }
        }
        }
        catch(Exception e){
            BLog.e(TAG, e.getMessage());
        }
    }

    public int[] aRGBtoBoardScreen(int[] sourceScreen) {

        if (this.pixels.isEmpty()) {
            return null;
        }

        if (layeredScreen == null || layeredScreen.length != this.board.boardWidth * this.board.boardHeight * 3) {
            layeredScreen = new int[this.board.boardWidth * this.board.boardHeight * 3];
        }

        for (int pixelNo = 0; pixelNo < (this.board.boardWidth * this.board.boardHeight); pixelNo++) {
            int pixel_offset = pixelNo * 3;
            RGB pixel = this.pixels.get(pixelNo);

            // Render the new text over the original

            if (pixel.isBlack()) {
                layeredScreen[pixel_offset] = sourceScreen[pixel_offset];
                layeredScreen[pixel_offset + 1] = sourceScreen[pixel_offset + 1];
                layeredScreen[pixel_offset + 2] = sourceScreen[pixel_offset + 2];
            }
            else if (pixel.isWhite()) {
                layeredScreen[pixel_offset] = pixel.r;
                layeredScreen[pixel_offset + 1] = pixel.g;
                layeredScreen[pixel_offset + 2] = pixel.b;
            }
            else {
                layeredScreen[pixel_offset] = pixel.r;
                layeredScreen[pixel_offset + 1] = pixel.g;
                layeredScreen[pixel_offset + 2] = pixel.b;
            }
        }
        return layeredScreen;
    }

    // Draw text on screen and delay for n seconds
    public void setText90(String text, int delay, int refreshRate, RGB color) {

        if (textBuffer == null || textBuffer.capacity() != board.boardWidth * board.boardHeight * 4) {
            this.textBuffer = IntBuffer.allocate(board.boardWidth * board.boardHeight * 4);
        }
        textBuffer.clear();

        textDisplayingCountdown = delay * refreshRate / 1000;

        try {
            Canvas canvas = new Canvas();
            Bitmap bitmap = Bitmap.createBitmap(this.board.boardWidth, this.board.boardHeight, Bitmap.Config.ARGB_8888);
            canvas.setBitmap(bitmap);
            canvas.scale(-1, -1, this.board.boardWidth / 2, this.board.boardHeight / 2);
            canvas.rotate(90, this.board.boardWidth / 2, this.board.boardHeight / 2);
            Paint textPaint = new TextPaint();
            textPaint.setDither(true);
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setColor(Color.rgb(color.r, color.g, color.b)); // Text Color
            textPaint.setTypeface(Typeface.create("Courier", Typeface.BOLD));
            textPaint.setTextSize(this.board.textSizeHorizontal); // Text Size
            canvas.drawText(text, (this.board.boardWidth / 2), this.board.boardHeight / 2 + (this.board.textSizeHorizontal / 3), textPaint);
            if (textBuffer != null) {
                textBuffer.rewind();
                bitmap.copyPixelsToBuffer(textBuffer);
            }

            int[] temp = textBuffer.array();
            
            // Ensure pixels ArrayList has sufficient capacity to avoid resizing
            int requiredPixels = this.board.boardWidth * this.board.boardHeight;
            if (pixels.size() != requiredPixels) {
                // Recycle old RGB objects before clearing
                for (RGB rgb : pixels) {
                    rgb.recycle();
                }
                pixels.clear();
                pixels.ensureCapacity(requiredPixels);
                for (int i = 0; i < requiredPixels; i++) {
                    RGB rgb = RGB.obtain();
                    rgb.setFromRGBAInt(temp[i]);
                    pixels.add(rgb);
                }
            } else {
                // Reuse existing RGB objects in the list without creating new ones
                for (int i = 0; i < requiredPixels; i++) {
                    RGB existingRGB = pixels.get(i);
                    existingRGB.setFromRGBAInt(temp[i]);
                }
            }
        }
        catch(Exception e){
            BLog.e(TAG, e.getMessage());
        }
    }

    // render text on screen
    public int[] renderText(int[] origScreen) {
        // Suppress updating when displaying a text message
        if (textDisplayingCountdown > 0 && this.board.renderTextOnScreen) {
            textDisplayingCountdown--;
            return this.aRGBtoBoardScreen(origScreen);
        }
        else {
            return origScreen;
        }
    }
}

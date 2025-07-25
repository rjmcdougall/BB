package com.richardmcdougall.bb.board;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bbcommon.BoardState;

import java.nio.IntBuffer;
import java.util.ArrayList;

public class LineBuilder {

    private String TAG = this.getClass().getSimpleName();
    private IntBuffer drawBuffer = null;
    private int[] argbData = null;
    public ArrayList<RGB> pixels = new ArrayList<>();
    private int[] layeredScreen;
    private BurnerBoard board = null;

    public LineBuilder(BurnerBoard board){
        this.board = board;
    }

    public void clearLine(){
        // Recycle existing RGB objects before clearing
        for (RGB rgb : pixels) {
            rgb.recycle();
        }
        this.pixels = new ArrayList<>();
    }
    
    // Cleanup method for proper resource management
    public void cleanup() {
        for (RGB rgb : pixels) {
            rgb.recycle();
        }
        pixels.clear();
    }

    public void drawLine() {
        if (drawBuffer == null || drawBuffer.capacity() != board.boardWidth * board.boardHeight * 4) {
            this.drawBuffer = IntBuffer.allocate(board.boardWidth * board.boardHeight * 4);
        }
        drawBuffer.clear();
        
        // Reuse argbData array to reduce GC pressure
        int requiredSize = this.board.boardWidth * this.board.boardHeight;
        if (argbData == null || argbData.length != requiredSize) {
            argbData = new int[requiredSize];
        }

        Canvas canvas = new Canvas();
        Bitmap bitmap = Bitmap.createBitmap(this.board.boardWidth, this.board.boardHeight, Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        canvas.scale(-1, -1, this.board.boardWidth / 2, this.board.boardHeight / 2);
        Paint linepaint = new Paint();
        linepaint.setColor(Color.BLUE); //  Color
        linepaint.setStrokeWidth(1);
        linepaint.setStyle(Paint.Style.FILL_AND_STROKE);

        canvas.drawLine(0, 0, 20, 180, linepaint);

        if (drawBuffer != null) {
            drawBuffer.rewind();
            bitmap.copyPixelsToBuffer(drawBuffer);
        }

        int[] temp = drawBuffer.array();
        
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
    RGB pixel;
    int pixel_offset;
    public int[] aRGBtoBoardScreen(int[] sourceScreen) {

        if (this.pixels.isEmpty()) {
            return sourceScreen;
        }

        if (layeredScreen == null || layeredScreen.length != this.board.boardWidth * this.board.boardHeight * 3) {
            layeredScreen = new int[this.board.boardWidth * this.board.boardHeight * 3];
        }

        for (int pixelNo = 0; pixelNo < (this.board.boardWidth * this.board.boardHeight); pixelNo++) {
            pixel_offset = pixelNo * 3;
            pixel = this.pixels.get(pixelNo);

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

    // render text on screen
    public int[] renderLine(int[] origScreen) {
        if(this.board.renderLineOnScreen)
            return this.aRGBtoBoardScreen(origScreen);
        else
            return origScreen;
    }

}

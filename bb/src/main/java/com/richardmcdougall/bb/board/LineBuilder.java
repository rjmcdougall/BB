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
    private int boardWidth = 0;
    private int boardHeight = 0;
    public ArrayList<RGB> pixels = new ArrayList<>();
    private BBService service = null;
    private int[] layeredScreen;

    public LineBuilder(BBService service, int boardWidth, int boardHeight){
        this.boardHeight = boardHeight;
        this.boardWidth = boardWidth;
        this.service = service;
        this.drawBuffer = IntBuffer.allocate(boardWidth * boardHeight * 4);
    }

    public void clear(){
        this.pixels = new ArrayList<>();
    }
    public void drawLine() {

        Canvas canvas = new Canvas();
        Bitmap bitmap = Bitmap.createBitmap(boardWidth, boardHeight, Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        canvas.scale(-1, -1, boardWidth / 2, boardHeight / 2);
        Paint linepaint = new Paint();
        linepaint.setColor(Color.WHITE); //  Color
        linepaint.setStrokeWidth(1);
        linepaint.setStyle(Paint.Style.FILL_AND_STROKE);

        canvas.drawLine(0, 0, 20, 180, linepaint);

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

    public int[] aRGBtoBoardScreen(int[] sourceScreen) {

        if (this.pixels.isEmpty()) {
            return sourceScreen;
        }

        layeredScreen = new int[this.boardWidth * this.boardHeight * 3];

        for (int pixelNo = 0; pixelNo < (boardWidth * boardHeight); pixelNo++) {
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

    // render text on screen
    public int[] renderLine(int[] origScreen) {
        if(renderLineOnScreen())
            return this.aRGBtoBoardScreen(origScreen);
        else
            return origScreen;
    }

    public boolean renderLineOnScreen(){
        return (this.service.boardState.GetBoardType() == BoardState.BoardType.panel
        || this.service.boardState.GetBoardType() == BoardState.BoardType.wspanel);
    }
}

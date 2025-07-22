package com.richardmcdougall.bb.board;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicConvolve3x3;
import android.text.TextPaint;

import com.richardmcdougall.bbcommon.BLog;

import java.nio.IntBuffer;
import java.util.ArrayList;


public class Sharpener {

    private final String TAG = "Sharpener";

    private final float factor = -3f;
    private final int iters = 1;

    private RenderScript rs;
    private ScriptIntrinsicConvolve3x3 convolution;
    private BurnerBoard board = null;

    public enum sharpenMode {NONE,LAPLACE1,LAPLACE2, THREExTHREE, FIVExFIVE};

    public Sharpener(BurnerBoard board) {
        this.board = board;

        rs = RenderScript.create(this.board.service.context);
        convolution = ScriptIntrinsicConvolve3x3.create(rs, Element.U8_4(rs));
        // 4. Set the sharpening kernel coefficients. This is a typical sharpening kernel.
        float[] sharpenKernel3x3 = {
                -0.5f, -1, -0.5f,
                -1, 8, -1,
                -0.5f, -1, -0.5f
        };

        float[] sharpenKernel3x3a = {
                -0, -1, -0,
                -1, 5, -1,
                -0, -1, -0
        };

        float[] laplacian = {
                0, 1, 0,
                1, -4, 1,
                0, 1, 0
        };


        // Alternative Laplacian
        float[] laplacian2 = {
                1, 1, 1,
                1, -8, 1,
                1, 1, 1
        };
        float[] laplacian3 = {
                .5f, .5f, .5f,
                .5f, -4, .5f,
                .5f, .5f, .5f
        };


        // Set kernel and input (Unsharp masking kernel)
        float[] sharpenKernel5x5 = {
                -0.0625f * factor, -0.0625f * factor, -0.0625f * factor, -0.0625f * factor, -0.0625f * factor,
                -0.0625f * factor, 0.25f * factor, 0.25f * factor, 0.25f * factor, -0.0625f * factor,
                -0.0625f * factor, 0.25f * factor, 1.0f + factor, 0.25f * factor, -0.0625f * factor,
                -0.0625f * factor, 0.25f * factor, 0.25f * factor, 0.25f * factor, -0.0625f * factor,
                -0.0625f * factor, -0.0625f * factor, -0.0625f * factor, -0.0625f * factor, -0.0625f * factor,
        };

        float[] sharpenKernel5x5a = {
                -0.00391f, -0.01563f, -0.02344f, -0.01563f, -0.00391f,
                -0.01563f, -0.06250f, -0.09375f, -0.06250f, -0.01563f,
                -0.02344f, -0.09375f, 1.7814f, -0.09375f, -0.02344f,
                -0.01563f, -0.06250f, -0.09375f, -0.06250f, -0.01563f,
                -0.00391f, -0.01563f, -0.02344f, -0.01563f, -0.00391f,
        };

        convolution.setCoefficients(laplacian2);

    }


    private IntBuffer pixelBuffer = null;
    private int[] argbData = null;
    public ArrayList<RGB> pixels = new ArrayList<>();

    public void sharpen(int[] boardScreen, sharpenMode mode) {

        if (mode == sharpenMode.NONE || true) {
            return;
        }

        if (pixelBuffer == null || pixelBuffer.capacity() != board.boardWidth * board.boardHeight * 4) {
            this.pixelBuffer = IntBuffer.allocate(board.boardWidth * board.boardHeight * 4);
        }
        pixelBuffer.clear();
        
        // Reuse argbData array to reduce GC pressure
        int requiredSize = this.board.boardWidth * this.board.boardHeight;
        if (argbData == null || argbData.length != requiredSize) {
            argbData = new int[requiredSize];
        }

        for (int pixelNo = 0; pixelNo < (this.board.boardWidth * this.board.boardHeight); pixelNo++) {
            int pixel_offset = pixelNo * 3;
            int r = boardScreen[pixel_offset + 0];
            int g = boardScreen[pixel_offset + 1];
            int b = boardScreen[pixel_offset + 2];

            argbData[pixelNo] = (0xFF << 24) | (r << 16) | (g << 8) | b;
        }

        Bitmap sharpenedBitmap = Bitmap.
                createBitmap(argbData,
                        this.board.boardWidth, this.board.boardHeight, Bitmap.Config.ARGB_8888);

        //Bitmap outBitmap = sharpenWithRenderScript(bitmap);

        Bitmap nextBitmap;

        for (int i = 0; i < iters; i++) {
            nextBitmap = sharpenWithRenderScript(sharpenedBitmap);
            if (i > 0) { //Recycle previous one, avoid OOM
                sharpenedBitmap.recycle();
            }
            sharpenedBitmap = nextBitmap;
        }

        if (pixelBuffer != null) {
            pixelBuffer.rewind();
            sharpenedBitmap.copyPixelsToBuffer(pixelBuffer);
        }

        int[] temp = pixelBuffer.array();
        
        // Ensure pixels ArrayList has sufficient capacity to avoid resizing
        int requiredPixels = this.board.boardWidth * this.board.boardHeight;
        if (pixels.size() != requiredPixels) {
            pixels.clear();
            pixels.ensureCapacity(requiredPixels);
            for (int i = 0; i < requiredPixels; i++) {
                pixels.add(RGB.fromRGBAInt(temp[i]));
            }
        } else {
            // Reuse existing RGB objects in the list
            for (int i = 0; i < requiredPixels; i++) {
                RGB existingRGB = pixels.get(i);
                RGB newRGB = RGB.fromRGBAInt(temp[i]);
                existingRGB.r = newRGB.r;
                existingRGB.g = newRGB.g;
                existingRGB.b = newRGB.b;
            }
        }

        for (int pixelNo = 0; pixelNo < (this.board.boardWidth * this.board.boardHeight); pixelNo++) {
            int pixel_offset = pixelNo * 3;
            RGB pixel = this.pixels.get(pixelNo);

            boardScreen[pixel_offset] = pixel.r;
            boardScreen[pixel_offset + 1] = pixel.g;
            boardScreen[pixel_offset + 2] = pixel.b;

        }
    }

    /**
     * Sharpens a bitmap using a 3x3 convolution kernel with RenderScript.
     * This method provides better quality than the ColorMatrix approach and is
     * generally faster than manual pixel manipulation.
     *
     * @param original The original bitmap.
     * @return The sharpened bitmap, or null if an error occurred.
     */
    public Bitmap sharpenWithRenderScript(Bitmap original) {
        if (original == null) {
            return null;
        }

        // Create a mutable copy of the original bitmap
        Bitmap sharpenedBitmap = original.copy(Bitmap.Config.ARGB_8888, true);

        // 1. Create RenderScript context

        // 2. Create Allocations for input and output
        Allocation input = Allocation.createFromBitmap(rs, original);
        Allocation output = Allocation.createFromBitmap(rs, sharpenedBitmap);


        // 5. Set the input Allocation
        convolution.setInput(input);

        // 6. Apply the convolution and store the result in the output Allocation
        convolution.forEach(output);

        // 7. Copy the data from the output Allocation to the sharpened bitmap
        output.copyTo(sharpenedBitmap);

        // 8. Destroy RenderScript context and allocations to free resources
        input.destroy();
        output.destroy();
        //convolution.destroy();
        //rs.destroy();

        return sharpenedBitmap;
    }


}
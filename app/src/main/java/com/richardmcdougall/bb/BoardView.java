package com.richardmcdougall.bb;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.media.audiofx.Visualizer;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by rmc on 3/5/17.
 */

public class BoardView extends View {
    private static final String TAG = "BoardView";
    private byte[] mBoardScreen;
    private Rect mRect = new Rect();
    private int mBoardWidth;
    private int mBoardHeight;
    Bitmap mCanvasBitmap;
    Canvas mCanvas;

    public BoardView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs);

        if (BBService.kEmulatingClassic) {
            mBoardWidth = 10;
            mBoardHeight = 70;
        } else {
            mBoardWidth = 46;
            mBoardHeight = 118;
        }

        if (!isInEditMode()) {
            System.out.println("Starting BoardView");
            init();
        }
    }

    public BoardView(Context context, AttributeSet attrs) {

        this(context, attrs, 0);
    }

    public BoardView(Context context) {

        this(context, null, 0);
    }

    private void init() {
        mBoardScreen = new byte[mBoardWidth * mBoardHeight * 3];
        setWillNotDraw(false);
    }

    static int PIXEL_RED = 0;
    static int PIXEL_GREEN = 1;
    static int PIXEL_BLUE = 2;
    int pixel2Offset(int x, int y, int rgb) {
        return (y * mBoardWidth + x) * 3 + rgb;
    }

    private Paint mPaint = new Paint();

    public void fillScreen(byte r, byte g, byte b) {
        System.out.println("fillScreen");
        if (mCanvas == null)
            return;
        //mPaint.setColor(Color.argb(0, r, g, b));

        if (mBoardScreen != null) {

            int x;
            int y;
            for (x = 0; x < mBoardWidth; x++) {
                for (y = 0; y < mBoardHeight; y++) {
                    mBoardScreen[pixel2Offset(x, y, PIXEL_RED)] = r;
                    mBoardScreen[pixel2Offset(x, y, PIXEL_GREEN)] = g;
                    mBoardScreen[pixel2Offset(x, y, PIXEL_BLUE)] = b;
                }
            }
            //System.out.println("fillScreen end" + (byte)r + "," + (byte)g + "," + (byte)b);
        }

        invalidate();
    }

    public void fadeScreen(int amount) {
        //System.out.println("fadeScreen");
        if (mCanvas == null)
            return;
        if (mBoardScreen != null) {

            int x;
            int y;
            for (x = 0; x < mBoardWidth; x++) {
                for (y = 0; y < mBoardHeight; y++) {
                    mBoardScreen[pixel2Offset(x, y, PIXEL_RED)] =
                            (byte)(mBoardScreen[pixel2Offset(x, y, PIXEL_RED)] & 0xFF - amount);
                    mBoardScreen[pixel2Offset(x, y, PIXEL_GREEN)] =
                            (byte)(mBoardScreen[pixel2Offset(x, y, PIXEL_GREEN)] & 0xFF - amount);
                    mBoardScreen[pixel2Offset(x, y, PIXEL_BLUE)] =
                            (byte)(mBoardScreen[pixel2Offset(x, y, PIXEL_BLUE)] & 0xFF - amount);
                }
            }
            //System.out.println("fillScreen end" + (byte)r + "," + (byte)g + "," + (byte)b);
        }
        invalidate();
    }

    public void scroll(int direction) {
        if (mCanvas == null)
            return;
        if (mBoardScreen != null) {
            int x;
            int y;
            if (direction == 0) {
                for (x = 0; x < mBoardWidth; x++) {
                    for (y = 0; y < mBoardHeight - 1; y++) {
                        mBoardScreen[pixel2Offset(x, y, PIXEL_RED)] =
                                mBoardScreen[pixel2Offset(x, y + 1, PIXEL_RED)];
                        mBoardScreen[pixel2Offset(x, y , PIXEL_GREEN)] =
                                mBoardScreen[pixel2Offset(x, y + 1, PIXEL_GREEN)];
                        mBoardScreen[pixel2Offset(x, y, PIXEL_RED)] =
                                mBoardScreen[pixel2Offset(x, y + 1, PIXEL_BLUE)];
                    }
                }
            } else {
                for (x = 0; x < mBoardWidth; x++) {
                    for (y = mBoardHeight - 2; y >= 0; y--) {
                        mBoardScreen[pixel2Offset(x, y, PIXEL_RED)] =
                                mBoardScreen[pixel2Offset(x, y + 1, PIXEL_RED)];
                        mBoardScreen[pixel2Offset(x, y , PIXEL_GREEN)] =
                                mBoardScreen[pixel2Offset(x, y + 1, PIXEL_GREEN)];
                        mBoardScreen[pixel2Offset(x, y, PIXEL_RED)] =
                                mBoardScreen[pixel2Offset(x, y + 1, PIXEL_BLUE)];
                    }
                }
            }
        }
        invalidate();
    }

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public void setRow(int row, byte[] pixels) {
        //System.out.print("setRow " + row + ":" + bytesToHex(pixels));
        //System.out.println("BB setRow " + row + " length " + pixels.length);
        if (mCanvas == null)
            return;

        //pixels[3] = (byte)255;

        //java.util.Arrays.fill(pixels, (byte)255);
        if (mBoardScreen != null) {
            int x;
            int y;
            if (row < mBoardHeight) {
                //System.out.println("BB setRow pixels " + row);
                for (x = 0; x < mBoardWidth; x++) {
                    // TODO: add sidelights
                    mBoardScreen[pixel2Offset(x, row, PIXEL_RED)] = pixels[x * 3]; //r
                    mBoardScreen[pixel2Offset(x, row, PIXEL_GREEN)] = pixels[x * 3 + 1]; //g
                    mBoardScreen[pixel2Offset(x, row, PIXEL_BLUE)] = pixels[x * 3 + 2]; //b
                }
            }
        }

        // TODO: invalidate only on update()
        invalidate();
    }



    public void setOtherLight(int other, byte[] pixels) {
        //System.out.print("setRow " + row + ":" + bytesToHex(pixels));
        //System.out.println("BB setRow " + row + " length " + pixels.length);
        if (mCanvas == null)
            return;

        // TODO: invalidate only on update()
        invalidate();
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        //System.out.println("onDraw");

        // Create canvas once we're ready to draw
        mRect.set(0, 0, getWidth(), getHeight());
        Paint paint = new Paint();
        paint.setStrokeWidth(50f);
        paint.setAntiAlias(true);

        if (mCanvasBitmap == null) {
            //System.out.println("onDraw bitmap");
            mCanvasBitmap = Bitmap.createBitmap(canvas.getWidth(), canvas.getHeight(),
                    Bitmap.Config.ARGB_8888);
            //System.out.println("Size is " + getWidth() + "," + getHeight());
        }
        if (mCanvas == null) {
            //System.out.println("onDraw canvas");
            mCanvas = new Canvas(mCanvasBitmap);
        }

        if (mBoardScreen != null) {
            //System.out.println("BB onDraw pixels ");

            int x;
            int y;
            for (x = 0; x < mBoardWidth; x += 1) {
                for (y = 0; y < mBoardHeight; y += 1) {
                    int r = (mBoardScreen[pixel2Offset(x, y, PIXEL_RED)] & 0xFF);
                    int g = (mBoardScreen[pixel2Offset(x, y, PIXEL_GREEN)] & 0xFF);
                    int b = (mBoardScreen[pixel2Offset(x, y, PIXEL_BLUE)] & 0xFF);
                    //if (isBoardPixel(x, y)) {
                        paint.setColor(Color.argb(255, r, g, b));
                        mCanvas.drawCircle(15 + y * 5, 5 + x * 5, 2, paint);
                    //}
                    //System.out.println("setcolor " + mBoardScreen[(x * mBoardHeight + y) * 3]);
                }
            }
            // Update board view
            //paint.setColor(Color.argb(255, 255, 0, 0));

            //mCanvas.drawLines(points, 0, 1, paint);
            //mCanvas.drawLine((float)0, (float)0, (float)getWidth(), (float)getHeight(), paint);
            canvas.drawBitmap(mCanvasBitmap, new Matrix(), null);
        }
        //mCanvas.drawPaint(mPaint);
    }

    // Check if the board pixel is real to draw the edge of the board
    boolean isBoardPixel(int x, int y) {
        int pixel = x * mBoardHeight + y + 1;
        int newpixel = 0;

        //1  20-50->1-31
        if (pixel >= 20 && pixel <=50)
            newpixel = pixel - 19;
        //2 83-127->76-32
        if (pixel >= 83 && pixel <= 127)
            newpixel = 127 - pixel + 32;
        //3 146-205->77-136
        if (pixel >= 146 && pixel <= 205)
            newpixel = pixel - 146 + 77;
        //4 213-278->202-137
        if (pixel >= 213 && pixel <= 278)
            newpixel = 278 - pixel + 137;
        //5 281-350->203-272
        if (pixel >= 281 && pixel <= 350)
            newpixel = pixel - 281 + 203;
        //6 351-420->342-273
        if (pixel >= 351 && pixel <=420)
            newpixel = 420 - pixel + 273;
        //7 423-488->343-408
        if (pixel >= 423 && pixel <= 488)
            newpixel = pixel - 423 + 343;
        //8 496-555->468-409
        if (pixel >= 496 && pixel <= 555)
            newpixel = 555 - pixel + 409;
        //9 573-617->469-513
        if (pixel >= 573 && pixel <= 617)
            newpixel = pixel - 573 + 469;
        //10 650-680->544-514
        if (pixel >= 650 && pixel <= 680)
            newpixel = 680 - pixel + 514;
        return newpixel != 0;
    }

}


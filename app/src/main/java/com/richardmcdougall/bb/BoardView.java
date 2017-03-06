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
    private int mBoardWidth = 10;
    private int mBoardHeight = 70;
    Bitmap mCanvasBitmap;
    Canvas mCanvas;

    public BoardView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs);
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


    private Paint mPaint = new Paint();

    public void fillScreen(int r, int g, int b) {
        //System.out.println("fillScreen start" + (byte)r + "," + (byte)g + "," + (byte)b);
        if (mCanvas == null)
            return;
        //mPaint.setColor(Color.argb(0, r, g, b));

        if (mBoardScreen != null) {
            //System.out.println("fillScreen: update");

            int x;
            int y;
            for (x = 0; x < mBoardWidth; x++) {
                for (y = 0; y < mBoardHeight; y++) {
                    mBoardScreen[(x * mBoardHeight + y) * 3] = (byte) r;
                    mBoardScreen[(x * mBoardHeight + y) * 3 + 1] = (byte) g;
                    mBoardScreen[(x * mBoardHeight + y) * 3 + 2] = (byte) b;
                    //mBoardScreen[100] = (byte)r;
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
        //mPaint.setColor(Color.argb(0, 0, 0, 0));
        //invalidate();
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
            System.out.println("onDraw bitmap");
            mCanvasBitmap = Bitmap.createBitmap(canvas.getWidth(), canvas.getHeight(),
                    Bitmap.Config.ARGB_8888);
            System.out.println("Size is " + getWidth() + "," + getHeight());
        }
        if (mCanvas == null) {
            System.out.println("onDraw canvas");
            mCanvas = new Canvas(mCanvasBitmap);
        }

        if (mBoardScreen != null) {
            int x;
            int y;
            for (x = 0; x < mBoardWidth; x += 1) {
                for (y = 0; y < mBoardHeight; y += 1) {
                    paint.setColor(Color.argb(255,
                            mBoardScreen[(x * mBoardHeight + y) * 3],
                            mBoardScreen[(x * mBoardHeight + y) * 3 + 1],
                            mBoardScreen[(x * mBoardHeight + y) * 3 + 2]));
                    mCanvas.drawCircle(150 + y * 10   , 5 + x * 20, 3, paint);
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

}


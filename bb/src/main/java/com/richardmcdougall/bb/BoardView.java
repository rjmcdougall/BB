package com.richardmcdougall.bb;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import com.richardmcdougall.bbcommon.BoardState;
import com.richardmcdougall.bbcommon.DebugConfigs;

/**
 * Created by rmc on 3/5/17.
 */

public class BoardView extends View {
    private static final String TAG = "BB.BoardView";
    private byte[] mBoardScreen;
    private Rect mRect = new Rect();
    private int mBoardWidth;
    private int mBoardHeight;
    private Bitmap mCanvasBitmap;
    private Canvas mCanvas;
    private static final int PIXEL_RED = 0;
    private static final int PIXEL_GREEN = 1;
    private static final int PIXEL_BLUE = 2;

    public BoardView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs);

        if (DebugConfigs.OVERRIDE_BOARD_TYPE == BoardState.BoardType.classic) {
            mBoardWidth = 10;
            mBoardHeight = 70;
        } else if (DebugConfigs.OVERRIDE_BOARD_TYPE == BoardState.BoardType.panel) {
            mBoardWidth = 32;
            mBoardHeight = 64;
        } else if (DebugConfigs.OVERRIDE_BOARD_TYPE == BoardState.BoardType.mast) {
            mBoardWidth = 24;
            mBoardHeight = 159;
        } else if (DebugConfigs.OVERRIDE_BOARD_TYPE == BoardState.BoardType.backpack) {
            mBoardWidth = 8;
            mBoardHeight = 256;
        } else if (DebugConfigs.OVERRIDE_BOARD_TYPE == BoardState.BoardType.wspanel) {
            mBoardWidth = 20;
            mBoardHeight = 180;
        } else if (DebugConfigs.OVERRIDE_BOARD_TYPE == BoardState.BoardType.azul || DebugConfigs.OVERRIDE_BOARD_TYPE == null) {
            mBoardWidth = 46;
            mBoardHeight = 118;
        }

        init();
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
    int pixel2Offset(int x, int y, int rgb) {
        return (y * mBoardWidth + x) * 3 + rgb;
    }

    public void fillScreen(byte r, byte g, byte b) {

        int x;
        int y;
        for (x = 0; x < mBoardWidth; x++) {
            for (y = 0; y < mBoardHeight; y++) {
                mBoardScreen[pixel2Offset(x, y, PIXEL_RED)] = r;
                mBoardScreen[pixel2Offset(x, y, PIXEL_GREEN)] = g;
                mBoardScreen[pixel2Offset(x, y, PIXEL_BLUE)] = b;
            }
        }

        invalidate();
    }

    public void fadeScreen(int amount) {
        if (mCanvas == null)
            return;
        if (mBoardScreen != null) {

            int x;
            int y;
            for (x = 0; x < mBoardWidth; x++) {
                for (y = 0; y < mBoardHeight; y++) {
                    mBoardScreen[pixel2Offset(x, y, PIXEL_RED)] =
                            (byte) (mBoardScreen[pixel2Offset(x, y, PIXEL_RED)] & 0xFF - amount);
                    mBoardScreen[pixel2Offset(x, y, PIXEL_GREEN)] =
                            (byte) (mBoardScreen[pixel2Offset(x, y, PIXEL_GREEN)] & 0xFF - amount);
                    mBoardScreen[pixel2Offset(x, y, PIXEL_BLUE)] =
                            (byte) (mBoardScreen[pixel2Offset(x, y, PIXEL_BLUE)] & 0xFF - amount);
                }
            }
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
                        mBoardScreen[pixel2Offset(x, y, PIXEL_GREEN)] =
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
                        mBoardScreen[pixel2Offset(x, y, PIXEL_GREEN)] =
                                mBoardScreen[pixel2Offset(x, y + 1, PIXEL_GREEN)];
                        mBoardScreen[pixel2Offset(x, y, PIXEL_RED)] =
                                mBoardScreen[pixel2Offset(x, y + 1, PIXEL_BLUE)];
                    }
                }
            }
        }
        invalidate();
    }

    public void setRow(int row, byte[] pixels) {
        if (mBoardScreen != null) {
            int x;
            int y;
            if (row < mBoardHeight) {
                for (x = 0; x < mBoardWidth; x++) {
                    // TODO: add sidelights
                    mBoardScreen[pixel2Offset(x, row, PIXEL_RED)] = pixels[x * 3]; //r
                    mBoardScreen[pixel2Offset(x, row, PIXEL_GREEN)] = pixels[x * 3 + 1]; //g
                    mBoardScreen[pixel2Offset(x, row, PIXEL_BLUE)] = pixels[x * 3 + 2]; //b
                }
            }
        }

        invalidate();
    }

    public void setOtherLight(int other, byte[] pixels) {

        if (mCanvas == null)
            return;

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        mRect.set(0, 0, getWidth(), getHeight());
        Paint paint = new Paint();
        paint.setStrokeWidth(50f);
        paint.setAntiAlias(true);

        if (mCanvasBitmap == null) {
            mCanvasBitmap = Bitmap.createBitmap(canvas.getWidth(), canvas.getHeight(),
                    Bitmap.Config.ARGB_8888);
        }
        if (mCanvas == null) {
            mCanvas = new Canvas(mCanvasBitmap);
        }

        if (mBoardScreen != null) {

            int x;
            int y;
            for (x = 0; x < mBoardWidth; x += 1) {
                for (y = 0; y < mBoardHeight; y += 1) {
                    int r = (mBoardScreen[pixel2Offset(x, y, PIXEL_RED)] & 0xFF);
                    int g = (mBoardScreen[pixel2Offset(x, y, PIXEL_GREEN)] & 0xFF);
                    int b = (mBoardScreen[pixel2Offset(x, y, PIXEL_BLUE)] & 0xFF);

                    paint.setColor(Color.argb(255, r, g, b));
                    mCanvas.drawCircle(15 + y * 13, 5 + x * 13, 6, paint);
                }
            }
            canvas.drawBitmap(mCanvasBitmap, new Matrix(), null);
        }
    }
}


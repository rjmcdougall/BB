package com.richardmcdougall.bb.visualization;

import com.richardmcdougall.bb.BoardVisualization;
import com.richardmcdougall.bb.BurnerBoard;
import com.richardmcdougall.bb.SimpleImage;

/**
 * Created by rmc on 6/21/18.
 */

public class Video extends Visualization {

    public SimpleImage currentVideoFrame = null;
    private VideoDecoder mVideoDecoder = new VideoDecoder();
    private int lastVideoMode = -1;

    public Video(BurnerBoard bb,
                 BoardVisualization visualization) {
        super(bb, visualization);
    }

    public void update(int mode) {

        int nVideos = mBurnerBoard.service.boardState.GetTotalVideo();

        if (nVideos == 0)
            return;

        int curVidIndex = mode % nVideos;

        if (curVidIndex!=lastVideoMode && mVideoDecoder.IsRunning()) {
            mVideoDecoder.Stop();
        }

        if (!mVideoDecoder.IsRunning()) {

            mVideoDecoder.Stop();
            mVideoDecoder.onFrameReady = new VideoDecoder.OnFrameReadyCallback() {
                public void callbackCall(SimpleImage img) {
                    //frameCnt++;
                    currentVideoFrame = img;
                    currentVideoFrame = img.dup();
                }
            };

            try {
                mVideoDecoder.Start(mBurnerBoard.service.boardState.GetVideoFile(curVidIndex), mBoardWidth, mBoardHeight);
            } catch (Throwable throwable) {
                //Log.d(TAG, "Unable to start decoder");
                //throwable.printStackTrace();
            }
            if (curVidIndex != lastVideoMode) {
                lastVideoMode = curVidIndex;
            }
        }
        if (currentVideoFrame!=null) {
            SimpleImage curVideo = currentVideoFrame;  // save pointer to current video frame, it might change in another thread
            int totalPixels = curVideo.width * curVideo.height;
            BurnerBoard ba = (BurnerBoard)mBurnerBoard;

            try {
                int srcOff = 0, dstOff = 0;
                int[] dst = ba.mBoardScreen;
                byte[] src = curVideo.mPixelBuf.array();

                // convert from 32 bit RGBA byte pixels to 96 bit RGB int pixels
                while (totalPixels !=0) {
                    dst[dstOff++] = src[srcOff+0] & 0xff;
                    dst[dstOff++] = src[srcOff+1] & 0xff;
                    dst[dstOff++] = src[srcOff+2] & 0xff;
                    srcOff+=4;                         // skip alpha channel, this isn't used
                    totalPixels--;
                }
            } catch (Exception e) {
                e.printStackTrace();;
            }
        }

        mBurnerBoard.flush();
    }
}

package com.richardmcdougall.bb.video;

import android.util.Log;

import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bb.board.BurnerBoard;
import com.richardmcdougall.bb.visualization.Visualization;

/**
 * Created by rmc on 6/21/18.
 */

public class Video extends Visualization {

    private String TAG = "Video";
    public SimpleImage currentVideoFrame = null;
    private VideoDecoder mVideoDecoder = new VideoDecoder();
    private int lastVideoMode = -1;
    private BBService service = null;

    public Video(BBService service ) {
        super(service );
        this.service = service;
    }

    public void update(int mode) {

        int nVideos = service.mediaManager.GetTotalVideo();

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
                mVideoDecoder.Start(service.mediaManager.GetVideoFile(curVidIndex), mBoardWidth, mBoardHeight);
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
            BurnerBoard ba = service.burnerBoard;

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
                e(e.getMessage());
            }
        }

        service.burnerBoard.flush();
    }public void e(String logMsg) {
        Log.e(TAG, logMsg);
    }
}

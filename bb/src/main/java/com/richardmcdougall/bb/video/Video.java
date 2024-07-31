package com.richardmcdougall.bb.video;

import android.util.Log;

import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bb.board.BurnerBoard;
import com.richardmcdougall.bb.visualization.Visualization;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by rmc on 6/21/18.
 */

public class Video extends Visualization {

    private String TAG = "Video";
    public SimpleImage currentVideoFrame = null;
    private VideoDecoder mVideoDecoder = new VideoDecoder();
    private int lastVideoMode = -1;
    private BBService service = null;
    private int videoContrastMultiplier = 1;
    private boolean lastFunMode = false;

    ScheduledThreadPoolExecutor sch = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1);
    Runnable videoSpeedController = this::videoSpeedController;

    public Video(BBService service ) {
        super(service );
        this.service = service;
        sch.scheduleWithFixedDelay(videoSpeedController, 1, 1, TimeUnit.SECONDS);

    }

    public void videoSpeedController() {

    }
    public void update(int mode) {

        int nVideos = service.mediaManager.GetTotalVideo();
        videoContrastMultiplier = service.boardState.videoContrastMultiplier;

        if (nVideos == 0)
            return;

        int curVidIndex = mode % nVideos;

        if(this.service.boardState.funMode != lastFunMode || curVidIndex != lastVideoMode
                && mVideoDecoder.IsRunning()){
            service.visualizationController.resetParkedTime();
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
                if(this.service.boardState.funMode)
                    mVideoDecoder.Start(service.mediaManager.GetVideoFile(curVidIndex), service.burnerBoard.boardWidth, service.burnerBoard.boardHeight);

            } catch (Throwable throwable) {
                //Log.d(TAG, "Unable to start decoder");
                //throwable.printStackTrace();
            }
            if (curVidIndex != lastVideoMode) {
                lastVideoMode = curVidIndex;
            }
            if(this.service.boardState.funMode != lastFunMode){
                lastFunMode = this.service.boardState.funMode;
            }
        }
        if (currentVideoFrame != null) {
            SimpleImage curVideo = currentVideoFrame;  // save pointer to current video frame, it might change in another thread
            int totalPixels = curVideo.width * curVideo.height;
            BurnerBoard ba = service.burnerBoard;

            try {
                int srcOff = 0, dstOff = 0;
                int[] dst = ba.boardScreen;
                byte[] src = curVideo.mPixelBuf.array();

                long maxPixels = totalPixels;

                // convert from 32 bit RGBA byte pixels to 96 bit RGB int pixels
                while (totalPixels !=0) {

                    dst[dstOff++] = IncreaseContrast(src[srcOff + 0] & 0xff); // 1111 0000 reduce the number of colors
                    dst[dstOff++] = IncreaseContrast(src[srcOff + 1] & 0xff); // 1111 0000 reduce the number of colors
                    dst[dstOff++] = IncreaseContrast(src[srcOff + 2] & 0xff); // 1111 0000 reduce the number of colors

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

    private int IncreaseContrast(int color){

        if(videoContrastMultiplier!=1){
            int R = (int)(((((color / 255.0) - 0.5) * videoContrastMultiplier) + 0.5) * 255.0);
            if(R < 0)
                R = 0;
            else if(R > 255)
                R = 255;
            return R;
        }
        else
            return color;
    }
}

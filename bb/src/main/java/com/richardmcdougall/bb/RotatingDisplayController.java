package com.richardmcdougall.bb;

import com.richardmcdougall.bbcommon.BLog;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class RotatingDisplayController {

    private String TAG = this.getClass().getSimpleName();
    private BBService service = null;
    private boolean isSwitching = false;

    ScheduledThreadPoolExecutor sch = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1);

    Runnable checkForRotatingDisplay = () -> {
        try {
            if (service.boardState.rotatingDisplay == true) {
                if(!isSwitching)
                    SwitchDisplayMode();
                isSwitching = true;
            }
            else
                isSwitching = false;

        } catch (Exception e) {
            BLog.e(TAG, e.getMessage());
        }
    };

    public RotatingDisplayController(BBService service) {
        this.service = service;
        sch.scheduleWithFixedDelay(checkForRotatingDisplay, 10, 1, TimeUnit.SECONDS);
    }

    public void EnableRotatingDisplay(boolean isRotatingDisplay) {
        this.service.boardState.rotatingDisplay = isRotatingDisplay;
    }

    Runnable switchVideo = () -> {
        try {
            if (service.boardState.rotatingDisplay == true)
                SwitchDisplayMode();
        } catch (Exception e) {
            BLog.e(TAG, e.getMessage());
        }
    };

    private void SwitchDisplayMode() {

        try {
            this.service.visualizationController.setMode(99);
            BLog.d(TAG, "Display Mode video switch to mode " + this.service.boardState.currentVideoMode);
            int secondsBeforeSwitch = 6;

            if (this.service.mediaManager.GetVideo(this.service.boardState.currentVideoMode).has("Length")) {
                secondsBeforeSwitch = this.service.mediaManager.GetVideo(this.service.boardState.currentVideoMode).getInt("Length");
            }

            sch.schedule(switchVideo, secondsBeforeSwitch, TimeUnit.SECONDS);
        } catch (Exception e) {
            BLog.e(TAG, "Error Switching Vide Mode for Sign " + e.getMessage());
        }
    }
}

package com.richardmcdougall.bb;

import android.widget.Switch;

import com.richardmcdougall.bb.visualization.BBColor;
import com.richardmcdougall.bbcommon.BLog;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class RotatingDisplayController {

    private String TAG = this.getClass().getSimpleName();
    private BBService service = null;

    ScheduledThreadPoolExecutor sch = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1);

    Runnable checkForRotatingDisplay = () -> {
        try{
            if(service.boardState.rotatingDisplay==true)
                SwitchDisplayMode();
        }catch(Exception e){
            BLog.e(TAG, e.getMessage());
        }
    };

    public RotatingDisplayController(BBService service) {
        this.service = service;
        sch.scheduleAtFixedRate(checkForRotatingDisplay, 10, 10, TimeUnit.SECONDS);
   }

   public void EnableRotatingDisplay(boolean isRotatingDisplay){
        this.service.boardState.rotatingDisplay = isRotatingDisplay;
   }

    private void SwitchDisplayMode() {
        this.service.boardVisualization.setMode(99);
        BLog.d(TAG, "Display Mode video switch to mode " + this.service.boardState.currentVideoMode);
    }

}

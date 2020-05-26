package com.richardmcdougall.bb;

import com.richardmcdougall.bb.visualization.RGBList;
import com.richardmcdougall.bbcommon.BLog;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class LocalCrisisController {

    private String TAG = this.getClass().getSimpleName();
    private BBService service = null;
    public int boardInCrisisPhase = 0;
    private int stashedVideoMode = 0;

    ScheduledThreadPoolExecutor sch = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1);

    Runnable moveToPhase1 = () -> {
        try{
            if(service.boardState.inCrisis && boardInCrisisPhase==2){
                StartCrisisPhase1();
            }
            else {
                StopCrisis();
            }
        }catch(Exception e){
            BLog.e(TAG, e.getMessage());
        }
    };

    Runnable moveToPhase2 = () -> {
        try{
            if(service.boardState.inCrisis && boardInCrisisPhase==1){
                Phase2();
            }
            else {
                StopCrisis();
            }
        }catch(Exception e){
            BLog.e(TAG, e.getMessage());
        }
    };

    public LocalCrisisController(BBService service) {
        this.service = service;
   }

    private void Phase2(){
        boardInCrisisPhase = 2;
        stashedVideoMode = service.boardState.currentVideoMode;
        this.service.visualizationController.setMode(this.service.mediaManager.GetMapMode());

        sch.schedule(moveToPhase1, 5, TimeUnit.SECONDS);
    }

    private void StopCrisis(){
        boardInCrisisPhase = 0;
        service.boardState.currentVideoMode = stashedVideoMode;
        stashedVideoMode = 0;
        service.musicController.Unmute();
    }

    private void StartCrisisPhase1() {

        boardInCrisisPhase = 1;
        service.musicController.Mute();

        service.burnerBoard.textBuilder.setText90("Please Help!", 10000, service.burnerBoard.getFrameRate(), new RGBList().getColor("white"));
        service.speak("Please Help!","mode");

        sch.schedule(moveToPhase2, 5, TimeUnit.SECONDS);
    }

    public void SetCrisis(boolean inCrisis){
        service.boardState.inCrisis = inCrisis;

        if(inCrisis)
            StartCrisisPhase1();
        else
            StopCrisis();
    }
}

package com.richardmcdougall.bb;

import android.speech.tts.TextToSpeech;

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
        this.service.boardVisualization.setMode(this.service.mediaManager.GetMapMode());

        sch.schedule(moveToPhase1, 10, TimeUnit.SECONDS);
    }

    private void StopCrisis(){
        boardInCrisisPhase = 0;
        service.boardState.currentVideoMode = stashedVideoMode;
        stashedVideoMode = 0;
        service.musicPlayer.Unmute();
    }

    private void StartCrisisPhase1() {

        boardInCrisisPhase = 1;
        service.musicPlayer.Mute();

        service.burnerBoard.setText("Requesting Help", 10000);
        service.voice.speak("Requesting Help! ", TextToSpeech.QUEUE_FLUSH, null, "mode");

        sch.schedule(moveToPhase2, 10, TimeUnit.SECONDS);
    }

    public void SetCrisis(boolean inCrisis){
        service.boardState.inCrisis = inCrisis;

        if(inCrisis)
            StartCrisisPhase1();
        else
            StopCrisis();
    }
}

package com.richardmcdougall.bb;

import android.speech.tts.TextToSpeech;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class RemoteCrisisController {

    private String TAG = this.getClass().getSimpleName();
    private BBService service = null;
    public int boardInCrisisPhase = 0;
    private int stashedVideoMode = 0;

    ScheduledThreadPoolExecutor sch = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1);

    Runnable checkForCrisis = () -> {
        try{
            if(service.boardLocations.BoardsInCrisis().size()>0 && boardInCrisisPhase==0){
                StartCrisisPhase1();
            }
            else if (service.boardLocations.BoardsInCrisis().size()==0){
                StopCrisis();
            }
        }catch(Exception e){
            BLog.e(TAG, e.getMessage());
        }
    };

    Runnable moveToPhase1 = () -> {
        try{
            if(service.boardLocations.BoardsInCrisis().size()>0 && boardInCrisisPhase==2){
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
            if(service.boardLocations.BoardsInCrisis().size()>0 && boardInCrisisPhase==1){
                Phase2();
            }
            else {
                StopCrisis();
            }
        }catch(Exception e){
            BLog.e(TAG, e.getMessage());
        }
    };

    public RemoteCrisisController(BBService service) {
        this.service = service;

        sch.scheduleAtFixedRate(checkForCrisis, 10, 1, TimeUnit.SECONDS);
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

        for (Integer addressInCrisis : service.boardLocations.BoardsInCrisis()) {
            service.burnerBoard.setText(service.allBoards.boardAddressToName(addressInCrisis), 10000);
            service.voice.speak("EMERGENCY! " + service.allBoards.boardAddressToName(addressInCrisis), TextToSpeech.QUEUE_FLUSH, null, "mode");
        }

        sch.schedule(moveToPhase2, 10, TimeUnit.SECONDS);
    }
}

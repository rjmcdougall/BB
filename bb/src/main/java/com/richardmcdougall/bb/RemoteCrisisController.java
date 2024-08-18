package com.richardmcdougall.bb;

import com.richardmcdougall.bb.visualization.RGBList;
import com.richardmcdougall.bbcommon.BLog;

import java.util.concurrent.Executors;
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
            if(service.localCrisisController.boardInCrisisPhase==0){ // of i am in crisis, i dont care if you are.
                if(service.boardLocations.BoardsInCrisis().size()>0 && boardInCrisisPhase==0){
                    Phase1();
                }
                else if (service.boardLocations.BoardsInCrisis().size()==0 && !(boardInCrisisPhase==0)){
                    StopCrisis();
                }
            }
        }catch(Exception e){
            BLog.e(TAG, e.getMessage());
        }
    };

    Runnable moveToPhase1 = () -> {
        try{
            if(service.boardLocations.BoardsInCrisis().size()>0 && boardInCrisisPhase==2){
                Phase1();
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

        sch.scheduleAtFixedRate(checkForCrisis, 10, 5, TimeUnit.SECONDS);
    }

    private void Phase2(){
        int mapMode = this.service.mediaManager.GetMapMode();
        if(mapMode != -1){
            boardInCrisisPhase = 2;
            stashedVideoMode = service.boardState.currentVideoMode;
            this.service.visualizationController.setMode(mapMode);
        }
        sch.schedule(moveToPhase1, 5, TimeUnit.SECONDS);
    }

    private void StopCrisis(){
        boardInCrisisPhase = 0;
        service.boardState.currentVideoMode = stashedVideoMode;
        stashedVideoMode = 0;
        service.musicController.Unmute();
    }

    private void Phase1() {

        boardInCrisisPhase = 1;
        service.musicController.Mute();

        for (Integer addressInCrisis : service.boardLocations.BoardsInCrisis()) {
            service.burnerBoard.textBuilder.setText(service.allBoards.boardAddressToName(addressInCrisis), 10000, service.burnerBoard.getFrameRate(),  new RGBList().getColor("white"));
            service.speak("EMERGENCY! " + service.allBoards.boardAddressToName(addressInCrisis),  "mode");
            BLog.i(TAG,"Board in crisis - " + service.allBoards.boardAddressToName(addressInCrisis));
        }

        sch.schedule(moveToPhase2, 5, TimeUnit.SECONDS);
    }
}

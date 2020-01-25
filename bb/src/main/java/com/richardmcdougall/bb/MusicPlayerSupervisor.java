package com.richardmcdougall.bb;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MusicPlayerSupervisor {
    private String TAG = this.getClass().getSimpleName();

    private BBService service;

    ScheduledThreadPoolExecutor sch = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1);

    MusicPlayerSupervisor(BBService service) {

        this.service = service;

        sch.schedule(setChannel, 1, TimeUnit.SECONDS);
        sch.scheduleWithFixedDelay(seekAndPlay, 2, 1, TimeUnit.SECONDS);
    }

    Runnable seekAndPlay = () -> {
        try{
            service.musicPlayer.SeekAndPlay();
        }catch(Exception e){
            BLog.e(TAG, e.getMessage());
        }
    };

    Runnable setChannel = () -> {
        try{
            service.musicPlayer.RadioMode();
        }catch(Exception e){
            BLog.e(TAG, e.getMessage());
        }
    };
}

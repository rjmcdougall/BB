package com.richardmcdougall.bb;

public class MusicPlayerSupervisor {
    private String TAG = this.getClass().getSimpleName();

    private BBService service;

    MusicPlayerSupervisor(BBService service) {
        this.service = service;
    }

    public void Run() {

        service.musicPlayer.RadioMode();

        Thread t = new Thread(() -> {
            Thread.currentThread().setName("BB Music Player");
            BLog.i(TAG, "Starting Music Supervisor");
            SupervisorThread();
        });
        t.start();
    }

    void SupervisorThread() {
        while (true) {

            service.musicPlayer.SeekAndPlay();

            try {
                // RMC try 15 seconds instead of 1
                Thread.sleep(1000);
            } catch (Throwable e) {
            }
        }
    }
}

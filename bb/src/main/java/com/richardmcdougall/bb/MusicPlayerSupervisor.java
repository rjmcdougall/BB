package com.richardmcdougall.bb;

import timber.log.Timber;

public class MusicPlayerSupervisor {

    private int musicState = 0;
    private BBService service;

    MusicPlayerSupervisor(BBService service) {
        this.service = service;
    }

    public void Run() {
        Thread t = new Thread(new Runnable() {
            public void run() {
                Thread.currentThread().setName("BB Music Player");
                Timber.i("Starting Music Supervisor");
                SupervisorThread();
            }
        });
        t.start();
    }

    void SupervisorThread() {
        while (true) {
            switch (musicState) {
                case 0:
                    if (service.mediaManager.GetTotalAudio() != 0) {
                        musicState = 1;

                        service.musicPlayer.RadioMode();

                    } else {
                        try {
                            Thread.sleep(1000);
                        } catch (Throwable e) {
                        }
                    }
                    break;

                case 1:
                    service.musicPlayer.SeekAndPlay();

                    try {
                        // RMC try 15 seconds instead of 1
                        Thread.sleep(1000);
                    } catch (Throwable e) {
                    }
                    if (service.boardState.currentRadioChannel == 0) {
                        musicState = 2;
                    }
                    break;

                case 2:
                    if (service.boardState.currentRadioChannel != 0) {
                        musicState = 1;
                    }
                    break;

                default:
                    break;
            }
        }
    }
}

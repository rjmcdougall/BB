package com.richardmcdougall.bb;

import android.speech.tts.TextToSpeech;

public class CrisisController {

    private BBService service = null;
    public boolean boardInCrisis = false;

    private long redScreenSeconds = 10;
    private long redScreenCount = 0;

    public CrisisController(BBService service) {
        this.service = service;
    }


    void Run() {
        Thread t = new Thread(new Runnable() {
            public void run() {
                Thread.currentThread().setName("Crisis Supervisor");
                runSupervisor();
            }
        });
        t.start();
    }

    private void StartCrisis(){

        boardInCrisis = true;

        for (Integer addressInCrisis : service.boardLocations.BoardsInCrisis()) {
            service.burnerBoard.setText(service.allBoards.boardAddressToName(addressInCrisis), 10000);
            service.voice.speak("EMERGENCY! " + service.allBoards.boardAddressToName(addressInCrisis), TextToSpeech.QUEUE_FLUSH, null, "mode");
        }
    }

    private void runSupervisor() {

        while (true) {

            if(service.boardLocations.BoardsInCrisis().size()>0){
                StartCrisis();
            }

            try {
                if(boardInCrisis) {
                    redScreenCount++;

                    if(redScreenCount >= redScreenSeconds){
                        redScreenCount = 0;
                        boardInCrisis = false;
                    }
                }
                Thread.sleep(1000);
            } catch (Throwable e) {
            }
        }
    }

}

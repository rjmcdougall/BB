package com.richardmcdougall.bb;

import com.richardmcdougall.bb.bms.BMS;
import com.richardmcdougall.bb.board.BurnerBoard;
import com.richardmcdougall.bbcommon.BLog;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MotionSupervisor {
    ScheduledThreadPoolExecutor sch = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1);
    private String TAG = this.getClass().getSimpleName();
    private BBService service = null;


    Runnable motionSupervisor = this::checkMotion;

    MotionSupervisor(BBService service) {
        this.service = service;

        /* Communicate the settings for the supervisor thread */
        BLog.d(TAG, "Enable Motion Monitoring? " + service.burnerBoard.enableMotionMonitoring);

        if (service.burnerBoard.enableMotionMonitoring)
            sch.scheduleWithFixedDelay(motionSupervisor, 5000, 100, TimeUnit.MILLISECONDS);

    }


    public void checkMotion() {

        float rpm = 0;
        float duty = 0;
        float motorCurrent = 0;
        motionStates motionState = motionStates.STATE_STOPPED;

        try {
            rpm = service.vesc.getRPM();
            duty = service.vesc.getDuty();
            motorCurrent = service.vesc.getMotorCurrent();

            BLog.d(TAG, "Board rpm is " + rpm + ", duty = " + duty + ", motor current = " + motorCurrent);

            if (rpm < 0) {
                motionState = motionStates.STATE_REVERSING;
            } else if (rpm > 0) {
                if ((duty < 0) || (motorCurrent < 0)) {
                    motionState = motionStates.STATE_BRAKING;
                } else {
                    motionState = motionStates.STATE_DRIVING;
                }
            } else {
                motionState = motionStates.STATE_STOPPED;
            }
        } catch (Exception e) {
            BLog.d(TAG, "Cannot get VESC rpm");
        }

        if (motionState == motionStates.STATE_BRAKING) {
            service.burnerBoard.brakeOverlayBuilder.setBrake(true);
        }
    }

    public enum motionStates {STATE_STOPPED, STATE_DRIVING, STATE_BRAKING, STATE_REVERSING}
}
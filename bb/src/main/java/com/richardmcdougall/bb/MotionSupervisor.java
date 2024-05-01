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

    private motionStates mMotionState = motionStates.STATE_STOPPED;

    Runnable motionSupervisor = this::checkMotion;

    MotionSupervisor(BBService service) {
        this.service = service;

        /* Communicate the settings for the supervisor thread */
        BLog.d(TAG, "Enable Motion Monitoring? " + service.burnerBoard.enableMotionMonitoring);

        if (service.burnerBoard.enableMotionMonitoring)
            sch.scheduleWithFixedDelay(motionSupervisor, 5000, 100, TimeUnit.MILLISECONDS);

    }

    public motionStates getState() {
        return mMotionState;
    }

    private int updates = 0;
    private int stopped = 0;

    public void checkMotion() {

        float rpm = 0;
        float duty = 0;
        float motorCurrent = 0;
        motionStates priorMotionState = mMotionState;


        updates++;

        try {
            // If never heard from a vesc, likely doesn't have one...
            if (service.vesc.vescHeard() == false) {
                mMotionState = motionStates.STATE_UNKNOWN;
                return;
            }

            rpm = service.vesc.getRPM();
            duty = service.vesc.getDuty();
            motorCurrent = service.vesc.getMotorCurrent();

            if (updates % 50 == 0) {
                BLog.d(TAG, mMotionState + " Board rpm is " + rpm + ", duty = " + duty + ", motor current = " + motorCurrent);
            }

            if (rpm < 0) {
                mMotionState = motionStates.STATE_REVERSING;
                stopped = 0;
            } else if (rpm > 0) {
                if ((duty < 0) || (motorCurrent < 0)) {
                    mMotionState = motionStates.STATE_BRAKING;
                } else {
                    mMotionState = motionStates.STATE_DRIVING;
                }
                stopped = 0;
            } else {
                stopped++;
                if (stopped > 500) {
                    mMotionState = motionStates.STATE_PARKED;
                } else {
                    mMotionState = motionStates.STATE_STOPPED;
                }
            }
        } catch (Exception e) {
            BLog.d(TAG, "Cannot get VESC rpm");
            mMotionState = motionStates.STATE_UNKNOWN;
        }

        if (mMotionState == motionStates.STATE_BRAKING) {
            service.burnerBoard.brakeOverlayBuilder.setBrake(true);
        }
        if (!mMotionState.equals(priorMotionState)) {
            BLog.d(TAG, "State changed to " + mMotionState);
        }
    }

    public enum motionStates {STATE_UNKNOWN, STATE_STOPPED, STATE_PARKED, STATE_DRIVING, STATE_BRAKING, STATE_REVERSING}
}
package com.richardmcdougall.bb;

import android.content.Context;

import com.richardmcdougall.bbcommon.BLog;
import com.richardmcdougall.bbcommon.BoardState;

import java.io.IOException;


public class Gyro {
    private String TAG = this.getClass().getSimpleName();
    private Mpu6050 mMpu = null;

    public Gyro(Context context, BoardState boardState) {
        try {
            mMpu = mMpu.open();
        } catch (IOException e) {
            BLog.e(TAG, "Cannot open Accelerometer");
        }

        try {
            BLog.e(TAG, "Acel: " + String.format("%f \t %f \t %f", mMpu.getAccelX(), mMpu.getAccelY(), mMpu.getAccelZ()));
            BLog.e(TAG, "Temp: " + String.format("%f", mMpu.getTemp()));
            BLog.e(TAG, "Gyro: " + String.format("%f \t %f \t %f", mMpu.getGyroX(), mMpu.getGyroY(), mMpu.getGyroZ()));
        } catch (IOException e) {
            BLog.e(TAG, "Cannot open Accelerometer");
        }
    }

    // Add accessors here...

}


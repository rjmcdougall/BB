package com.richardmcdougall.bb;

import android.os.SystemClock;

import com.richardmcdougall.bbcommon.BLog;

import java.util.Calendar;

// TESTING after time has been reset to 1970
// adb shell "su 0 toybox date 010100071970.00"
// adb shell "su 0 toybox date 010100072020.00"
public class TimeSync {
    private static String TAG = "TimeSync";
    private static long startElapsedTime;
    private static long serverTimeOffset = 0;
    private static long serverRoundTripTime = 0;
    private static long startClock = 0;

    public static void InitClock() {
        startElapsedTime = SystemClock.uptimeMillis();
        startElapsedTime = SystemClock.elapsedRealtime();
        //startClock = Calendar.getInstance().getTimeInMillis();
    }

    public static long GetCurrentClock() {
        return SystemClock.elapsedRealtime() - startElapsedTime ;
        //return SystemClock.uptimeMillis() ;
    }

    public static long CurrentClockAdjusted() {
        long cca = GetCurrentClock() + serverTimeOffset;
        BLog.d(TAG, GetCurrentClock() + " + " + serverTimeOffset + " = " + cca);
        return cca;
    }

    public static void SetServerClockOffset(long serverClockOffset, long roundTripTime) {
        serverTimeOffset = serverClockOffset;
        serverRoundTripTime = roundTripTime;
    }

    public static long getServerClockOffset() {
        return serverTimeOffset;
    }

    public static long getServerRoundTripTime() {
        return serverRoundTripTime;
    }

}

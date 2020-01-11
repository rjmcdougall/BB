package com.richardmcdougall.bb;

import android.os.SystemClock;

import java.util.Calendar;

// TESTING after time has been reset to 1970
// adb shell "su 0 toybox date 010100071970.00"
// adb shell "su 0 toybox date 010100072020.00"
public class TimeSync {
    private static String TAG = "TimeSync";
    public static long startElapsedTime;
    public static long startClock;

    public static long serverTimeOffset = 0;
    public static long serverRoundTripTime = 0;

    public static void InitClock() {
        startElapsedTime = SystemClock.elapsedRealtime();
        startClock = Calendar.getInstance().getTimeInMillis();
    }

    public static long GetCurrentClock() {
        long t = SystemClock.elapsedRealtime() - startElapsedTime + startClock;
        return t;
    }

    public static long CurrentClockAdjusted() {
        long cca = GetCurrentClock() + serverTimeOffset;
        return cca;
    }

    public static void SetServerClockOffset(long serverClockOffset, long roundTripTime) {
        serverTimeOffset = serverClockOffset;
        serverRoundTripTime = roundTripTime;
    }

}

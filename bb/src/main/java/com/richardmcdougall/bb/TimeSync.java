package com.richardmcdougall.bb;

import android.os.SystemClock;

import java.util.Calendar;

public class TimeSync {

    public static long startElapsedTime;
    public static long startClock;

    public static long serverTimeOffset = 0;
    public static long serverRTT = 0;

    public static void InitClock() {
        startElapsedTime = SystemClock.elapsedRealtime();
        startClock = Calendar.getInstance().getTimeInMillis();
    }

    public static long GetCurrentClock() {
        return SystemClock.elapsedRealtime() - startElapsedTime + startClock;
    }

    public static long CurrentClockAdjusted() {
        return GetCurrentClock() + serverTimeOffset;
    }

    public static void SetServerClockOffset(long serverClockOffset, long rtt) {
        serverTimeOffset = serverClockOffset;
        serverRTT = rtt;
    }
}

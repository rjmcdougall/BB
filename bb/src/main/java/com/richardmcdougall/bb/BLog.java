package com.richardmcdougall.bb;

import android.util.Log;

public class BLog {

    public static void v(String tag, String msg) {
        log(Log.VERBOSE, tag, msg);
    }

    public static void d(String tag, String msg) {
        log(Log.DEBUG, tag, msg);
    }

    public static void w(String tag, String msg) {
        log(Log.WARN, tag, msg);
    }

    public static void e(String tag, String msg) {
        log(Log.ERROR, tag, msg);
    }

    public static void i(String tag, String msg) {
        log(Log.INFO, tag, msg);
    }

    static void log(int priority, String tag, String msg) {
        Log.println(priority, "BB." + tag, msg);
    }

}

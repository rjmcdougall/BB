package com.richardmcdougall.bb.hardware;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bbcommon.BLog;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class PowerController {

    BBService service;
    VescController vesc;

    private String TAG = this.getClass().getSimpleName();

    // BBPower holds an LED/amp "keep on" lease for kKeeponSeconds (60s in firmware),
    // so we refresh with margin. While a hold is active the board stays lit/amped even
    // with the VESC off (BBPower auto mode turns the outputs on for the lease duration).
    private static final int kLeaseRefreshSeconds = 45;

    private volatile boolean ledsHold = false;
    private volatile boolean ampHold = false;

    private final ScheduledThreadPoolExecutor leaseScheduler =
            (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1);

    public PowerController(BBService service) {
        this.service = service;
        vesc = service.vesc;
        // Periodically renew any active hold so the outputs stay on with the VESC off.
        leaseScheduler.scheduleWithFixedDelay(this::refreshLeases,
                kLeaseRefreshSeconds, kLeaseRefreshSeconds, TimeUnit.SECONDS);
    }

    /**
     * True when the LEDs have been forced on via BLE (an active lease). Used by
     * BatterySupervisor to keep rendering graphics even with the VESC off.
     */
    public boolean areLedsForced() {
        return ledsHold;
    }

    /**
     * Release any BLE force-on (LED/amp lease). Called when the VESC turns on so
     * the VESC/BBPower auto-mode becomes the sole controller again -- otherwise a
     * BLE "on" would keep the outputs on indefinitely, overriding the VESC. We
     * just stop renewing the lease; it expires within kKeeponSeconds and, while
     * the VESC is on, BBPower keeps the outputs on anyway.
     */
    public void clearForceOn() {
        if (ledsHold || ampHold) {
            BLog.d(TAG, "clearForceOn: releasing BLE led/amp hold (VESC took over)");
        }
        ledsHold = false;
        ampHold = false;
    }

    private void refreshLeases() {
        try {
            // Only renew the lease here -- do NOT re-assert lm1/l1/amp1. The lease
            // keeps the LEDs/amp on while the VESC is off (the feature); when the
            // VESC is on, BBPower's auto mode owns the relays. Re-asserting the
            // direct on-commands every cycle would override the VESC entirely.
            if (ledsHold) {
                vesc.sendPowerCommand("lx");
            }
            if (ampHold) {
                vesc.sendPowerCommand("ampx");
            }
        } catch (Exception e) {
            BLog.e(TAG, "lease refresh failed: " + e.getMessage());
        }
    }

    public void leds(boolean state) {
        try {
            BLog.d(TAG, "leds: " + state);
            ledsHold = state;
            if (state) {
                // Turn on immediately regardless of VESC state (lx alone only
                // lights up when the VESC is off), then hold via the lease so
                // they stay on if the VESC later powers off.
                vesc.sendPowerCommand("lm1");
                vesc.sendPowerCommand("l1");
                vesc.sendPowerCommand("lx");
            } else {
                // Stop renewing and turn the LEDs off promptly.
                vesc.sendPowerCommand("l0");
                vesc.sendPowerCommand("lm0");
            }
        } catch (Exception e) {
            BLog.e(TAG, "leds failed: " + e.getMessage());
        }
    }

    public void amp(boolean state) {
        try {
            BLog.d(TAG, "amp: " + state);
            ampHold = state;
            if (state) {
                // Turn on immediately regardless of VESC state, then hold via
                // the lease so it stays on if the VESC later powers off.
                vesc.sendPowerCommand("amp1");
                vesc.sendPowerCommand("ampx");
            } else {
                vesc.sendPowerCommand("amp0");
            }
        } catch (Exception e) {
            BLog.e(TAG, "amp failed: " + e.getMessage());
        }
    }

    public void fud() {
        try {
            BLog.d(TAG, "fud: restarting bb app");
            restartApp();
        } catch (Exception e) {
            BLog.e(TAG, "fud failed: " + e.getMessage());
        }
    }

    /**
     * Restart the bb Android app. Schedules the launcher activity to relaunch shortly,
     * then kills this process. The foreground START_STICKY service is also restarted by
     * Android, so the app comes back even if the alarm relaunch is throttled.
     */
    private void restartApp() {
        try {
            Context ctx = service.getApplicationContext();
            Intent intent = ctx.getPackageManager().getLaunchIntentForPackage(ctx.getPackageName());
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                int flags = PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE;
                PendingIntent pending = PendingIntent.getActivity(ctx, 0, intent, flags);
                AlarmManager mgr = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
                if (mgr != null) {
                    mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 1000, pending);
                }
            }
        } catch (Exception e) {
            BLog.e(TAG, "restartApp scheduling failed: " + e.getMessage());
        }
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(0);
    }

    public void auto(boolean state) {
        try {
            BLog.d(TAG, "auto:" + state);
            vesc.sendPowerCommand(state ? "a1" : "a0");
        } catch (Exception e) {
            BLog.e(TAG, "auto failed: " + e.getMessage());
        }
    }

    public float getVoltage() {
        float value = 0;
        try {
            BLog.d(TAG, "getVoltage");
            value = vesc.getVoltage();
        } catch (Exception e) {
        }
        return value;
    }

    public float getTemp() {
        float value = 0;
        try {
            BLog.d(TAG, "getVoltage");
            value = vesc.getLedTemp();
        } catch (Exception e) {
        }
        return value;
    }

    public float getCurrent() {
        float value = 0;
        try {
            BLog.d(TAG, "getCurrent");
            value = vesc.getLedCurrent();
        } catch (Exception e) {
        }
        return value;
    }

    public float getPower() {
        float value = 0;
        try {
            BLog.d(TAG, "getPower");
            value = vesc.getLedPower();
        } catch (Exception e) {
        }
        return value;
    }
}

package com.richardmcdougall.bb.hardware;

import android.os.SystemClock;

import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bbcommon.BLog;

// https://github.com/vedderb/bldc/blob/a9a1faf317d6e88eee48a6f212b11083ed87156d/util/buffer.c#L169
// https://github.com/vedderb/bldc/blob/master/datatypes.h
// https://vesc-project.com/sites/default/files/imce/u15301/VESC6_CAN_CommandsTelemetry.pdf
// https://electric-skateboard.builders/t/vesc-can-message-structure/98092/5



public class VescController implements CanListener {
    private String TAG = this.getClass().getSimpleName();

    public VESC_CAN_PACKET_ID vesc_can_packet_id;
    public VESC_CAN_STATUS_MSG vesc_can_status_msg = new VESC_CAN_STATUS_MSG();
    public VESC_CAN_STATUS_MSG_2 vesc_can_status_msg_2 = new VESC_CAN_STATUS_MSG_2();
    public VESC_CAN_STATUS_MSG_3 vesc_can_status_msg_3 = new VESC_CAN_STATUS_MSG_3();
    public VESC_CAN_STATUS_MSG_4 vesc_can_status_msg_4 = new VESC_CAN_STATUS_MSG_4();
    public VESC_CAN_STATUS_MSG_5 vesc_can_status_msg_5 = new VESC_CAN_STATUS_MSG_5();
    public VESC_CAN_STATUS_MSG_6 vesc_can_status_msg_6 = new VESC_CAN_STATUS_MSG_6();
    public VESC_CHUCK_DATA vesc_chuck_data;
    public VESC_GNSS_DATA vesc_gnss_data;
    public VESC_IOBOARD_ADC_VALUES vesc_ioboard_adc_values;
    public VESC_IOBOARD_DIGITAL_VALUES vesc_ioboard_digital_values;
    public VESC_CAN_BURNERBOARD_POWER1 vesc_burnerboard_power1 = new VESC_CAN_BURNERBOARD_POWER1();
    public VESC_CAN_BURNERBOARD_POWER2 vesc_burnerboard_power2 = new VESC_CAN_BURNERBOARD_POWER2();

    private static int kAliveCheckMilliSeconds = 1000;

    private static final float kPresumedCurrentLoadLights = 3.0f;

    public VescController(BBService service, Canable canbus) {
        canbus.addListener(this);
    }

    public boolean vescOn() {
        BLog.d(TAG, "e " + SystemClock.elapsedRealtime() + " - r " + vesc_can_status_msg.rx_time + " = " + (SystemClock.elapsedRealtime() - vesc_can_status_msg.rx_time));
        if ((SystemClock.elapsedRealtime() - vesc_can_status_msg.rx_time) < kAliveCheckMilliSeconds) {
            return true;
        }
        return false;
    }

    public boolean vescMoving() {
        if (((SystemClock.elapsedRealtime() - vesc_can_status_msg.rx_time) < kAliveCheckMilliSeconds)
                && (vesc_can_status_msg.rpm > 0)) {
            return true;
        }
        return false;
    }

    public float getVoltage() {
        if (vesc_burnerboard_power1.voltage > 0) {
            return (vesc_burnerboard_power1.voltage);
        } else {
            return (vesc_can_status_msg_5.v_in);
        }
    }

    public float getRPM() {
        if ((SystemClock.elapsedRealtime() - vesc_can_status_msg.rx_time) < kAliveCheckMilliSeconds) {
            return (vesc_can_status_msg.rpm);
        }
        return 0;
    }

    public float getBatteryCurrent() {
        if (vesc_can_status_msg_4.current_in == 0) {
            return (vesc_burnerboard_power1.current);
        } else {
            return (kPresumedCurrentLoadLights + vesc_can_status_msg_4.current_in);
        }
    }

    private int getInt32(int[] data, int offset) {
        return data[offset] * 16777216 + data[offset + 1] * 65536 + data[offset + 2] * 256 + data[offset + 3];
    }

    private int getInt16(int[] data, int offset) {
        return data[offset] * 256 + data[offset + 1];
    }

    private float getFloat16(int[] data, int offset, float divider) {
        return (float) getInt16(data, offset) / divider;
    }

    private float getFloat32(int[] data, int offset, float divider) {
        return (float) getInt32(data, offset) / divider;
    }

    @Override
    public void canReceived(CanFrame f) {

        BLog.d(TAG, "VESC received id " + f.getId() + " DLC " + f.getDlc());
        int id = f.getId() & 0xFF;
        int cmd = f.getId() >> 8;
        int[] data = f.getData();
        BLog.d(TAG, "id " + id + " cmd " + cmd);

        if (cmd == VESC_CAN_PACKET_ID.CAN_PACKET_STATUS) {
            vesc_can_status_msg.rx_time = SystemClock.elapsedRealtime();
            vesc_can_status_msg.rpm = getInt32(data, 0);
            vesc_can_status_msg.current = getInt16(data, 4);
            vesc_can_status_msg.duty = getInt16(data, 6);
            BLog.d(TAG, " rpm " + vesc_can_status_msg.rpm + " current " + vesc_can_status_msg.current + " duty " + vesc_can_status_msg.duty);
        } else if (cmd == VESC_CAN_PACKET_ID.CAN_PACKET_STATUS_4) {
            vesc_can_status_msg_4.rx_time = SystemClock.elapsedRealtime();
            vesc_can_status_msg_4.temp_fet = getFloat16(data, 0, 10.0f);
            vesc_can_status_msg_4.temp_motor = getFloat16(data, 2, 10.0f);
            vesc_can_status_msg_4.current_in = getFloat16(data, 4, 1.0f);
            vesc_can_status_msg_4.pid_pos_now = getFloat16(data, 6, 1.0f);
            BLog.d(TAG, " temp_fet " + vesc_can_status_msg_4.temp_fet + " current_in " + vesc_can_status_msg_4.current_in);
        } else if (cmd == VESC_CAN_PACKET_ID.CAN_PACKET_STATUS_5) {
            vesc_can_status_msg_5.rx_time = SystemClock.elapsedRealtime();
            vesc_can_status_msg_5.v_in = getFloat16(data, 4, 10.0f);
            vesc_can_status_msg_5.tacho_value = getInt32(data, 0);
            BLog.d(TAG, " v_in " + vesc_can_status_msg_5.v_in + " tacho_value " + vesc_can_status_msg_5.tacho_value);
        } else if (cmd == VESC_CAN_PACKET_ID.CAN_PACKET_BURNERBOARD_POWER1) {
            vesc_burnerboard_power1.rx_time = SystemClock.elapsedRealtime();
            vesc_burnerboard_power1.voltage = getFloat32(data, 0, 100.0f);
            vesc_burnerboard_power1.current = getFloat32(data, 4, 100.0f);
            BLog.d(TAG, " voltage " + vesc_burnerboard_power1.voltage + " current " + vesc_burnerboard_power1.current);
        } else if (cmd == VESC_CAN_PACKET_ID.CAN_PACKET_BURNERBOARD_POWER2) {
            vesc_burnerboard_power2.rx_time = SystemClock.elapsedRealtime();
            vesc_burnerboard_power2.power = getFloat32(data, 0, 100.0f);
            vesc_burnerboard_power2.temp = getFloat32(data, 4, 100.0f);
            BLog.d(TAG,  " power " + vesc_burnerboard_power2.power + " temp " + vesc_burnerboard_power2.temp);
        }
    }

}

package com.richardmcdougall.bb.hardware;

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

    public VescController(BBService service, Canable canbus) {
        canbus.addListener(this);
    }

    public float getVoltage() {
        return (vesc_can_status_msg_5.v_in);
    }

    public int getRPM() {
        return (vesc_can_status_msg_5.tacho_value);
    }

    public float getBatteryCurrent() {
        return (vesc_can_status_msg_4.current_in);
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

        //BLog.d(TAG, "VESC received id " + f.getId() + " DLC " + f.getDlc());
        int id = f.getId() & 0xFF;
        int cmd = f.getId() >> 8;
        int[] data = f.getData();
        //BLog.d(TAG, "id " + id + " cmd " + cmd);

        if (cmd == vesc_can_packet_id.CAN_PACKET_STATUS) {
            vesc_can_status_msg.rpm = getInt32(data, 0);
            vesc_can_status_msg.current = getInt16(data, 4);
            vesc_can_status_msg.duty = getInt16(data, 6);
            //BLog.d(TAG, " rpm " + vesc_can_status_msg.rpm + " current " + vesc_can_status_msg.current + " duty " + vesc_can_status_msg.duty);
        } else if (cmd == vesc_can_packet_id.CAN_PACKET_STATUS_4) {
            vesc_can_status_msg_4.temp_fet = getFloat16(data, 0, 1.0f);
            vesc_can_status_msg_4.temp_motor = getFloat16(data, 2, 1.0f);
            vesc_can_status_msg_4.current_in = getFloat16(data, 4, 1.0f);
            vesc_can_status_msg_4.pid_pos_now = getFloat16(data, 6, 1.0f);
            //BLog.d(TAG, " temp_fet " + vesc_can_status_msg_4.temp_fet + " current_in " + vesc_can_status_msg_4.current_in);
        } else if (cmd == vesc_can_packet_id.CAN_PACKET_STATUS_5) {
            vesc_can_status_msg_5.v_in = getFloat16(data, 4, 10.0f);
            vesc_can_status_msg_5.tacho_value = getInt32(data, 0);
            //BLog.d(TAG, " v_in " + vesc_can_status_msg_5.v_in + " tacho_value " + vesc_can_status_msg_5.tacho_value);
        }
    }
}

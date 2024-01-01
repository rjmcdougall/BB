package com.richardmcdougall.bb.hardware;

public class VESC_CAN_STATUS_MSG {
    int id;
    int rx_time;
    float rpm;
    float current;
    float duty;

    public VESC_CAN_STATUS_MSG() {
    }
}

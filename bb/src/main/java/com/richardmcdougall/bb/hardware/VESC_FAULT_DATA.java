package com.richardmcdougall.bb.hardware;

// Logged fault data
public class VESC_FAULT_DATA {
    int motor;
    int fault;
    float current;
    float current_filtered;
    float voltage;
    float gate_driver_voltage;
    float duty;
    float rpm;
    int tacho;
    int cycles_running;
    int tim_val_samp;
    int tim_current_samp;
    int tim_top;
    int comm_step;
    float temperature;
    int drv8301_faults;
    String info_str;
    int info_argn;
    float info_args[];

    private VESC_FAULT_DATA() {

    }
}

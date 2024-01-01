package com.richardmcdougall.bb.hardware;

public class VESC_GNSS_DATA {
    double lat;
    double lon;
    float height;
    float speed;
    float hdop;
    int ms_today;
    int yy;
    int mo;
    int dd;
    int last_update;

    private VESC_GNSS_DATA() {

    }
}

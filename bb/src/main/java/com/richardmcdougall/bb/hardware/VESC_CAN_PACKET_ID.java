package com.richardmcdougall.bb.hardware;

// CAN commands
public final class VESC_CAN_PACKET_ID {
    public static final int CAN_PACKET_SET_DUTY = 0;
    public static final int CAN_PACKET_SET_CURRENT = 1;
    public static final int CAN_PACKET_SET_CURRENT_BRAKE = 2;
    public static final int CAN_PACKET_SET_RPM = 3;
    public static final int CAN_PACKET_SET_POS = 4;
    public static final int CAN_PACKET_FILL_RX_BUFFER = 5;
    public static final int CAN_PACKET_FILL_RX_BUFFER_LONG = 6;
    public static final int CAN_PACKET_PROCESS_RX_BUFFER = 7;
    public static final int CAN_PACKET_PROCESS_SHORT_BUFFER = 8;
    public static final int CAN_PACKET_STATUS = 9;
    public static final int CAN_PACKET_SET_CURRENT_REL = 10;
    public static final int CAN_PACKET_SET_CURRENT_BRAKE_REL = 11;
    public static final int CAN_PACKET_SET_CURRENT_HANDBRAKE = 12;
    public static final int CAN_PACKET_SET_CURRENT_HANDBRAKE_REL = 13;
    public static final int CAN_PACKET_STATUS_2 = 14;
    public static final int CAN_PACKET_STATUS_3 = 15;
    public static final int CAN_PACKET_STATUS_4 = 16;
    public static final int CAN_PACKET_PING = 17;
    public static final int CAN_PACKET_PONG = 18;
    public static final int CAN_PACKET_DETECT_APPLY_ALL_FOC = 19;
    public static final int CAN_PACKET_DETECT_APPLY_ALL_FOC_RES = 20;
    public static final int CAN_PACKET_CONF_CURRENT_LIMITS = 21;
    public static final int CAN_PACKET_CONF_STORE_CURRENT_LIMITS = 22;
    public static final int CAN_PACKET_CONF_CURRENT_LIMITS_IN = 23;
    public static final int CAN_PACKET_CONF_STORE_CURRENT_LIMITS_IN = 24;
    public static final int CAN_PACKET_CONF_FOC_ERPMS = 25;
    public static final int CAN_PACKET_CONF_STORE_FOC_ERPMS = 26;
    public static final int CAN_PACKET_STATUS_5 = 27;
    public static final int CAN_PACKET_POLL_TS5700N8501_STATUS = 28;
    public static final int CAN_PACKET_CONF_BATTERY_CUT = 29;
    public static final int CAN_PACKET_CONF_STORE_BATTERY_CUT = 30;
    public static final int CAN_PACKET_SHUTDOWN = 31;
    public static final int CAN_PACKET_IO_BOARD_ADC_1_TO_4 = 32;
    public static final int CAN_PACKET_IO_BOARD_ADC_5_TO_8 = 33;
    public static final int CAN_PACKET_IO_BOARD_ADC_9_TO_12 = 34;
    public static final int CAN_PACKET_IO_BOARD_DIGITAL_IN = 35;
    public static final int CAN_PACKET_IO_BOARD_SET_OUTPUT_DIGITAL = 36;
    public static final int CAN_PACKET_IO_BOARD_SET_OUTPUT_PWM = 37;
    public static final int CAN_PACKET_BMS_V_TOT = 38;
    public static final int CAN_PACKET_BMS_I = 39;
    public static final int CAN_PACKET_BMS_AH_WH = 40;
    public static final int CAN_PACKET_BMS_V_CELL = 41;
    public static final int CAN_PACKET_BMS_BAL = 42;
    public static final int CAN_PACKET_BMS_TEMPS = 43;
    public static final int CAN_PACKET_BMS_HUM = 44;
    public static final int CAN_PACKET_BMS_SOC_SOH_TEMP_STAT = 45;
    public static final int CAN_PACKET_PSW_STAT = 46;
    public static final int CAN_PACKET_PSW_SWITCH = 47;
    public static final int CAN_PACKET_BMS_HW_DATA_1 = 48;
    public static final int CAN_PACKET_BMS_HW_DATA_2 = 49;
    public static final int CAN_PACKET_BMS_HW_DATA_3 = 50;
    public static final int CAN_PACKET_BMS_HW_DATA_4 = 51;
    public static final int CAN_PACKET_BMS_HW_DATA_5 = 52;
    public static final int CAN_PACKET_BMS_AH_WH_CHG_TOTAL = 53;
    public static final int CAN_PACKET_BMS_AH_WH_DIS_TOTAL = 54;
    public static final int CAN_PACKET_UPDATE_PID_POS_OFFSET = 55;
    public static final int CAN_PACKET_POLL_ROTOR_POS = 56;
    public static final int CAN_PACKET_NOTIFY_BOOT = 57;
    public static final int CAN_PACKET_STATUS_6 = 58;
    public static final int CAN_PACKET_GNSS_TIME = 59;
    public static final int CAN_PACKET_GNSS_LAT = 60;
    public static final int CAN_PACKET_GNSS_LON = 61;
    public static final int CAN_PACKET_GNSS_ALT_SPEED_HDOP = 62;
    public static final int CAN_PACKET_BURNERBOARD_POWER1 = 100;
    public static final int CAN_PACKET_BURNERBOARD_POWER2 = 101;
    public static final int CAN_PACKET_MAKE_ENUM_32_BITS = 0xFFFFFFFF;

    private VESC_CAN_PACKET_ID() {
        throw new IllegalAccessError("Non-instantiable class");
    }
}

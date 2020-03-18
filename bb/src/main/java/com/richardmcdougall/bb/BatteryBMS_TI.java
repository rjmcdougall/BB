package com.richardmcdougall.bb;

import android.content.Context;

import com.richardmcdougall.bbcommon.BLog;
import com.richardmcdougall.bbcommon.BoardState;

import java.io.IOException;


public class BatteryBMS_TI {
    private String TAG = this.getClass().getSimpleName();
    private BQ34Z100 mMBS = null;

    public BatteryBMS_TI(Context context, BoardState boardState) {

        BLog.e(TAG, "TI BMS startihng");
        try {
            BLog.e(TAG, "TI BMS  opening" + mMBS);
            mMBS = BQ34Z100.open("I2C2");
            BLog.e(TAG, "TI BMS  opened" + mMBS);

        } catch (IOException e) {
            BLog.e(TAG, "Cannot open TI BMS ");
        }


        BLog.e(TAG, "Status: voltage_volts:             " + String.format("%f", mMBS.voltage_volts()));
        BLog.e(TAG, "Status: average_current_amps:      " + String.format("%f", mMBS.average_current_amps()));
        BLog.e(TAG, "Status: current_amps:              " + String.format("%f", mMBS.current_amps()));
        BLog.e(TAG, "Status: state_of_charge_pct:       " + String.format("%d", mMBS.state_of_charge_pct()));
        BLog.e(TAG, "Status: state_of_charge_max_error: " + String.format("%d", mMBS.state_of_charge_max_error()));
        BLog.e(TAG, "Status: remaining_capacity:        " + String.format("%d", mMBS.remaining_capacity()));
        BLog.e(TAG, "Status: full_charge_capacity:      " + String.format("%d", mMBS.full_charge_capacity()));
        BLog.e(TAG, "Status: temperature:               " + String.format("%d", mMBS.temperature()));
        BLog.e(TAG, "Status: flags:                     " + String.format("%x", mMBS.flags()));
        BLog.e(TAG, "Status: flags_b:                   " + String.format("%x", mMBS.flags_b()));
        BLog.e(TAG, "Status: average_time_to_empty:     " + String.format("%d", mMBS.average_time_to_empty()));
        BLog.e(TAG, "Status: average_time_to_full:      " + String.format("%d", mMBS.average_time_to_full()));
        BLog.e(TAG, "Status: passed_charge:             " + String.format("%d", mMBS.passed_charge()));
        BLog.e(TAG, "Status: do_d0_time:                " + String.format("%d", mMBS.do_d0_time()));
        BLog.e(TAG, "Status: available_energy:          " + String.format("%d", mMBS.available_energy()));
        BLog.e(TAG, "Status: average_power:             " + String.format("%d", mMBS.average_power()));
        BLog.e(TAG, "Status: internal_temperature:      " + String.format("%d", mMBS.internal_temperature()));
        BLog.e(TAG, "Status: state_of_health:           " + String.format("%d", mMBS.state_of_health()));
        BLog.e(TAG, "Status: cycle_count:               " + String.format("%d", mMBS.cycle_count()));
        BLog.e(TAG, "Status: grid_number:               " + String.format("%d", mMBS.grid_number()));
        BLog.e(TAG, "Status: learned_status:            " + String.format("%d", mMBS.learned_status()));
        BLog.e(TAG, "Status: dod_at_eoc:                " + String.format("%d", mMBS.dod_at_eoc()));
        BLog.e(TAG, "Status: q_start:                   " + String.format("%d", mMBS.q_start()));
        BLog.e(TAG, "Status: true_fcc:                  " + String.format("%d", mMBS.true_fcc()));
        BLog.e(TAG, "Status: state_time:                " + String.format("%d", mMBS.state_time()));
        BLog.e(TAG, "Status: q_max_passed_q:            " + String.format("%d", mMBS.q_max_passed_q()));
        BLog.e(TAG, "Status: dod_0:                     " + String.format("%d", mMBS.dod_0()));
        BLog.e(TAG, "Status: q_max_dod_0:               " + String.format("%d", mMBS.q_max_dod_0()));
        BLog.e(TAG, "Status: q_max_time:                " + String.format("%d", mMBS.q_max_time()));

        BLog.e(TAG, "BMS:    design_capacity:           " + String.format("%d", mMBS.design_capacity()));
        BLog.e(TAG, "BMS:    control_status:            " + String.format("%x", mMBS.control_status()));
        BLog.e(TAG, "BMS:    fw_version:                " + String.format("%d", mMBS.fw_version()));
        BLog.e(TAG, "BMS:    hw_version:                " + String.format("%d", mMBS.hw_version()));
        BLog.e(TAG, "BMS:    chem_id:                   " + String.format("%d", mMBS.chem_id()));
        BLog.e(TAG, "BMS:    prev_macwrite:             " + String.format("%d", mMBS.prev_macwrite()));
        BLog.e(TAG, "BMS:    board_offset:              " + String.format("%d", mMBS.board_offset()));
        BLog.e(TAG, "BMS:    cc_offset:                 " + String.format("%d", mMBS.cc_offset()));
        BLog.e(TAG, "BMS:    df_version:                " + String.format("%d", mMBS.df_version()));
        BLog.e(TAG, "BMS:    sealed:                    " + String.format("%d", mMBS.sealed()));
        BLog.e(TAG, "BMS:    control_status:            " + String.format("%d", mMBS.control_status()));
        BLog.e(TAG, "BMS:    pack_configuration:        " + String.format("%d", mMBS.pack_configuration()));
        BLog.e(TAG, "BMS:    serial_number:             " + String.format("%d", mMBS.serial_number()));
        BLog.e(TAG, "BMS:    charge_voltage:            " + String.format("%d", mMBS.charge_voltage()));
        BLog.e(TAG, "BMS:    charge_current:            " + String.format("%d", mMBS.charge_current()));
    }

}


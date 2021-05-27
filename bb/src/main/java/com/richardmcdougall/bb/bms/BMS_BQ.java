package com.richardmcdougall.bb.bms;

import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bb.hardware.BQ34Z100;
import com.richardmcdougall.bbcommon.BLog;

import java.io.IOException;
import java.util.Arrays;

public class BMS_BQ extends BMS {
    private String TAG = this.getClass().getSimpleName();
    private BQ34Z100 mBQ = null;

    public BMS_BQ(BBService service) {
        super(service);

        BLog.e(TAG, "TI BMS startihng");
        try {
            BLog.e(TAG, "TI BMS  opening" + mBQ);
            mBQ = BQ34Z100.open("I2C2");
            BLog.e(TAG, "TI BMS  opened" + mBQ);

        } catch (IOException e) {
            BLog.e(TAG, "Cannot open TI BMS ");
        }

        try {
            BLog.e(TAG, "Status: voltage_volts:             " + String.format("%f", mBQ.voltage_volts()));
            BLog.e(TAG, "Status: average_current_amps:      " + String.format("%f", mBQ.average_current_amps()));
            BLog.e(TAG, "Status: current_amps:              " + String.format("%f", mBQ.current_amps()));
            BLog.e(TAG, "Status: state_of_charge_pct:       " + String.format("%d", mBQ.state_of_charge_pct()));
            BLog.e(TAG, "Status: state_of_charge_max_error: " + String.format("%d", mBQ.state_of_charge_max_error()));
            BLog.e(TAG, "Status: remaining_capacity:        " + String.format("%d", mBQ.remaining_capacity()));
            BLog.e(TAG, "Status: full_charge_capacity:      " + String.format("%d", mBQ.full_charge_capacity()));
            BLog.e(TAG, "Status: temperature:               " + String.format("%d", mBQ.temperature()));
            BLog.e(TAG, "Status: flags:                     " + String.format("%x", mBQ.flags()));
            BLog.e(TAG, "Status: flags_b:                   " + String.format("%x", mBQ.flags_b()));
            BLog.e(TAG, "Status: average_time_to_empty:     " + String.format("%d", mBQ.average_time_to_empty()));
            BLog.e(TAG, "Status: average_time_to_full:      " + String.format("%d", mBQ.average_time_to_full()));
            BLog.e(TAG, "Status: passed_charge:             " + String.format("%d", mBQ.passed_charge()));
            BLog.e(TAG, "Status: do_d0_time:                " + String.format("%d", mBQ.do_d0_time()));
            BLog.e(TAG, "Status: available_energy:          " + String.format("%d", mBQ.available_energy()));
            BLog.e(TAG, "Status: average_power:             " + String.format("%d", mBQ.average_power()));
            BLog.e(TAG, "Status: internal_temperature:      " + String.format("%d", mBQ.internal_temperature()));
            BLog.e(TAG, "Status: state_of_health:           " + String.format("%d", mBQ.state_of_health()));
            BLog.e(TAG, "Status: cycle_count:               " + String.format("%d", mBQ.cycle_count()));
            BLog.e(TAG, "Status: grid_number:               " + String.format("%d", mBQ.grid_number()));
            BLog.e(TAG, "Status: learned_status:            " + String.format("%d", mBQ.learned_status()));
            BLog.e(TAG, "Status: dod_at_eoc:                " + String.format("%d", mBQ.dod_at_eoc()));
            BLog.e(TAG, "Status: q_start:                   " + String.format("%d", mBQ.q_start()));
            BLog.e(TAG, "Status: true_fcc:                  " + String.format("%d", mBQ.true_fcc()));
            BLog.e(TAG, "Status: state_time:                " + String.format("%d", mBQ.state_time()));
            BLog.e(TAG, "Status: q_max_passed_q:            " + String.format("%d", mBQ.q_max_passed_q()));
            BLog.e(TAG, "Status: dod_0:                     " + String.format("%d", mBQ.dod_0()));
            BLog.e(TAG, "Status: q_max_dod_0:               " + String.format("%d", mBQ.q_max_dod_0()));
            BLog.e(TAG, "Status: q_max_time:                " + String.format("%d", mBQ.q_max_time()));

            BLog.e(TAG, "BMS:    design_capacity:           " + String.format("%d", mBQ.design_capacity()));
            BLog.e(TAG, "BMS:    control_status:            " + String.format("%x", mBQ.control_status()));
            BLog.e(TAG, "BMS:    fw_version:                " + String.format("%d", mBQ.fw_version()));
            BLog.e(TAG, "BMS:    hw_version:                " + String.format("%d", mBQ.hw_version()));
            BLog.e(TAG, "BMS:    chem_id:                   " + String.format("%d", mBQ.chem_id()));
            BLog.e(TAG, "BMS:    prev_macwrite:             " + String.format("%d", mBQ.prev_macwrite()));
            BLog.e(TAG, "BMS:    board_offset:              " + String.format("%d", mBQ.board_offset()));
            BLog.e(TAG, "BMS:    cc_offset:                 " + String.format("%d", mBQ.cc_offset()));
            BLog.e(TAG, "BMS:    df_version:                " + String.format("%d", mBQ.df_version()));
            BLog.e(TAG, "BMS:    sealed:                    " + String.format("%d", mBQ.sealed()));
            BLog.e(TAG, "BMS:    control_status:            " + String.format("%d", mBQ.control_status()));
            BLog.e(TAG, "BMS:    pack_configuration:        " + String.format("%d", mBQ.pack_configuration()));
            BLog.e(TAG, "BMS:    serial_number:             " + String.format("%d", mBQ.serial_number()));
            BLog.e(TAG, "BMS:    charge_voltage:            " + String.format("%d", mBQ.charge_voltage()));
            BLog.e(TAG, "BMS:    charge_current:            " + String.format("%d", mBQ.charge_current()));
        } catch (IOException e) {
            BLog.e(TAG, "Cannot read battery: " + e.getMessage());
        }
    }

    public void update() throws IOException {
        // TODO: why do we keep this state here?
        service.boardState.batteryLevel = mBQ.state_of_charge_pct();
    }

    public float get_voltage() throws IOException {
        return mBQ.voltage_volts();
    }

    public float get_current()  throws IOException {
        return mBQ.average_current_amps();
    }

    public float get_current_instant()  throws IOException {
        return mBQ.current_amps();
    }

    public float get_level()  throws IOException {
        return mBQ.state_of_charge_pct();
    }
}


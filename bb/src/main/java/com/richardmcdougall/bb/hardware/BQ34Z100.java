package com.richardmcdougall.bb.hardware;

import com.google.android.things.pio.I2cDevice;
import com.google.android.things.pio.PeripheralManager;
import com.richardmcdougall.bbcommon.BLog;

import java.io.IOException;
import java.util.List;

public class BQ34Z100 implements AutoCloseable {
    private final static String TAG = "BQ34Z100";

    private I2cDevice mDevice;
    private byte[] mBuffer = null;
    private boolean mIsReady = false;

    private static final int kScale = 10;

    byte[] flash_block_data = new byte[32];

    private final static int BQ34Z100_G1_ADDRESS = 0x55;

    private BQ34Z100(I2cDevice device) {
        mDevice = device;
        mBuffer = new byte[2];
        init();
    }

    private void init() {
        BLog.e(TAG, "MPU init");
        mIsReady = true;
    }

    // FFS, make some redundant methods to open this passing the bus
    public static BQ34Z100 open() throws IOException {
        BLog.e(TAG, "open getDefaultBus");
        String bus = getDefaultBus();
        BLog.e(TAG, "open getDefaultBus: " + bus);

        return open(bus);
    }

    public static BQ34Z100 open(String busName) throws IOException {
        BLog.e(TAG, "open bus PeripheralManager.getInstance():" + busName);

        PeripheralManager pioService = null;

        try {
            pioService = PeripheralManager.getInstance();
            BLog.e(TAG, "open bus pioService.openI2cDevice:" + busName);
            I2cDevice device = pioService.openI2cDevice(busName, BQ34Z100_G1_ADDRESS);
            return new BQ34Z100(device);
        } catch (Exception e) {
            BLog.e(TAG, "Cannot open android things...");
            return null;
        }
    }

    protected static String getDefaultBus() {
        BLog.e(TAG, "MPU getDefaultBus");

        PeripheralManager peripheralManagerService = PeripheralManager.getInstance();
        List<String> deviceList = peripheralManagerService.getI2cBusList();
        if (deviceList.isEmpty()) {
            return "I2C2";
        } else {
            return deviceList.get(0);
        }
    }

    public boolean IsReady() {
        return mIsReady;
    }
/*
 1. Update design capacity.
 2. Update Q max.
 3. Update design energy.
 4. Update cell charge voltage range.
 5. Update number of series cells.
 6. Update pack configuration.
 7. Update charge termination parameters.
 8. Calibrate cc offset. (No current applied)
 9. Calibrate board offset. (No currrent applied)
 10. Calibrate voltage divider. (Apply known voltage and use that)
 11. Calibrate sense resistor. (Apply known current, current from battery is negetive)
 12. Set current deadband if current is non zero without load.
 13. Set ready and start learning cycle.
 */


    private int read_register(int address, int length) throws IOException {
        try {
            mDevice.readRegBuffer(address, mBuffer, length);
        } catch (Exception e) {
            BLog.e(TAG, "Cannot read reg  " + address);
            throw new IOException("Cannot read reg  " + address);
        }
        int temp = 0;
        for (byte i = 0; i < length; i++) {
            // BLog.d(TAG, "byte: " + (mBuffer[i] & 0xff));
            temp |= (mBuffer[i] & 0xff) << (8 * i);
        }
        // BLog.d(TAG, "result: " + temp);
        return temp;
    }

    int read_control(int address_lsb, int address_msb) throws IOException {
        byte[] address = {(byte) (address_lsb & 0xff), (byte) (address_msb & 0xff)};
        int response = 0;
        try {
            mDevice.writeRegBuffer(0x00, address, 2);
            response = mDevice.readRegWord(0x00);
        } catch (Exception e) {
            BLog.e(TAG, "Cannot read control  " + address);
            throw new IOException(("Cannot read control  " + address));
        }

        return response;
    }

    void read_flash_block(int sub_class, int offset) throws IOException {
        write_reg(0x61, 0x00); // Block control
        write_reg(0x3e, sub_class); // Flash class
        write_reg(0x3f, offset / 32); // Flash block
        read_reg_buffer(0x40, flash_block_data); // Data
    }

    void read_reg_buffer(int addr, byte[] buffer) throws IOException {
        try {
            mDevice.readRegBuffer(addr, buffer, buffer.length);
        } catch (Exception e) {
            BLog.e(TAG, "Cannot write Battery reg " + addr);
            throw new IOException("Cannot write Battery reg " + addr);
        }
    }

    int scaled(int value) {
        return kScale * value;
    }

    void write_reg(int addr, int val) throws IOException {
        try {
            mDevice.writeRegByte(addr, (byte) (val & 0xff));
        } catch (Exception e) {
            BLog.e(TAG, "Cannot write Battery  reg " + addr);
            throw new IOException("Cannot write Battery  reg " + addr);
        }
    }

    void write_reg_buffer(int addr, byte[] buffer) throws IOException {
        try {
            mDevice.writeRegBuffer(addr, buffer, buffer.length);
        } catch (Exception e) {
            BLog.e(TAG, "Cannot write Battery reg " + addr);
            throw new IOException("Cannot write Battery reg " + addr);
        }
    }


    void write_flash_block(int sub_class, int offset) throws IOException {
        write_reg(0x61, 0x00); // Block control
        write_reg(0x3e, sub_class); // Flash class
        write_reg(0x3f, (offset / 32)); // Flash block
        write_reg_buffer(0x40, flash_block_data); // Data
    }

    int flash_block_checksum() {
        int temp = 0;
        for (int i = 0; i < 32; i++) {
            temp += flash_block_data[i];
        }
        return 255 - temp;
    }

    double xemics_to_double(int value) {
        boolean is_negative = false;
        if ((value & 0x800000) > 0) {
            is_negative = true;
        }
        int exp_gain = (value >> 24) - 128 - 24;
        double exponent = 2 ^ exp_gain;
        double mantissa = ((value & 0xffffff) | 0x800000);
        if (is_negative) {
            return mantissa * exponent * -1;
        }
        return mantissa * exponent;
    }

    int bit_log2(int value) {
        int result = 0;
        while ((value >> 1) > 0) {
            result++;
        }
        return result;
    }


    // Check use of bit-wise log
    int double_to_xemics(double value) {
        boolean is_negative = false;
        if (value < 0) {
            is_negative = true;
            value *= -1;
        }
        int exponent;
        if (value > 1) {
            exponent = (bit_log2((int) value)) + 1;
        } else {
            exponent = (bit_log2((int) value));
        }
        double mantissa = value / (2 ^ exponent * 2 ^ -24);
        if (is_negative) {
            return (((exponent + 128) << 24) | (int) mantissa) | 0x800000;
        }
        return ((exponent + 128) << 24) | ((int) mantissa & 0x7fffff);
    }

    void unsealed() throws IOException {
        byte[] control1 = {0x14, 0x04};
        byte[] control2 = {0x72, 0x36};
        write_reg_buffer(0, control1);
        write_reg_buffer(0, control2);
    }

    void delay(int ms) {
        try {
            Thread.sleep(ms);
        } catch (Exception e) {
        }
    }


    void enter_calibration() throws IOException {
        unsealed();
        do {
            cal_enable();
            enter_cal();
            delay(1000);
        } while ((control_status() & 0x1000) == 0); // CALEN
    }

    void exit_calibration() throws IOException {
        do {
            exit_cal();
            delay(1000);
        } while ((control_status() & ~0x1000) == 0); // CALEN

        delay(150);
        reset();
        delay(150);
    }

    boolean update_design_capacity(int capacity) throws IOException {
        unsealed();
        read_flash_block(48, 0);

        flash_block_data[6] = 0; // Cycle Count
        flash_block_data[7] = 0;

        flash_block_data[8] = (byte) ((capacity & 0xff) >> 8); // CC Threshold
        flash_block_data[9] = (byte) (capacity & 0xff);

        flash_block_data[11] = (byte) ((capacity & 0xff) >> 8); // Design Capacity
        flash_block_data[12] = (byte) (capacity & 0xff);

        for (byte i = 6; i <= 9; i++) {
            write_reg(0x40 + i, flash_block_data[i]);
        }

        for (byte i = 11; i <= 12; i++) {
            write_reg(0x40 + i, flash_block_data[i]);
        }

        write_reg(0x60, flash_block_checksum());

        delay(150);
        reset();
        delay(150);

        unsealed();
        read_flash_block(48, 0);
        int updated_cc_threshold = flash_block_data[8] << 8;
        updated_cc_threshold |= flash_block_data[9];

        int updated_capacity = flash_block_data[11] << 8;
        updated_capacity |= flash_block_data[12];

        if (flash_block_data[6] != 0 || flash_block_data[7] != 0) {
            return false;
        }
        if (capacity != updated_cc_threshold) {
            return false;
        }
        if (capacity != updated_capacity) {
            return false;
        }
        return true;
    }

    boolean update_q_max(int capacity) throws IOException {
        unsealed();
        read_flash_block(82, 0);
        flash_block_data[0] = (byte) ((capacity >> 8) & 0xff); // Q Max
        flash_block_data[1] = (byte) (capacity & 0xff);

        flash_block_data[2] = 0; // Cycle Count
        flash_block_data[3] = 0;

        for (int i = 0; i <= 3; i++) {
            write_reg(0x40 + i, flash_block_data[i]);
        }

        write_reg(0x60, flash_block_checksum());

        delay(150);
        reset();
        delay(150);

        unsealed();
        read_flash_block(82, 0);
        int updated_q_max = flash_block_data[0] << 8;
        updated_q_max |= flash_block_data[1];

        if (capacity != updated_q_max) {
            return false;
        }
        return true;
    }

    boolean update_design_energy(int energy) throws IOException {
        unsealed();
        read_flash_block(48, 0);
        flash_block_data[13] = (byte) ((energy >> 8) & 0xff); // Design Energy
        flash_block_data[14] = (byte) (energy & 0xff);

        for (byte i = 13; i <= 14; i++) {
            write_reg(0x40 + i, flash_block_data[i]);
        }

        write_reg(0x60, flash_block_checksum());

        delay(150);
        reset();
        delay(150);

        unsealed();
        read_flash_block(48, 0);
        int updated_energy = flash_block_data[13] << 8;
        updated_energy |= flash_block_data[14];

        if (energy != updated_energy) {
            return false;
        }
        return true;
    }

    boolean update_cell_charge_voltage_range(int t1_t2, int t2_t3, int t3_t4) throws IOException {
        unsealed();
        read_flash_block(48, 0);

        flash_block_data[17] = (byte) ((t1_t2 >> 8) & 0xff); // Cell Charge Voltage T1-T2
        flash_block_data[18] = (byte) (t1_t2 & 0xff);

        flash_block_data[19] = (byte) ((t2_t3 >> 8) & 0xff); // Cell Charge Voltage T2-T3
        flash_block_data[20] = (byte) (t2_t3 & 0xff);

        flash_block_data[21] = (byte) ((t3_t4 >> 8) & 0xff); // Cell Charge Voltage T3-T4
        flash_block_data[22] = (byte) (t3_t4 & 0xff);

        for (byte i = 17; i <= 22; i++) {
            write_reg(0x40 + i, flash_block_data[i]);
        }

        write_reg(0x60, flash_block_checksum());

        delay(150);
        reset();
        delay(150);

        unsealed();
        read_flash_block(48, 0);
        int updated_t1_t2 = flash_block_data[17] << 8;
        updated_t1_t2 |= flash_block_data[18];

        int updated_t2_t3 = flash_block_data[19] << 8;
        updated_t2_t3 |= flash_block_data[20];

        int updated_t3_t4 = flash_block_data[21] << 8;
        updated_t3_t4 |= flash_block_data[22];

        if (t1_t2 != updated_t1_t2 || t2_t3 != updated_t2_t3 || t3_t4 != updated_t3_t4) {
            return false;
        }
        return true;
    }

    boolean update_number_of_series_cells(int cells) throws IOException {
        unsealed();
        read_flash_block(64, 0);

        flash_block_data[7] = (byte) (cells & 0xff); // Number of Series Cell

        write_reg(0x40 + 7, flash_block_data[7]);

        write_reg(0x60, flash_block_checksum());

        delay(150);
        reset();
        delay(150);

        unsealed();
        read_flash_block(64, 0);

        if (cells != flash_block_data[7]) {
            return false;
        }
        return true;
    }

    boolean update_pack_configuration(int config) throws IOException {
        unsealed();
        read_flash_block(64, 0);

        flash_block_data[0] = (byte) ((config >> 8) & 0xff); // Pack Configuration
        flash_block_data[1] = (byte) (config & 0xff);

        for (byte i = 0; i <= 1; i++) {
            write_reg(0x40 + i, flash_block_data[i]);
        }

        write_reg(0x60, flash_block_checksum());

        delay(150);
        reset();
        delay(150);

        unsealed();
        read_flash_block(64, 0);
        int updated_config = flash_block_data[0] << 8;
        updated_config |= flash_block_data[1];
        if (config != updated_config) {
            return false;
        }
        return true;
    }

    boolean update_charge_termination_parameters(int taper_current,
                                                 int min_taper_capacity,
                                                 int cell_taper_voltage,
                                                 int taper_window,
                                                 int tca_set,
                                                 int tca_clear,
                                                 int fc_set,
                                                 int fc_clear) throws IOException {
        unsealed();
        read_flash_block(36, 0);

        flash_block_data[0] = (byte) ((taper_current >> 8) & 0xff); // Taper Current
        flash_block_data[1] = (byte) (taper_current & 0xff);

        flash_block_data[2] = (byte) ((min_taper_capacity >> 8) & 0xff); // Min Taper Capacity
        flash_block_data[3] = (byte) (min_taper_capacity & 0xff);

        flash_block_data[4] = (byte) ((cell_taper_voltage >> 8) & 0xff); // Cell Taper Voltage
        flash_block_data[5] = (byte) (cell_taper_voltage & 0xff);

        flash_block_data[6] = (byte) (taper_window & 0xff); // Current Taper Window

        flash_block_data[7] = (byte) (tca_set & 0xff); // TCA Set %

        flash_block_data[8] = (byte) (tca_clear & 0xff); // TCA Clear %

        flash_block_data[9] = (byte) (fc_set & 0xff); // FC Set %

        flash_block_data[10] = (byte) (fc_clear & 0xff); // FC Clear %

        for (byte i = 0; i <= 10; i++) {
            write_reg(0x40 + i, flash_block_data[i]);
        }

        write_reg(0x60, flash_block_checksum());

        delay(150);
        reset();
        delay(150);

        unsealed();
        read_flash_block(36, 0);
        int updated_taper_current, updated_min_taper_capacity, updated_cell_taper_voltage;
        byte updated_taper_window;
        int updated_tca_set, updated_tca_clear, updated_fc_set, updated_fc_clear;

        updated_taper_current = flash_block_data[0] << 8;
        updated_taper_current |= flash_block_data[1];

        updated_min_taper_capacity = flash_block_data[2] << 8;
        updated_min_taper_capacity |= flash_block_data[3];

        updated_cell_taper_voltage = flash_block_data[4] << 8;
        updated_cell_taper_voltage |= flash_block_data[5];

        updated_taper_window = flash_block_data[6];

        updated_tca_set = flash_block_data[7] & 0xff;

        updated_tca_clear = flash_block_data[8] & 0xff;

        updated_fc_set = flash_block_data[9] & 0xff;

        updated_fc_clear = flash_block_data[10] & 0xff;

        if (taper_current != updated_taper_current) {
            return false;
        }
        if (min_taper_capacity != updated_min_taper_capacity) {
            return false;
        }
        if (cell_taper_voltage != updated_cell_taper_voltage) {
            return false;
        }
        if (taper_window != updated_taper_window) {
            return false;
        }
        if (tca_set != updated_tca_set) {
            return false;
        }
        if (tca_clear != updated_tca_clear) {
            return false;
        }
        if (fc_set != updated_fc_set) {
            return false;
        }
        if (fc_clear != updated_fc_clear) {
            return false;
        }
        return true;
    }

    void calibrate_cc_offset() throws IOException {
        enter_calibration();
        do {
            cc_offset();
            delay(1000);
        } while ((control_status() & 0x0800) != 0); // CCA

        do {
            delay(1000);
        } while ((control_status() & ~0x0800) != 0); // CCA

        cc_offset_save();
        exit_calibration();
    }

    void calibrate_board_offset() throws IOException {
        enter_calibration();
        do {
            board_offset();
            delay(1000);
        } while ((control_status() & 0x0c00) != 0); // CCA + BCA

        do {
            delay(1000);
        } while ((control_status() & ~0x0c00) != 0); // CCA + BCA

        cc_offset_save();
        exit_calibration();
    }

    void calibrate_voltage_divider(int applied_voltage, int cells_count) throws IOException  {
        double[] volt_array = new double[50];
        for (byte i = 0; i < 50; i++) {
            volt_array[i] = voltage();
            delay(150);
        }
        double volt_mean = 0;
        for (byte i = 0; i < 50; i++) {
            volt_mean += volt_array[i];
        }
        volt_mean /= 50.0;

        double volt_sd = 0;
        for (byte i = 0; i < 50; i++) {
            volt_sd += Math.pow(volt_array[i] - volt_mean, 2);
        }
        volt_sd /= 50.0;
        volt_sd = Math.sqrt(volt_sd);

        if (volt_sd > 100) {
            return;
        }

        unsealed();
        read_flash_block(104, 0);

        int current_voltage_divider = flash_block_data[14] << 8;
        current_voltage_divider |= flash_block_data[15];

        int new_voltage_divider = (int) (((double) applied_voltage / volt_mean) * (double) current_voltage_divider);

        flash_block_data[14] = (byte) ((new_voltage_divider >> 8) & 0xff);
        flash_block_data[15] = (byte) (new_voltage_divider & 0xff);

        for (byte i = 14; i <= 15; i++) {
            write_reg(0x40 + i, flash_block_data[i]);
        }

        write_reg(0x60, flash_block_checksum());
        delay(150);

        unsealed();
        read_flash_block(68, 0);

        int flash_update_of_cell_voltage = (int) ((double) (2800 * cells_count * 5000) / (double) new_voltage_divider);

        flash_block_data[0] = (byte) ((flash_update_of_cell_voltage << 8) & 0xff);
        flash_block_data[1] = (byte) (flash_update_of_cell_voltage & 0xff);

        for (byte i = 0; i <= 1; i++) {
            write_reg(0x40 + i, flash_block_data[i]);
        }

        write_reg(0x60, flash_block_checksum());

        delay(150);
        reset();
        delay(150);
    }

    void calibrate_sense_resistor(int applied_current) throws IOException  {
        double[] current_array = new double[50];
        for (byte i = 0; i < 50; i++) {
            current_array[i] = current_ma();
            delay(150);
        }
        double current_mean = 0;
        for (byte i = 0; i < 50; i++) {
            current_mean += current_array[i];
        }
        current_mean /= 50.0;

        double current_sd = 0;
        for (byte i = 0; i < 50; i++) {
            current_sd += Math.pow(current_array[i] - current_mean, 2);
        }
        current_sd /= 50.0;
        current_sd = Math.sqrt(current_sd);

        if (current_sd > 100) {
            return;
        }

        unsealed();
        read_flash_block(104, 0);

        int cc_gain = flash_block_data[0] << 24;
        cc_gain |= flash_block_data[1] << 16;
        cc_gain |= flash_block_data[2] << 8;
        cc_gain |= flash_block_data[3];

        double gain_resistence = 4.768 / xemics_to_double(cc_gain);

        double temp = (current_mean * gain_resistence) / (double) applied_current;

        int new_cc_gain = double_to_xemics(4.768 / temp);
        flash_block_data[0] = (byte) ((new_cc_gain >> 24) & 0xff);
        flash_block_data[1] = (byte) ((new_cc_gain >> 16) & 0xff);
        flash_block_data[2] = (byte) ((new_cc_gain >> 8) & 0xff);
        flash_block_data[3] = (byte) (new_cc_gain & 0xff);

        new_cc_gain = double_to_xemics(5677445.6 / temp);
        flash_block_data[4] = (byte) ((new_cc_gain >> 24) & 0xff);
        flash_block_data[5] = (byte) ((new_cc_gain >> 16) & 0xff);
        flash_block_data[6] = (byte) ((new_cc_gain >> 8) & 0xff);
        flash_block_data[7] = (byte) (new_cc_gain & 0xff);


        for (byte i = 0; i <= 3; i++) {
            write_reg(0x40 + i, flash_block_data[i]);
        }

        for (byte i = 4; i <= 7; i++) {
            write_reg(0x40 + i, flash_block_data[i]);
        }

        write_reg(0x60, flash_block_checksum());
        delay(150);
        reset();
        delay(150);
    }

    void set_current_deadband(byte deadband) throws IOException  {
        unsealed();
        read_flash_block(107, 0);

        flash_block_data[1] = deadband;

        write_reg(0x40 + 1, flash_block_data[1]);

        write_reg(0x60, flash_block_checksum());

        delay(150);
        reset();
        delay(150);
    }

    void ready() throws IOException  {
        unsealed();
        it_enable();
        sealed();
    }

    public int control_status() throws IOException {
        return read_control(0x00, 0x00);
    }

    public int device_type() throws IOException {
        return read_control(0x01, 0x00);
    }

    public int fw_version() throws IOException {
        return read_control(0x02, 0x00);
    }

    public int hw_version() throws IOException {
        return read_control(0x03, 0x00);
    }

    public int reset_data() throws IOException {
        return read_control(0x05, 0x00);
    }

    public int prev_macwrite() throws IOException {
        return read_control(0x07, 0x00);
    }

    public int chem_id() throws IOException {
        return read_control(0x08, 0x00);
    }

    public int board_offset() throws IOException  {
        return read_control(0x09, 0x00);
    }

    public int cc_offset() throws IOException  {
        return read_control(0x0a, 0x00);
    }

    public int cc_offset_save() throws IOException  {
        return read_control(0x0b, 0x00);
    }

    public int df_version() throws IOException  {
        return read_control(0x0c, 0x00);
    }

    public int set_fullsleep() throws IOException  {
        return read_control(0x10, 0x00);
    }

    public int static_chem_chksum() throws IOException  {
        return read_control(0x17, 0x00);
    }

    public int sealed() throws IOException  {
        return read_control(0x20, 0x00);
    }

    public int it_enable() throws IOException  {
        return read_control(0x21, 0x00);
    }

    public int cal_enable() throws IOException  {
        return read_control(0x2d, 0x00);
    }

    public int reset() throws IOException  {
        return read_control(0x41, 0x00);
    }

    public int exit_cal() throws IOException  {
        return read_control(0x80, 0x00);
    }

    public int enter_cal() throws IOException  {
        return read_control(0x81, 0x00);
    }

    public int offset_cal() throws IOException  {
        return read_control(0x82, 0x00);
    }

    public int state_of_charge_pct() throws IOException  {
        return (byte) read_register(0x02, 1);
    }

    public int state_of_charge_max_error() throws IOException  {
        return (byte) read_register(0x03, 1);
    }

    public int remaining_capacity() throws IOException  {
        return scaled(read_register(0x04, 2));
    }

    public int full_charge_capacity() throws IOException  {
        return scaled(read_register(0x06, 2));
    }

    public int voltage() throws IOException  {
        return read_register(0x08, 2);
    }

    public float voltage_volts() throws IOException  {
        return (float)read_register(0x08, 2) / 1000;
    }

    public int average_current_ma() throws IOException  {
        return (int) scaled(read_register(0x0a, 2));
    }

    public float average_current_amps() throws IOException  {
        return ((float) scaled(read_register(0x0a, 2))) / 1000.0f;
    }

    public int temperature() throws IOException  {
        return read_register(0x0c, 2);
    }

    public int flags() throws IOException  {
        return read_register(0x0e, 2);
    }

    public int flags_b() throws IOException  {
        return read_register(0x12, 2);
    }

    public int current_ma() throws IOException  {
        return scaled(read_register(0x10, 2));
    }

    public float current_amps() throws IOException  {
        return ((float)scaled(read_register(0x10, 2))) / 1000.0f;
    }

    public int average_time_to_empty() throws IOException  {
        return read_register(0x18, 2);
    }

    public int average_time_to_full() throws IOException  {
        return read_register(0x1a, 2);
    }

    public int passed_charge() throws IOException  {
        return scaled(read_register(0x1c, 2));
    }

    public int do_d0_time() throws IOException  {
        return read_register(0x1e, 2);
    }

    public int available_energy() throws IOException  {
        return scaled(read_register(0x24, 2));
    }

    public int average_power() throws IOException  {
        return scaled(read_register(0x26, 2));
    }

    public int serial_number() throws IOException  {
        return read_register(0x28, 2);
    }

    public int internal_temperature() throws IOException  {
        return read_register(0x2a, 2);
    }

    public int cycle_count() throws IOException  {
        return read_register(0x2c, 2);
    }

    public int state_of_health() throws IOException  {
        return read_register(0x2e, 2);
    }

    public int charge_voltage() throws IOException  {
        return read_register(0x30, 2);
    }

    public int charge_current() throws IOException  {
        return scaled(read_register(0x32, 2));
    }

    public int pack_configuration() throws IOException  {
        return read_register(0x3a, 2);
    }

    public int design_capacity() throws IOException  {
        return scaled(read_register(0x3c, 2));
    }

    public byte grid_number() throws IOException  {
        return (byte) read_register(0x62, 1);
    }

    public byte learned_status() throws IOException  {
        return (byte) read_register(0x63, 1);
    }

    public int dod_at_eoc() throws IOException  {
        return scaled(read_register(0x64, 2));
    }

    public int q_start() throws IOException  {
        return scaled(read_register(0x66, 2));
    }

    public int true_fcc() throws IOException  {
        return scaled(read_register(0x6a, 2));
    }

    public int state_time() throws IOException  {
        return read_register(0x6c, 2);
    }

    public int q_max_passed_q() throws IOException  {
        return scaled(read_register(0x6e, 2));
    }

    public int dod_0() throws IOException  {
        return scaled(read_register(0x70, 2));
    }

    public int q_max_dod_0() throws IOException  {
        return scaled(read_register(0x72, 2));
    }

    public int q_max_time() throws IOException  {
        return read_register(0x74, 2);
    }


    @Override
    public void close() throws IOException {
        if (mDevice != null) {
            mDevice.close();
            mDevice = null;
        }

        if (mBuffer != null) {
            mBuffer = null;
        }
    }

}

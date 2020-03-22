package com.richardmcdougall.bb.hardware;

import com.google.android.things.pio.PeripheralManager;
import com.google.android.things.pio.SpiDevice;
import com.richardmcdougall.bbcommon.BLog;

import java.io.IOException;
import java.util.List;

// MCP2515 chip driver to send/recieve messages to BBv3  Motor controller and BMS over CAN

public class MCP2515 {

    private final static String TAG = "MCP2515";

    private SpiDevice mDevice;
    private byte[] mBuffer = null;
    private boolean mIsReady = false;

    public static final byte MCP2515_TIMINGS_10K[] = {(byte) 0x0F, (byte) 0xBF, (byte) 0x87};
    public static final byte MCP2515_TIMINGS_20K[] = {(byte) 0x07, (byte) 0xBF, (byte) 0x87};
    public static final byte MCP2515_TIMINGS_50K[] = {(byte) 0x03, (byte) 0xB4, (byte) 0x86};
    public static final byte MCP2515_TIMINGS_100K[] = {(byte) 0x01, (byte) 0xB4, (byte) 0x86};
    public static final byte MCP2515_TIMINGS_125K[] = {(byte) 0x01, (byte) 0xB1, (byte) 0x85};
    public static final byte MCP2515_TIMINGS_250K[] = {(byte) 0x00, (byte) 0xB1, (byte) 0x85};
    public static final byte MCP2515_TIMINGS_500K[] = {(byte) 0x00, (byte) 0x90, (byte) 0x82};
    public static final byte MCP2515_TIMINGS_1M[] = {(byte) 0x00, (byte) 0x80, (byte) 0x80};

    // command definitions
    public static final byte MCP2515_CMD_RESET = (byte) 0xC0;
    public static final byte MCP2515_CMD_READ = (byte) 0x03;
    public static final byte MCP2515_CMD_WRITE = (byte) 0x02;
    public static final byte MCP2515_CMD_BIT_MODIFY = (byte) 0x05;
    public static final byte MCP2515_CMD_READ_STATUS = (byte) 0xA0;
    public static final byte MCP2515_CMD_LOAD_TX = (byte) 0x40;
    public static final byte MCP2515_CMD_RTS = (byte) 0x80;
    public static final byte MCP2515_CMD_RX_STATUS = (byte) 0xB0;
    public static final byte MCP2515_CMD_READ_RX = (byte) 0x90;

    public static final byte MCP_RXB_RX_ANY = (byte) 0x60;
    public static final byte MCP_RXB_RX_EXT = (byte) 0x40;
    public static final byte MCP_RXB_RX_STD = (byte) 0x20;
    public static final byte MCP_RXB_RX_STDEXT = (byte) 0x00;
    public static final byte MCP_RXB_RX_MASK = (byte) 0x60;

    public static final byte MCP_STAT_RXIF_MASK = ((byte) 0x03);
    public static final byte MCP_STAT_RX0IF = (1 << 0);
    public static final byte MCP_STAT_RX1IF = (1 << 1);

    public static final byte MCP_RXB0SIDH = (byte) 0x61;
    public static final byte MCP_RXB1SIDH = (byte) 0x71;

    // register definitions
    public static final byte MCP2515_REG_CNF1 = (byte) 0x2A;
    public static final byte MCP2515_REG_CNF2 = (byte) 0x29;
    public static final byte MCP2515_REG_CNF3 = (byte) 0x28;
    public static final byte MCP2515_REG_CANCTRL = (byte) 0x0F;
    public static final byte MCP2515_REG_RXB0CTRL = (byte) 0x60;
    public static final byte MCP2515_REG_RXB1CTRL = (byte) 0x70;
    public static final byte MCP2515_REG_BFPCTRL = (byte) 0x0C;
    public static final byte MCP2515_REG_CANINTF = (byte) 0x2C;
    public static final byte MCP2515_REG_CANINTE = (byte) 0x2B;
    public static final byte MCP2515_REG_TXB0CTR = (byte) 0x30;
    public static final byte MCP2515_REG_TXB1CTR = (byte) 0x40;
    public static final byte MCP2515_REG_TXB2CTR = (byte) 0x50;

    public static final byte MCP2515_REG_RXF0SIDH = (byte) 0x00;
    public static final byte MCP2515_REG_RXF0SIDL = (byte) 0x01;
    public static final byte MCP2515_REG_RXF1SIDH = (byte) 0x04;
    public static final byte MCP2515_REG_RXF1SIDL = (byte) 0x05;
    public static final byte MCP2515_REG_RXF2SIDH = (byte) 0x08;
    public static final byte MCP2515_REG_RXF2SIDL = (byte) 0x09;
    public static final byte MCP2515_REG_RXF3SIDH = (byte) 0x10;
    public static final byte MCP2515_REG_RXF3SIDL = (byte) 0x11;
    public static final byte MCP2515_REG_RXF4SIDH = (byte) 0x14;
    public static final byte MCP2515_REG_RXF4SIDL = (byte) 0x15;
    public static final byte MCP2515_REG_RXF5SIDH = (byte) 0x18;
    public static final byte MCP2515_REG_RXF5SIDL = (byte) 0x19;

    public static final byte MCP2515_REG_RXM0SIDH = (byte) 0x20;
    public static final byte MCP2515_REG_RXM0SIDL = (byte) 0x21;
    public static final byte MCP2515_REG_RXM0EID8 = (byte) 0x22;
    public static final byte MCP2515_REG_RXM0EID0 = (byte) 0x23;
    public static final byte MCP2515_REG_RXM1SIDH = (byte) 0x24;
    public static final byte MCP2515_REG_RXM1SIDL = (byte) 0x25;
    public static final byte MCP2515_REG_RXM1EID8 = (byte) 0x26;
    public static final byte MCP2515_REG_RXM1EID0 = (byte) 0x27;
    public static final byte MCP2515_REG_EFLG = (byte) 0x2d;

    //public GpioPinDigitalInput mcp2515Interrupt = gpio.provisionDigitalInputPin(RaspiPin.GPIO_06);

    private MCP2515(SpiDevice device) {
        mDevice = device;
        mBuffer = new byte[2];
        init();
    }

    private void init() {
        BLog.e(TAG, "MPU init");
        try {

            mcp2515_reset();

            mcp2515_write_register(MCP2515.MCP2515_REG_CANCTRL, (byte) 0x80); // set config mode

            byte flags = mcp2515_read_register(MCP2515.MCP2515_REG_CANCTRL);
            if (flags == (byte) 0x80) {
                System.out.println("MCP2515 Present");
                // configure filter
                mcp2515_write_register(MCP2515.MCP2515_REG_RXB0CTRL, (byte) 0x04); // use filter for standard and extended frames
                mcp2515_write_register(MCP2515.MCP2515_REG_RXB1CTRL, (byte) 0x00); // use filter for standard and extended frames

                // initialize filter mask
                mcp2515_write_register(MCP2515.MCP2515_REG_RXM0SIDH, (byte) 0x00);
                mcp2515_write_register(MCP2515.MCP2515_REG_RXM0SIDL, (byte) 0x00);
                mcp2515_write_register(MCP2515.MCP2515_REG_RXM0EID8, (byte) 0x00);
                mcp2515_write_register(MCP2515.MCP2515_REG_RXM0EID0, (byte) 0x00);
                mcp2515_write_register(MCP2515.MCP2515_REG_RXM1SIDH, (byte) 0x00);
                mcp2515_write_register(MCP2515.MCP2515_REG_RXM1SIDL, (byte) 0x00);
                mcp2515_write_register(MCP2515.MCP2515_REG_RXM1EID8, (byte) 0x00);
                mcp2515_write_register(MCP2515.MCP2515_REG_RXM1EID0, (byte) 0x00);

                mcp2515_write_register(MCP2515.MCP2515_REG_RXF0SIDL, (byte) 0x00);
                mcp2515_write_register(MCP2515.MCP2515_REG_RXF1SIDL, (byte) 0x08);
                mcp2515_write_register(MCP2515.MCP2515_REG_RXF2SIDL, (byte) 0x00);
                mcp2515_write_register(MCP2515.MCP2515_REG_RXF3SIDL, (byte) 0x08);

                mcp2515_write_register(MCP2515.MCP2515_REG_CANINTE, (byte) 0x03); // RX interrupt
                //mcp2515_setInterrupt();

                mcp2515_set_bittiming(MCP2515.MCP2515_TIMINGS_1M); //1Mbit Baudrate

                mcp2515_bit_modify(MCP2515.MCP2515_REG_CANCTRL, (byte) 0xE0, (byte) 0x00); // set normal operating mode
                mcp2515_write_register(MCP2515.MCP2515_REG_EFLG, (byte) 0x00);    //limpa flags de erro

                mIsReady = true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (mIsReady != true) {
            BLog.e(TAG, "MCP2515 not found");
        }
    }

    public static MCP2515 open(String devicename) throws IOException {
        BLog.e(TAG, "open bus PeripheralManager.getInstance():" + devicename);

        PeripheralManager pioService = null;

        try {
            pioService = PeripheralManager.getInstance();
            BLog.e(TAG, "open bus pioService.openI2cDevice:" + devicename);
            SpiDevice device = pioService.openSpiDevice(devicename);
            // Low clock, leading edge transfer
            device.setMode(SpiDevice.MODE0);

            // 16MHz, 8BPW, MSB first
            device.setFrequency(16000000);
            device.setBitsPerWord(8);
            device.setBitJustification(SpiDevice.BIT_JUSTIFICATION_MSB_FIRST);
            return new MCP2515(device);
        } catch (Exception e) {
            BLog.e(TAG, "Cannot open android things...");
            return null;
        }
    }

    protected static String getDefaultBus() {
        BLog.e(TAG, "MCP2515 getDefaultBus");

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

    int txprio = 3;

    public class CAN_Message {
        int id;            // identifier (11 or 29 bit)
        boolean rtr;
        boolean extended;
        int dlc;                  // data length code
        byte data[] = new byte[8];        // payload data
    }

    ;

    /**
     * \brief Write to given register
     * <p>
     * \param address Register address \param data Value to write to given
     * register
     */
    void mcp2515_reset() {
        try {
            byte buffer[] = new byte[]{
                    (byte) 0xC0
            };
            mDevice.write(buffer, buffer.length);

            Thread.sleep(100);
        } catch (Exception e) {
            BLog.e(TAG, "Erro on Reset MCP2515");
        }

    }

    /**
     * \brief Write to given register
     * <p>
     * \param address Register address \param data Value to write to given
     * register
     */
    void mcp2515_write_register(byte address, byte data) throws IOException {
        byte buffer[] = new byte[]{
                MCP2515.MCP2515_CMD_WRITE,
                address,
                data
        };
        mDevice.write(buffer, buffer.length);
    }

    /**
     * \brief Read from given register
     * <p>
     * \param address Register address \return register value
     */
    byte mcp2515_read_register(byte address) throws IOException {
        byte buffer[] = new byte[]{
                MCP2515.MCP2515_CMD_READ,
                address,
                (byte) 0xFF
        };
        mDevice.write(buffer, buffer.length);
        byte[] response = new byte[32];
        mDevice.read(response, response.length);
        return response[2];
    }

    /**
     * \brief Modify bit of given register
     * <p>
     * \param address Register address \param mask Mask of bits to set \param
     * data Values to set
     * <p>
     * This function works only on a few registers. Please check the datasheet!
     */
    void mcp2515_bit_modify(byte address, byte mask, byte data) throws IOException {
        byte buffer[] = new byte[]{
                MCP2515.MCP2515_CMD_BIT_MODIFY,
                address,
                mask,
                data
        };
        mDevice.write(buffer, buffer.length);
    }

    /**
     * \brief Set bit timing registers
     * <p>
     * \param cnf1 Configuration register 1 \param cnf2 Configuration register 2
     * \param cnf3 Configuration register 3
     * <p>
     * This function has only affect if mcp2515 is in configuration mode
     */
    void mcp2515_set_bittiming(byte[] cnfs) throws IOException {
        mcp2515_write_register(MCP2515.MCP2515_REG_CNF1, cnfs[0]);
        mcp2515_write_register(MCP2515.MCP2515_REG_CNF2, cnfs[1]);
        mcp2515_write_register(MCP2515.MCP2515_REG_CNF3, cnfs[2]);
    }

    /**
     * \brief Read status byte of MCP2515
     * <p>
     * \return status byte of MCP2515
     */
    byte mcp2515_read_status() throws IOException {
        byte buffer[] = new byte[]{
                MCP2515.MCP2515_CMD_READ_STATUS,
                (byte) 0xFF
        };
        mDevice.write(buffer, buffer.length);
        byte[] result = new byte[32];
        mDevice.read(result, result.length);

        return result[1];
    }

    /**
     * \brief Read RX status byte of MCP2515
     * <p>
     * \return RX status byte of MCP2515
     */
    byte mcp2515_rx_status() throws IOException {
        byte buffer[] = new byte[]{
                MCP2515.MCP2515_CMD_RX_STATUS,
                (byte) 0xFF
        };
        mDevice.write(buffer, buffer.length);
        byte[] result = new byte[32];
        mDevice.read(result, result.length);
        return result[1];
    }

    /**
     * \brief Send given CAN message
     * <p>
     * \ p_canmsg Pointer to can message to send \return 1 if transmitted
     * successfully to MCP2515 transmit buffer, 0 on error (= no free buffer
     * available)
     */
    public boolean send_message(CAN_Message p_canmsg) {
        if (mIsReady == true) {

            try {
                byte status = mcp2515_read_status();
                byte address;
                byte ctrlreg;
                int length;
                byte txBuffer[] = new byte[20];

                // check length
                length = p_canmsg.dlc;
                if (length > 8) {
                    length = 8;
                }

                // do some priority fiddling to get fifo behavior
                switch (status & (byte) 0x54) {
                    case 0x00:
                        // all three buffers free
                        ctrlreg = MCP2515.MCP2515_REG_TXB2CTR;
                        address = (byte) 0x04;
                        txprio = 3;
                        break;

                    case 0x40:
                    case 0x44:
                        ctrlreg = MCP2515.MCP2515_REG_TXB1CTR;
                        address = (byte) 0x02;
                        break;

                    case 0x10:
                    case 0x50:
                        ctrlreg = MCP2515.MCP2515_REG_TXB0CTR;
                        address = (byte) 0x00;
                        break;

                    case 0x04:
                    case 0x14:
                        ctrlreg = MCP2515.MCP2515_REG_TXB2CTR;
                        address = (byte) 0x04;

                        if (txprio == 0) {
                            // set priority of buffer 1 and buffer 0 to highest
                            mcp2515_bit_modify(MCP2515.MCP2515_REG_TXB1CTR, (byte) 0x03, (byte) 0x03);
                            mcp2515_bit_modify(MCP2515.MCP2515_REG_TXB0CTR, (byte) 0x03, (byte) 0x03);
                            txprio = 2;
                        } else {
                            txprio--;
                        }
                        break;

                    default:
                        // no free transmit buffer
                        return false;
                }

                // pull SS to low level
                int index = 0;
                txBuffer[index++] = (byte) (MCP2515.MCP2515_CMD_LOAD_TX | address);

                if (p_canmsg.extended) {
                    txBuffer[index++] = (byte) ((p_canmsg.id >> 21));
                    txBuffer[index++] = (byte) ((((p_canmsg.id >> 13) & (byte) 0xe0) | ((p_canmsg.id >> 16) & (byte) 0x03) | (byte) 0x08));
                    txBuffer[index++] = (byte) ((p_canmsg.id >> 8));
                    txBuffer[index++] = (byte) (p_canmsg.id);
                } else {
                    txBuffer[index++] = (byte) (p_canmsg.id >> 3);
                    txBuffer[index++] = (byte) (p_canmsg.id << 5);
                    txBuffer[index++] = 0;
                    txBuffer[index++] = 0;
                }

                // length and data
                if (p_canmsg.rtr) {
                    txBuffer[index++] = (byte) (length | (byte) 0x40);
                } else {
                    txBuffer[index++] = (byte) length;
                    for (int i = 0; i < length; i++) {
                        txBuffer[index + i] = (p_canmsg.data[i]);
                    }
                    index += length;
                }

                // RMC Check this
                mDevice.write(txBuffer, index);

                mcp2515_write_register(ctrlreg, (byte) (txprio | (byte) 0x08));

            } catch (Exception e) {
                BLog.e(TAG, "Error on Send Message");
            }

            return true;
        } else {
            return false;
        }
    }

    /*
     * \brief Read out one can message from MCP2515
     *
     * \param p_canmsg Pointer to can message structure to fill
     * \return 1 on success, 0 if there is no message to read
     */
    CAN_Message receive_message() {
        CAN_Message canMessage = null;

        if (mIsReady == true) {

            try {
                byte status = mcp2515_rx_status();
                int index = 0;
                byte address;
                byte txBuffer[] = new byte[20];

                if ((status & (byte) 0x40) == (byte) 0x40) {
                    address = 0x00;
                } else if ((status & (byte) 0x80) == (byte) 0x80) {
                    address = 0x04;
                } else {
                    // no message in receive buffer
                    return canMessage;
                }

                canMessage = new CAN_Message();
                // store flags
                canMessage.rtr = ((status >> 3) & 0x01) == 0x01 ? true : false;
                canMessage.extended = ((status >> 4) & 0x01) == 0x01 ? true : false;

                txBuffer[index] = (byte) (MCP2515.MCP2515_CMD_READ_RX | address);
                index += 14;
                // Check this
                byte[] rxBuffer = new byte[32];
                mDevice.write(txBuffer, index);
                mDevice.read(rxBuffer, rxBuffer.length);

                if (canMessage.extended) {
                    canMessage.id = (int) rxBuffer[1] << 21;
                    int temp = rxBuffer[2];
                    canMessage.id |= (temp & 0xe0) << 13;
                    canMessage.id |= (temp & 0x03) << 16;
                    canMessage.id |= (int) (rxBuffer[3] << 8) & 0x0000FFFF;
                    canMessage.id |= (int) rxBuffer[4] & 0x000000FF;
                    canMessage.id &= 0x1FFFFFFF;
                } else {
                    canMessage.id = (int) (rxBuffer[1] << 3) & 0x000007FF;
                    canMessage.id |= (int) (rxBuffer[2] >> 5) & 0x000007FF;
                }

                canMessage.dlc = (rxBuffer[5] & (byte) 0x0f);
                if (!canMessage.rtr) {

                    for (int i = 0; i < canMessage.dlc; i++) {
                        canMessage.data[i] = rxBuffer[6 + i];
                    }
                }

            } catch (Exception e) {
                BLog.e(TAG, "Erro on Receive Message");
            }

            return canMessage;
        } else {
            return canMessage;
        }
    }
}

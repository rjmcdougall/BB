package com.richardmcdougall.bb;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;


import com.friendlyarm.FriendlyThings.HardwareControler;

import java.io.IOException;
import java.util.Arrays;

public class CANBus {
    private static final String TAG = "BB.CANBus";
    public Context mContext = null;
    public BBService mBBService = null;

    private int devFD = -1;
    private int spi_mode = 0;
    private int spi_bits = 8;
    private int spi_delay = 0;
    private int spi_speed = 500000;
    private int spi_byte_order = hardware.LSBFIRST;

    //private int mBoardType = HardwareControler.getBoardType();
    //private boolean mBoardSupported = true;

    private final String spiDevPath = "/dev/spidev1.0";   /*For S5P4418*/

    private void sendLogMsg(String msg) {
        Intent in = new Intent(BBService.ACTION_STATS);
        in.putExtra("resultCode", Activity.RESULT_OK);
        in.putExtra("msgType", 4);
        // Put extras into the intent as usual
        in.putExtra("logMsg", msg);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(in);
    }

    public void l(String s) {
        Log.v(TAG, s);
        sendLogMsg(s);
    }

	public CANBus(BBService service, Context context) throws IOException {

        mBBService = service;
        mContext = context;
	}

	public int init() {

        devFD = hardware.open("/dev/spidev0.0",hardware.O_RDWR);
        if (devFD < 0) {
            l("Fail to open SPI device: " + spiDevPath);
            return -1;
        }
        l("Open " + spiDevPath + " Ok.");

        try {
            Thread.sleep(500);
        } catch (Exception e) {
            l("Exception reading CAN");
        }


        spi_mode &= ~(hardware.SPI_CPHA|hardware.SPI_CPOL);


        if (hardware.setSPIMode(devFD, spi_mode) != 0) {
            l("setSPIMode failed");
            return -1;
        }
        l("setSPIMode " + spiDevPath + " Ok.");

        try {
            Thread.sleep(500);
        } catch (Exception e) {
            l("Exception reading CAN");
        }

        spi_byte_order = hardware.MSBFIRST;
        if (hardware.setSPIBitOrder(devFD, spi_byte_order) != 0) {
            l("setSPIBitOrder failed");
            return -1;
        }
        l("setSPIBitOrder " + spiDevPath + " Ok.");


        try {
            Thread.sleep(500);
        } catch (Exception e) {
            l("Exception reading CAN");
        }


        return 0;
    }

    public void start() {
        //Thread CanThread = new Thread(new Runnable() {
        //    public void run() {

//                Thread.currentThread().setName("BB CAN Monitor");



                while (true) {
                    try {
                        Read();
                    } catch (Exception e) {
                        l("Exception reading CAN");
                    }

                }
      //      }
  //      });
    //    CanThread.start();
    }

    private void shutdown() {
        hardware.close(devFD);
        devFD = -1;
    }

    private void checkresult(int r) {
        if (r == -1) {
            shutdown();
        }
    }

    static final int[] read_buffer_0 = {0x92, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07};
    static final int[] read_buffer_1 = {0x96, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
    static final int[] read_buffer_2 = {0x92, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x00};



    static final byte spiMode = 0;
    static final char spiBits = 8;
    static final int spiSpeed = 10000000;
    static short spiDelay = 10;



    public byte[] Read() {

        byte[] b = new byte[8];
        byte[] b2 = new byte[32];

        try {
            Thread.sleep(100);
        } catch (Exception e) {
            l("Exception reading CAN");
        }

        if (devFD == -1) {
            if (init() != 0) {
                l("init failed");
            }
        }

        l("Writing SPI on fd " + devFD);
        //int r = HardwareControler.writeBytesToSPI(devFD, read_buffer_0, spiDelay, spiSpeed, spiBits);
        int  [] r   = hardware.transferArray(devFD, read_buffer_0, spiDelay ,spiSpeed, spiBits);
        //int r = HardwareControler.SPItransferOneByte(devFD, b[0], spiDelay, spiSpeed , spiBits);
        //checkresult(r);
        l("Writing SPI = " + r);
        //HardwareControler.SPItransferBytes( devFD, read_buffer_0);

        try {
            Thread.sleep(100);
        } catch (Exception e) {
            l("Exception reading CAN");
        }


        l("Return SPI bytes: " + intsToHex(r));
        try {
            Thread.sleep(100);
        } catch (Exception e) {
            l("Exception reading CAN");
        }
        return b;
    }

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {

        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static String intsToHex(int[] ints) {

        char[] hexChars = new char[ints.length * 2];
        for (int j = 0; j < ints.length; j++) {
            int v = ints[j];
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }


    static {
        try {
            System.loadLibrary("bbhardware");
        } catch (UnsatisfiedLinkError e) {
            Log.d("BB.CANBus", "libbbhardware library not found!");
        }
    }

}

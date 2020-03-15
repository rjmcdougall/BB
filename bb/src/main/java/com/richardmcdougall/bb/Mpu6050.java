package com.richardmcdougall.bb;


//import I2cDevice;

import com.google.android.things.pio.I2cDevice;
import com.google.android.things.pio.PeripheralManager;

import java.io.IOException;
import java.util.List;


public class Mpu6050 implements AutoCloseable  {
    private String TAG = this.getClass().getSimpleName();

    private I2cDevice mDevice;
    private byte[] mBuffer = null;
    private boolean mIsReady = false;

    //public static final int CHIP_ID_MPU6050;
    public static final int DEFAULT_I2C_ADDRESS = 0x68;
    public static final float SENSOR_RESOLUTION = 32768;

    private static final int GYRO_CONFIG = 0x1B;

    public static final byte GYRO_FS_250 = 0x00;
    public static final byte GYRO_FS_500 = 0x08;
    public static final byte GYRO_FS_1000 = 0x10;
    public static final byte GYRO_FS_2000 = 0x18;

    private static byte mGyroFs = GYRO_FS_250;
    private static float mGyroCoef = 1;//(float)( 250. / SENSOR_RESOLUTION);

    private static final int ACCEL_CONFIG = 0x1C;

    public static final byte ACCEL_FS_2G = 0x00;
    public static final byte ACCEL_FS_4G = 0x08;
    public static final byte ACCEL_FS_8G = 0x10;
    public static final byte ACCEL_FS_16G = 0x18;

    private static byte mAccelFs = ACCEL_FS_2G;
    private static float mAccelCoef = 1;//(float)(1. / 2. / SENSOR_RESOLUTION);

    private static final int ACCEL_XOUT_H = 0x3B;
    private static final int ACCEL_XOUT_L = 0x3C;
    private static final int ACCEL_YOUT_H = 0x3D;
    private static final int ACCEL_YOUT_L = 0x3E;
    private static final int ACCEL_ZOUT_H = 0x3F;
    private static final int ACCEL_ZOUT_L = 0x40;

    private static final int TEMP_OUT_H = 0x41;
    private static final int TEMP_OUT_L = 0x42;

    private static final int GYRO_XOUT_H = 0x43;
    private static final int GYRO_XOUT_L = 0x44;
    private static final int GYRO_YOUT_H = 0x45;
    private static final int GYRO_YOUT_L = 0x46;
    private static final int GYRO_ZOUT_H = 0x47;
    private static final int GYRO_ZOUT_L = 0x48;

    private static final int PWR_MGMT_1 = 0X6B;
    private static final int PWR_MGMT_2 = 0X6C;

    public static final byte UNSLEEP = 0x00;

    private Mpu6050(I2cDevice device){
        mDevice = device;
        mBuffer = new byte[2];
        init();
    }

    private void init() {
        try {
            mDevice.writeRegByte(PWR_MGMT_1, UNSLEEP);
            setAccelFS(ACCEL_FS_2G);
            setGyroFS(GYRO_FS_250);
            mIsReady = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // FFS, make some redundant methods to open this passing the bus
    public static Mpu6050 open() throws IOException {
        return open(getDefaultBus());
    }

    public static Mpu6050 open(String busName) throws IOException {
        PeripheralManager pioService = PeripheralManager.getInstance();

        I2cDevice device = pioService.openI2cDevice(busName, 0x68);

        return new Mpu6050(device);
    }

    protected static String getDefaultBus() {
        PeripheralManager peripheralManagerService = PeripheralManager.getInstance();
        List<String> deviceList = peripheralManagerService.getI2cBusList();
        if (deviceList.isEmpty()) {
            return "I2C1";
        } else {
            return deviceList.get(0);
        }
    }

    public boolean IsReady() {
        return mIsReady;
    }

    public void setGyroFS(byte gyro_fs) throws IOException {
        mDevice.writeRegByte(GYRO_CONFIG, gyro_fs);
        mGyroFs = gyro_fs;
        switch (mGyroFs) {
            case GYRO_FS_250:
                mGyroCoef = (float) 250. / SENSOR_RESOLUTION;
                break;
            case GYRO_FS_500:
                mGyroCoef = (float) 500. / SENSOR_RESOLUTION;
                break;
            case GYRO_FS_1000:
                mGyroCoef = (float) 1000. / SENSOR_RESOLUTION;
                break;
            case GYRO_FS_2000:
                mGyroCoef = (float) 2000. / SENSOR_RESOLUTION;
                break;
        }
    }

    public void setAccelFS(byte accel_fs) throws IOException {
        mDevice.writeRegByte(ACCEL_CONFIG, accel_fs);
        mAccelFs = accel_fs;
        switch (mAccelFs) {
            case ACCEL_FS_2G:
                mAccelCoef = (float) 2. / SENSOR_RESOLUTION;
                break;
            case ACCEL_FS_4G:
                mAccelCoef = (float) 4. / SENSOR_RESOLUTION;
                break;
            case ACCEL_FS_8G:
                mAccelCoef = (float) 8. / SENSOR_RESOLUTION;
                break;
            case ACCEL_FS_16G:
                mAccelCoef = (float) 16. / SENSOR_RESOLUTION;
                break;
        }
    }

    public float getAccelX() throws IOException {   return (float) readRegWord(ACCEL_XOUT_H) * mAccelCoef;}
    public float getAccelY() throws IOException {   return (float) readRegWord(ACCEL_YOUT_H) * mAccelCoef;}
    public float getAccelZ() throws IOException {   return (float) readRegWord(ACCEL_ZOUT_H) * mAccelCoef;}
    public float getTemp() throws IOException {     return (float) (readRegWord(TEMP_OUT_H) / 340. + 36.53);}
    public float getGyroX() throws IOException {    return (float) readRegWord(GYRO_XOUT_H) * mGyroCoef;}
    public float getGyroY() throws IOException {    return (float) readRegWord(GYRO_YOUT_H) * mGyroCoef;}
    public float getGyroZ() throws IOException {    return (float) readRegWord(GYRO_ZOUT_H) * mGyroCoef;}

    private short readRegWord(int i) throws IOException {
        mDevice.readRegBuffer(i, mBuffer, 2);
        return (short)(((mBuffer[0] << 8) & 0xff00) | (mBuffer[1] & 0x00ff));
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
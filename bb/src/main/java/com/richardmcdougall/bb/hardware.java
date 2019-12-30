package com.richardmcdougall.bb;
import android.util.Log;

import java.lang.UnsatisfiedLinkError;

public class hardware {
	private static final String TAG = "BBHardware";
	   public final static int O_ACCMODE   = 00000003;
    public final static int O_RDONLY    = 00000000;
    public final static int O_WRONLY    = 00000001;
    public final static int O_RDWR              = 00000002;
    public final static int O_CREAT             = 00000100;     /* not fcntl */
    public final static int O_EXCL              = 00000200;     /* not fcntl */
    public final static int O_NOCTTY    = 00000400;     /* not fcntl */
    public final static int O_TRUNC             = 00001000;     /* not fcntl */
    public final static int O_APPEND    = 00002000;
    public final static int O_NONBLOCK  = 00004000;
    public final static int O_DSYNC             = 00010000;     /* used to be O_SYNC, see below */
    public final static int FASYNC              = 00020000;     /* fcntl, for BSD compatibility */
    public final static int O_DIRECT    = 00040000;     /* direct disk access hint */
    public final static int O_LARGEFILE = 00100000;
    public final static int O_DIRECTORY = 00200000;     /* must be a directory */
    public final static int O_NOFOLLOW  = 00400000;     /* don't follow links */
    public final static int O_NOATIME   = 01000000;
    public final static int O_CLOEXEC   = 02000000;     /* set close_on_exec */

	static public native int open(String Path, int flag);
	static public native int close(int fd);
//	static public native int write(int fd, byte[] data);
//	static public native int read(int fd, int len);

/*---------------------------------------------spi application--------------------------------------------------------*/
        // SPIBitOrder
        public final static int LSBFIRST = 0;  ///< LSB First
        public final static int MSBFIRST = 1;   ///< MSB First

        // SPIMode
        public final static int SPI_MODE0 = 0;  ///< CPOL = 0, CPHA = 0
        public final static int SPI_MODE1 = 1;  ///< CPOL = 0, CPHA = 1
        public final static int SPI_MODE2 = 2;  ///< CPOL = 1, CPHA = 0
        public final static int SPI_MODE3 = 3;  ///< CPOL = 1, CPHA = 1


        public final static int SPI_CPHA = 0x01;
        public final static int SPI_CPOL = 0x02;
        public final static int SPI_CS_HIGH = 0x04;
        public final static int SPI_LSB_FIRST = 0x08;
        public final static int SPI_3WIRE = 0x10;
        public final static int SPI_LOOP = 0x20;
        public final static int SPI_NO_CS = 0x40;
        public final static int SPI_READY = 0x80;


        static public native int setSPIBits(int fd, int Bits);
        static public native int getSPIBits(int fd);
        static public native int setSPIBitOrder(int fd, int order);
        static public native int setSPIMaxSpeed(int fd, int speed);
        static public native int setSPIMode(int fd, int mode);
        static public native int[] transferArray(int fd, int[] writebuf,int delay, int speed, int bits);
/*---------------------------------------------i2c application--------------------------------------------------------*/
	public final static int I2C_SMBUS_READ = 1;
	public final static int I2C_SMBUS_WRITE = 0;
	public final static int I2C_SMBUS_QUICK = 0;
	public final static int I2C_SMBUS_BYTE = 1;
	public final static int I2C_SMBUS_BYTE_DATA = 2;
	public final static int I2C_SMBUS_WORD_DATA = 3;
	public final static int I2C_SMBUS_PROC_CALL = 4;
	public final static int I2C_SMBUS_BLOCK_DATA = 5;
	public final static int I2C_SMBUS_I2C_BLOCK_BROKEN = 6;
	public final static int I2C_SMBUS_BLOCK_PROC_CALL = 7;
	public final static int I2C_SMBUS_I2C_BLOCK_DATA = 8;


	static public native int setI2CSlaveAddr(int fd, int addr, int force);
        static public native int setI2CTimeout(int fd, int timeout);
	static public native int setI2CRetries(int fd, int retries);
	static public native int I2CCheck(int fd, int size);
	static public native int I2CReadByte(int fd, int reg);
	static public native int I2CWriteByte(int fd, int reg, int data);	


   static {
        try {
                System.loadLibrary("bbhardwarejni");
        } catch (UnsatisfiedLinkError e) {
            Log.d(TAG, "bbhardware library not found!");
        }
    }
/*--------------------------------------------------------------------------------------------------------------------*/

}

/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
#include <string.h>
#include <jni.h>

#include <stdint.h>
#include <unistd.h>
#include <stdio.h>
#include <stdlib.h>
#include <getopt.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <linux/types.h>
#include <linux/spi/spidev.h>
#include <linux/i2c-dev.h>
#include <linux/i2c.h>
#include <cutils/log.h>

#define LOG_TAG             "LMK"

#define DEBUG 1


//#if DEBUG
#define LOGD(x...)  do {printf("D/"LOG_TAG": "); printf(x);}while(0)
//#else
//#  define  LOGV(...)  do {} while (0)
//#endif

/* This is a trivial JNI example where we use a native method
 * to return a new VM String. See the corresponding Java source
 * file located at:
 *
 */
jint
Java_com_lmk_hardware_open( JNIEnv* env, jobject thiz, jstring Path, jint flag)
{
    int fd = -1;
    const char *path = (*env)->GetStringUTFChars(env, Path, NULL);

    fd = open(path, flag);
    if(!fd){
    	ALOGW("open fail!\n");	
    }
   
    (*env)->ReleaseStringUTFChars(env,Path,path);

    return fd;
}

jint
Java_com_lmk_hardware_close( JNIEnv* env, jobject thiz, jint fd)
{
    ALOGW("Close!\n");
    close(fd);
    return 0;
}

jint
Java_com_lmk_hardware_write( JNIEnv* env, jobject thiz, jint fd, jbyteArray data)
{
	    jint cnt = -1;
	    jsize len;
            jbyte *dataArray;
		
	    len = (*env)->GetArrayLength(env, data);
            dataArray = (*env)->GetByteArrayElements(env, data, 0);
	
	    cnt = write(fd, dataArray, len);
	    if(cnt){
		ALOGW("Fail write!\n");
	    }
	    (*env)->ReleaseByteArrayElements(env,data,dataArray,NULL);
	    return (cnt != len?0:1);
}

jint
Java_com_lmk_hardware_read( JNIEnv* env, jobject thiz, jint fd, jbyteArray data, jint len)
{
    	jint cnt = -1;
        jbyte *dataArray = (*env)->GetByteArrayElements(env, data, 0);
		
	cnt = read(fd, dataArray, len);

	(*env)->ReleaseByteArrayElements(env,data,dataArray,0);
	return (cnt != len?0:1);
}

/**********************************************spi*************************************************************/
jint
Java_com_lmk_hardware_setSPIBits( JNIEnv* env, jobject thiz, jint fd, jint Bits)
{
	jint cnt = -1;
	
	cnt = ioctl(fd, SPI_IOC_WR_BITS_PER_WORD, &Bits);
	if(cnt != 0){
		ALOGW("setBit fail!\n");
	}else{
		ALOGW("set Bit(%d)\n",Bits);
		return 1;
	}
	return 0;
}

jint
Java_com_lmk_hardware_getSPIBits( JNIEnv* env, jobject thiz, jint fd)
{
        jint cnt = -1;
	jint Bits = 0;

        cnt = ioctl(fd, SPI_IOC_RD_BITS_PER_WORD, &Bits);
	if(cnt != 0){
                ALOGW("getBit fail!\n");
        } else{
		ALOGW("Get Bit(%d)\n",Bits);
	}
        return Bits;
}

jint
Java_com_lmk_hardware_setSPIBitOrder( JNIEnv* env, jobject thiz, jint fd, jint order)
{
        jint cnt = -1;
	if(order != -1){
        	cnt = ioctl(fd, SPI_IOC_WR_LSB_FIRST, &order);
		if(cnt != 0){
			ALOGW("setOrder fail!\n");
		} else{
			ALOGW("setOrder(%d)\n",order);
			return 1;
		}		
	}
        return 0;
}


jint
Java_com_lmk_hardware_setSPIMaxSpeed( JNIEnv* env, jobject thiz, jint fd, jint speed)
{
        jint cnt = -1;
        if(speed != -1)
		cnt = ioctl(fd, SPI_IOC_WR_MAX_SPEED_HZ, &speed);
	if(cnt != 0){
		ALOGW("setMaxSpeed fail!\n");
	}else{
		ALOGW("setMaxSpeed(%d)\n",speed);
		return 1;
	}

        return 0;
}


jint
Java_com_lmk_hardware_setSPIMode( JNIEnv* env, jobject thiz, jint fd, jint mode)
{
        jint cnt = -1;
	if(mode != -1)
        	cnt = ioctl(fd, SPI_IOC_WR_MODE, &mode);
	if(cnt != 0){
		ALOGW("setMode fail!\n");
	}else{
		ALOGW("setMode(%d)\n",mode);
		return 1;
	} 

        return 0;
}

jintArray
Java_com_lmk_hardware_transferArray( JNIEnv* env, jobject thiz, jint fd, jintArray writebuf,jint delay,jint speed,jint bits)
{
        jint cnt = -1;
        jint i;
        jsize len;
        jintArray read;

        len = (*env)->GetArrayLength(env, writebuf);

        uint8_t rb[len];
	uint8_t wb[len];
        uint32_t w[len];
	uint32_t r[len];

        (*env)->GetIntArrayRegion(env,writebuf,0,len,w);
	for(i=0;i<(sizeof(w)/4);i++){
		wb[i] = w[i]&0xFF;
	}
        read = (*env)->NewIntArray(env,len);

        struct spi_ioc_transfer xfer = {
                .tx_buf = (unsigned long)wb,
                .rx_buf = (unsigned long)rb,
                .len = sizeof(wb),
                .delay_usecs = delay,
                .speed_hz = speed,
                .bits_per_word = bits,
        };

        cnt = ioctl(fd, SPI_IOC_MESSAGE(1), &xfer);
        if(cnt < 1){
                ALOGW("transferBytes FAIL!\n");
        }else{
                ALOGW("transferBytes success!\n");
                for(i = 0; i < sizeof(wb);i++)
			r[i]=(int)(rb[i]&0xFF);
                       // ALOGW("w[%d]=0x%02x,r[%d]=0x%02x\n",i,wb[i],i,r[i]);
        }

        (*env)->SetIntArrayRegion(env, read, 0, len, r);

        return read;
}

/*************************************************UART**********************************************************/
jobject
Java_com_lmk_hardware_setUartMode(JNIEnv* env, jobject thiz, jint fd,  jint nSpeed, jint nBits, jint nEvent, jint nStop)
{
	struct termios newtio, oldtio;
	jobject mFileDescriptor;
	if (tcgetattr(fd, &oldtio) != 0) {
		ALOGW("SetupSerial---------------------------------------------\n");	
		return -1;
	}
	bzero(&newtio, sizeof(newtio));
	newtio.c_cflag |= CLOCAL | CREAD;
	newtio.c_cflag &= ~CSIZE;
	
	switch (nBits) {
	case 7:
		newtio.c_cflag |= CS7;
		break;
	case 8:
		newtio.c_cflag |= CS8;
		break;
	}

	switch (nEvent) {
	case 'O':                     //奇校验
		newtio.c_cflag |= PARENB;
		newtio.c_cflag |= PARODD;
		newtio.c_iflag |= (INPCK | ISTRIP);
		break;
	case 'E':                     //偶校验
		newtio.c_iflag |= (INPCK | ISTRIP);
		newtio.c_cflag |= PARENB;
		newtio.c_cflag &= ~PARODD;
		break;
	case 'N':                    //无校验
		newtio.c_cflag &= ~PARENB;
		break;
	}

	switch (nSpeed) {
	case 2400:
		cfsetispeed(&newtio, B2400);
		cfsetospeed(&newtio, B2400);
		break;
	case 4800:
		cfsetispeed(&newtio, B4800);
		cfsetospeed(&newtio, B4800);
		break;
	case 9600:
		cfsetispeed(&newtio, B9600);
		cfsetospeed(&newtio, B9600);
		break;
	case 115200:
		cfsetispeed(&newtio, B115200);
		cfsetospeed(&newtio, B115200);
		break;
	default:
		cfsetispeed(&newtio, B9600);
		cfsetospeed(&newtio, B9600);
		break;
	}
	if (nStop == 1) {
		newtio.c_cflag &= ~CSTOPB;
	} else if (nStop == 2) {
		newtio.c_cflag |= CSTOPB;
	}
	newtio.c_cc[VTIME] = 0;
	newtio.c_cc[VMIN] = 0;
	tcflush(fd, TCIFLUSH);
	if ((tcsetattr(fd, TCSANOW, &newtio)) != 0) {
		ALOGW("com set error---------------------------------------------\n");	
		return -1;
	}
	printf("set done!----------------------------------------\n");
	ALOGW("set done!---------------------------------------------\n");	
		/* Create a corresponding file descriptor */
	{
		jclass cFileDescriptor = (*env)->FindClass(env,
				"java/io/FileDescriptor");
		jmethodID iFileDescriptor = (*env)->GetMethodID(env, cFileDescriptor,
				"<init>", "()V");
		jfieldID descriptorID = (*env)->GetFieldID(env, cFileDescriptor,
				"descriptor", "I");
		mFileDescriptor = (*env)->NewObject(env, cFileDescriptor,
				iFileDescriptor);
		(*env)->SetIntField(env, mFileDescriptor, descriptorID,
				(jint) fd);
	}
	ALOGW("set mFileDescriptor done!---------------------------------------------\n");
	return mFileDescriptor;
}
/**************************************************************************************************************/

/*************************************************i2c**********************************************************/
jint
Java_com_lmk_hardware_setI2CSlaveAddr( JNIEnv* env, jobject thiz, jint fd, jint addr, jint force)
{
	if (ioctl(fd, force ? I2C_SLAVE_FORCE : I2C_SLAVE, addr) < 0) {
                ALOGW("Error: Could not set address to 0x%02x: %s\n",
                        addr, strerror(errno));
                return -errno;
        }

        return 0;
}

jint
Java_com_lmk_hardware_setI2CTimeout( JNIEnv* env, jobject thiz, jint fd, jint timeout)
{
        if (ioctl(fd, I2C_TIMEOUT, timeout) < 0) {
                ALOGW("Error: Could not set i2c timeout to %d: %s\n",
                        timeout, strerror(errno));
                return -errno;
        }

        return 0;
}

jint
Java_com_lmk_hardware_setI2CRetries( JNIEnv* env, jobject thiz, jint fd, jint retries)
{
        if (ioctl(fd, I2C_RETRIES, retries) < 0) {
                ALOGW("Error: Could not set i2c retries to %d: %s\n",
                        retries, strerror(errno));
                return -errno;
        }

        return 0;
}

jint
Java_com_lmk_hardware_I2CCheck( JNIEnv* env, jobject thiz, jint fd, jint size)
{
	unsigned long funcs;
	if (ioctl(fd, I2C_FUNCS, &funcs) < 0) {
                ALOGW("Error: Could not get the adapter "
                        "functionality matrix: %s\n", strerror(errno));
                return -1;
        }
	switch (size) {
        case I2C_SMBUS_BYTE:
                if (!(funcs & I2C_FUNC_SMBUS_READ_BYTE)) {
                        ALOGW("MISSING_FUNC_FMT SMBus receive byte");
                        return -1;
                }
                if (!(funcs & I2C_FUNC_SMBUS_WRITE_BYTE)) {
                        ALOGW("MISSING_FUNC_FMT SMBus send byte");
                        return -1;
                }
                break;

        case I2C_SMBUS_BYTE_DATA:
                if (!(funcs & I2C_FUNC_SMBUS_READ_BYTE_DATA)) {
                        ALOGW("MISSING_FUNC_FMT SMBus read byte");
                        return -1;
                }
                break;

        case I2C_SMBUS_WORD_DATA:
                if (!(funcs & I2C_FUNC_SMBUS_READ_WORD_DATA)) {
                        ALOGW("MISSING_FUNC_FMT SMBus read word");
                        return -1;
                }
                break;
        }

        return 0;
}

jint
Java_com_lmk_hardware_I2CReadByte( JNIEnv* env, jobject thiz, jint fd, jint reg)
{
	uint8_t i2c_reg;
	i2c_reg = reg&0xFF;

        return i2c_smbus_read_byte_data(fd, i2c_reg);
}

jint
Java_com_lmk_hardware_I2CWriteByte( JNIEnv* env, jobject thiz, jint fd, jint reg, jint data)
{
	uint8_t i2c_reg,i2c_data;
	i2c_reg = reg&0xFF;
	i2c_data = data&0xFF;

        return i2c_smbus_write_byte_data(fd, i2c_reg, i2c_data);
}
/**************************************************************************************************************/

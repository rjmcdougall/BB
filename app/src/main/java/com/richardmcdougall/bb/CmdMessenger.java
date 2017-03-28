package com.richardmcdougall.bb;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.InputDevice;
import android.view.MotionEvent;

import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.util.HashMap;

/**
 * A java clone of CmdMessenger by rjmcdougall on 8/8/16.
 */
public class CmdMessenger implements SerialInputOutputManager.Listener {

    static final int MESSENGERBUFFERSIZE = 64;   // The length of the commandBufferTmpIn  (default: 64)
    static final int DEFAULT_TIMEOUT = 5000; // Time out on unanswered messages. (default: 5s)
    public final static String TAG = "CmdListener";

    boolean startCommand;          // Indicates if sending of a command is underway
    int lastCommandId;                // ID of last received command
    char ArglastChar;                 // Bookkeeping of argument escape char
    boolean pauseProcessing;          // pauses processing of new commands, during sending
    boolean print_newlines;           // Indicates if \r\n should be added after send command
    ByteArrayOutputStream commandBufferTmp; // Buffer that holds the data
    ByteArrayInputStream commandBuffer; // Buffer that holds the data
    ByteArrayInputStream streamBuffer; // Buffer that holds the data
    byte messageState;                // Current state of message processing
    boolean dumped;                   // Indicates if last argument has been externally read
    boolean ArgOk;                    // Indicated if last fetched argument could be read
    byte[] currentArg;                // Current Arg

    byte command_separator;           // Character indicating end of command (default: ';')
    byte field_separator;             // Character indicating end of argument (default: ',')
    byte escape_character;            // Character indicating escaping of special chars

    final int kProccesingMessage = 0; // Message is being received, not reached command separator
    final int kEndOfMessage = 1;      // Message is fully received, reached command separator
    final int kProcessingArguments = 2; // Message is received, arguments are being read parsed

    // threads for comms
    UsbSerialPort Serial;
    private Handler mHandler;
    PipedInputStream mPipedInputStream;
    PipedOutputStream mPipedOutputStream;
    private Runnable mProcessMessageRunnable;
    private Runnable mLogLineRunnable;
    private BufferedReader mBufferedReader;
    InputStreamReader mInputStreamReader;
    ByteArrayOutputStream sendBuffer; // Buffer that holds the data to send to serial port


    HashMap<Integer, CmdEvents> callbackList = new HashMap<Integer, CmdEvents>();

    private CmdEvents default_callback;

    public interface CmdEvents {
        void CmdAction(String msg);
    }

    public CmdMessenger(UsbSerialPort Sport, final char fld_separator,
                        final char cmd_separator, final char esc_character) {
        print_newlines = false;
        commandBufferTmp = new ByteArrayOutputStream();
//        commandBufferTmp.reset();
        pauseProcessing = false;

        field_separator = (byte) fld_separator;
        command_separator = (byte) cmd_separator;
        escape_character = (byte) esc_character;

        sendBuffer = new ByteArrayOutputStream();

        Serial = Sport;

        mHandler = new Handler();

        try {//div setup
            mPipedInputStream = new PipedInputStream();
            mPipedOutputStream = new PipedOutputStream(mPipedInputStream);//connect input <=> output
            mInputStreamReader = new InputStreamReader(mPipedInputStream);
            mBufferedReader = new BufferedReader(mInputStreamReader);
        } catch (Exception e) {
            e("div setup failed: " + e.getMessage());
        }

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


    public void flushWrites() {
        //System.out.println("flushWrites: " + sendBuffer.size() + "," + bytesToHex(sendBuffer.toByteArray()));
        if (sendBuffer.size() > 0) {
            try {
                Serial.write(sendBuffer.toByteArray(), 500);
                sendBuffer.reset();
            } catch (Exception e) {
                l("Write Failed: " + e.toString());
            }
        }
    }

    /**
     * Enables printing newline after a sent command
     */
    void printLfCr(boolean addNewLine) {

        print_newlines = addNewLine;
    }

    /**
     * Attaches an default function for commands that are not explicitly attached
     */
    public void attach(CmdEvents callback) {

        default_callback = callback;
    }

    /**
     * Attaches a function to a command ID
     */
    void attach(int msgId, CmdEvents newFunction) {
        if (msgId >= 0)
            callbackList.put(msgId, newFunction);
    }


    /**
     * Waits for reply from sender or timeout before continuing
     */
    boolean blockedTillReply(int timeout, int ackCmdId) {

        int time = 0;
        boolean receivedAck = false;
        while (time < timeout && !receivedAck) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                return false;
            }
            receivedAck = checkForAck(ackCmdId);
            time++;
        }
        return receivedAck;
    }


    /**
     * Loops as long data is available to determine if acknowledge has come in
     */
    boolean checkForAck(int ackCommand) {

        // Check if ackCommand came in
        if (lastCommandId == ackCommand)
            return true;
        /*
         * took this out because all commands are processed upon receive anyway
        while (streamBuffer.available() > 0) {
            //Processes a byte and determines if an acknowlegde has come in
            int messageState = processLine();
            if (messageState == kEndOfMessage) {
                int id = readIntArg();
                if (ackCommand == id && ArgOk) {
                    return true;
                } else {
                    return false;
                }
            }
            return false;
        }
        */
        return false;
    }

    /**
     * Gets next argument. Returns true if an argument is available
     */
    boolean next() {
        // Currently, cmd messenger only supports 1 char for the field separator
        //switch (messageState) {
        //    case kProccesingMessage:
        //       return false;
        //    case kEndOfMessage:
        //        messageState = kProcessingArguments;
        //    default:
        //        if (dumped)
        //            currentArg = getArg(field_separator);
        //        if (commandBuffer.available() > 0) {
        //            System.out.println("cmdMessenger:  next: Current arg: " + new String(currentArg));
        //            dumped = true;
        //            return true;
        //        }
        //}
        if (commandBuffer != null && commandBuffer.available() > 0) {
            currentArg = getArg(field_separator);
            System.out.println("cmdMessenger:  next: Current arg: " + new String(currentArg));
            return true;
        }
        return false;
    }

    /**
     * Returns if an argument is available. Alias for next()
     */
    boolean available() {
        return next();
    }

    /**
     * Returns if the latest argument is well formed.
     */
    boolean isArgOk() {
        return ArgOk;
    }

    /**
     * Returns the commandID of the current command
     */
    int commandID() {
        return lastCommandId;
    }

    // ****  Command sending ****


    /**
     * Send start of command. This makes it easy to send multiple arguments per command
     */
    void sendCmdStart(int cmdId) {
        if (!startCommand) {
            startCommand = true;
            pauseProcessing = true;
            printInt(cmdId);
        }
    }

    /**
     * Send an escaped command argument
     */
    void sendCmdEscArg(byte[] arg) {
        if (startCommand) {
            printByte(field_separator);
            printEsc(arg);
        }
    }

    /**
     * Send formatted argument.
     * Note that floating points are not supported and resulting string is limited to 128 chars
     */
    void sendCmdfArg(String template, String... args) {
        String tmp;
        String[] theargs = new String[args.length + 1];
        // fill the array 'theargs'
        System.arraycopy(args, 0, theargs, 0, args.length);
        if (startCommand) {
            tmp = String.format(template, theargs);
            printByte(field_separator);
            printStr(tmp.getBytes());
        }
    }

    /**
     * Send double argument in scientific format.
     * This will overcome the boundary of normal float sending which is limited to abs(f) <= MAXLONG
     */
    void sendCmdSciArg(double arg, int n) {
        if (startCommand) {
            printByte(field_separator);
            printSci(arg, n);
        }
    }

    /**
     * Send int argument
     */
    void sendCmdArg(int arg) {
        if (startCommand) {
            printByte(field_separator);
            printInt(arg);
        }
    }

    /**
     * Send end of command
     */
    boolean sendCmdEnd(boolean reqAc, int ackCmdId, int timeout) {
        boolean ackReply = false;
        if (startCommand) {
            printByte(command_separator);
            if (print_newlines)
                printStr("\r\n".getBytes()); // should append BOTH \r\n
            //flushWrites();
            if (reqAc) {
                ackReply = blockedTillReply(timeout, ackCmdId);
            }
        }
        pauseProcessing = false;
        startCommand = false;
        return ackReply;
    }

    /**
     * Send end of command
     */
    boolean sendCmdEnd() {
        return sendCmdEnd(false, 1, DEFAULT_TIMEOUT);
    }

    /**
     * Send a command without arguments, with acknowledge
     */
    boolean sendCmd(int cmdId, boolean reqAc, int ackCmdId) {
        if (!startCommand) {
            sendCmdStart(cmdId);
            return sendCmdEnd(reqAc, ackCmdId, DEFAULT_TIMEOUT);
        }
        return false;
    }

    /**
     * Send a command without arguments, without acknowledge
     */
    boolean sendCmd(int cmdId) {
        if (!startCommand) {
            sendCmdStart(cmdId);
            return sendCmdEnd(false, 1, DEFAULT_TIMEOUT);
        }
        return false;
    }

// **** Command receiving ****

    /**
     * Read the next argument as int
     */
    public int readIntArg() {
        if (next()) {
            dumped = true;
            ArgOk = true;
            try {
                return (Integer.parseInt(new String(currentArg)));
            } catch (NumberFormatException e) {
                return (0);
            }
        }
        ArgOk = false;
        return -1;
    }

    /**
     * Read the next argument as bool
     */
    public boolean readBoolArg() {

        try {
            return (readIntArg() != 0) ? true : false;
        } catch (NumberFormatException e) {
            return (false);
        }
    }

    /**
     * Read the next argument as char
     */
    public byte[] readCharArg() {
        if (next()) {
            dumped = true;
            ArgOk = true;
            return currentArg;
        }
        ArgOk = false;
        return null;
    }

    /**
     * Read the next argument as float
     */
    public float readFloatArg() {
        if (next()) {
            dumped = true;
            ArgOk = true;
            //return atof(current);
            try {
                return Float.parseFloat(new String(currentArg));
            } catch (NumberFormatException e) {
                return (0);
            }
        }
        ArgOk = false;
        return 0;
    }

    /**
     * Read the next argument as double
     */
    public double readDoubleArg() {
        if (next()) {
            dumped = true;
            ArgOk = true;
            try {
                return Double.parseDouble(new String(currentArg));
            } catch (NumberFormatException e) {
                return (0);
            }
        }
        ArgOk = false;
        return 0;
    }

    /**
     * Read next argument as string.
     * Note that the String is valid until the current command is replaced
     */
    public String readStringArg() {
        if (next()) {
            dumped = true;
            ArgOk = true;
            return (new String(currentArg));
        }
        ArgOk = false;
        return "";
    }


    /**
     * Compare the next argument with a string
     */
    int compareStringArg(String string) {
        if (next()) {
            if (string.compareTo(new String(currentArg)) == 0) {
                dumped = true;
                ArgOk = true;
                return 1;
            } else {
                ArgOk = false;
                return 0;
            }
        }
        return 0;
    }

// **** Escaping tools ****

    /**
     * Unescapes a string
     * Note that this is done inline
     */
    byte[] unescape(byte[] str) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        // Skip escaped chars
        for (byte ch : str) {

            if (ch != escape_character) {
                outputStream.write(ch);
            }
        }
        return (outputStream.toByteArray());
    }

    /**
     * Split string in different tokens, based on delimiter
     * Note that this is basically strtok_r, but with support for an escape character
     */
    byte[] getArg(final int delim) {
        int ch = 0;

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        while (commandBuffer.available() > 0 && ch != delim) {
            ch = commandBuffer.read();
            boolean escaped = (ch == escape_character);
            if (escaped && commandBuffer.available() > 0) {
                ch = commandBuffer.read();
            }
            if (ch != delim) {
                outputStream.write(ch);
            }
        }
        return (outputStream.toByteArray());
    }


    void printInt(int i) {
        ;
        String s = String.format("%d", i);
        printStr(s.getBytes().clone());
    }

    void printByte(byte b) {
        sendBuffer.write(b);
    }

    void printStr(byte[] str) {
        //System.out.println("cmdMessenger: Send \"" + new String(str) + "\"");
        sendBuffer.write(str, 0, str.length);
    }

    /**
     * Escape and print a string
     */
    void printEsc(byte[] str) {
        for (byte ch : str) {
            if (ch == field_separator || ch == command_separator || ch == escape_character) {
                sendBuffer.write(escape_character);
            }
            if (ch == 0) {
                sendBuffer.write((byte)1);
            } else if (ch == 1) {
                sendBuffer.write(escape_character);
                sendBuffer.write((byte)'1');
            } else {
                sendBuffer.write(ch);
            }
        }
    }

    /**
     * Escape and print a character
     */
    void printEsc(byte ch) {
        if (ch == field_separator || ch == command_separator || ch == escape_character) {
            printByte(escape_character);
        }
        if (ch == 0) {
            printByte((byte)1);
        } else if (ch == 1) {
            printByte(escape_character);
            printByte((byte)'1');
        } else {
            printByte(ch);
        }
    }

    /**
     * Print float and double in scientific format
     */
    void printSci(double f, int digits) {
        // handle sign
        if (f < 0.0) {
            printStr("-".getBytes());
            f = -f;
        }

        // handle infinite values
        if (f == Double.POSITIVE_INFINITY) {
            printStr("INF".getBytes());
            return;
        }

        // handle Not a Number
        if (f == Double.NaN) {
            printStr("NaN".getBytes());
            return;
        }

        // max digits
        if (digits > 6) digits = 6;
        double multiplier = Math.pow(10, digits);     // fix int => long

        long exponent;

        if (Math.abs(f) < 10.0) {
            exponent = 0;
        } else {
            exponent = Math.round(Math.log10(f));
        }
        float g = (float) (f / Math.pow(10, exponent));
        if ((g < 1.0) && (g != 0.0)) {
            g *= 10;
            exponent--;
        }

        long whole = Math.round(g);                     // single digit
        long part = Math.round((g - whole) * multiplier + 0.5);  // # digits
        // Check for rounding above .99:
        if (part == 100) {
            whole++;
            part = 0;
        }
        String format;
        format = String.format("%%ld.%%0%dldE%%+d", digits);
        String output;
        output = String.format(format, whole, part, exponent);
        printStr(output.getBytes());
    }


    // comms stuff

    private void l(String str) {
        Log.d(TAG, ">>>>>" + str);
    }

    private void e(String str) {
        Log.e(TAG, ">>>>>" + str);
    }

/*
    public void logInNewThread(String s) {

        mLogLineRunnable = new logLineRunnable(s);
        mHandler.post(mLogLineRunnable);
    }

    class logLineRunnable implements Runnable {

        private String mStr = null;

        public logLineRunnable(String s) {
            mStr = s;
        }

        @Override
        public void run() {
            if (mStr != null) default_callback.CmdAction(">>>USB " + mStr + "\n");
            return;
        }

    }*/

    //listener interface
    @Override
    public void onNewData(byte[] data) {
        if (data.length > 0) {

            System.out.println("cmdMessenger: Received " + data.length + "bytes: " + new String(data));

            synchronized (mPipedInputStream) {
                for (int i = 0; i < data.length; i++) {
                    try {
                        mPipedOutputStream.write(data[i]);
                        mPipedOutputStream.flush();
                        // for some reason, readline blocks after ready if no carriage-return
                        if (data[i] == command_separator) {
                            mPipedOutputStream.write(10);
                            mPipedOutputStream.flush();
                        }
                        if ((data[i] == 10) || (data[i] == command_separator)) {//newline
                            ProcessMessageInNewThread();//read newline and forward to application
                        }

                        //if (data[i] == 0) {
                        //    //if we receive data 0, it is definitely binary data. so send the Arduino program 'r' commmand to switch to 'r'eadable
                        //   Serial.write("binary".getBytes(), 200);
                        //    //SerialInputOutputManager comms = this;
                        //    break;
                        //}

                    } catch (Exception e) {
                        e("onNewData.write failed");
                    }
                }
            }//sync

        }//length >0

    }

    //----------------------------------
    public void ProcessMessageInNewThread() {
        mProcessMessageRunnable = new ProcessMessageRunnable();
        mHandler.post(mProcessMessageRunnable);
        //Thread myThread = new Thread(new ProcessMessageRunnable());
        //myThread.start();
    }


    // **** Command processing ****

    /**
     * Processes bytes and determines message state
     * Reads the streamBuffer and calls processLine() to fill a commandBufferTmp for each message
     * When end-of-message arrives, copy commandBufferTmp into commandBuffer for processing.
     * Calls handleMessage()
     */
    class ProcessMessageRunnable implements Runnable {

        @Override
        public void run() {
            String msg = readLine();//readLine should be in new thread according to pipeline documentation
            //System.out.print("processmessagerunnable\n");
            if (msg != null && msg != "") {
                streamBuffer = new ByteArrayInputStream(msg.getBytes());
                while (streamBuffer.available() > 0) {
                    if (processLine() == kEndOfMessage) {
                        byte[] tmp = commandBufferTmp.toByteArray();
                        System.out.println("cmdMessenger: run() commandBuffer= " + new String(tmp));
                        commandBuffer = new ByteArrayInputStream(tmp);
                        handleMessage();
                        commandBufferTmp.reset();
                    }
                }
            }
        }
    }

    public String readLine() {

        synchronized (mPipedInputStream) {

            String ret = null;
            try {
                if (mBufferedReader.ready()) {
                    ret = mBufferedReader.readLine();
                }
            } catch (Exception e) {
                e("readLine failed");
            }
            return ret;
        }//sync

    }


    int processLine() {
        int ch;

        /*
         * Process the bytes in the stream buffer,
         * and handles dispatches callbacks, if commands are received
         *
         *
         *
         */
        messageState = kProccesingMessage;
        while (messageState != kEndOfMessage && streamBuffer.available() > 0) {
            ch = streamBuffer.read();
            boolean escaped = (ch == escape_character);
            if (escaped && streamBuffer.available() > 0) {
                ch = streamBuffer.read();
            }
            if ((ch == command_separator) && !escaped) {
                if (commandBufferTmp.size() > 0) {
                    messageState = kEndOfMessage;
                }
                //commandBufferTmp.reset();
                dumped = true;
            } else {
                commandBufferTmp.write(ch);
                if (commandBufferTmp.size() == MESSENGERBUFFERSIZE) {
                    //commandBufferTmp.reset();
                    dumped = true;
                }
            }
        }
        return (messageState);
    }


    /**
     * Dispatches attached callbacks based on command
     */
    void handleMessage() {
        CmdEvents callback;

        System.out.println("cmdMessenger: Handling: ");

        lastCommandId = readIntArg();
        // if command attached, we will call it
        if (lastCommandId >= 0 && ((callback = callbackList.get(lastCommandId)) != null) && ArgOk) {
            callback.CmdAction("callback cmd: " + lastCommandId);
        }

        /*else {// If command not attached, call  callback (if attached)
            if (default_callback != null)
                default_callback.CmdAction("default callback\n");
        }
        */

    }

    void onDestroy() {

        mHandler.removeCallbacks(null);
    }

    @Override
    public void onRunError(Exception e) {

        e("onRunError: " + e.getMessage());
    }


}


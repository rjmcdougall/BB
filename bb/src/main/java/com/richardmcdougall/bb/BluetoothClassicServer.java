package com.richardmcdougall.bb;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;


import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class BluetoothClassicServer {

    private static final String TAG = "BB.BluetoothCSvr";
    private int mState;
    private int mNewState;
    private Context mContext;
    private ConnectedThread mConnectedThread;
    private AcceptThread mSecureAcceptThread;
    private BluetoothAdapter mAdapter;

    // Name for the SDP record when creating server socket
    private static final String NAME_SECURE = "Burner Board";
    private static final String NAME_INSECURE = "Burner Board";

    // Unique UUIDs
    private static final UUID BB_UUID_SECURE =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
//            UUID.fromString("683d6e30-f08b-11e8-8eb2-f2801f1b9fd1");
    private static final UUID BB_UUID_INSECURE =
//            UUID.fromString("683d70ce-f08b-11e8-8eb2-f2801f1b9fd1");
            UUID.fromString("00001101-1000-1000-8000-00805F9B34FB");


    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

    public BluetoothClassicServer(BBService bbservice, Context context) {
        mContext = context;
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mNewState = mState;
    }

    public void l(String s) {
        Log.v(TAG, s);
        sendLogMsg(s);
    }

    public void d(String s) {
        if (BBService.debug == true) {
            Log.v(TAG, s);
            sendLogMsg(s);
        }
    }

    private void sendLogMsg(String msg) {
        Intent in = new Intent(BBService.ACTION_STATS);
        in.putExtra("resultCode", Activity.RESULT_OK);
        in.putExtra("msgType", 4);
        // Put extras into the intent as usual
        in.putExtra("logMsg", msg);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(in);
    }

    // Callbacks for consumers of Bluetooth events
    private HashMap<Integer, BluetoothCallback> callbackFuncs =
            new HashMap<Integer, BluetoothCallback>();
    private HashMap<Integer, String> callbackCommands =
            new HashMap<>();
    private int callbackId = 0;

    // TODO: do we need per-connection contexts in callbacks?
    /**
     * Command is JSON Object with "command" as a mandatory first field
     * Response is JSON with arbitrary format
     */
    public interface BluetoothCallback {
        void onConnected(String clientId);
        void onDisconnected(String clientId);
        void OnAction(String clientId, String command, JSONObject payload);
    }

    /**
     * Registers a callback for a response type
     */
    public int addCallback(String command, BluetoothCallback newFunction) {
        int thisCallbackId = callbackId;
        if (command.length() > 0) {
            callbackFuncs.put(callbackId, newFunction);
            callbackCommands.put(callbackId, command);
        }
        callbackId++;

        return thisCallbackId;
    }

    /**
     * Return the current connection state.
     */
    public synchronized int getState() {
        return mState;
    }

    public synchronized void start() {
        Log.d(TAG, "start");

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to listen on a BluetoothServerSocket
        if (mSecureAcceptThread == null) {
            mSecureAcceptThread = new AcceptThread(true);
            mSecureAcceptThread.start();
        }
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        Log.d(TAG, "stop");

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }

        mState = STATE_NONE;
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice
            device, final String socketType) {
        d("connected, Socket Type:" + socketType);

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Cancel the accept thread because we only want to connect to one device
        // TODO: do we need multiple client conns for BB?
        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket, socketType);
        mConnectedThread.start();

        // Notify all callbacks
        Iterator it = callbackFuncs.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry callback = (Map.Entry)it.next();
            l("calling callback: " + callback.getKey() + " = " + callback.getValue());
            try {
                BluetoothCallback bcb = (BluetoothCallback)callback.getValue();
                bcb.onConnected(socket.toString());
            } catch (Exception e) {
                l("Cannot notify callback about connect");
            }
            //it.remove(); // avoids a ConcurrentModificationException
        }
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     *
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }

    /**
     * Indicate that the connection was lost
     */
    private void connectionLost(BluetoothSocket socket) {
        d("connectionLost: " + socket.toString());

        mState = STATE_NONE;
        // Notify callbacks
        Iterator func = callbackFuncs.entrySet().iterator();
        while (func.hasNext()) {
            Map.Entry callback = (Map.Entry)func.next();
            l("calling callback: " + callback.getKey() + " = " + callback.getValue());
            try {
                BluetoothCallback bcb = (BluetoothCallback)callback.getValue();
                bcb.onDisconnected(socket.toString());
            } catch (Exception e) {
                l("Cannot notify callback about connect");
            }
            //func.remove(); // avoids a ConcurrentModificationException
        }
        // Start the service over to restart listening mode
        this.start();
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptThread extends Thread {
        // The local server socket
        private final BluetoothServerSocket mmServerSocket;
        private String mSocketType;

        public AcceptThread(boolean secure) {
            BluetoothServerSocket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";

            d("AcceptThread start");

            // Create a new listening server socket
            try {
                if (secure) {
                    tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME_SECURE,
                            BB_UUID_SECURE);
                } else {
                    tmp = mAdapter.listenUsingInsecureRfcommWithServiceRecord(
                            NAME_INSECURE, BB_UUID_INSECURE);
                }
            } catch (IOException e) {
                l("Error Socket Type: " + mSocketType + "listen() failed"  + e.getMessage());
            }
            mmServerSocket = tmp;
            mState = STATE_LISTEN;
        }

        public void run() {
            d("run Socket Type: " + mSocketType +
                    "BEGIN mAcceptThread" + this);
            setName("AcceptThread" + mSocketType);

            l("AcceptThread callbackFuncs = " + callbackFuncs.toString());
            l("AcceptThread callbackCommands = " + callbackCommands.toString());

            BluetoothSocket socket = null;

            // Listen to the server socket if we're not connected
            while (mState != STATE_CONNECTED) {
                if (mmServerSocket == null) {
                    l("socket null");
                    try {
                        Thread.sleep(5000);
                    } catch (Exception e) {
                    }
                    continue;
                }
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    l("socket accept() waiting....");
                    socket = mmServerSocket.accept();
                    l("socket accept() return....");

                } catch (IOException e) {
                    l("Socket Type: " + mSocketType + "accept() failed" + e.getMessage());
                    break;
                }

                l("AcceptThread2 callbackFuncs = " + callbackFuncs.toString());
                l("AcceptThread2 callbackCommands = " + callbackCommands.toString());

                // If a connection was accepted
                if (socket != null) {
                    synchronized (this) {
                        switch (mState) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                // Situation normal. Start the connected thread.
                                connected(socket, socket.getRemoteDevice(),
                                        mSocketType);
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                // Either not ready or already connected. Terminate new socket.
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    Log.e(TAG, "Could not close unwanted socket", e);
                                }
                                break;
                        }
                    }
                }
            }
            l("END mAcceptThread, socket Type: " + mSocketType);

        }

        public void cancel() {
            d("Socket Type" + mSocketType + "cancel " + this);
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                l( "Socket Type" + mSocketType + "close() of server failed" + e.getMessage());
            }
        }
    }

    static final int kReceiveStateInprogress = 1;
    static final int kReceiveStateMessageComplete = 2;

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private int mReceiveState = kReceiveStateInprogress;
        ByteArrayOutputStream mReceiveBuffer;

        public ConnectedThread(BluetoothSocket socket, String socketType) {
            Log.d(TAG, "create ConnectedThread: " + socketType);
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
            mState = STATE_CONNECTED;
            mReceiveBuffer = new ByteArrayOutputStream();
            mReceiveBuffer.reset();
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];
            int bytes = 0;

            l("callbackFuncs = " + callbackFuncs.toString());
            l("callbackCommands = " + callbackCommands.toString());

            // Keep listening to the InputStream while connected
            while (mState == STATE_CONNECTED) {
                try {
                    // Read from the InputStream
                    d("waiting for read...");
                    bytes = mmInStream.read(buffer);
                    d("read: " + bytes + " bytes: " + buffer);
                    if (bytes > 0) {
                        processReceive(Arrays.copyOf(buffer, bytes));
                    }
                } catch (IOException e) {
                    d("disconnected: " + e.getMessage());
                    connectionLost(mmSocket);
                    break;
                }
            }
        }

        // Process stream fragments and delimeters
        boolean processReceive(byte[] bytes) {

            boolean success = false;
            for (byte oneChar: bytes) {
                if ((oneChar == ';')) {
                    // Execute complete command
                    JSONObject cmd;
                    if ((cmd = extractCommand(mReceiveBuffer.toString())) != null) {
                        success = processCommand(cmd);
                    }
                    mReceiveBuffer.reset();
                } else {
                    // Write to command buffer
                    try {
                        mReceiveBuffer.write(bytes);
                    } catch (Exception e) {
                        l("could not write bytes: " + e.getMessage());
                    }
                }
            }
            return success;
        }

        // Checks and extracts the commmand in JSON format from the string
        JSONObject extractCommand(String cmd) {
            JSONObject command = null;
            try {
                command = new JSONObject(cmd);
            } catch (Exception e) {
                l("Could not parse message");
                return null;
            }
            if (!command.has("command")) {
                l("Command has no command field");
                return null;
            }
            return (command);
        }

        // Run the callbacks
        // TODO: do we run these on the calling thread, or a separate thread?
        boolean processCommand(JSONObject cmdJson) {

            l("callbackFuncs = " + callbackFuncs.toString());
            l("callbackCommands = " + callbackCommands.toString());

            Iterator it = callbackCommands.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry callbackCmd = (Map.Entry)it.next();
                l("checking callback: " + callbackCmd.getKey() + " = " + callbackCmd.getValue());
                try {
                        String thisCallbackCmd = (String)callbackCmd.getValue();
                        int thisCallbackId = (int)callbackCmd.getKey();
                        String requestedCommand = cmdJson.getString("command");

                    if (requestedCommand.equals(thisCallbackCmd)) {
                        BluetoothCallback bcb = (BluetoothCallback)callbackFuncs.get(thisCallbackId);
                        if (bcb == null) {
                            l("Could not find callback for id: " + thisCallbackId);
                        }
                        bcb.OnAction(mmSocket.toString(), requestedCommand, cmdJson);
                        }
                } catch (Exception e) {
                    l("error on bluetooth command: " + e.getMessage());
                    return  false;
                }
                //it.remove(); // avoids a ConcurrentModificationException
            }
            return true;
        }


            /*
                    final String kDelimeter = "[\\r\\n]+";

            String sbytes = new String(bytes);

            // complete messages or potential partial
            String fragements[] = sbytes.split(kDelimeter);
            for (String frag: fragements) {
                if (sbytes.contains(kDelimeter)) {


                    }
            } else {
                // Partial message
                try {
                    mReceiveBuffer.write(bytes);
                } catch (Exception e) {
                    l("could not write bytes: " + e.getMessage());
                }
            }
            */

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

}

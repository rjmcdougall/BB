package com.richardmcdougall.bb.mesh;


import com.richardmcdougall.bbcommon.BLog;



public class PacketProcessor {
    private String TAG = this.getClass().getSimpleName();
    private static final byte START1 = (byte) 0x94;
    private static final byte START2 = (byte) 0xc3;
    private static final int MAX_TO_FROM_RADIO_SIZE = 512;

    public interface PacketInterface {
        void onReceivePacket(byte[] bytes);
        void onConnect();
        void flushBytes();
        void sendBytes(byte[] bytes);
    }

    public PacketInterface mCallback;
    private final StringBuilder debugLineBuf = new StringBuilder();

    private int ptr = 0;
    private int msb = 0;
    private int lsb = 0;
    private int packetLen = 0;

    public PacketProcessor(PacketInterface callback) {
        mCallback = callback;
    }

    public void connect() {
        byte[] wakeBytes = new byte[]{START1, START1, START1, START1};
        sendBytes(wakeBytes);
        mCallback.onConnect();
    }

    private void sendBytes(byte[] p) {
        mCallback.sendBytes(p);
    };
    private void flushBytes() {
        mCallback.flushBytes();
    }

    public void sendToRadio(byte[] p) {
        byte[] header = new byte[4];
        header[0] = START1;
        header[1] = START2;
        header[2] = (byte) (p.length >> 8);
        header[3] = (byte) (p.length & 0xff);

        sendBytes(header);
        sendBytes(p);
        flushBytes();
    }

    private void debugOut(byte b) {
        char c = (char) b;
        if (c == '\r') {
            // ignore
        } else if (c == '\n') {
            BLog.d(TAG, "DeviceLog: " + debugLineBuf);
            debugLineBuf.setLength(0); // Clear the StringBuilder
        } else {
            debugLineBuf.append(c);
        }
    }

    private final byte[] rxPacket = new byte[MAX_TO_FROM_RADIO_SIZE];

    public void processByte(byte c) {
        int nextPtr = ptr + 1;

        Runnable deliverPacket = () -> {
            byte[] buf = new byte[packetLen];
            System.arraycopy(rxPacket, 0, buf, 0, packetLen);
            mCallback.onReceivePacket(buf);
        };

        switch (ptr) {
            case 0: // looking for START1
                if (c != START1) {
                    debugOut(c);
                    nextPtr = 0; // Restart from scratch
                }
                break;
            case 1: // Looking for START2
                if (c != START2) {
                    BLog.d(TAG, "Lost protocol sync");
                    nextPtr = 0;
                }
                break;
            case 2: // Looking for MSB of our 16 bit length
                msb = c & 0xff;
                break;
            case 3: { // Looking for LSB of our 16 bit length
                lsb = c & 0xff;

                packetLen = (msb << 8) | lsb;
                if (packetLen > MAX_TO_FROM_RADIO_SIZE) {
                    BLog.d(TAG, "Lost protocol sync");
                    nextPtr = 0;
                } else if (packetLen == 0) {
                    deliverPacket.run(); // zero length packets are valid and should be delivered immediately (because there won't be a next byte of payload)
                    nextPtr = 0; // Start parsing the next packet
                }
                break;
            }
            default: {
                // We are looking at the packet bytes now
                rxPacket[ptr - 4] = c;

                // Note: we have to check if ptr +1 is equal to packet length (for example, for a 1 byte packetlen, this code will be run with ptr of 4
                if (ptr - 4 + 1 == packetLen) {
                    deliverPacket.run();
                    nextPtr = 0; // Start parsing the next packet
                }
                break;
            }
        }
        ptr = nextPtr;
    }
}


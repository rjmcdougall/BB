package com.richardmcdougall.bb.rf;

import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bbcommon.BLog;

import net.sf.marineapi.nmea.util.Position;
import net.sf.marineapi.nmea.util.Time;
import net.sf.marineapi.provider.event.PositionEvent;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class FindMyFriends {
    private String TAG = this.getClass().getSimpleName();

    private BBService service;
    private long mLastFix = 0;
    private static final int kMaxFixAge = 30000;
    private static final int krepeatedBy = 0;
    private int mLat;
    private int mLon;
    private int mAlt;
    private int mAmIAccurate;
    private int mTheirAddress;
    private double mTheirLat;
    private double mTheirLon;
    private int mTheirBatt;
    private int mThereAccurate;
    private long mLastSend = 0;
    private long mLastRecv = 0;
    private byte[] mLastHeardLocation;
    private boolean mTheirInCrisis = false;

    public FindMyFriends(BBService service) {
        this.service = service;
        BLog.d(TAG, "Starting FindMyFriends");

        if (service.radio == null) {
            BLog.d(TAG, "No Radio!");
            return;
        }

        service.radio.attach(new RF.radioEvents() {
            @Override
            public void receivePacket(byte[] bytes, int sigStrength) {
                BLog.d(TAG, "FMF Packet: len(" + bytes.length + "), data: " + RFUtil.bytesToHex(bytes));
                if (processReceive(bytes, sigStrength)) {

                }
            }

            @Override
            public void GPSevent(PositionEvent gps) {
                BLog.d(TAG, "FMF Position: " + gps.toString());
                Position pos = gps.getPosition();
                mLat = (int) (pos.getLatitude() * 1000000);
                mLon = (int) (pos.getLongitude() * 1000000);
                mAlt = (int) (pos.getAltitude() * 1000000);
                long sinceLastFix = System.currentTimeMillis() - mLastFix;

                if (sinceLastFix > kMaxFixAge) {
                    BLog.d(TAG, "FMF: sending GPS update");
                    service.iotClient.sendUpdate("bbevent", "[" +
                            service.boardState.BOARD_ID + "," + 0 + "," + mLat / 1000000.0 + "," + mLon / 1000000.0 + "]");
                    broadcastGPSpacket(mLat, mLon, mAlt, mAmIAccurate, 0, 0);
                    mLastFix = System.currentTimeMillis();

                }
            }

            @Override
            public void timeEvent(Time time) {
                BLog.d(TAG, "FMF Time: " + time.toString());
            }
        });
        this.service.boardLocations.addTestBoardLocations();

    }

    // GPS Packet format =
    //         [0] kGPSMagicNumber byte one
    //         [1] kGPSMagicNumber byte two
    //         [2] board address bits 0-7
    //         [3] board address bits 8-15
    //         [4] repeatedBy
    //         [5] lat bits 0-7
    //         [6] lat bits 8-15
    //         [7] lat bits 16-23
    //         [8] lat bits 24-31
    //         [9] lon bits 0-7
    //         [10] lon bits 8-15
    //         [11] lon bits 16-23
    //         [12] lon bits 24-31
    //         [13] elev bits 0-7
    //         [14] elev bits 8-15
    //         [15] elev bits 16-23
    //         [16] elev bits 24-31
    //         [17] i'm accurate flag

    //         [18] heading bits 0-7
    //         [19] heading bits 8-15
    //         [20] heading bits 16-23
    //         [21] heading bits 24-31
    //         [22] speed bits 0-7
    //         [23] speed bits 8-15
    //         [24] speed bits 16-23
    //         [25] speed bits 24-31

    public final static int BBGPSPACKETSIZE = 18;

    private void broadcastGPSpacket(int lat, int lon, int elev, int iMAccurate,
                                    int heading, int speed) {

        // Check GPS data is not stale
        int len = 2 * 4 + 1 + RFUtil.kMagicNumberLen + 1;
        ByteArrayOutputStream radioPacket = new ByteArrayOutputStream();

        for (int i = 0; i < RFUtil.kMagicNumberLen; i++) {
            radioPacket.write(RFUtil.kGPSMagicNumber[i]);
        }

        radioPacket.write(service.boardState.address & 0xFF);
        radioPacket.write((service.boardState.address >> 8) & 0xFF);
        radioPacket.write(krepeatedBy);
        radioPacket.write(lat & 0xFF);
        radioPacket.write((lat >> 8) & 0xFF);
        radioPacket.write((lat >> 16) & 0xFF);
        radioPacket.write((lat >> 24) & 0xFF);
        radioPacket.write(lon & 0xFF);
        radioPacket.write((lon >> 8) & 0xFF);
        radioPacket.write((lon >> 16) & 0xFF);
        radioPacket.write((lon >> 24) & 0xFF);
        radioPacket.write(service.boardState.batteryLevel & 0xFF);
        RFUtil.boolToPacket(radioPacket, service.boardState.inCrisis);
        radioPacket.write((0) & 0xFF); // spare
        radioPacket.write((0) & 0xFF); // spare

        radioPacket.write(iMAccurate);
        radioPacket.write(0);

        BLog.d(TAG, "Sending packet...");
        service.radio.broadcast(radioPacket.toByteArray());
        mLastSend = System.currentTimeMillis();
        BLog.d(TAG, "Sent packet...");
        this.service.boardLocations.updateBoardLocations(service.boardState.address, 999,
                lat / 1000000.0, lon / 1000000.0, service.boardState.batteryLevel, radioPacket.toByteArray(), service.boardState.inCrisis);
    }

    boolean processReceive(byte[] packet, int sigStrength) {

        try {
            ByteArrayInputStream bytes = new ByteArrayInputStream(packet);

            int recvMagicNumber = RFUtil.magicNumberToInt(
                    new int[]{bytes.read(), bytes.read()});

            if (recvMagicNumber == RFUtil.magicNumberToInt(RFUtil.kGPSMagicNumber)) {
                BLog.d(TAG, "BB GPS Packet");
                mTheirAddress = (int) ((bytes.read() & 0xff) +
                        ((bytes.read() & 0xff) << 8));
                int repeatedBy = bytes.read();
                mTheirLat = (double) ((bytes.read() & 0xff) +
                        ((bytes.read() & 0xff) << 8) +
                        ((bytes.read() & 0xff) << 16) +
                        ((bytes.read() & 0xff) << 24)) / 1000000.0;
                mTheirLon = (double) ((bytes.read() & 0xff) +
                        ((bytes.read() & 0xff) << 8) +
                        ((bytes.read() & 0xff) << 16) +
                        ((bytes.read() & 0xff) << 24)) / 1000000.0;
                mTheirBatt = (int) (bytes.read() & 0xff);
                mTheirInCrisis = RFUtil.boolFromPacket(bytes);
                bytes.read(); // spare
                bytes.read(); // spare
                mThereAccurate = bytes.read();
                mLastRecv = System.currentTimeMillis();
                mLastHeardLocation = packet.clone();
                BLog.d(TAG, service.allBoards.boardAddressToName(mTheirAddress) +
                        " strength " + sigStrength +
                        "theirLat = " + mTheirLat + ", " +
                        "theirLon = " + mTheirLon +
                        "theirBatt = " + mTheirBatt);
                service.iotClient.sendUpdate("bbevent", "[" +
                        service.allBoards.boardAddressToName(mTheirAddress) + "," +
                        sigStrength + "," + mTheirLat + "," + mTheirLon + "]");
                this.service.boardLocations.updateBoardLocations(mTheirAddress, sigStrength, mTheirLat, mTheirLon, mTheirBatt, packet.clone(), mTheirInCrisis);
                return true;
            }
            else {
                BLog.d(TAG, "rogue packet not for us!");
            }
            return false;
        } catch (Exception e) {
            BLog.e(TAG, "Error processing a received packet " + e.getMessage());
            return false;
        }
    }
}

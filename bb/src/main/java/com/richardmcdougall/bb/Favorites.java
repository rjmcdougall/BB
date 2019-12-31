package com.richardmcdougall.bb;

import android.content.Context;

import timber.log.Timber;

import net.sf.marineapi.nmea.util.Time;
import net.sf.marineapi.provider.event.PositionEvent;

import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

/**
 * Created by rmc on 2/7/18.
 */

public class Favorites {

    private static final String FAVORITES_JSON = "favorites.json";
    private BBService service;
    private favoritesCallback mFavoritesCallback = null;
    private static final int krepeatedBy = 0;
    private int mThereAccurate;
    private long mLastSend = 0;
    private long mLastRecv = 0;
    private int mBoardAddress = 0;
    private byte[] mLastHeardLocation;

    public Favorites(Context context, BBService service,
                     final RF radio, Gps gps, IoTClient iotclient) {
        this.service = service;
        Timber.d("Starting favorites");

        if (service.radio == null) {
            Timber.d("No Radio!");
            return;
        }

        service.radio.attach(new RF.radioEvents() {
            @Override
            public void receivePacket(byte[] bytes, int sigStrength) {
                Timber.d("Favorites Packet: len(" + bytes.length + "), data: " + RFUtil.bytesToHex(bytes));
                if (processReceive(bytes, sigStrength)) {

                }
            }

            @Override
            public void GPSevent(PositionEvent gps) {
                Timber.d("Favorites GPS Event");
            }

            @Override
            public void timeEvent(Time time) {
                Timber.d("Favorites Time: " + time.toString());
            }
        });
        addTestFavorite();
        getFavorites(); // load from disk
    }

    public final static int BBFavoritesPacketSize = 18;

    private void broadcastFavoritesPacket(int lat, int lon, int iMAccurate, String message) {

        byte[] b = message.substring(0, 4).getBytes();

        // Check GPS data is not stale
        int len = 2 * 4 + 1 + RFUtil.kMagicNumberLen + 1;
        ByteArrayOutputStream radioPacket = new ByteArrayOutputStream();

        for (int i = 0; i < RFUtil.kMagicNumberLen; i++) {
            radioPacket.write(RFUtil.kFavoritesMagicNumber[i]);
        }

        radioPacket.write(mBoardAddress & 0xFF);
        radioPacket.write((mBoardAddress >> 8) & 0xFF);
        radioPacket.write(krepeatedBy);
        radioPacket.write(lat & 0xFF);
        radioPacket.write((lat >> 8) & 0xFF);
        radioPacket.write((lat >> 16) & 0xFF);
        radioPacket.write((lat >> 24) & 0xFF);
        radioPacket.write(lon & 0xFF);
        radioPacket.write((lon >> 8) & 0xFF);
        radioPacket.write((lon >> 16) & 0xFF);
        radioPacket.write((lon >> 24) & 0xFF);
        radioPacket.write((b[0]) & 0xFF); // spare
        radioPacket.write((b[1]) & 0xFF); // spare
        radioPacket.write((b[2]) & 0xFF); // spare
        radioPacket.write((b[3]) & 0xFF); // spare

        radioPacket.write(iMAccurate);
        radioPacket.write(0);

        Timber.d("Sending Favorites packet...");
        service.radio.broadcast(radioPacket.toByteArray());
        mLastSend = System.currentTimeMillis();
        Timber.d("Sent Favorites packet...");

        Fav f = new Fav();
        f.r = mBoardAddress;
        f.d = DateTime.now();
        f.a = lat;
        f.o = lon;
        f.n = b.toString();

        UpdateFavorites(f);
    }


    public interface favoritesCallback {

    }

    public void attach(Favorites.favoritesCallback newfunction) {

        mFavoritesCallback = newfunction;
    }

    boolean processReceive(byte[] packet, int sigStrength) {

        try {
            ByteArrayInputStream bytes = new ByteArrayInputStream(packet);

            Fav f = new Fav();

            int recvMagicNumber = RFUtil.magicNumberToInt(
                    new int[]{bytes.read(), bytes.read()});

            if (recvMagicNumber == RFUtil.magicNumberToInt(RFUtil.kFavoritesMagicNumber)) {
                Timber.d("BB Favorites Packet");
                f.r = (int) ((bytes.read() & 0xff) +
                        ((bytes.read() & 0xff) << 8));
                int repeatedBy = bytes.read();
                f.a = (double) ((bytes.read() & 0xff) +
                        ((bytes.read() & 0xff) << 8) +
                        ((bytes.read() & 0xff) << 16) +
                        ((bytes.read() & 0xff) << 24)) / 1000000.0;
                f.o = (double) ((bytes.read() & 0xff) +
                        ((bytes.read() & 0xff) << 8) +
                        ((bytes.read() & 0xff) << 16) +
                        ((bytes.read() & 0xff) << 24)) / 1000000.0;
                f.n += (char) bytes.read(); // note character 1
                f.n += (char) bytes.read(); //  note character 2
                f.n += (char) bytes.read(); //  note character 3
                f.n += (char) bytes.read(); //  note character 4
                mThereAccurate = bytes.read();
                mLastRecv = System.currentTimeMillis();
                mLastHeardLocation = packet.clone();
                Timber.d(service.allBoards.boardAddressToName(f.r) +
                        " strength " + sigStrength +
                        "favorites lat = " + f.a + ", " +
                        "favorites Lon = " + f.o);
                service.iotClient.sendUpdate("bbevent", "[" +
                        service.allBoards.boardAddressToName(f.r) + "," +
                        sigStrength + "," + f.a + "," + f.o + "]");


                UpdateFavorites(f);
                return true;
            } else {
                Timber.d("rogue packet not for us!");
            }
            return false;
        } catch (Exception e) {
            Timber.e("Error processing a received packet " + e.getMessage());
            return false;
        }
    }

    // keep a historical list of minimal location data
    private class Fav {
        public int r; // address
        public DateTime d; // dateTime
        public double a; // latitude
        public double o; // longitude
        public String n;
    }

    private HashMap<String, Fav> mFavorites = new HashMap<>();

    public void UpdateFavorites(Fav fav) {
        try {

            if (!mFavorites.containsKey(fav.n)) {
                mFavorites.put(fav.n, fav);
                saveFavorites();
            }
            // Update the JSON blob in the ContentProvider. Used in integration with BBMoblie for the Panel.
            //ContentValues v = new ContentValues(1);
            // v.put("0",getBoardLocationsJSON().toString());
            //mContext.getContentResolver().update(Contract.CONTENT_URI, v, null, null);

        } catch (Exception e) {
            Timber.e("Error storing the favorites history " + e.getMessage());
        }

    }

    private void addTestFavorite() {

        Fav f = new Fav();
        f.r = mBoardAddress;
        f.o = -122.391883;
        f.a = 37.77728;
        f.d = new DateTime("2/2/2020");
        f.n = "yes!";
        UpdateFavorites(f);
    }

    public boolean saveFavorites() {

        try {

            JSONArray favorites = new JSONArray(mFavorites);

            FileWriter fw = new FileWriter(service.filesDir + "/" + FAVORITES_JSON);
            fw.write(favorites.toString());
            fw.close();
        } catch (JSONException e) {
            Timber.e(e.getMessage());
            return false;
        } catch (IOException e) {
            Timber.e(e.getMessage());
            return false;
        }

        return true;
    }

    // keep favorites persisted between reboots.
    public void getFavorites() {
        try {

            File f = new File(service.filesDir + "/" + FAVORITES_JSON);
            InputStream is = null;
            try {
                is = new FileInputStream(f);
            } catch (FileNotFoundException e) {
                Timber.e(e.getMessage());
            }
            BufferedReader buf = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder(buf.readLine());
            Timber.d("contents of favorites.json: " + sb.toString());
            JSONArray j = new JSONArray(sb.toString());

            HashMap<String, Fav> favs = new HashMap<>();
            for (int i = 0; i < j.length(); i++) {
                JSONObject jFav = j.getJSONObject(i);
                Fav ff = new Fav();
                ff.r = jFav.getInt("r");
                ff.o = jFav.getInt("o");
                ff.a = jFav.getInt("a");
                //  ff.d =  jFav.get("");
                ff.n = jFav.getString("n");
                UpdateFavorites(ff);
                favs.put(jFav.getString(""), ff);
            }

        } catch (Throwable e) {
            Timber.e(e.getMessage());
        }
    }
}

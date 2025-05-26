package com.richardmcdougall.bb.mesh;

import android.os.Parcel;
import android.os.Parcelable;

import com.geeksville.mesh.ConfigProtos;
import com.geeksville.mesh.MeshProtos;

public class Position  {

    public double latitude;
    public double longitude;
    public int altitude;
    public int time; // in secs (NOT MILLISECONDS!)
    public int satellitesInView;
    public int groundSpeed;
    public int groundTrack; // "heading"
    public int precisionBits;

    // Constructors

    public Position(double latitude, double longitude, int altitude) {
        this(latitude, longitude, altitude, currentTime()); // Default to current time
    }

    public Position(double latitude, double longitude, int altitude, int time) {
        this(latitude, longitude, altitude, time, 0, 0, 0, 0); // Default other parameters
    }

    public Position(double latitude, double longitude, int altitude, int time, int satellitesInView,
                    int groundSpeed, int groundTrack, int precisionBits) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
        this.time = time;
        this.satellitesInView = satellitesInView;
        this.groundSpeed = groundSpeed;
        this.groundTrack = groundTrack;
        this.precisionBits = precisionBits;
    }

    // Constructor from Protobuf

    public Position(MeshProtos.Position position) {
        this(position, currentTime());
    }

    public Position(MeshProtos.Position position, int defaultTime) {
        this(
                degD(position.getLatitudeI()),
                degD(position.getLongitudeI()),
                position.getAltitude(),
                position.getTime() != 0 ? position.getTime() : defaultTime,
                position.getSatsInView(),
                position.getGroundSpeed(),
                position.getGroundTrack(),
                position.getPrecisionBits()
        );
    }

    public static int currentTime() {
        return (int) (System.currentTimeMillis() / 1000);
    }

    public static double degD(int i) {
        return i * 1e-7;
    }

    public static int degI(double d) {
        return (int) (d * 1e7);
    }

    // Instance Methods

    public double distance(Position o) {
        return LocationUtils.latLongToMeter(latitude, longitude, o.latitude, o.longitude);
    }

    public double bearing(Position o) {
        return LocationUtils.bearing(latitude, longitude, o.latitude, o.longitude);
    }

    public boolean isValid() {
        return latitude != 0.0 && longitude != 0.0 &&
                (latitude >= -90 && latitude <= 90.0) &&
                (longitude >= -180 && longitude <= 180);
    }


    public String gpsString(int gpsFormat) {
        switch (gpsFormat) {
            case ConfigProtos.Config.DisplayConfig.GpsCoordinateFormat.DEC_VALUE:
                return GPSFormat.DEC(this);
            case ConfigProtos.Config.DisplayConfig.GpsCoordinateFormat.DMS_VALUE:
                return GPSFormat.DMS(this);
            case ConfigProtos.Config.DisplayConfig.GpsCoordinateFormat.UTM_VALUE:
                return GPSFormat.UTM(this);
            case ConfigProtos.Config.DisplayConfig.GpsCoordinateFormat.MGRS_VALUE:
                return GPSFormat.MGRS(this);
            default:
                return GPSFormat.DEC(this);
        }
    }


    @Override
    public String toString() {
        return "Position(lat=" + latitude + ", lon=" + longitude +
                ", alt=" + altitude + ", time=" + time + ")";
    }
}



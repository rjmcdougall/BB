package com.richardmcdougall.bb.mesh;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Pair;

import com.geeksville.mesh.ConfigProtos;
import com.geeksville.mesh.MeshProtos;

public class NodeInfo implements Parcelable {

    public int num; // This is immutable, and used as a key
    //@Embedded(prefix = "user_") // Not needed in pure Java
    public MeshUser user;
    //@Embedded(prefix = "position_") // Not needed in pure Java
    public Position position;
    public float snr = Float.MAX_VALUE;
    public int rssi = Integer.MAX_VALUE;
    public int lastHeard; // the last time we've seen this node in secs since 1970
    public DeviceMetrics deviceMetrics;
    public int channel;
    public EnvironmentMetrics environmentMetrics;
    //@ColumnInfo(name = "hopsAway", defaultValue = "0") // Not needed in pure Java
    public int hopsAway;

    // Constructors

    public NodeInfo(int num, MeshUser user) {
        this.num = num;
        this.user = user;
    }

    public NodeInfo(int num, MeshUser user, Position position) {
        this(num, user);
        this.position = position;
    }

    public NodeInfo(int num, MeshUser user, Position position, DeviceMetrics metrics) {
        this(num, user);
        this.position = position;
        this.deviceMetrics = metrics;
    }

    // Parcelable Implementation

    public NodeInfo(Parcel in) {
        num = in.readInt();
        user = in.readParcelable(MeshUser.class.getClassLoader());
        position = in.readParcelable(Position.class.getClassLoader());
        snr = in.readFloat();
        rssi = in.readInt();
        lastHeard = in.readInt();
        deviceMetrics = in.readParcelable(DeviceMetrics.class.getClassLoader());
        channel = in.readInt();
        environmentMetrics = in.readParcelable(EnvironmentMetrics.class.getClassLoader());
        hopsAway = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(num);
        dest.writeParcelable(user, flags);
        dest.writeParcelable(position, flags);
        dest.writeFloat(snr);
        dest.writeInt(rssi);
        dest.writeInt(lastHeard);
        dest.writeParcelable(deviceMetrics, flags);
        dest.writeInt(channel);
        dest.writeParcelable(environmentMetrics, flags);
        dest.writeInt(hopsAway);
    }

    public final Parcelable.Creator<NodeInfo> CREATOR = new Parcelable.Creator<NodeInfo>() {
        public NodeInfo createFromParcel(Parcel in) {
            return new NodeInfo(in);
        }

        public NodeInfo[] newArray(int size) {
            return new NodeInfo[size];
        }
    };

    // Other methods

    public Pair<Integer, Integer> getColors() {
        int r = (num & 0xFF0000) >> 16;
        int g = (num & 0x00FF00) >> 8;
        int b = num & 0x0000FF;
        double brightness = ((r * 0.299) + (g * 0.587) + (b * 0.114)) / 255;
        return new Pair<>(brightness > 0.5 ? Color.BLACK : Color.WHITE, Color.rgb(r, g, b));
    }

    public Integer getBatteryLevel() {
        return deviceMetrics != null ? deviceMetrics.batteryLevel : null;
    }

    public Float getVoltage() {
        return deviceMetrics != null ? deviceMetrics.voltage : null;
    }

    public String getBatteryStr() {
        Integer level = getBatteryLevel();
        return (level != null && level >= 1 && level <= 100) ? String.format("%d%%", level) : "";
    }

    public boolean isOnline() {
        int now = (int) (System.currentTimeMillis() / 1000);
        int timeout = 15 * 60;
        return (now - lastHeard <= timeout);
    }

    public Position getValidPosition() {
        if (position != null && position.isValid()) {
            return position;
        }
        return null;
    }

    public Integer distance(NodeInfo o) {
        Position p = getValidPosition();
        Position op = o.getValidPosition();
        if (p != null && op != null) {
            return (int) p.distance(op);
        }
        return null;
    }

    public Integer bearing(NodeInfo o) {
        Position p = getValidPosition();
        Position op = o.getValidPosition();
        if (p != null && op != null) {
            return (int) p.bearing(op);
        }
        return null;
    }

    public String distanceStr(NodeInfo o, int prefUnits) {
        Integer dist = distance(o);
        if (dist == null) {
            return null;
        }

        if (dist == 0) {
            return null; // Same point
        } else if (prefUnits == ConfigProtos.Config.DisplayConfig.DisplayUnits.METRIC_VALUE && dist < 1000) {
            return String.format("%.0f m", dist.doubleValue());
        } else if (prefUnits == ConfigProtos.Config.DisplayConfig.DisplayUnits.METRIC_VALUE && dist >= 1000) {
            return String.format("%.1f km", dist / 1000.0);
        } else if (prefUnits == ConfigProtos.Config.DisplayConfig.DisplayUnits.IMPERIAL_VALUE && dist < 1609) {
            return String.format("%.0f ft", dist.doubleValue() * 3.281);
        } else if (prefUnits == ConfigProtos.Config.DisplayConfig.DisplayUnits.IMPERIAL_VALUE && dist >= 1609) {
            return String.format("%.1f mi", dist / 1609.34);
        } else {
            return null;
        }
    }
}
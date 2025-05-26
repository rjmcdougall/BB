package com.richardmcdougall.bb.mesh;

import android.graphics.Color;
import android.util.Pair;

import com.geeksville.mesh.ConfigProtos;
import com.geeksville.mesh.MeshProtos;
import com.geeksville.mesh.TelemetryProtos;

public class Node {

    public int num; // This is immutable, and used as a key
    public MeshProtos.User user;
    public MeshProtos.Position position;
    public float snr = Float.MAX_VALUE;
    public int rssi = Integer.MAX_VALUE;
    public int lastHeard; // the last time we've seen this node in secs since 1970
    public TelemetryProtos.DeviceMetrics deviceMetrics;
    public int channel;
    public TelemetryProtos.EnvironmentMetrics environmentMetrics;
    public int hopsAway;

    // Constructors

    public Node(int num, MeshProtos.User user) {
        this.num = num;
        this.user = user;
    }

    public Node(int num, MeshProtos.User user, MeshProtos.Position position) {
        this(num, user);
        this.position = position;
    }

    public Node(int num, MeshProtos.User user, MeshProtos.Position position, TelemetryProtos.DeviceMetrics metrics) {
        this(num, user);
        this.position = position;
        this.deviceMetrics = metrics;
    }

    // Other methods

    public Pair<Integer, Integer> getColors() {
        int r = (num & 0xFF0000) >> 16;
        int g = (num & 0x00FF00) >> 8;
        int b = num & 0x0000FF;
        double brightness = ((r * 0.299) + (g * 0.587) + (b * 0.114)) / 255;
        return new Pair<>(brightness > 0.5 ? Color.BLACK : Color.WHITE, Color.rgb(r, g, b));
    }

    public Integer getBatteryLevel() {
        return deviceMetrics != null ? deviceMetrics.getBatteryLevel() : null;
    }

    public Float getVoltage() {
        return deviceMetrics != null ? deviceMetrics.getVoltage() : null;
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

    /*

    public MeshProtos.Position getValidPosition() {
        if (position != null && position.isValid()) {
            return position;
        }
        return null;
    }

    public Integer distance(Node o) {
        MeshProtos.Position p = getValidPosition();
        MeshProtos.Position op = o.getValidPosition();
        if (p != null && op != null) {
            return (int) p.distance(op);
        }
        return null;
    }

    public Integer bearing(Node o) {
        Position p = getValidPosition();
        Position op = o.getValidPosition();
        if (p != null && op != null) {
            return (int) p.bearing(op);
        }
        return null;
    }

    public String distanceStr(Node o, int prefUnits) {
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

     */
}
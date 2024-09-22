package com.richardmcdougall.bb.mesh;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Pair;

import com.geeksville.mesh.ConfigProtos;
import com.geeksville.mesh.MeshProtos;
import com.geeksville.mesh.TelemetryProtos;


public class DeviceMetrics implements Parcelable {

    public int time; // in secs (NOT MILLISECONDS!)
    public int batteryLevel = 0;
    public float voltage;
    public float channelUtilization;
    public float airUtilTx;
    public int uptimeSeconds;

    // Constructors

    public DeviceMetrics(float voltage, float channelUtilization, float airUtilTx, int uptimeSeconds) {
        this(currentTime(), 0, voltage, channelUtilization, airUtilTx, uptimeSeconds);
    }

    public DeviceMetrics(int time, int batteryLevel, float voltage,
                         float channelUtilization, float airUtilTx, int uptimeSeconds) {
        this.time = time;
        this.batteryLevel = batteryLevel;
        this.voltage = voltage;
        this.channelUtilization = channelUtilization;
        this.airUtilTx = airUtilTx;
        this.uptimeSeconds = uptimeSeconds;
    }

    // Parcelable Implementation

    public DeviceMetrics(Parcel in) {
        time = in.readInt();
        batteryLevel = in.readInt();
        voltage = in.readFloat();
        channelUtilization = in.readFloat();
        airUtilTx = in.readFloat();
        uptimeSeconds = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }


    /**
     * Converts this DeviceMetrics object to a MeshProtos.DeviceMetrics Protobuf message.
     *
     * @return The equivalent MeshProtos.DeviceMetrics object.
     */
    public TelemetryProtos.DeviceMetrics toProto() {
        return TelemetryProtos.DeviceMetrics.newBuilder()
                .setBatteryLevel(batteryLevel)
                .setVoltage(voltage)
                .setChannelUtilization(channelUtilization)
                .setAirUtilTx(airUtilTx)
                .setUptimeSeconds(uptimeSeconds)
                .build();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(time);
        dest.writeInt(batteryLevel);
        dest.writeFloat(voltage);
        dest.writeFloat(channelUtilization);
        dest.writeFloat(airUtilTx);
        dest.writeInt(uptimeSeconds);
    }

    public final Parcelable.Creator<DeviceMetrics> CREATOR = new Parcelable.Creator<DeviceMetrics>() {
        public DeviceMetrics createFromParcel(Parcel in) {
            return new DeviceMetrics(in);
        }

        public DeviceMetrics[] newArray(int size) {
            return new DeviceMetrics[size];
        }
    };

    // Helper Method

    public static int currentTime() {
        return (int) (System.currentTimeMillis() / 1000);
    }
}


package com.richardmcdougall.bb.mesh;


import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Parcelable;

public class EnvironmentMetrics implements Parcelable {

    public int time; // in secs (NOT MILLISECONDS!)
    public float temperature;
    public float relativeHumidity;
    public float barometricPressure;
    public float gasResistance;
    public float voltage;
    public float current;
    public int iaq;

    // Constructors

    public EnvironmentMetrics(float temperature, float relativeHumidity, float barometricPressure, float gasResistance, float voltage, float current, int iaq) {
        this(0, temperature, relativeHumidity, barometricPressure, gasResistance, voltage, current, iaq);
    }

    public EnvironmentMetrics(int time, float temperature, float relativeHumidity, float barometricPressure, float gasResistance, float voltage, float current, int iaq) {
        this.time = currentTime();
        this.temperature = temperature;
        this.relativeHumidity = relativeHumidity;
        this.barometricPressure = barometricPressure;
        this.gasResistance = gasResistance;
        this.voltage = voltage;
        this.current = current;
        this.iaq = iaq;
    }

    // Parcelable Implementation

    public EnvironmentMetrics(Parcel in) {
        time = in.readInt();
        temperature = in.readFloat();
        relativeHumidity = in.readFloat();
        barometricPressure = in.readFloat();
        gasResistance = in.readFloat();
        voltage = in.readFloat();
        current = in.readFloat();
        iaq = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(time);
        dest.writeFloat(temperature);
        dest.writeFloat(relativeHumidity);
        dest.writeFloat(barometricPressure);
        dest.writeFloat(gasResistance);
        dest.writeFloat(voltage);
        dest.writeFloat(current);
        dest.writeInt(iaq);
    }

    public final Parcelable.Creator<EnvironmentMetrics> CREATOR = new Parcelable.Creator<EnvironmentMetrics>() {
        public EnvironmentMetrics createFromParcel(Parcel in) {
            return new EnvironmentMetrics(in);
        }

        public EnvironmentMetrics[] newArray(int size) {
            return new EnvironmentMetrics[size];
        }
    };

    // Helper Method

    public int currentTime() {
        return (int) (System.currentTimeMillis() / 1000);
    }

    // Method to get display string (adapted from Kotlin)

    public String getDisplayString(boolean inFahrenheit) {
        @SuppressLint("DefaultLocale") String temp = temperature != 0f ? (inFahrenheit ?
                String.format("%.1f°F", temperature * 1.8f + 32) :
                String.format("%.1f°C", temperature)) : null;
        @SuppressLint("DefaultLocale") String humidity = relativeHumidity != 0f ? String.format("%.0f%%", relativeHumidity) : null;
        @SuppressLint("DefaultLocale") String pressure = barometricPressure != 0f ? String.format("%.1fhPa", barometricPressure) : null;
        @SuppressLint("DefaultLocale") String gas = gasResistance != 0f ? String.format("%.0fMΩ", gasResistance) : null;
        @SuppressLint("DefaultLocale") String voltage = this.voltage != 0f ? String.format("%.2fV", this.voltage) : null;
        @SuppressLint("DefaultLocale") String current = this.current != 0f ? String.format("%.1fmA", this.current) : null;

        String iaq = this.iaq != 0 ? "IAQ: " + this.iaq : null;

        return String.join(" ", temp, humidity, pressure, gas, voltage, current, iaq);
    }
}

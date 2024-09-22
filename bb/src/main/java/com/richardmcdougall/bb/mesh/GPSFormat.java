package com.richardmcdougall.bb.mesh;

import android.annotation.SuppressLint;

import mil.nga.grid.features.Point;
import mil.nga.mgrs.MGRS;
import mil.nga.mgrs.utm.UTM;

public class GPSFormat {

    public static String DEC(Position p) {
        return String.format("%.5f %.5f", p.latitude, p.longitude).replace(",", ".");
    }

    public static String DMS(Position p) {
        String[] lat = degreesToDMS(p.latitude, true);
        String[] lon = degreesToDMS(p.longitude, false);
        return formatDMS(lat) + " " + formatDMS(lon);
    }

    public static String UTM(Position p) {
        UTM utm = UTM.from(Point.point(p.longitude, p.latitude));
        return String.format("%s%s %.6s %.7s", utm.getZone(), utm.toMGRS().getBand(), utm.getEasting(), utm.getNorthing());
    }

    public static String MGRS(Position p) {
        MGRS mgrs = MGRS.from(Point.point(p.longitude, p.latitude));
        return String.format("%s%s %s%s %05d %05d", mgrs.getZone(), mgrs.getBand(), mgrs.getColumn(), mgrs.getRow(),
                mgrs.getEasting(), mgrs.getNorthing());
    }

    // Helper function to format DMS string
    private static String formatDMS(String[] dms) {
        return String.format("%s°%s'%.5s\"%s", dms[0], dms[1], dms[2], dms[3]);
    }

    /**
     * Format as degrees, minutes, seconds.
     *
     * @param degIn     The decimal degrees input
     * @param isLatitude True if it's latitude, false if it's longitude
     * @return An array of strings representing degrees, minutes, seconds, and direction.
     */
    @SuppressLint("DefaultLocale")
    public static String[] degreesToDMS(double degIn, boolean isLatitude) {
        boolean isPos = degIn >= 0;
        char dirLetter = isLatitude ? (isPos ? 'N' : 'S') : (isPos ? 'E' : 'W');

        degIn = Math.abs(degIn);
        int degOut = (int) degIn;
        double minutes = 60 * (degIn - degOut);
        int minWhole = (int) minutes;
        double seconds = (minutes - minWhole) * 60;

        return new String[]{String.valueOf(degOut), String.valueOf(minWhole),
                String.format("%.5f", seconds), String.valueOf(dirLetter)};
    }
}
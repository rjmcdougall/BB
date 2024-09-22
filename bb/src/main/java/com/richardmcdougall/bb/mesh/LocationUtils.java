package com.richardmcdougall.bb.mesh;

import android.annotation.SuppressLint;

import com.geeksville.mesh.MeshProtos;

import mil.nga.grid.features.Point;
import mil.nga.mgrs.MGRS;
import mil.nga.mgrs.utm.UTM;
import mil.nga.mgrs.*;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;

public class LocationUtils {

    public static double latLongToMeter(double lat_a, double lng_a, double lat_b, double lng_b) {
        double pk = (180 / 3.14169);
        double a1 = lat_a / pk;
        double a2 = lng_a / pk;
        double b1 = lat_b / pk;
        double b2 = lng_b / pk;

        double t1 = Math.cos(a1) * Math.cos(a2) * Math.cos(b1) * Math.cos(b2);
        double t2 = Math.cos(a1) * Math.sin(a2) * Math.cos(b1) * Math.sin(b2);
        double t3 = Math.sin(a1) * Math.sin(b1);

        double tt = Math.acos(t1 + t2 + t3);

        if (Double.isNaN(tt)) {
            tt = 0.0; // Must have been the same point
        }
        return 6366000 * tt;
    }

    public static double positionToMeter(MeshProtos.Position a, MeshProtos.Position b) {
        return latLongToMeter(
                a.getLatitudeI() * 1e-7,
                a.getLongitudeI() * 1e-7,
                b.getLatitudeI() * 1e-7,
                b.getLongitudeI() * 1e-7
        );
    }

    public static double bearing(double lat1, double lon1, double lat2, double lon2) {
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double deltaLonRad = Math.toRadians(lon2 - lon1);

        double y = Math.sin(deltaLonRad) * Math.cos(lat2Rad);
        double x = Math.cos(lat1Rad) * Math.sin(lat2Rad) -
                (Math.sin(lat1Rad) * Math.cos(lat2Rad) * Math.cos(deltaLonRad));

        return radToBearing(Math.atan2(y, x));
    }

    public static double radToBearing(double rad) {
        return (Math.toDegrees(rad) + 360) % 360;
    }

    public static double requiredZoomLevel(BoundingBox boundingBox) {
        GeoPoint topLeft = new GeoPoint(boundingBox.getLatNorth(), boundingBox.getLonWest());
        GeoPoint bottomRight = new GeoPoint(boundingBox.getLatSouth(), boundingBox.getLonEast());
        double latLonWidth = topLeft.distanceToAsDouble(new GeoPoint(topLeft.getLatitude(), bottomRight.getLongitude()));
        double latLonHeight = topLeft.distanceToAsDouble(new GeoPoint(bottomRight.getLatitude(), topLeft.getLongitude()));
        double requiredLatZoom = Math.log(360.0 / (latLonHeight / 111320)) / Math.log(2);
        double requiredLonZoom = Math.log(360.0 / (latLonWidth / 111320)) / Math.log(2);
        return Math.max(requiredLatZoom, requiredLonZoom);
    }

    public static BoundingBox zoomIn(BoundingBox boundingBox, double zoomFactor) {
        GeoPoint center = new GeoPoint((boundingBox.getLatNorth() + boundingBox.getLatSouth()) / 2,
                (boundingBox.getLonWest() + boundingBox.getLonEast()) / 2);
        double latDiff = boundingBox.getLatNorth() - boundingBox.getLatSouth();
        double lonDiff = boundingBox.getLonEast() - boundingBox.getLonWest();

        double newLatDiff = latDiff / Math.pow(2.0, zoomFactor);
        double newLonDiff = lonDiff / Math.pow(2.0, zoomFactor);

        return new BoundingBox(
                center.getLatitude() + newLatDiff / 2,
                center.getLongitude() + newLonDiff / 2,
                center.getLatitude() - newLatDiff / 2,
                center.getLongitude() - newLonDiff / 2
        );
    }
    // ... (GPSFormat class and other utility functions) ...
}

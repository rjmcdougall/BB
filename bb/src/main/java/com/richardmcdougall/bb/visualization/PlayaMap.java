package com.richardmcdougall.bb.visualization;

import android.os.SystemClock;

import com.richardmcdougall.bb.BBColor;
import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bb.BoardVisualization;
import com.richardmcdougall.bb.BurnerBoard;
import com.richardmcdougall.bb.BurnerBoardUtil;
import com.richardmcdougall.bb.FindMyFriends;


public class PlayaMap extends Visualization {

    FindMyFriends mFMF = null;
    BBColor bbColor = new BBColor();

    public PlayaMap(BurnerBoard bb, BoardVisualization visualization) {

        super(bb, visualization);
        BBService bbService = bb.getBBService();
        if (bbService != null) {
            mFMF = bbService.getFindMyFriends();
        }
    }

    static final double kMapSizeRatio = 2. / 3.;

    public void update(int mode) {


        if (BurnerBoardUtil.isBBAzul()) {
            final float outerRing = mBoardWidth * (float)kMapSizeRatio;
            final float innerRing = outerRing / (float)kRingRatio;
            final float theMan = 2;


            //mBurnerBoard.fillScreen(0, 30, 0);

            // Ourer ring
            mBurnerBoard.drawArc((mBoardWidth - outerRing) / 2,
                    (mBoardHeight - outerRing) / 2,
                    (mBoardWidth - outerRing) / 2 + outerRing,
                    (mBoardHeight - outerRing) / 2 + outerRing,
                    (float)(90 + kDegrees2),
                    (float)(kDegrees10 - kDegrees2),
                    true,
                    true,
                    BurnerBoard.getRGB(50, 50, 50));
            // Inner ring
            mBurnerBoard.drawArc((mBoardWidth - innerRing) / 2,
                    (mBoardHeight - innerRing) / 2,
                    (mBoardWidth - innerRing) / 2 + innerRing,
                    (mBoardHeight - innerRing) / 2 + innerRing,
                    (float)(90 + kDegrees2),
                    (float)(kDegrees10 - kDegrees2),
                    true,
                    true,
                    BurnerBoard.getRGB(0, 0, 0));

        } else if (BurnerBoardUtil.isBBPanel()) {
            mBurnerBoard.fillScreen(30, 30, 30);
            mBurnerBoard.drawArc(0, mBoardHeight, mBoardWidth,0,
                    (float)kDegrees2, (float)(kDegrees10 - kDegrees2),
            true,
                    true,
                    BurnerBoard.getRGB(50, 50, 50));

        } else if (BurnerBoardUtil.isBBClassic()) {
            mBurnerBoard.fillScreen(30, 0, 0);

        }

        if (SystemClock.elapsedRealtime() % 10 == 0) {
            FindMyFriends.boardLocation bl = mFMF.getRecentCoordinate(120);
            if (bl != null) {
                String color = mFMF.getBoardColor(bl.address);
                BBColor.ColorName c = null;
                if (color != null) {
                    c = bbColor.getColor(color);
                }
                if (c == null) {
                    c = bbColor.getColor("white");
                }
                plotBoard(bl.lat, bl.lon,
                        flashColor(0, BurnerBoard.getRGB(c.r, c.g, c.b)));
            }
        }
        mBurnerBoard.flush();

    }

    private int flashColor(int seed, int color) {
        if ((seed * 100 + SystemClock.elapsedRealtime()) % 1000 > 500) {
            return color;
        } else {
            return 0;
        }
    }

    // Plot a board on the map
    public void plotBoard(double lat, double lon, int color) {

        // Convert to angle/distance from man and then back again
        // given the rotation of map.
        double dlat = lat - kManLat;
        double dlon = lon - kManLon;

        //System.out.println("Burn radius = " + kBurnRadius);

        double m_dx = dlon * kMeterPerDegree * Math.cos(kManLat / kDegPerRAD);
        double m_dy = dlat * kMeterPerDegree;

        //System.out.println("dx/dy = " + m_dx + "," + m_dy);

        double dist = kScale * Math.sqrt(m_dx * m_dx + m_dy * m_dy);
        double bearing = kDegPerRAD * Math.atan2(m_dx, m_dy);

        double degreesMapOffset = (12. - kNorth) * 360 / 12;

        double adjustedBearing = (bearing + degreesMapOffset) % 360;

        //System.out.println("bearing = " + bearing);

        double new_dx = Math.cos(adjustedBearing / kDegPerRAD) * dist; // cos (angle ) / dist
        double new_dy = Math.sin(adjustedBearing / kDegPerRAD) * dist; // sin (andgle) / dist

        //System.out.println("new dx/dy = " + new_dx + "," + new_dy);
        //System.out.println("adjusted bearing = " + adjustedBearing);

        int x = mBoardWidth / 2 + (int) (new_dx / kBurnRadius * mBoardWidth  / 2 * kMapSizeRatio);
        int y = mBoardHeight / 2 + (int) (new_dy / kBurnRadius * mBoardWidth  / 2 * kMapSizeRatio);

        //System.out.println("x/y = " + x + "," + y);

        mBurnerBoard.drawArc(x - 1,
                y - 1,
                x + 1,
                y + 1,
                0,
                360,
                true,
                true,
                color);

        // figure out x offset
        // factor in +/- offsets
    }

    /*
    // 2016 Playa
    static final double  kManLat = 40.786400;
    static final double  kManLon = -119.206500;
    */

    /*
    // Test @ RMC Home
    static final double  kManLat = 37.476222;
    static final double  kManLon = -122.1551087;
    */

    // Test Man =  Shop
    //static final double  kManLat = 37.4829995;
    //static final double  kManLon = -122.1800015;

    static final double kCenterCampLat = 40.780822;
    static final double kCenterCampLon = -119.213934;

    static final double k6LLat = 40.775561;
    static final double k6LLon = -119.220796;

    static final double k9FLat = 40.794431;
    static final double k9FLon = -119.217205;

    static final double kDeepPlayaLat = 40.80198694462115;
    static final double kDeepPlayaLon = -119.20037881809395;

    // 2018 Playa
    static final double  kManLat =  40.786590041367525;
    static final double  kManLon = -119.20656204564455;

    static final double  kPlayaElev = 1190.;  // m
    static final double  kScale = 1.;
    static final double  kDegPerRAD = (180. / 3.1415926535);
    static final int     kClockMinutes = (12 * 60);
    static final double  kMeterPerDegree = (40030230. / 360.);
    // Direction of north in clock units
    static final double  kNorth = 10.5;  // hours
    static final int     kNumRings = 13;  // Esplanade through L
    static final double  kEsplanadeRadius = (2500 * .3048);  // m
    static final double  kFirstBlockDepth = (440 * .3048);  // m
    static final double  kBlockDepth = (240 * .3048);  // m
    // How far in from Esplanade to show distance relative to Esplanade rather than the man
    static final double  kEsplanadeInnerBuffer = (250 * .3048);  // m
    // Radial size on either side of 12 w/ no city streets
    static final double  kRadialGap = 2.;  // hours
    // How far radially from edge of city to show distance relative to city streets
    static final double  kRadialBuffer = .25;  // hours

    static final double kDegrees2 = 360 * 2 / 12;
    static final double kDegrees10 = 360 * 10 / 12;

    static final double kBurnRadius = 11 * kBlockDepth + kEsplanadeRadius + kFirstBlockDepth;

    static final double kRingRatio = kBurnRadius / kEsplanadeRadius;

    // 0=man, 1=espl, 2=A, 3=B, ...
    double ringRadius(int n) {
        if (n == 0) {
            return 0;
        } else if (n == 1) {
            return kEsplanadeRadius;
        } else if (n == 2) {
            return kEsplanadeRadius + kFirstBlockDepth;
        } else {
            return kEsplanadeRadius + kFirstBlockDepth + (n - 2) * kBlockDepth;
        }
    }

    // Distance inward from ring 'n' to show distance relative to n vs. n-1
    double ringInnerBuffer(int n) {
        if (n == 0) {
            return 0;
        } else if (n == 1) {
            return kEsplanadeInnerBuffer;
        } else if (n == 2) {
            return .5 * kFirstBlockDepth;
        } else {
            return .5 * kBlockDepth;
        }
    }

    int getReferenceRing(double dist) {
        for (int n = kNumRings; n > 0; n--) {
            //l( "getReferenceRing: " + n + ":" + ringRadius(n) + " " + ringInnerBuffer(n));
            if (ringRadius(n) - ringInnerBuffer(n) <= dist) {
                return n;
            }
        }
        return 0;
    }

    String getRefDisp(int n) {
        if (n == 0) {
            return ")(";
        } else if (n == 1) {
            return "Espl";
        } else {
            int charA = (int)'A';
            int charRef = charA + n - 2;
            return Character.toString((char)charRef);
        }
    }

    String playaStr(double lat, double lon, boolean accurate) {
        double dlat = lat - kManLat;
        double dlon = lon - kManLon;

        double m_dx = dlon * kMeterPerDegree * Math.cos(kManLat / kDegPerRAD);
        double m_dy = dlat * kMeterPerDegree;

        double dist = kScale * Math.sqrt(m_dx * m_dx + m_dy * m_dy);
        double bearing = kDegPerRAD * Math.atan2(m_dx, m_dy);

        double clock_hours = (bearing / 360. * 12. + kNorth);
        int clock_minutes = (int)(clock_hours * 60 + .5);
        // Force into the range [0, CLOCK_MINUTES)
        clock_minutes = ((clock_minutes % kClockMinutes) + kClockMinutes) % kClockMinutes;

        int hour = clock_minutes / 60;
        int minute = clock_minutes % 60;
        String clock_disp = String.valueOf(hour) + ":" + (minute < 10 ? "0" : "") +
                String.valueOf(minute);

        int refRing;
        if (6 - Math.abs(clock_minutes/60. - 6) < kRadialGap - kRadialBuffer) {
            refRing = 0;
        } else {
            refRing = getReferenceRing(dist);
        }
        double refDelta = dist - ringRadius(refRing);
        long refDeltaRounded = (long)(refDelta + .5);

        return clock_disp + " & " + getRefDisp(refRing) + (refDeltaRounded >= 0 ? "+" : "-") +
                String.valueOf(refDeltaRounded < 0 ? -refDeltaRounded : refDeltaRounded) + "m" +
                (accurate ? "" : "-ish");
    }


}

package com.richardmcdougall.bbmonitor;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Layout;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.GridLayout;
import android.widget.Toast;

import java.util.HashMap;
import android.app.admin.DeviceAdminReceiver;

/**
 * Skeleton of an Android Things activity.
 * <p>
 * Android Things peripheral APIs are accessible through the class
 * PeripheralManagerService. For example, the snippet below will open a GPIO pin and
 * set it to HIGH:
 * <p>
 * <pre>{@code
 * PeripheralManagerService service = new PeripheralManagerService();
 * mLedGpio = service.openGpio("BCM6");
 * mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
 * mLedGpio.setValue(true);
 * }</pre>
 * <p>
 * For more complex peripherals, look for an existing user-space driver, or implement one if none
 * is available.
 *
 * @see <a href="https://github.com/androidthings/contrib-drivers#readme">https://github.com/androidthings/contrib-drivers#readme</a>
 */
public class MainActivity extends Activity {

    Context mContext = null;
    private static final String TAG = "BB.Main";
    EditText log;

    // function to append a string to a TextView as a new line
    // and scroll to the bottom if needed
    private void l(String msg) {
        if (log == null)
            return;

        Log.v(TAG, msg);
    }

    // function to append a string to a TextView as a new line
    // and scroll to the bottom if needed
    private void logToScreen(String msg) {
        if (log == null)
            return;

        // append the new string
        log.append(msg + "\n");

        String tMsg = log.getText().toString();
        int msgLen = tMsg.length();
        if (msgLen > 1000) {
            tMsg = tMsg.substring(msgLen - 1000, msgLen);
        }
        log.setText(tMsg);

        // find the amount we need to scroll.  This works by
        // asking the TextView's internal layout for the position
        // of the final line and then subtracting the TextView's height
        final android.text.Layout layout = log.getLayout();

        if (layout != null) {
            final int scrollAmount = layout.getLineTop(log.getLineCount()) - log.getHeight();
            // if there is no need to scroll, scrollAmount will be <=0
            if (scrollAmount > 0)
                log.scrollTo(0, scrollAmount);
            else
                log.scrollTo(0, 0);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getApplicationContext();

        l("Starting");


        int screenSize = getResources().getConfiguration().screenLayout &
                Configuration.SCREENLAYOUT_SIZE_MASK;



        startService(new Intent(getBaseContext(), BBService.class));


        // Register for the particular broadcast based on Stats Action
        IntentFilter statFilter = new IntentFilter(BBService.ACTION_BB_LOCATION);
        LocalBroadcastManager.getInstance(this).registerReceiver(BBRadioReciever, statFilter);

        setContentView(R.layout.activity_main);

        // Create the logging window
        log = (EditText) findViewById(R.id.editTextLog);
        log.setMovementMethod(new android.text.method.ScrollingMovementMethod());
        log.setMaxLines(10);


        String toastMsg;
        switch(screenSize) {
            case Configuration.SCREENLAYOUT_SIZE_XLARGE:
                toastMsg = "XL screen";
                break;
            case Configuration.SCREENLAYOUT_SIZE_LARGE:
                toastMsg = "Large screen";
                break;
            case Configuration.SCREENLAYOUT_SIZE_NORMAL:
                toastMsg = "Normal screen";
                break;
            case Configuration.SCREENLAYOUT_SIZE_SMALL:
                toastMsg = "Small screen";
                break;
            default:
                toastMsg = "Screen size is neither xl, large, normal or small";
        }
        l(toastMsg);

        l("Services Started");


    }

    // Define the callback for what to do when stats are received
    private BroadcastReceiver BBRadioReciever = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String name = intent.getStringExtra("name");
            double lat = intent.getDoubleExtra("lat", 0);
            double lon = intent.getDoubleExtra("lon", 0);
            int sigStrength = intent.getIntExtra("sig", 0);
            int batteryLevel = intent.getIntExtra("batt", 0);

            updateBoardLocations(name, sigStrength, lat,lon, batteryLevel);

            if (name.contains("repeater")) {
                logToScreen(name + " " +
                        sigStrength + "/" + batteryLevel + "% ");
            } else {
                logToScreen(name + " " +
                        sigStrength + "/" + batteryLevel + "% " +
                        playaStr(lat, lon, true));
            }
        }

    };

/*
    GridLayout gridLayout = new GridLayout(mContext);
    int total = facilities.size();
    int column =  2;
    int row = total / column;
    gridLayout.setAlignmentMode(GridLayout.ALIGN_BOUNDS);
    gridLayout.setColumnCount(column);
    gridLayout.setRowCount(row + 1);
    TextView titleText;
    for(int i =0, c = 0, r = 0; i < total; i++, c++)
    {
        if(c == column)
        {
            c = 0;
            r++;
        }
        titleText = new TextView(getContext());
        titleText.setText(facilities.get(i));
        gridLayout.addView(titleText, i);
        titleText.setCompoundDrawablesWithIntrinsicBounds(rightIc, 0, 0, 0);
        GridLayout.LayoutParams param =new GridLayout.LayoutParams();
        param.height = LayoutParams.WRAP_CONTENT;
        param.width = LayoutParams.WRAP_CONTENT;
        param.rightMargin = 5;
        param.topMargin = 5;
        param.setGravity(Gravity.CENTER);
        param.columnSpec = GridLayout.spec(c);
        param.rowSpec = GridLayout.spec(r);
        titleText.setLayoutParams (param);


    }

    */

    private static final int kOldEntry = 300000;

    // Keep a list of board GPS locations
    class boardLocation {
        int sigStrength;
        long lastHeard;
        double lat;
        double lon;
        int batteryLevel;
        long lastHeardDate;
    }
    private HashMap<String, boardLocation> mBoardLocations = new HashMap<>();

    private void updateBoardLocations(String name, int sigstrength, double lat, double lon,
                                      int batteryLevel) {

        boardLocation loc = new boardLocation();
        loc.lastHeard = SystemClock.elapsedRealtime();
        loc.sigStrength = sigstrength;
        loc.lat = lat;
        loc.lon = lon;
        loc.batteryLevel = batteryLevel;
        loc.lastHeardDate = System.currentTimeMillis();
        mBoardLocations.put(name, loc);
        l("Location entry list size: " + mBoardLocations.size());
        for (String n: mBoardLocations.keySet()) {
            boardLocation l = mBoardLocations.get(n);

            long age = (SystemClock.elapsedRealtime() - l.lastHeard);

            l("Location Entry:" + n + ", age:" + age +
                    ", lat: " + l.lat + ", lon: " + l.lon + ", batt: " + l.batteryLevel);

            // Draw battery level for this board
            int viewId = this.getResources().getIdentifier(
                    getViewforBoard("battery", n),
                    "id", this.getPackageName());;
            if (viewId > 0) {
                if (age < kOldEntry) {
                    drawBattery((LinearLayout) findViewById(viewId), l.batteryLevel, false);
                } else {
                    clearImage((LinearLayout) findViewById(viewId));
                }
            } else {
                l("Can't find view id for board");
            }
            // Playa location for this board
            viewId = this.getResources().getIdentifier(
                    getViewforBoard("location", n),
                    "id", this.getPackageName());;
            if (viewId > 0) {
                TextView locView = (TextView) findViewById(viewId);
                if (age < kOldEntry) {
                    locView.setText(playaStr(l.lat, l.lon, true));
                } else {
                    locView.setText("no signal");
                }
            } else {
                l("Can't find view id for board");
            }
        }
    }

    private void clearImage(View v) {
        ImageView image = new ImageView(getApplicationContext());
        ((ViewGroup) v).removeAllViews();
        image.setImageDrawable(null);
        ((ViewGroup) v).addView(image);
    }

    private String getViewforBoard(String base, String boardname) {
        if (boardname.contains("akula")) {
            return base + 1;
        } else if (boardname.contains("artemis")) {
            return base + 2;
        } else if (boardname.contains("biscuit")) {
            return base + 3;
        } else if (boardname.contains("boadie")) {
            return base + 4;
        } else if (boardname.contains("candy")) {
            return base + 5;
        } else if (boardname.contains("goofy")) {
            return base + 6;
        } else if (boardname.contains("joon")) {
            return base + 7;
        } else if (boardname.contains("monaco")) {
            return base + 8;
        } else if (boardname.contains("pegasus")) {
            return base + 9;
        } else if (boardname.contains("ratchet")) {
            return base + 10;
        } else if (boardname.contains("squeeze")) {
            return base + 11;
        } else if (boardname.contains("vega")) {
            return base + 12;
        } else {
            return "";
        }
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
    static final double  kManLat = 37.4829995;
    static final double  kManLon = -122.1800015;

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

    private Bitmap rotateDrawable(int resId) {
        Bitmap bmpOriginal = BitmapFactory.decodeResource(getResources(), resId);
        Bitmap bmpResult = Bitmap.createBitmap(bmpOriginal.getHeight(),
                bmpOriginal.getWidth(), Bitmap.Config.ARGB_8888);
        Canvas tempCanvas = new Canvas(bmpResult);
        int pivot = bmpOriginal.getHeight() / 2;
        tempCanvas.rotate(90, pivot, pivot);
        tempCanvas.drawBitmap(bmpOriginal, 0, 0, null);
        return bmpResult;
    }

    Bitmap resizeBattery(Bitmap batt) {
        int newContainerHeight = 120;
        int newContainerWidth = 300;
        Bitmap copy = Bitmap.createScaledBitmap(batt,
                (int) newContainerWidth, (int) newContainerHeight, false);
        return copy;
    }

    private Bitmap createSingleImageFromMultipleImages(Bitmap firstImage, Bitmap secondImage){

        Bitmap result = Bitmap.createBitmap(firstImage.getWidth(),
                firstImage.getHeight(), firstImage.getConfig());
        Canvas canvas = new Canvas(result);
        canvas.drawBitmap(firstImage, 0f, 0f, null);
        canvas.drawBitmap(secondImage, 0f, 0f, null);
        return result;
    }


    private void drawBattery(View v, final int level, final boolean isCharging) {
        ImageView image = new ImageView(getApplicationContext());
        Bitmap pctBattery;
        Bitmap baseBattery = resizeBattery(rotateDrawable(R.drawable.battery));
        if (level <= 100 && level > 90) {
            pctBattery = resizeBattery(rotateDrawable(R.drawable.lic_100));
        } else if (level <= 90 && level > 80) {
            pctBattery = resizeBattery(rotateDrawable(R.drawable.lic_90));
        } else if (level <= 80 && level > 70) {
            pctBattery = resizeBattery(rotateDrawable(R.drawable.lic_80));
        } else if (level <= 70 && level > 60) {
            pctBattery = resizeBattery(rotateDrawable(R.drawable.lic_70));
        } else if (level <= 60 && level > 50) {
            pctBattery = resizeBattery(rotateDrawable(R.drawable.lic_60));
        } else if (level <= 50 && level > 40) {
            pctBattery = resizeBattery(rotateDrawable(R.drawable.lic_50));
        } else if (level <= 40 && level > 30) {
            pctBattery = resizeBattery(rotateDrawable(R.drawable.lic_40));
        } else if (level <= 30 && level > 20) {
            pctBattery = resizeBattery(rotateDrawable(R.drawable.lic_30));
        } else if (level <= 20 && level > 10) {
            pctBattery = resizeBattery(rotateDrawable(R.drawable.lic_20));
        } else if (level <= 10 && level > 0) {
            pctBattery = resizeBattery(rotateDrawable(R.drawable.lic_10));
        } else {
            pctBattery = baseBattery;
        }
        Bitmap mergedImages = createSingleImageFromMultipleImages(baseBattery, pctBattery);
        image.setImageBitmap(mergedImages);
        ((ViewGroup) v).removeAllViews();
        ((ViewGroup) v).addView(image);
    }


}

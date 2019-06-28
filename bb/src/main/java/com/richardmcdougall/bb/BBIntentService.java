package com.richardmcdougall.bb;


import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.util.Log;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class BBIntentService extends IntentService {
    private static final String TAG = "BB.BBINTENTService";

    public void l(String s) {
        Log.v(TAG, s);
    }

    // TODO: Rename actions, choose action names that describe tasks that this
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    private static final String ACTION_LIGHTS = "com.richardmcdougall.bb.action.LIGHTS";
    private static final String ACTION_MUSIC = "com.richardmcdougall.bb.action.MUSIC";

    // TODO: Rename parameters
    private static final String EXTRA_PARAM1 = "com.richardmcdougall.bb.extra.PARAM1";
    private static final String EXTRA_PARAM2 = "com.richardmcdougall.bb.extra.PARAM2";

    public BBIntentService() {
        super("bb-service");
    }

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void startActionLights(Context context, String param1, String param2) {
        Intent intent = new Intent(context, BBIntentService.class);
        intent.setAction(ACTION_LIGHTS);
        intent.putExtra(EXTRA_PARAM1, param1);
        intent.putExtra(EXTRA_PARAM2, param2);
        context.startService(intent);
    }

    /**
     * Starts this service to perform action Baz with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void startActionMusic(Context context, String param1, String param2) {
        Intent intent = new Intent(context, BBIntentService.class);
        intent.setAction(ACTION_MUSIC);
        intent.putExtra(EXTRA_PARAM1, param1);
        intent.putExtra(EXTRA_PARAM2, param2);
        context.startService(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        l("onCreate");


    }

    @Override
    public void onDestroy() {
        //super.onDestroy();
        l("onDestroy");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        l("onHandleIntent");
        //mContext = getApplicationContext();
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_LIGHTS.equals(action)) {
                final String param1 = intent.getStringExtra(EXTRA_PARAM1);
                final String param2 = intent.getStringExtra(EXTRA_PARAM2);
                handleActionLights(param1, param2);
            } else if (ACTION_MUSIC.equals(action)) {
                final String param1 = intent.getStringExtra(EXTRA_PARAM1);
                final String param2 = intent.getStringExtra(EXTRA_PARAM2);
                handleActionMusic(param1, param2);
            }
        }
    }



    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    Thread musicPlayer = null;
    private void handleActionMusic(String param1, String param2) {
        l("handleActionMusic");
    }

    /**
     * Handle action Baz in the provided background thread with the provided
     * parameters.
     */
    private void handleActionLights(String param1, String param2) {


    }





}

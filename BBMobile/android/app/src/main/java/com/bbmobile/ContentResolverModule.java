package com.bbmobile;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;

import java.util.Map;
import java.util.HashMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableNativeArray;

public class ContentResolverModule extends ReactContextBaseJavaModule {

    public ContentResolverModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return "ContentResolver";
    }

    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> constants = new HashMap<>();
        return constants;
    }

    @ReactMethod
    public void getLocationJSON(Promise promise ) {

        String JSON = "";
        WritableArray locationJSON = new WritableNativeArray();

        try{
            Context mContext = getReactApplicationContext().getApplicationContext();
            Cursor cursor = mContext.getContentResolver().query(Uri.parse("content://com.richardmcdougall.bb.provider/locationsJSON"),null,null,null,null);

            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    do {
                        JSON = cursor.getString(0);
                        locationJSON.pushString(JSON);
                   } while (cursor.moveToNext());
                } else {
                }
                cursor.close();
            } else {
                throw new Exception("Cursor is null");
            }
            Log.i("JSON", "JSON array returned from ContentProvider: " + locationJSON.toString());

            promise.resolve(locationJSON);

        } catch (Exception e) {
            Log.i("JSON", "Failed to get JSON via react native bridge." + e.getMessage());
            promise.reject("Failed to get JSON via react native bridge.", e);
        }
    }
}
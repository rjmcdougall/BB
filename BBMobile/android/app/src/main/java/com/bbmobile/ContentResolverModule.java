package com.bbmobile;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;

import java.util.Map;
import java.util.HashMap;

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
    public void getLocationJSON(Callback successCallback, Callback errorCallback
                              ) {

        String JSON = "";

        try{
            Context mContext = getReactApplicationContext().getApplicationContext();
            Cursor cursor = mContext.getContentResolver().query(Uri.parse("content://com.richardmcdougall.bb.provider/locationsJSON"),null,null,null,null);

            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    do {
                        JSON = cursor.getString(0);
                        Log.i("JSON", "JSON returned from ContentProvider: " + JSON);
                    } while (cursor.moveToNext());
                } else {
                }
                cursor.close();
            } else {
                throw new Exception("Cursor is null");
            }
            successCallback.invoke(JSON);
        }
        catch (Exception e){
            errorCallback.invoke(e.getMessage());
        }
    }
}
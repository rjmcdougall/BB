package com.richardmcdougall.bbcommon;

import android.content.Context;
import android.content.SharedPreferences;

public class PersistentCache {

    private static final String PREFS_NAME = "persistent_cache";
    private SharedPreferences sharedPreferences;

    public PersistentCache(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveData(String key, String value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public String getData(String key) {
        return sharedPreferences.getString(key, null);
    }

    public void removeData(String key) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(key);
        editor.apply();
    }

    public void clearCache() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }

    public boolean exists(String key) {
        return sharedPreferences.contains(key);
    }
}

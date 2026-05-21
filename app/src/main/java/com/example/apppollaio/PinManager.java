package com.example.apppollaio;

import android.content.Context;
import android.content.SharedPreferences;

public class PinManager {

    private static final String PREFS   = "pollaio_prefs";
    private static final String KEY_PIN = "user_pin";

    private final SharedPreferences prefs;

    public PinManager(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public boolean isPinSet() {
        return prefs.contains(KEY_PIN);
    }

    public void savePin(String pin) {
        prefs.edit().putString(KEY_PIN, pin).apply();
    }

    public boolean verifyPin(String pin) {
        return pin.equals(prefs.getString(KEY_PIN, ""));
    }

    public void clearPin() {
        prefs.edit().remove(KEY_PIN).apply();
    }
}

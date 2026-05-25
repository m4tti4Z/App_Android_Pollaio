package com.example.apppollaio;

import android.content.Context;
import android.content.SharedPreferences;

public class PinManager {

    private static final String PREFS  = "pollaio_prefs";
    private static final String KEY_EMAIL = "user_email";
    private static final String KEY_PIN   = "user_pin";

    private final SharedPreferences prefs;

    public PinManager(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public boolean isRegistered() {
        return prefs.contains(KEY_PIN) && prefs.contains(KEY_EMAIL);
    }

    public void save(String email, String pin) {
        prefs.edit()
            .putString(KEY_EMAIL, email.trim().toLowerCase())
            .putString(KEY_PIN, pin)
            .apply();
    }

    public boolean verify(String email, String pin) {
        String savedEmail = prefs.getString(KEY_EMAIL, "");
        String savedPin   = prefs.getString(KEY_PIN, "");
        return email.trim().toLowerCase().equals(savedEmail) && pin.equals(savedPin);
    }

    public String getSavedEmail() {
        return prefs.getString(KEY_EMAIL, "");
    }

    public void clear() {
        prefs.edit().remove(KEY_EMAIL).remove(KEY_PIN).apply();
    }
}

package com.example.apppollaio;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

public class ShakeDetector implements SensorEventListener {

    private static final float SOGLIA_G    = 2.7f;  // G-force minima per rilevare shake
    private static final long  COOLDOWN_MS = 2000;  // ms tra uno shake e l'altro

    public interface OnShakeListener {
        void onShake();
    }

    private final OnShakeListener listener;
    private long lastShakeTime = 0;

    public ShakeDetector(OnShakeListener listener) {
        this.listener = listener;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        // Calcola accelerazione totale togliendo la gravità approssimativamente
        double gForce = Math.sqrt(x * x + y * y + z * z) / 9.81;

        if (gForce > SOGLIA_G) {
            long now = System.currentTimeMillis();
            if (now - lastShakeTime > COOLDOWN_MS) {
                lastShakeTime = now;
                if (listener != null) listener.onShake();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}

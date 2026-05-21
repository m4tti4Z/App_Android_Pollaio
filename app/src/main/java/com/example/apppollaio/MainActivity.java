package com.example.apppollaio;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity {

    // MQTT
    private MqttManager mqtt;
    private NotificationHelper notifHelper;
    private SensorData lastData = new SensorData();

    // Shake
    private SensorManager sensorManager;
    private ShakeDetector shakeDetector;

    // UI — Status
    private View      statusDot;
    private TextView  statusTxt;

    // UI — Cards sensori
    private TextView tvTemp, tvHum, tvAcqua, tvLuce;
    private TextView badgeTemp, badgeHum, badgeAcqua, badgeLuce;
    private ProgressBar progressAcqua;

    // UI — Bottoni controllo
    private MaterialButton btnLuce, btnMangime;
    private boolean luceOn = false;
    private boolean mangimeInCorso = false;

    // UI — Shake feedback
    private CardView shakeCard;
    private TextView shakeStatus;

    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initMqtt();
        initShake();
        setupButtons();
    }

    private void initViews() {
        statusDot    = findViewById(R.id.status_dot);
        statusTxt    = findViewById(R.id.status_txt);

        tvTemp       = findViewById(R.id.tv_temp);
        tvHum        = findViewById(R.id.tv_hum);
        tvAcqua      = findViewById(R.id.tv_acqua);
        tvLuce       = findViewById(R.id.tv_luce);

        badgeTemp    = findViewById(R.id.badge_temp);
        badgeHum     = findViewById(R.id.badge_hum);
        badgeAcqua   = findViewById(R.id.badge_acqua);
        badgeLuce    = findViewById(R.id.badge_luce);

        progressAcqua = findViewById(R.id.progress_acqua);

        btnLuce      = findViewById(R.id.btn_luce);
        btnMangime   = findViewById(R.id.btn_mangime);

        shakeCard    = findViewById(R.id.shake_card);
        shakeStatus  = findViewById(R.id.shake_status);

        // Nav
        findViewById(R.id.nav_orari).setOnClickListener(v ->
                startActivity(new Intent(this, OrariActivity.class)));
    }

    private void initMqtt() {
        notifHelper = new NotificationHelper(this);
        mqtt = new MqttManager();
        setStatus(false);

        mqtt.connect(new MqttManager.MessageListener() {
            @Override
            public void onMessage(String topic, String payload) {
                if (MqttManager.TOPIC_TELEMETRIA.equals(topic)) {
                    SensorData data = SensorData.fromJson(payload);
                    lastData = data;
                    runOnUiThread(() -> updateSensors(data));
                    notifHelper.checkAndNotify(data);
                }
            }
            @Override
            public void onConnected() {
                runOnUiThread(() -> setStatus(true));
            }
            @Override
            public void onConnectionLost() {
                runOnUiThread(() -> setStatus(false));
            }
        });
    }

    private void initShake() {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        shakeDetector = new ShakeDetector(() -> runOnUiThread(this::erogaMangimeShake));
        Sensor acc = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (acc != null)
            sensorManager.registerListener(shakeDetector, acc, SensorManager.SENSOR_DELAY_UI);
    }

    private void setupButtons() {
        btnLuce.setOnClickListener(v -> toggleLuce());
        btnMangime.setOnClickListener(v -> erogaMangime());
    }

    // ── CONTROLLO LUCE ──
    private void toggleLuce() {
        luceOn = !luceOn;
        mqtt.publish(MqttManager.TOPIC_CMD_LUCE, luceOn ? "ON" : "OFF");
        if (luceOn) {
            btnLuce.setText("Spegni Luce");
            btnLuce.setBackgroundTintList(
                    ContextCompat.getColorStateList(this, R.color.yellow_luce));
            btnLuce.setIconResource(R.drawable.ic_light_off);
        } else {
            btnLuce.setText("Accendi Luce");
            btnLuce.setBackgroundTintList(
                    ContextCompat.getColorStateList(this, R.color.green_primary));
            btnLuce.setIconResource(R.drawable.ic_light_on);
        }
    }

    // ── EROGA MANGIME (bottone) ──
    private void erogaMangime() {
        if (mangimeInCorso) return;
        inviaComandoMangime();
    }

    // ── EROGA MANGIME (shake) ──
    private void erogaMangimeShake() {
        if (mangimeInCorso) return;
        shakeCard.setVisibility(View.VISIBLE);
        shakeStatus.setText("📳 Shake rilevato! Erogo mangime...");
        ObjectAnimator.ofFloat(shakeCard, "alpha", 0f, 1f).setDuration(300).start();
        inviaComandoMangime();
        handler.postDelayed(() -> {
            ObjectAnimator anim = ObjectAnimator.ofFloat(shakeCard, "alpha", 1f, 0f);
            anim.setDuration(500);
            anim.start();
            handler.postDelayed(() -> shakeCard.setVisibility(View.GONE), 500);
        }, 2500);
    }

    // ── LOGICA COMUNE EROGAZIONE ──
    private void inviaComandoMangime() {
        mangimeInCorso = true;
        mqtt.publish(MqttManager.TOPIC_CMD_MANGIME, "APRI");

        btnMangime.setText("Erogazione...");
        btnMangime.setEnabled(false);
        btnMangime.setBackgroundTintList(
                ContextCompat.getColorStateList(this, R.color.teal_mangime));

        // Vibrazione breve di conferma
        new NotificationHelper(this); // già crea il canale
        android.os.Vibrator vib = (android.os.Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vib != null)
            vib.vibrate(android.os.VibrationEffect.createWaveform(
                    new long[]{0, 100, 80, 100}, -1));

        handler.postDelayed(() -> {
            btnMangime.setText("Eroga Mangime");
            btnMangime.setEnabled(true);
            btnMangime.setBackgroundTintList(
                    ContextCompat.getColorStateList(this, R.color.green_primary));
            mangimeInCorso = false;
        }, 3000);
    }

    // ── AGGIORNA UI SENSORI ──
    private void updateSensors(SensorData d) {
        // Temperatura
        tvTemp.setText(d.temperatura < 0 ? "--" :
                String.format("%.1f°C", d.temperatura));
        if (d.temperatura < 0) {
            badgeTemp.setText("N/D");
        } else if (d.temperatura < 5) {
            badgeTemp.setText("❄ Freddo"); badgeTemp.setBackgroundTintList(
                    ContextCompat.getColorStateList(this, R.color.badge_cold));
        } else if (d.temperatura > 35) {
            badgeTemp.setText("🔥 Caldo"); badgeTemp.setBackgroundTintList(
                    ContextCompat.getColorStateList(this, R.color.badge_hot));
        } else {
            badgeTemp.setText("✓ Ottimale"); badgeTemp.setBackgroundTintList(
                    ContextCompat.getColorStateList(this, R.color.badge_ok));
        }

        // Umidità
        tvHum.setText(d.umidita < 0 ? "--" :
                String.format("%.1f%%", d.umidita));
        if (d.umidita >= 40 && d.umidita <= 80) {
            badgeHum.setText("✓ Ottimale"); badgeHum.setBackgroundTintList(
                    ContextCompat.getColorStateList(this, R.color.badge_ok));
        } else {
            badgeHum.setText("⚠ Fuori range"); badgeHum.setBackgroundTintList(
                    ContextCompat.getColorStateList(this, R.color.badge_warn));
        }

        // Acqua
        tvAcqua.setText(d.acqua < 0 ? "--" :
                String.format("%.0f%%", d.acqua));
        int progVal = d.acqua < 0 ? 0 : (int) d.acqua;
        progressAcqua.setProgress(progVal);
        if (d.acqua < 20) {
            badgeAcqua.setText("⚠ Basso"); badgeAcqua.setBackgroundTintList(
                    ContextCompat.getColorStateList(this, R.color.badge_hot));
        } else if (d.acqua < 60) {
            badgeAcqua.setText("~ Medio"); badgeAcqua.setBackgroundTintList(
                    ContextCompat.getColorStateList(this, R.color.badge_warn));
        } else {
            badgeAcqua.setText("✓ Alto"); badgeAcqua.setBackgroundTintList(
                    ContextCompat.getColorStateList(this, R.color.badge_ok));
        }

        // Luce
        tvLuce.setText(d.luce < 0 ? "--" :
                String.format("%.0f lx", d.luce));
        if (d.luce < 50) {
            badgeLuce.setText("🌙 Buio"); badgeLuce.setBackgroundTintList(
                    ContextCompat.getColorStateList(this, R.color.badge_warn));
        } else {
            badgeLuce.setText("☀ Attivo"); badgeLuce.setBackgroundTintList(
                    ContextCompat.getColorStateList(this, R.color.badge_ok));
        }
    }

    private void setStatus(boolean connected) {
        statusDot.setBackgroundTintList(ContextCompat.getColorStateList(this,
                connected ? R.color.green_primary : R.color.badge_hot));
        statusTxt.setText(connected ? "Online · HiveMQ" : "Offline");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Sensor acc = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (acc != null)
            sensorManager.registerListener(shakeDetector, acc, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(shakeDetector);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mqtt != null) mqtt.disconnect();
    }
}

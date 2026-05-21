package com.example.apppollaio;

import android.os.Bundle;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class OrariActivity extends AppCompatActivity {

    private TimePicker picker1, picker2;
    private MqttManager mqtt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_orari);

        picker1 = findViewById(R.id.time_picker_1);
        picker2 = findViewById(R.id.time_picker_2);
        picker1.setIs24HourView(true);
        picker2.setIs24HourView(true);

        // Valori di default (come da firmware)
        picker1.setHour(8);  picker1.setMinute(0);
        picker2.setHour(17); picker2.setMinute(0);

        mqtt = new MqttManager();
        mqtt.connect(new MqttManager.MessageListener() {
            @Override public void onMessage(String t, String p) {}
            @Override public void onConnected() {}
            @Override public void onConnectionLost() {}
        });

        MaterialButton btnSalva = findViewById(R.id.btn_salva_orari);
        btnSalva.setOnClickListener(v -> salvaOrari());

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    private void salvaOrari() {
        int h1 = picker1.getHour(), m1 = picker1.getMinute();
        int h2 = picker2.getHour(), m2 = picker2.getMinute();

        String json = String.format(
                "{\"h1\":%d,\"m1\":%d,\"h2\":%d,\"m2\":%d}", h1, m1, h2, m2);

        mqtt.publish(MqttManager.TOPIC_ORARI, json);

        Toast.makeText(this,
                String.format("Orari salvati: %02d:%02d e %02d:%02d", h1, m1, h2, m2),
                Toast.LENGTH_SHORT).show();

        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mqtt != null) mqtt.disconnect();
    }
}

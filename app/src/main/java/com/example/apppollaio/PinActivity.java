package com.example.apppollaio;

import android.content.Intent;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import android.widget.EditText;

import org.json.JSONObject;

public class PinActivity extends AppCompatActivity {

    private static final int PIN_LENGTH = 4;

    private StringBuilder   pinInput = new StringBuilder();

    private TextView          tvTitle, tvSubtitle, tvError;
    private EditText           etEmail;
    private ImageView[]       dots = new ImageView[PIN_LENGTH];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin);

        initViews();
        updateTitle();
    }

    private void initViews() {
        tvTitle    = findViewById(R.id.tv_pin_title);
        tvSubtitle = findViewById(R.id.tv_pin_subtitle);
        tvError    = findViewById(R.id.tv_pin_error);
        etEmail    = findViewById(R.id.et_username); // Questo è il tuo campo di testo per l'email

        dots[0] = findViewById(R.id.dot1);
        dots[1] = findViewById(R.id.dot2);
        dots[2] = findViewById(R.id.dot3);
        dots[3] = findViewById(R.id.dot4);

        int[] btnIds = { R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
                R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9 };
        for (int i = 0; i < btnIds.length; i++) {
            final String digit = String.valueOf(i);
            findViewById(btnIds[i]).setOnClickListener(v -> addDigit(digit));
        }
        findViewById(R.id.btn_del).setOnClickListener(v -> deleteDigit());
    }

    private void updateTitle() {
        tvTitle.setText("Pollaio IoT");
        tvSubtitle.setText("Inserisci email e PIN");
    }

    private void addDigit(String digit) {
        if (pinInput.length() >= PIN_LENGTH) return;
        pinInput.append(digit);
        updateDots();
        if (pinInput.length() == PIN_LENGTH) processPin();
    }

    private void deleteDigit() {
        if (pinInput.length() == 0) return;
        pinInput.deleteCharAt(pinInput.length() - 1);
        updateDots();
        tvError.setVisibility(View.GONE);
    }

    private void updateDots() {
        for (int i = 0; i < PIN_LENGTH; i++) {
            dots[i].setImageResource(
                    i < pinInput.length() ? R.drawable.dot_filled : R.drawable.dot_empty);
        }
    }

    private void processPin() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String pin = pinInput.toString();

        Log.d("POLLAIO_DEBUG", "Inizio Login -> Email: " + email + " | PIN: " + pin);

        if (email.isEmpty()) {
            shakeError("Inserisci la tua email.");
            pinInput.setLength(0);
            updateDots();
            return;
        }

        View loadingOverlay = findViewById(R.id.loading_overlay);
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(View.VISIBLE);
        }

        // Prepara il JSON inserendo SIA l'email SIA il pin
        JSONObject body = new JSONObject();
        try {
            body.put("email", email); // MANDIAMO L'EMAIL AL PHP!
            body.put("pin", pin);     // MANDIAMO IL PIN IN CHIARO (ci pensa il PHP ad hasharlo)
        } catch (Exception e) {
            Log.e("POLLAIO_DEBUG", "Errore creazione JSON: " + e.getMessage());
        }

        // Chiamata all'API VIP
        ApiClient.post("/API_login.php", body, new ApiClient.Callback() {
            @Override
            public void onSuccess(JSONObject response) {
                Log.i("POLLAIO_DEBUG", "Login effettuato! Risposta: " + response.toString());

                runOnUiThread(() -> {
                    if (loadingOverlay != null) loadingOverlay.setVisibility(View.GONE);
                    Toast.makeText(PinActivity.this, "Accesso eseguito con successo!", Toast.LENGTH_SHORT).show();
                    goToMain();
                });
            }

            @Override
            public void onError(String errorMsg) {
                Log.e("POLLAIO_DEBUG", "Errore Login: " + errorMsg);

                runOnUiThread(() -> {
                    if (loadingOverlay != null) loadingOverlay.setVisibility(View.GONE);
                    shakeError(errorMsg);
                    pinInput.setLength(0);
                    updateDots();
                });
            }
        });
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void shakeError(String msg) {
        tvError.setText(msg);
        tvError.setVisibility(View.VISIBLE);
        findViewById(R.id.dots_row).startAnimation(
                AnimationUtils.loadAnimation(this, R.anim.shake));
        Vibrator vib = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vib != null)
            vib.vibrate(VibrationEffect.createWaveform(new long[]{0, 80, 60, 80}, -1));
    }
}
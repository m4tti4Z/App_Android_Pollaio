package com.example.apppollaio;

import android.content.Intent;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.button.MaterialButton;

public class PinActivity extends AppCompatActivity {

    private static final int PIN_LENGTH = 4;

    private PinManager pinManager;
    private StringBuilder pinInput = new StringBuilder();
    private String firstPin = null; // usato solo in fase di impostazione
    private boolean isSettingUp = false;

    // UI
    private TextView tvTitle, tvSubtitle;
    private ImageView[] dots = new ImageView[PIN_LENGTH];
    private TextView tvError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin);

        pinManager = new PinManager(this);
        isSettingUp = !pinManager.isPinSet();

        initViews();
        updateTitle();
    }

    private void initViews() {
        tvTitle    = findViewById(R.id.tv_pin_title);
        tvSubtitle = findViewById(R.id.tv_pin_subtitle);
        tvError    = findViewById(R.id.tv_pin_error);

        dots[0] = findViewById(R.id.dot1);
        dots[1] = findViewById(R.id.dot2);
        dots[2] = findViewById(R.id.dot3);
        dots[3] = findViewById(R.id.dot4);

        // Tasto cancella
        findViewById(R.id.btn_del).setOnClickListener(v -> deleteDigit());

        // Tasti numerici 0-9
        int[] btnIds = {
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
            R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9
        };
        for (int i = 0; i < btnIds.length; i++) {
            final String digit = String.valueOf(i);
            findViewById(btnIds[i]).setOnClickListener(v -> addDigit(digit));
        }
    }

    private void updateTitle() {
        if (isSettingUp) {
            if (firstPin == null) {
                tvTitle.setText("Imposta PIN");
                tvSubtitle.setText("Scegli un PIN di 4 cifre");
            } else {
                tvTitle.setText("Conferma PIN");
                tvSubtitle.setText("Reinserisci il PIN scelto");
            }
        } else {
            tvTitle.setText("Bentornato 🐔");
            tvSubtitle.setText("Inserisci il tuo PIN");
        }
    }

    private void addDigit(String digit) {
        if (pinInput.length() >= PIN_LENGTH) return;
        pinInput.append(digit);
        updateDots();
        if (pinInput.length() == PIN_LENGTH) {
            processPin();
        }
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
                i < pinInput.length()
                    ? R.drawable.dot_filled
                    : R.drawable.dot_empty
            );
        }
    }

    private void processPin() {
        String pin = pinInput.toString();

        if (isSettingUp) {
            if (firstPin == null) {
                // Prima inserzione — salva e chiedi conferma
                firstPin = pin;
                pinInput.setLength(0);
                updateDots();
                updateTitle();
                tvError.setVisibility(View.GONE);
            } else {
                // Seconda inserzione — confronta
                if (pin.equals(firstPin)) {
                    pinManager.savePin(pin);
                    goToMain();
                } else {
                    shakeError("I PIN non coincidono. Riprova.");
                    firstPin = null;
                    pinInput.setLength(0);
                    updateDots();
                    updateTitle();
                }
            }
        } else {
            // Verifica PIN
            if (pinManager.verifyPin(pin)) {
                goToMain();
            } else {
                shakeError("PIN errato. Riprova.");
                pinInput.setLength(0);
                updateDots();
            }
        }
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void shakeError(String msg) {
        tvError.setText(msg);
        tvError.setVisibility(View.VISIBLE);

        // Shake animazione sui dot
        View dotsRow = findViewById(R.id.dots_row);
        dotsRow.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake));

        // Vibrazione errore
        Vibrator vib = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vib != null)
            vib.vibrate(VibrationEffect.createWaveform(new long[]{0, 80, 60, 80}, -1));
    }
}

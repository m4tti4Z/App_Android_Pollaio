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

import com.google.android.material.textfield.TextInputEditText;

public class PinActivity extends AppCompatActivity {

    private static final int PIN_LENGTH = 4;

    private PinManager      pinManager;
    private StringBuilder   pinInput = new StringBuilder();
    private String          firstPin = null; // usato solo in fase di registrazione
    private boolean         isRegistering = false;

    private TextView          tvTitle, tvSubtitle, tvError;
    private TextInputEditText etEmail;
    private ImageView[]       dots = new ImageView[PIN_LENGTH];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin);

        pinManager    = new PinManager(this);
        isRegistering = !pinManager.isRegistered();

        initViews();
        updateTitle();

        // Pre-compila email se già salvata
        if (!isRegistering) {
            etEmail.setText(pinManager.getSavedEmail());
        }
    }

    private void initViews() {
        tvTitle    = findViewById(R.id.tv_pin_title);
        tvSubtitle = findViewById(R.id.tv_pin_subtitle);
        tvError    = findViewById(R.id.tv_pin_error);
        etEmail    = findViewById(R.id.et_username);

        // Nascondi loading overlay — non serve più
        View overlay = findViewById(R.id.loading_overlay);
        if (overlay != null) overlay.setVisibility(View.GONE);

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
        if (isRegistering) {
            if (firstPin == null) {
                tvTitle.setText("Benvenuto 🐔");
                tvSubtitle.setText("Inserisci email e scegli un PIN");
            } else {
                tvTitle.setText("Conferma PIN");
                tvSubtitle.setText("Reinserisci il PIN scelto");
            }
        } else {
            tvTitle.setText("Bentornato 🐔");
            tvSubtitle.setText("Inserisci email e PIN");
        }
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
        String email = etEmail.getText() != null
                ? etEmail.getText().toString().trim() : "";
        String pin = pinInput.toString();

        if (email.isEmpty()) {
            shakeError("Inserisci la tua email.");
            pinInput.setLength(0);
            updateDots();
            return;
        }

        if (isRegistering) {
            if (firstPin == null) {
                // Prima inserzione — chiedi conferma
                firstPin = pin;
                pinInput.setLength(0);
                updateDots();
                updateTitle();
                tvError.setVisibility(View.GONE);
            } else {
                // Seconda inserzione — confronta
                if (pin.equals(firstPin)) {
                    pinManager.save(email, pin);
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
            // Verifica locale
            if (pinManager.verify(email, pin)) {
                goToMain();
            } else {
                shakeError("Email o PIN errati. Riprova.");
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
        findViewById(R.id.dots_row).startAnimation(
                AnimationUtils.loadAnimation(this, R.anim.shake));
        Vibrator vib = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vib != null)
            vib.vibrate(VibrationEffect.createWaveform(new long[]{0, 80, 60, 80}, -1));
    }
}

package com.example.apppollaio;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.AlphaAnimation;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        TextView logo = findViewById(R.id.splash_logo);
        AlphaAnimation fade = new AlphaAnimation(0f, 1f);
        fade.setDuration(900);
        logo.startAnimation(fade);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(this, PinActivity.class));
            finish();
        }, 1600);
    }
}

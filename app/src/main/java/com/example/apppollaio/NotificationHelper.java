package com.example.apppollaio;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

import androidx.core.app.NotificationCompat;

public class NotificationHelper {

    private static final String CHANNEL_ID   = "pollaio_alerts";
    private static final String CHANNEL_NAME = "Pollaio IoT Avvisi";
    private int notifId = 1;

    private final Context context;
    private final NotificationManager nm;
    private final Vibrator vibrator;

    // Soglie di allerta
    public static final float SOGLIA_ACQUA_BASSA   = 20f;
    public static final float SOGLIA_TEMP_ALTA      = 35f;
    public static final float SOGLIA_TEMP_BASSA     = 5f;
    public static final float SOGLIA_UMIDITA_ALTA   = 85f;
    public static final float SOGLIA_UMIDITA_BASSA  = 30f;

    public NotificationHelper(Context context) {
        this.context = context;
        nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vm = (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            vibrator = vm.getDefaultVibrator();
        } else {
            vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        }

        createChannel();
    }

    private void createChannel() {
        NotificationChannel ch = new NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription("Avvisi sensori pollaio IoT");
        ch.enableVibration(true);
        ch.setVibrationPattern(new long[]{0, 400, 200, 400});
        nm.createNotificationChannel(ch);
    }

    public void checkAndNotify(SensorData data) {
        if (data.acqua >= 0 && data.acqua < SOGLIA_ACQUA_BASSA) {
            sendNotification("💧 Acqua Bassa!",
                    "Livello acqua al " + (int) data.acqua + "% — rabbocca l'abbeveratoio.",
                    new long[]{0, 500, 200, 500, 200, 500});
        }
        if (data.temperatura > 0 && data.temperatura > SOGLIA_TEMP_ALTA) {
            sendNotification("🔥 Temperatura Alta!",
                    "Rilevati " + data.temperatura + "°C nel pollaio — ventilare.",
                    new long[]{0, 300, 150, 300});
        }
        if (data.temperatura > 0 && data.temperatura < SOGLIA_TEMP_BASSA) {
            sendNotification("❄️ Temperatura Bassa!",
                    "Rilevati " + data.temperatura + "°C — le galline potrebbero soffrire il freddo.",
                    new long[]{0, 300, 150, 300});
        }
        if (data.umidita > 0 && data.umidita > SOGLIA_UMIDITA_ALTA) {
            sendNotification("💦 Umidità Eccessiva",
                    "Umidità al " + data.umidita + "% — rischio muffe e malattie.",
                    new long[]{0, 250, 250, 250});
        }
        if (data.umidita > 0 && data.umidita < SOGLIA_UMIDITA_BASSA) {
            sendNotification("🌵 Umidità Troppo Bassa",
                    "Umidità al " + data.umidita + "% — ambiente troppo secco.",
                    new long[]{0, 250, 250, 250});
        }
    }

    public void sendNotification(String title, String text, long[] pattern) {
        vibrate(pattern);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);
        nm.notify(notifId++, builder.build());
    }

    private void vibrate(long[] pattern) {
        VibrationEffect effect = VibrationEffect.createWaveform(pattern, -1);
        vibrator.vibrate(effect);
    }
}

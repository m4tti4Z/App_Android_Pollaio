package com.example.apppollaio;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

import androidx.core.app.NotificationCompat;

public class NotificationHelper {

    private static final String CHANNEL_ID   = "pollaio_alerts";
    private static final String CHANNEL_NAME = "Pollaio IoT Avvisi";

    // Cooldown 5 minuti — evita spam notifiche
    private static final long COOLDOWN_MS = 5 * 60 * 1000;
    private long lastNotifAcqua     = 0;
    private long lastNotifTempAlta  = 0;
    private long lastNotifTempBassa = 0;
    private long lastNotifHumAlta   = 0;
    private long lastNotifHumBassa  = 0;

    // Soglie
    public static final float SOGLIA_ACQUA_BASSA   = 20f;
    public static final float SOGLIA_TEMP_ALTA     = 35f;
    public static final float SOGLIA_TEMP_BASSA    = 5f;
    public static final float SOGLIA_UMIDITA_ALTA  = 85f;
    public static final float SOGLIA_UMIDITA_BASSA = 30f;

    private final Context context;
    private final NotificationManager nm;
    private final Vibrator vibrator;

    // Enum con ID fissi — evita notifiche duplicate dello stesso tipo
    private enum NotifType {
        ACQUA_BASSA, TEMP_ALTA, TEMP_BASSA, UMIDITA_ALTA, UMIDITA_BASSA
    }

    public NotificationHelper(Context context) {
        this.context = context.getApplicationContext();
        nm = (NotificationManager) this.context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vm = (VibratorManager) this.context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            vibrator = vm.getDefaultVibrator();
        } else {
            vibrator = (Vibrator) this.context.getSystemService(Context.VIBRATOR_SERVICE);
        }

        createChannel();
    }

    private void createChannel() {
        NotificationChannel ch = new NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription("Avvisi sensori pollaio IoT");
        ch.enableVibration(true);
        ch.setVibrationPattern(new long[]{0, 300, 150, 300});
        ch.setShowBadge(true);
        nm.createNotificationChannel(ch);
    }

    public void checkAndNotify(SensorData data) {
        long now = System.currentTimeMillis();

        if (data.acqua >= 0 && data.acqua < SOGLIA_ACQUA_BASSA) {
            if (now - lastNotifAcqua > COOLDOWN_MS) {
                lastNotifAcqua = now;
                send(
                        NotifType.ACQUA_BASSA,
                        "💧 Acqua Bassa — " + (int) data.acqua + "%",
                        "Il livello dell'acqua è al " + (int) data.acqua + "%. Rabbocca subito l'abbeveratoio.",
                        "Livello attuale: " + (int) data.acqua + "% · Soglia minima: " + (int) SOGLIA_ACQUA_BASSA + "%",
                        new long[]{0, 500, 200, 500, 200, 500}
                );
            }
        }

        if (data.temperatura > 0 && data.temperatura > SOGLIA_TEMP_ALTA) {
            if (now - lastNotifTempAlta > COOLDOWN_MS) {
                lastNotifTempAlta = now;
                send(
                        NotifType.TEMP_ALTA,
                        "🔥 Temperatura Alta — " + String.format("%.1f", data.temperatura) + "°C",
                        "Nel pollaio ci sono " + String.format("%.1f", data.temperatura) + "°C. Apri le finestre o attiva la ventilazione.",
                        "Temperatura: " + String.format("%.1f", data.temperatura) + "°C · Soglia: " + (int) SOGLIA_TEMP_ALTA + "°C",
                        new long[]{0, 400, 150, 400}
                );
            }
        }

        if (data.temperatura > 0 && data.temperatura < SOGLIA_TEMP_BASSA) {
            if (now - lastNotifTempBassa > COOLDOWN_MS) {
                lastNotifTempBassa = now;
                send(
                        NotifType.TEMP_BASSA,
                        "❄️ Temperatura Bassa — " + String.format("%.1f", data.temperatura) + "°C",
                        "Nel pollaio ci sono solo " + String.format("%.1f", data.temperatura) + "°C. Le galline rischiano il freddo.",
                        "Temperatura: " + String.format("%.1f", data.temperatura) + "°C · Soglia: " + (int) SOGLIA_TEMP_BASSA + "°C",
                        new long[]{0, 300, 150, 300}
                );
            }
        }

        if (data.umidita > 0 && data.umidita > SOGLIA_UMIDITA_ALTA) {
            if (now - lastNotifHumAlta > COOLDOWN_MS) {
                lastNotifHumAlta = now;
                send(
                        NotifType.UMIDITA_ALTA,
                        "💦 Umidità Eccessiva — " + (int) data.umidita + "%",
                        "L'umidità è al " + (int) data.umidita + "%. Rischio muffe e malattie respiratorie.",
                        "Umidità: " + (int) data.umidita + "% · Soglia massima: " + (int) SOGLIA_UMIDITA_ALTA + "%",
                        new long[]{0, 250, 200, 250}
                );
            }
        }

        if (data.umidita > 0 && data.umidita < SOGLIA_UMIDITA_BASSA) {
            if (now - lastNotifHumBassa > COOLDOWN_MS) {
                lastNotifHumBassa = now;
                send(
                        NotifType.UMIDITA_BASSA,
                        "🌵 Umidità Troppo Bassa — " + (int) data.umidita + "%",
                        "L'umidità è al " + (int) data.umidita + "%. L'ambiente è troppo secco per le galline.",
                        "Umidità: " + (int) data.umidita + "% · Soglia minima: " + (int) SOGLIA_UMIDITA_BASSA + "%",
                        new long[]{0, 250, 200, 250}
                );
            }
        }
    }

    private void send(NotifType type, String title, String text, String detail, long[] vibPattern) {
        // Tap sulla notifica → apre MainActivity
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(
                context, type.ordinal(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(title)
                .setContentText(text)
                // Espandendo la notifica mostra il dettaglio con la soglia
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(text + "\n\n📊 " + detail))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(pi)
                .setAutoCancel(true);

        // ID fisso per tipo — sovrascrive la stessa notifica invece di crearne una nuova
        nm.notify(type.ordinal(), builder.build());
        vibrate(vibPattern);
    }

    private void vibrate(long[] pattern) {
        VibrationEffect effect = VibrationEffect.createWaveform(pattern, -1);
        vibrator.vibrate(effect);
    }
}
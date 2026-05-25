package com.example.apppollaio;

import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ApiClient {

    private static final String TAG     = "ApiClient";
    // Cambia con l'IP del tuo PC sulla rete locale — es. "http://192.168.1.10"
    public static final String BASE_URL = "http://100.115.223.39/Pollaio_Progetto_IoT_WebApp";

    public interface Callback {
        void onSuccess(JSONObject response);
        void onError(String errore);
    }

    public static void post(String endpoint, JSONObject body, Callback callback) {
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(BASE_URL + endpoint);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "application/json");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.toString().getBytes(StandardCharsets.UTF_8));
                }

                int code = conn.getResponseCode();
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        code >= 400 ? conn.getErrorStream() : conn.getInputStream(),
                        StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                JSONObject json = new JSONObject(sb.toString());
                if (json.optBoolean("ok", false)) {
                    callback.onSuccess(json);
                } else {
                    callback.onError(json.optString("errore", "Errore sconosciuto"));
                }

            } catch (Exception e) {
                Log.e(TAG, "Errore API: " + e.getMessage());
                callback.onError("Impossibile contattare il server. Sei connesso al WiFi di casa?");
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }
}

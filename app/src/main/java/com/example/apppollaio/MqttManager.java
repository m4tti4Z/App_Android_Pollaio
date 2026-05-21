package com.example.apppollaio;

import android.util.Log;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class MqttManager {

    private static final String TAG = "MqttManager";

    public static final String BROKER   = "ssl://a9c3e175b18b46c3a22cb8391d81209b.s1.eu.hivemq.cloud";
    public static final int    PORT     = 8883;
    public static final String USERNAME = "Cobra";
    public static final String PASSWORD = "Password1234";

    public static final String TOPIC_TELEMETRIA  = "pollaio/telemetria";
    public static final String TOPIC_CMD_LUCE    = "pollaio/comando/luce";
    public static final String TOPIC_CMD_MANGIME = "pollaio/comando/mangime";
    public static final String TOPIC_ORARI       = "pollaio/config/orari";

    public interface MessageListener {
        void onMessage(String topic, String payload);
        void onConnected();
        void onConnectionLost();
    }

    private MqttClient client;
    private MessageListener listener;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public void connect(MessageListener l) {
        this.listener = l;
        executor.execute(() -> {
            try {
                String clientId = "AndroidPollaio-" + System.currentTimeMillis();
                client = new MqttClient(BROKER + ":" + PORT, clientId, new MemoryPersistence());

                MqttConnectOptions opts = new MqttConnectOptions();
                opts.setUserName(USERNAME);
                opts.setPassword(PASSWORD.toCharArray());
                opts.setCleanSession(true);
                opts.setKeepAliveInterval(30);
                opts.setAutomaticReconnect(true);
                opts.setSocketFactory(getInsecureSSLFactory()); // come setInsecure() ESP32

                client.setCallback(new MqttCallback() {
                    @Override
                    public void connectionLost(Throwable cause) {
                        Log.w(TAG, "Connessione persa: " + cause.getMessage());
                        if (listener != null) listener.onConnectionLost();
                    }
                    @Override
                    public void messageArrived(String topic, MqttMessage message) {
                        if (listener != null)
                            listener.onMessage(topic, new String(message.getPayload()));
                    }
                    @Override
                    public void deliveryComplete(IMqttDeliveryToken token) {}
                });

                client.connect(opts);
                client.subscribe(TOPIC_TELEMETRIA, 1);
                Log.d(TAG, "Connesso a HiveMQ Cloud SSL");
                if (listener != null) listener.onConnected();

            } catch (Exception e) {
                Log.e(TAG, "Errore connessione MQTT: " + e.getMessage());
                if (listener != null) listener.onConnectionLost();
            }
        });
    }

    public void publish(String topic, String payload) {
        executor.execute(() -> {
            try {
                if (client != null && client.isConnected()) {
                    MqttMessage msg = new MqttMessage(payload.getBytes());
                    msg.setQos(1);
                    client.publish(topic, msg);
                    Log.d(TAG, "Pubblicato su " + topic + ": " + payload);
                }
            } catch (MqttException e) {
                Log.e(TAG, "Errore publish: " + e.getMessage());
            }
        });
    }

    public boolean isConnected() {
        return client != null && client.isConnected();
    }

    public void disconnect() {
        executor.execute(() -> {
            try {
                if (client != null && client.isConnected()) {
                    client.disconnect();
                }
            } catch (MqttException e) {
                Log.e(TAG, "Errore disconnect: " + e.getMessage());
            }
        });
    }

    // Accetta tutti i certificati SSL, come setInsecure() sull'ESP32
    private SSLSocketFactory getInsecureSSLFactory() {
        try {
            TrustManager[] trustAll = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                    public void checkClientTrusted(X509Certificate[] c, String a) {}
                    public void checkServerTrusted(X509Certificate[] c, String a) {}
                }
            };
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, trustAll, new SecureRandom());
            return ctx.getSocketFactory();
        } catch (Exception e) {
            Log.e(TAG, "Errore SSL factory: " + e.getMessage());
            return (SSLSocketFactory) SSLSocketFactory.getDefault();
        }
    }
}

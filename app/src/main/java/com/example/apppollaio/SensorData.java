package com.example.apppollaio;

public class SensorData {
    public float acqua       = -1;
    public float temperatura = -1;
    public float umidita     = -1;
    public float luce        = -1;

    public static SensorData fromJson(String json) {
        SensorData d = new SensorData();
        try {
            com.google.gson.JsonObject obj =
                com.google.gson.JsonParser.parseString(json).getAsJsonObject();
            if (obj.has("acqua") && !obj.get("acqua").isJsonNull())
                d.acqua = obj.get("acqua").getAsFloat();
            if (obj.has("temperatura") && !obj.get("temperatura").isJsonNull())
                d.temperatura = obj.get("temperatura").getAsFloat();
            if (obj.has("umidita") && !obj.get("umidita").isJsonNull())
                d.umidita = obj.get("umidita").getAsFloat();
            if (obj.has("luce") && !obj.get("luce").isJsonNull())
                d.luce = obj.get("luce").getAsFloat();
        } catch (Exception ignored) {}
        return d;
    }

    public String fmt(float v, String unit) {
        return v < 0 ? "--" : String.format("%.1f %s", v, unit);
    }
}

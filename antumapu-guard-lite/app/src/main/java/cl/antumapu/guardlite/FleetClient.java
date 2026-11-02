package cl.antumapu.guardlite;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Build;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class FleetClient {
    public static final class Command {
        public final String name;
        public final String id;

        Command(String name, String id) {
            this.name = name == null ? "" : name;
            this.id = id == null ? "" : id;
        }

        public boolean hasCommand() {
            return name.length() > 0 && id.length() > 0;
        }
    }

    private FleetClient() {}

    public static Command heartbeat(Context context, Location location) {
        if (!Prefs.consoleConfigured(context)) return new Command("", "");

        try {
            StringBuilder q = base(context, "heartbeat");
            add(q, "label", Prefs.deviceLabel(context));
            add(q, "manufacturer", Build.MANUFACTURER);
            add(q, "model", Build.MODEL);
            add(q, "android", Build.VERSION.RELEASE);
            add(q, "sdk", Integer.toString(Build.VERSION.SDK_INT));

            Intent battery = context.registerReceiver(
                    null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (battery != null) {
                int level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
                int status = battery.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                int pct = scale > 0 && level >= 0 ? Math.round(level * 100f / scale) : -1;
                boolean charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL;
                add(q, "battery", Integer.toString(pct));
                add(q, "charging", charging ? "1" : "0");
            }

            LocationManager lm =
                    (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            if (lm != null) {
                boolean gps = false;
                try { gps = lm.isProviderEnabled(LocationManager.GPS_PROVIDER); }
                catch (Exception ignored) {}
                add(q, "gps", gps ? "1" : "0");
            }

            if (location != null) {
                add(q, "lat", Double.toString(location.getLatitude()));
                add(q, "lon", Double.toString(location.getLongitude()));
                add(q, "accuracy", Float.toString(location.getAccuracy()));
                Prefs.saveLocation(
                        context,
                        location.getLatitude(),
                        location.getLongitude(),
                        location.getAccuracy(),
                        System.currentTimeMillis());
            } else if (Prefs.hasLocation(context)) {
                add(q, "lat", Double.toString(Prefs.lastLat(context)));
                add(q, "lon", Double.toString(Prefs.lastLon(context)));
                add(q, "accuracy", Float.toString(Prefs.lastAccuracy(context)));
            }

            JSONObject json = request(Prefs.consoleUrl(context) + "?" + q);
            Prefs.markHeartbeat(context);

            return new Command(
                    json.optString("command", ""),
                    json.optString("commandId", ""));
        } catch (Exception e) {
            return new Command("", "");
        }
    }

    public static void ack(Context context, String commandId, String status) {
        if (!Prefs.consoleConfigured(context) || commandId == null || commandId.length() == 0) return;
        try {
            StringBuilder q = base(context, "ack");
            add(q, "commandId", commandId);
            add(q, "status", status == null ? "DONE" : status);
            request(Prefs.consoleUrl(context) + "?" + q);
        } catch (Exception ignored) {
        }
    }

    public static void shutdown(Context context) {
        if (!Prefs.consoleConfigured(context)) return;
        try {
            StringBuilder q = base(context, "shutdown");
            request(Prefs.consoleUrl(context) + "?" + q);
        } catch (Exception ignored) {
        }
    }

    public static boolean test(Context context) {
        if (!Prefs.consoleConfigured(context)) return false;
        try {
            JSONObject json = request(Prefs.consoleUrl(context) + "?" + base(context, "ping"));
            return json.optBoolean("ok", false);
        } catch (Exception e) {
            return false;
        }
    }

    private static StringBuilder base(Context context, String action) throws Exception {
        StringBuilder q = new StringBuilder();
        add(q, "action", action);
        add(q, "token", Prefs.consoleToken(context));
        add(q, "deviceId", Prefs.deviceId(context));
        return q;
    }

    private static void add(StringBuilder q, String key, String value) throws Exception {
        if (q.length() > 0) q.append('&');
        q.append(URLEncoder.encode(key, "UTF-8"));
        q.append('=');
        q.append(URLEncoder.encode(value == null ? "" : value, "UTF-8"));
    }

    private static JSONObject request(String urlString) throws Exception {
        HttpURLConnection c = null;
        try {
            URL url = new URL(urlString);
            c = (HttpURLConnection) url.openConnection();
            c.setConnectTimeout(9000);
            c.setReadTimeout(9000);
            c.setRequestMethod("GET");
            c.setInstanceFollowRedirects(true);
            c.setUseCaches(false);
            c.setRequestProperty("User-Agent", "Pangi/3.1 Android");

            int code = c.getResponseCode();
            InputStream stream = code >= 200 && code < 400
                    ? c.getInputStream() : c.getErrorStream();
            if (stream == null) throw new IllegalStateException("Sin respuesta");

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8));
            StringBuilder body = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) body.append(line);
            reader.close();

            return new JSONObject(body.toString());
        } finally {
            if (c != null) c.disconnect();
        }
    }
}

package cl.antumapu.guardlite;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.UUID;

public final class Prefs {
    private static final String FILE = "guard_lite";
    private static final String PIN_HASH = "pin_hash";
    private static final String PIN_SALT = "pin_salt";
    private static final String UNLOCK_UNTIL = "unlock_until";
    private static final String UNLOCK_SECONDS = "unlock_seconds";
    private static final String LAST_SPLASH_BOOT = "last_splash_boot";
    private static final String LAST_SPLASH_WALL = "last_splash_wall";
    private static final String ORIGINAL_HOME = "original_home_component";

    private static final String FLEET_ENABLED = "fleet_enabled";
    private static final String CONSOLE_URL = "console_url";
    private static final String CONSOLE_TOKEN = "console_token";
    private static final String DEVICE_ID = "fleet_device_id";
    private static final String DEVICE_LABEL = "fleet_device_label";
    private static final String LAST_HEARTBEAT = "fleet_last_heartbeat";
    private static final String LAST_LAT = "fleet_last_lat";
    private static final String LAST_LON = "fleet_last_lon";
    private static final String LAST_ACCURACY = "fleet_last_accuracy";
    private static final String LAST_LOCATION_AT = "fleet_last_location_at";

    private Prefs() {}

    private static SharedPreferences p(Context c) {
        return c.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static boolean hasPin(Context c) {
        return p(c).contains(PIN_HASH);
    }

    public static void setPin(Context c, String pin) {
        try {
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);
            p(c).edit()
                    .putString(PIN_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
                    .putString(PIN_HASH, hash(pin, salt))
                    .apply();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean checkPin(Context c, String pin) {
        try {
            String salt64 = p(c).getString(PIN_SALT, "");
            String expected = p(c).getString(PIN_HASH, "");
            if (salt64.length() == 0 || expected.length() == 0) return false;
            byte[] salt = Base64.decode(salt64, Base64.NO_WRAP);
            String actual = hash(pin, salt);
            return MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    actual.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false;
        }
    }

    private static String hash(String pin, byte[] salt) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(salt);
        md.update(pin.getBytes(StandardCharsets.UTF_8));
        return Base64.encodeToString(md.digest(), Base64.NO_WRAP);
    }

    public static void unlock(Context c) {
        long until = System.currentTimeMillis() + getUnlockSeconds(c) * 1000L;
        p(c).edit().putLong(UNLOCK_UNTIL, until).apply();
    }

    public static void lock(Context c) {
        p(c).edit().putLong(UNLOCK_UNTIL, 0L).apply();
    }

    public static boolean isUnlocked(Context c) {
        return System.currentTimeMillis() < p(c).getLong(UNLOCK_UNTIL, 0L);
    }

    public static long remainingMs(Context c) {
        return Math.max(0L, p(c).getLong(UNLOCK_UNTIL, 0L) - System.currentTimeMillis());
    }

    public static int getUnlockSeconds(Context c) {
        return p(c).getInt(UNLOCK_SECONDS, 300);
    }

    public static void setUnlockSeconds(Context c, int value) {
        p(c).edit().putInt(UNLOCK_SECONDS, value).apply();
    }

    private static boolean get(Context c, String key, boolean def) {
        return p(c).getBoolean(key, def);
    }

    private static void set(Context c, String key, boolean value) {
        p(c).edit().putBoolean(key, value).apply();
    }

    public static boolean protectPlay(Context c) { return get(c, "play", true); }
    public static void setProtectPlay(Context c, boolean v) { set(c, "play", v); }

    public static boolean protectInstall(Context c) { return get(c, "install", true); }
    public static void setProtectInstall(Context c, boolean v) { set(c, "install", v); }

    public static boolean protectApps(Context c) { return get(c, "apps", true); }
    public static void setProtectApps(Context c, boolean v) { set(c, "apps", v); }

    public static boolean protectAccessibility(Context c) { return get(c, "accessibility", true); }
    public static void setProtectAccessibility(Context c, boolean v) { set(c, "accessibility", v); }

    public static boolean protectDeveloper(Context c) { return get(c, "developer", false); }
    public static void setProtectDeveloper(Context c, boolean v) { set(c, "developer", v); }

    public static boolean blockUnknownSources(Context c) { return get(c, "do_unknown_sources", true); }
    public static void setBlockUnknownSources(Context c, boolean v) { set(c, "do_unknown_sources", v); }

    public static boolean blockAppsControl(Context c) { return get(c, "do_apps_control", true); }
    public static void setBlockAppsControl(Context c, boolean v) { set(c, "do_apps_control", v); }

    public static boolean blockPlayStore(Context c) { return get(c, "do_play_store", true); }
    public static void setBlockPlayStore(Context c, boolean v) { set(c, "do_play_store", v); }

    public static boolean shouldShowSplashForBoot(Context c, int bootCount) {
        if (bootCount >= 0) {
            return p(c).getInt(LAST_SPLASH_BOOT, Integer.MIN_VALUE) != bootCount;
        }
        long last = p(c).getLong(LAST_SPLASH_WALL, 0L);
        return System.currentTimeMillis() - last > 30000L;
    }

    public static void markSplashShownForBoot(Context c, int bootCount) {
        SharedPreferences.Editor e = p(c).edit().putLong(LAST_SPLASH_WALL, System.currentTimeMillis());
        if (bootCount >= 0) e.putInt(LAST_SPLASH_BOOT, bootCount);
        e.apply();
    }

    public static void setOriginalHome(Context c, String flattenedComponent) {
        if (flattenedComponent == null || flattenedComponent.length() == 0) return;
        p(c).edit().putString(ORIGINAL_HOME, flattenedComponent).apply();
    }

    public static String getOriginalHome(Context c) {
        return p(c).getString(ORIGINAL_HOME, "");
    }

    public static boolean fleetEnabled(Context c) {
        return p(c).getBoolean(FLEET_ENABLED, false);
    }

    public static void setFleetEnabled(Context c, boolean enabled) {
        p(c).edit().putBoolean(FLEET_ENABLED, enabled).apply();
    }

    public static String consoleUrl(Context c) {
        return p(c).getString(CONSOLE_URL, "");
    }

    public static String consoleToken(Context c) {
        return p(c).getString(CONSOLE_TOKEN, "");
    }

    public static void setConsoleConfig(Context c, String url, String token, String label) {
        p(c).edit()
                .putString(CONSOLE_URL, url == null ? "" : url.trim())
                .putString(CONSOLE_TOKEN, token == null ? "" : token.trim())
                .putString(DEVICE_LABEL, label == null ? "" : label.trim())
                .apply();
    }

    public static boolean consoleConfigured(Context c) {
        return consoleUrl(c).startsWith("https://") && consoleToken(c).length() >= 12;
    }

    public static String deviceId(Context c) {
        String id = p(c).getString(DEVICE_ID, "");
        if (id.length() == 0) {
            id = UUID.randomUUID().toString();
            p(c).edit().putString(DEVICE_ID, id).apply();
        }
        return id;
    }

    public static String deviceLabel(Context c) {
        String label = p(c).getString(DEVICE_LABEL, "");
        if (label.length() == 0) {
            label = Build.MANUFACTURER + " " + Build.MODEL;
        }
        return label;
    }

    public static void markHeartbeat(Context c) {
        p(c).edit().putLong(LAST_HEARTBEAT, System.currentTimeMillis()).apply();
    }

    public static long lastHeartbeat(Context c) {
        return p(c).getLong(LAST_HEARTBEAT, 0L);
    }

    public static void saveLocation(Context c, double lat, double lon, float accuracy, long at) {
        p(c).edit()
                .putString(LAST_LAT, Double.toString(lat))
                .putString(LAST_LON, Double.toString(lon))
                .putFloat(LAST_ACCURACY, accuracy)
                .putLong(LAST_LOCATION_AT, at)
                .apply();
    }

    public static boolean hasLocation(Context c) {
        return p(c).contains(LAST_LAT) && p(c).contains(LAST_LON);
    }

    public static double lastLat(Context c) {
        try { return Double.parseDouble(p(c).getString(LAST_LAT, "0")); }
        catch (Exception e) { return 0d; }
    }

    public static double lastLon(Context c) {
        try { return Double.parseDouble(p(c).getString(LAST_LON, "0")); }
        catch (Exception e) { return 0d; }
    }

    public static float lastAccuracy(Context c) {
        return p(c).getFloat(LAST_ACCURACY, 0f);
    }

    public static long lastLocationAt(Context c) {
        return p(c).getLong(LAST_LOCATION_AT, 0L);
    }
}

package cl.antumapu.guardlite;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

public final class Prefs {
    private static final String FILE = "guard_lite";
    private static final String PIN_HASH = "pin_hash";
    private static final String PIN_SALT = "pin_salt";
    private static final String UNLOCK_UNTIL = "unlock_until";
    private static final String UNLOCK_SECONDS = "unlock_seconds";

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
}

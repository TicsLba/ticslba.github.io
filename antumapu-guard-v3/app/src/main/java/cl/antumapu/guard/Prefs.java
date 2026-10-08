package cl.antumapu.guard;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

public final class Prefs {
    private static final String FILE = "guard_prefs";
    private static final String K_HASH = "pin_hash";
    private static final String K_SALT = "pin_salt";
    private static final String K_UNLOCK = "unlocked_until";
    private static final String K_SECONDS = "unlock_seconds";

    private Prefs() {}

    private static SharedPreferences p(Context c) {
        return c.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static boolean hasPin(Context c) { return p(c).contains(K_HASH); }

    public static void setPin(Context c, String pin) {
        try {
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);
            String hash = hash(pin, salt);
            p(c).edit()
                    .putString(K_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
                    .putString(K_HASH, hash)
                    .apply();
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public static boolean checkPin(Context c, String pin) {
        try {
            String salt64 = p(c).getString(K_SALT, "");
            String expected = p(c).getString(K_HASH, "");
            if (salt64.length() == 0 || expected.length() == 0) return false;
            byte[] salt = Base64.decode(salt64, Base64.NO_WRAP);
            return MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    hash(pin, salt).getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) { return false; }
    }

    private static String hash(String pin, byte[] salt) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(salt);
        md.update(pin.getBytes(StandardCharsets.UTF_8));
        return Base64.encodeToString(md.digest(), Base64.NO_WRAP);
    }

    public static int unlockSeconds(Context c) { return p(c).getInt(K_SECONDS, 300); }
    public static void setUnlockSeconds(Context c, int seconds) {
        p(c).edit().putInt(K_SECONDS, seconds).apply();
    }

    public static void unlock(Context c) {
        p(c).edit().putLong(K_UNLOCK, System.currentTimeMillis() + unlockSeconds(c) * 1000L).apply();
    }
    public static void lock(Context c) { p(c).edit().putLong(K_UNLOCK, 0L).apply(); }
    public static boolean isUnlocked(Context c) {
        return System.currentTimeMillis() < p(c).getLong(K_UNLOCK, 0L);
    }

    private static boolean getB(Context c, String k, boolean d) { return p(c).getBoolean(k, d); }
    private static void setB(Context c, String k, boolean v) { p(c).edit().putBoolean(k, v).apply(); }

    public static boolean protectPlay(Context c) { return getB(c, "protect_play", true); }
    public static void setProtectPlay(Context c, boolean v) { setB(c, "protect_play", v); }

    public static boolean protectInstaller(Context c) { return getB(c, "protect_installer", true); }
    public static void setProtectInstaller(Context c, boolean v) { setB(c, "protect_installer", v); }

    public static boolean protectApps(Context c) { return getB(c, "protect_apps", true); }
    public static void setProtectApps(Context c, boolean v) { setB(c, "protect_apps", v); }

    public static boolean protectAccessibility(Context c) { return getB(c, "protect_accessibility", true); }
    public static void setProtectAccessibility(Context c, boolean v) { setB(c, "protect_accessibility", v); }

    public static boolean protectDeveloper(Context c) { return getB(c, "protect_developer", false); }
    public static void setProtectDeveloper(Context c, boolean v) { setB(c, "protect_developer", v); }
}

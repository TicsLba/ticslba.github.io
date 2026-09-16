package cl.antumapu.aulacontrol;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

final class Compatibility {
    private Compatibility() {}

    static boolean androidSupported() { return Build.VERSION.SDK_INT >= 26; }

    static boolean managedUsers(Context c) {
        try { return c.getPackageManager().hasSystemFeature(PackageManager.FEATURE_MANAGED_USERS); }
        catch (Exception ignored) { return false; }
    }

    static boolean screenCapture(Context c) {
        try { return c.getSystemService(Context.MEDIA_PROJECTION_SERVICE) != null; }
        catch (Exception ignored) { return false; }
    }

    static boolean deviceAdmin(Context c) {
        try { return c.getSystemService(DevicePolicyManager.class) != null; }
        catch (Exception ignored) { return false; }
    }

    static boolean ephemeralNative() { return Build.VERSION.SDK_INT >= 28; }

    static boolean sessionsSupported(Context c) {
        return androidSupported() && managedUsers(c) && deviceAdmin(c) && screenCapture(c);
    }

    static String model() {
        String maker = Build.MANUFACTURER == null ? "Android" : Build.MANUFACTURER.trim();
        String model = Build.MODEL == null ? "" : Build.MODEL.trim();
        return (maker + " " + model).trim();
    }

    static String summary(Context c) {
        return model() + " · Android " + Build.VERSION.RELEASE + " · API " + Build.VERSION.SDK_INT;
    }
}

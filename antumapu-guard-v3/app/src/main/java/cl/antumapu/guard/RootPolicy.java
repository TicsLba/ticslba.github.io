package cl.antumapu.guard;

import android.content.ComponentName;
import android.content.Context;

public final class RootPolicy {
    private RootPolicy() {}

    private static final String[] INSTALL_SOURCES = new String[] {
            "com.android.chrome",
            "org.mozilla.firefox",
            "com.sec.android.app.sbrowser",
            "com.google.android.apps.nbu.files",
            "com.google.android.apps.docs",
            "com.microsoft.emmx",
            "com.opera.browser"
    };

    public static boolean ensureAccessibility(Context c) {
        if (!Shell.hasRoot()) return false;
        String component = new ComponentName(c, GuardAccessibilityService.class).flattenToString();
        Shell.Result read = Shell.su("settings get secure enabled_accessibility_services");
        String cur = read.output == null ? "" : read.output.trim();
        if ("null".equals(cur)) cur = "";
        if (!cur.contains(component)) cur = cur.length() == 0 ? component : cur + ":" + component;
        String cmd = "settings put secure enabled_accessibility_services '" +
                cur.replace("'", "") + "'; settings put secure accessibility_enabled 1";
        return Shell.su(cmd).ok;
    }

    public static void applyLocked(Context c, boolean closeProtected) {
        if (!Shell.hasRoot()) return;
        StringBuilder cmd = new StringBuilder();

        if (Prefs.protectInstaller(c)) {
            for (String p : INSTALL_SOURCES) {
                cmd.append("cmd appops set ").append(p)
                   .append(" REQUEST_INSTALL_PACKAGES ignore >/dev/null 2>&1 || true; ");
            }
            cmd.append("settings put secure install_non_market_apps 0 >/dev/null 2>&1 || true; ");
        } else {
            for (String p : INSTALL_SOURCES) {
                cmd.append("cmd appops set ").append(p)
                   .append(" REQUEST_INSTALL_PACKAGES allow >/dev/null 2>&1 || true; ");
            }
        }

        if (closeProtected) {
            if (Prefs.protectPlay(c))
                cmd.append("am force-stop com.android.vending >/dev/null 2>&1 || true; ");
            if (Prefs.protectInstaller(c)) {
                cmd.append("am force-stop com.android.packageinstaller >/dev/null 2>&1 || true; ");
                cmd.append("am force-stop com.google.android.packageinstaller >/dev/null 2>&1 || true; ");
                cmd.append("am force-stop com.samsung.android.packageinstaller >/dev/null 2>&1 || true; ");
            }
            if (Prefs.protectApps(c) || Prefs.protectAccessibility(c) || Prefs.protectDeveloper(c))
                cmd.append("am force-stop com.android.settings >/dev/null 2>&1 || true; ");
        }

        Shell.su(cmd.toString());
    }

    public static void applyUnlocked(Context c) {
        if (!Shell.hasRoot()) return;
        if (!Prefs.protectInstaller(c)) return;
        StringBuilder cmd = new StringBuilder();
        for (String p : INSTALL_SOURCES) {
            cmd.append("cmd appops set ").append(p)
               .append(" REQUEST_INSTALL_PACKAGES allow >/dev/null 2>&1 || true; ");
        }
        cmd.append("settings put secure install_non_market_apps 1 >/dev/null 2>&1 || true; ");
        Shell.su(cmd.toString());
    }
}

package cl.antumapu.guard;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;

import java.util.Locale;

public class GuardAccessibilityService extends AccessibilityService {
    private long lastLaunch = 0L;

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null) return;

        String pkg = String.valueOf(event.getPackageName());
        String cls = event.getClassName() == null ? "" : String.valueOf(event.getClassName());
        String lowCls = cls.toLowerCase(Locale.ROOT);

        if (pkg.equals(getPackageName())) return;
        if (Prefs.isUnlocked(this)) return;
        if (!isSensitive(pkg, lowCls, event)) return;

        RootPolicy.applyLocked(this, false);

        long now = SystemClock.elapsedRealtime();
        if (now - lastLaunch < 700L) return;
        lastLaunch = now;

        Intent i = new Intent(this, LockActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS |
                Intent.FLAG_ACTIVITY_NO_ANIMATION |
                Intent.FLAG_ACTIVITY_CLEAR_TOP);
        i.putExtra("target_pkg", pkg);
        i.putExtra("target_cls", cls);
        startActivity(i);
    }

    private boolean isSensitive(String pkg, String cls, AccessibilityEvent event) {
        if (Prefs.protectPlay(this) && pkg.equals("com.android.vending")) return true;

        if (Prefs.protectInstaller(this)) {
            if (pkg.equals("com.android.packageinstaller") ||
                    pkg.equals("com.google.android.packageinstaller") ||
                    pkg.equals("com.samsung.android.packageinstaller") ||
                    pkg.equals("com.miui.packageinstaller")) return true;

            if (pkg.equals("com.google.android.permissioncontroller") ||
                    pkg.equals("com.android.permissioncontroller")) {
                if (cls.contains("manageexternalsources") ||
                        cls.contains("packageinstaller") ||
                        cls.contains("specialaccess")) return true;
            }
        }

        if (pkg.equals("com.android.settings")) {
            if (Prefs.protectApps(this)) {
                if (cls.contains("manageapplications") ||
                        cls.contains("installedappdetails") ||
                        cls.contains("applicationdetails") ||
                        cls.contains("manageapplicationssettings") ||
                        cls.contains("appandnotification")) return true;
            }

            if (Prefs.protectInstaller(this)) {
                if (cls.contains("manageexternalsources") ||
                        cls.contains("specialappaccess")) return true;
            }

            if (Prefs.protectAccessibility(this)) {
                if (cls.contains("accessibilitysettings") ||
                        cls.contains("accessibilitydetails") ||
                        cls.contains("accessibilityservice")) return true;
            }

            if (Prefs.protectDeveloper(this)) {
                if (cls.contains("developmentsettings") ||
                        cls.contains("development")) return true;
            }

            String text = event.getText() == null ? "" :
                    event.getText().toString().toLowerCase(Locale.ROOT);

            if (Prefs.protectApps(this) &&
                    (text.contains("información de la aplicación") ||
                     text.contains("informacion de la aplicacion") ||
                     text.contains("app info") ||
                     text.contains("desinstalar") ||
                     text.contains("uninstall") ||
                     text.contains("inhabilitar") ||
                     text.contains("disable"))) return true;

            if (Prefs.protectInstaller(this) &&
                    (text.contains("instalar apps desconocidas") ||
                     text.contains("install unknown apps") ||
                     text.contains("fuentes desconocidas") ||
                     text.contains("unknown sources"))) return true;

            if (Prefs.protectAccessibility(this) &&
                    (text.contains("accesibilidad") ||
                     text.contains("accessibility"))) return true;

            if (Prefs.protectDeveloper(this) &&
                    (text.contains("opciones de desarrollador") ||
                     text.contains("developer options"))) return true;
        }
        return false;
    }

    @Override public void onInterrupt() {}
}

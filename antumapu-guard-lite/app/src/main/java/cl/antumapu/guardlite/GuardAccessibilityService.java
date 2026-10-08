package cl.antumapu.guardlite;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GuardAccessibilityService extends AccessibilityService {
    private long lastLaunch = 0L;

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null) return;

        String pkg = String.valueOf(event.getPackageName());
        if (pkg.equals(getPackageName())) return;
        if (!Prefs.hasPin(this)) return;
        if (Prefs.isUnlocked(this)) return;

        String cls = event.getClassName() == null ? "" : String.valueOf(event.getClassName());
        String reason = detectReason(pkg, cls);
        if (reason == null) return;

        long now = SystemClock.elapsedRealtime();
        if (now - lastLaunch < 700L) return;
        lastLaunch = now;

        Intent i = new Intent(this, LockActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS |
                Intent.FLAG_ACTIVITY_NO_ANIMATION);
        i.putExtra("reason", reason);
        startActivity(i);
    }

    private String detectReason(String pkg, String cls) {
        String p = norm(pkg);
        String c = norm(cls);

        if (Prefs.protectPlay(this) && p.equals("com.android.vending")) {
            return "Google Play Store";
        }

        boolean installerPkg =
                p.equals("com.android.packageinstaller") ||
                p.equals("com.google.android.packageinstaller") ||
                p.equals("com.samsung.android.packageinstaller") ||
                p.equals("com.sec.android.app.packageinstaller") ||
                p.equals("com.miui.packageinstaller");

        if (installerPkg) {
            if (Prefs.protectInstall(this)) return "Instalación de aplicaciones";
            if (Prefs.protectApps(this)) return "Desinstalación de aplicaciones";
        }

        boolean settingsPkg =
                p.equals("com.android.settings") ||
                p.equals("com.samsung.android.settings") ||
                p.contains("settings");

        if (!settingsPkg) return null;

        if (Prefs.protectInstall(this) &&
                containsAny(c, "manageexternalsources", "unknownsources", "externalapp", "specialappaccess")) {
            return "Instalar aplicaciones desconocidas";
        }

        if (Prefs.protectApps(this) &&
                containsAny(c, "manageapplications", "installedappdetails", "applicationdetails",
                        "appinfo", "applicationsettings", "manageapplicationssettings",
                        "applicationmanager")) {
            return "Aplicaciones y desinstalación";
        }

        if (Prefs.protectAccessibility(this) && c.contains("accessibility")) {
            return "Accesibilidad";
        }

        if (Prefs.protectDeveloper(this) &&
                containsAny(c, "developmentsettings", "developeroptions", "development")) {
            return "Opciones de desarrollador";
        }

        AccessibilityNodeInfo root = getRootInActiveWindow();

        // Samsung Experience / Android 8.1 tablets use a two-pane Settings UI.
        // Many sections keep the generic com.android.settings/.Settings activity,
        // so the right-pane toolbar is the reliable source of the currently open section.
        if (root != null) {
            List<String> rightPane = textsFromViewId(root,
                    "com.android.settings:id/right_pane_toolbar", 8);

            if (Prefs.protectInstall(this) && titleMatches(rightPane,
                    "instalar apps desconocidas", "instalar aplicaciones desconocidas",
                    "fuentes desconocidas", "install unknown apps", "unknown sources")) {
                return "Instalar aplicaciones desconocidas";
            }

            if (Prefs.protectApps(this) && titleMatches(rightPane,
                    "aplicaciones", "apps", "administrador de aplicaciones",
                    "informacion de la aplicacion", "app info", "application manager")) {
                return "Aplicaciones y desinstalación";
            }

            if (Prefs.protectAccessibility(this) && titleMatches(rightPane,
                    "accesibilidad", "accessibility")) {
                return "Accesibilidad";
            }

            if (Prefs.protectDeveloper(this) && titleMatches(rightPane,
                    "opciones de desarrollador", "developer options")) {
                return "Opciones de desarrollador";
            }
        }

        // Fallback for phones and Settings variants that expose a dedicated sub-screen activity.
        if (c.contains("subsettings") || c.contains("dashboard")) {
            List<String> top = firstTexts(root, 10);

            if (Prefs.protectInstall(this) && titleMatches(top,
                    "instalar apps desconocidas", "instalar aplicaciones desconocidas",
                    "fuentes desconocidas", "install unknown apps", "unknown sources")) {
                return "Instalar aplicaciones desconocidas";
            }

            if (Prefs.protectApps(this) && titleMatches(top,
                    "aplicaciones", "apps", "administrador de aplicaciones",
                    "informacion de la aplicacion", "app info", "application manager")) {
                return "Aplicaciones y desinstalación";
            }

            if (Prefs.protectAccessibility(this) && titleMatches(top,
                    "accesibilidad", "accessibility")) {
                return "Accesibilidad";
            }

            if (Prefs.protectDeveloper(this) && titleMatches(top,
                    "opciones de desarrollador", "developer options")) {
                return "Opciones de desarrollador";
            }
        }

        return null;
    }

    private List<String> textsFromViewId(AccessibilityNodeInfo root, String viewId, int max) {
        ArrayList<String> out = new ArrayList<>();
        if (root == null) return out;

        try {
            List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByViewId(viewId);
            if (nodes == null) return out;
            for (AccessibilityNodeInfo node : nodes) {
                collect(node, out, max);
                if (out.size() >= max) break;
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    private boolean titleMatches(List<String> texts, String... terms) {
        int n = Math.min(8, texts.size());
        for (int i = 0; i < n; i++) {
            String t = norm(texts.get(i));
            for (String term : terms) {
                String q = norm(term);
                if (t.equals(q) || t.startsWith(q + " ")) return true;
            }
        }
        return false;
    }

    private List<String> firstTexts(AccessibilityNodeInfo root, int max) {
        ArrayList<String> out = new ArrayList<>();
        collect(root, out, max);
        return out;
    }

    private void collect(AccessibilityNodeInfo node, List<String> out, int max) {
        if (node == null || out.size() >= max) return;
        CharSequence txt = node.getText();
        if (txt != null && txt.length() > 0) out.add(txt.toString());
        CharSequence desc = node.getContentDescription();
        if (desc != null && desc.length() > 0 && out.size() < max) out.add(desc.toString());
        for (int i = 0; i < node.getChildCount() && out.size() < max; i++) {
            collect(node.getChild(i), out, max);
        }
    }

    private boolean containsAny(String value, String... needles) {
        for (String n : needles) if (value.contains(n)) return true;
        return false;
    }

    private String norm(String s) {
        if (s == null) return "";
        String x = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return x.toLowerCase(Locale.ROOT).trim();
    }

    @Override public void onInterrupt() {}
}

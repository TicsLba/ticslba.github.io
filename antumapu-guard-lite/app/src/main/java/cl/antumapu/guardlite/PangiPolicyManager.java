package cl.antumapu.guardlite;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.UserManager;

public final class PangiPolicyManager {
    private PangiPolicyManager() {}

    public static ComponentName admin(Context context) {
        return new ComponentName(context, PangiDeviceAdminReceiver.class);
    }

    public static DevicePolicyManager dpm(Context context) {
        return (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
    }

    public static boolean isDeviceOwner(Context context) {
        DevicePolicyManager dpm = dpm(context);
        return dpm != null && dpm.isDeviceOwnerApp(context.getPackageName());
    }

    public static void applyDefaults(Context context) {
        if (!isDeviceOwner(context)) return;

        // Pangi 3.0 never acts as HOME. This also cleans any preference left by 2.2.
        clearLegacyLauncherOverride(context);

        setSelfUninstallBlocked(context, true);
        setUnknownSourcesBlocked(context, Prefs.blockUnknownSources(context));
        setAppsControlBlocked(context, Prefs.blockAppsControl(context));
        setPlayStoreBlocked(context, Prefs.blockPlayStore(context));
        applyInstitutionalBranding(context);
    }

    public static void clearLegacyLauncherOverride(Context context) {
        DevicePolicyManager dpm = dpm(context);
        if (dpm == null || !isDeviceOwner(context)) return;
        try {
            dpm.clearPackagePersistentPreferredActivities(admin(context), context.getPackageName());
        } catch (Exception ignored) {
        }
    }

    private static void applyInstitutionalBranding(Context context) {
        DevicePolicyManager dpm = dpm(context);
        if (dpm == null || !isDeviceOwner(context)) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                dpm.setOrganizationName(admin(context), "Liceo Bicentenario Antumapu");
            } catch (Exception ignored) {
            }

            try {
                dpm.setDeviceOwnerLockScreenInfo(
                        admin(context),
                        "Liceo Bicentenario Antumapu · Dispositivo institucional · Pangi 3.0");
            } catch (Exception ignored) {
            }
        }
    }

    public static void setSelfUninstallBlocked(Context context, boolean blocked) {
        DevicePolicyManager dpm = dpm(context);
        if (dpm == null || !isDeviceOwner(context)) return;
        try {
            dpm.setUninstallBlocked(admin(context), context.getPackageName(), blocked);
        } catch (Exception ignored) {
        }
    }

    public static void setUnknownSourcesBlocked(Context context, boolean blocked) {
        DevicePolicyManager dpm = dpm(context);
        if (dpm == null || !isDeviceOwner(context)) return;

        try {
            if (blocked) {
                dpm.addUserRestriction(admin(context), UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES);
            } else {
                dpm.clearUserRestriction(admin(context), UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES);
            }
            Prefs.setBlockUnknownSources(context, blocked);
        } catch (Exception ignored) {
        }
    }

    public static void setAppsControlBlocked(Context context, boolean blocked) {
        DevicePolicyManager dpm = dpm(context);
        if (dpm == null || !isDeviceOwner(context)) return;

        try {
            if (blocked) {
                dpm.addUserRestriction(admin(context), UserManager.DISALLOW_APPS_CONTROL);
            } else {
                dpm.clearUserRestriction(admin(context), UserManager.DISALLOW_APPS_CONTROL);
            }
            Prefs.setBlockAppsControl(context, blocked);
        } catch (Exception ignored) {
        }
    }

    public static void setPlayStoreBlocked(Context context, boolean blocked) {
        DevicePolicyManager dpm = dpm(context);
        if (dpm == null || !isDeviceOwner(context)) return;

        try {
            dpm.setApplicationHidden(admin(context), "com.android.vending", blocked);
            Prefs.setBlockPlayStore(context, blocked);
        } catch (Exception ignored) {
        }
    }

    public static String getCurrentHomePackage(Context context) {
        try {
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.addCategory(Intent.CATEGORY_HOME);
            ResolveInfo r = context.getPackageManager().resolveActivity(
                    home, PackageManager.MATCH_DEFAULT_ONLY);
            if (r != null && r.activityInfo != null) {
                return r.activityInfo.packageName;
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    public static boolean isNativeHomeIntact(Context context) {
        String pkg = getCurrentHomePackage(context);
        return pkg.length() > 0 && !context.getPackageName().equals(pkg);
    }
}

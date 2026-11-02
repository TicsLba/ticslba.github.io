package cl.antumapu.guardlite;

import android.Manifest;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.UserManager;
import android.provider.Settings;

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

        clearLegacyLauncherOverride(context);
        setSelfUninstallBlocked(context, true);
        setUnknownSourcesBlocked(context, Prefs.blockUnknownSources(context));
        setAppsControlBlocked(context, Prefs.blockAppsControl(context));
        setPlayStoreBlocked(context, Prefs.blockPlayStore(context));
        applyInstitutionalBranding(context);

        if (Prefs.fleetEnabled(context) && Prefs.consoleConfigured(context)) {
            grantFleetPermissions(context);
            ensureLocationEnabled(context);
            PangiFleetService.start(context);
        }
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
                        "Liceo Bicentenario Antumapu · Dispositivo institucional · Pangi 3.1");
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

    public static void setFleetMonitoring(Context context, boolean enabled) {
        Prefs.setFleetEnabled(context, enabled);

        if (!enabled) {
            PangiFleetService.stop(context);
            return;
        }

        if (!isDeviceOwner(context) || !Prefs.consoleConfigured(context)) {
            Prefs.setFleetEnabled(context, false);
            return;
        }

        grantFleetPermissions(context);
        ensureLocationEnabled(context);
        PangiFleetService.start(context);
    }

    public static void grantFleetPermissions(Context context) {
        if (!isDeviceOwner(context) || Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;

        DevicePolicyManager dpm = dpm(context);
        String pkg = context.getPackageName();

        try {
            dpm.setPermissionGrantState(
                    admin(context), pkg, Manifest.permission.ACCESS_FINE_LOCATION,
                    DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);
            dpm.setPermissionGrantState(
                    admin(context), pkg, Manifest.permission.ACCESS_COARSE_LOCATION,
                    DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                dpm.setPermissionGrantState(
                        admin(context), pkg, Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                        DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);
            }
        } catch (Exception ignored) {
        }
    }

    public static void ensureLocationEnabled(Context context) {
        if (!isDeviceOwner(context)) return;
        DevicePolicyManager dpm = dpm(context);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                dpm.setLocationEnabled(admin(context), true);
            } else {
                dpm.setSecureSetting(
                        admin(context),
                        Settings.Secure.LOCATION_MODE,
                        String.valueOf(Settings.Secure.LOCATION_MODE_HIGH_ACCURACY));
            }
        } catch (Exception ignored) {
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                dpm.addUserRestriction(admin(context), UserManager.DISALLOW_CONFIG_LOCATION);
            } catch (Exception ignored) {
            }
        }
    }

    public static boolean lockDevice(Context context) {
        DevicePolicyManager dpm = dpm(context);
        if (dpm == null || !isDeviceOwner(context)) return false;

        try {
            dpm.lockNow();
            return true;
        } catch (Exception e) {
            return false;
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

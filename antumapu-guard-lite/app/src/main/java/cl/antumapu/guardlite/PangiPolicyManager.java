package cl.antumapu.guardlite;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
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
        setSelfUninstallBlocked(context, true);
        setUnknownSourcesBlocked(context, Prefs.blockUnknownSources(context));
        setAppsControlBlocked(context, Prefs.blockAppsControl(context));
        setPlayStoreBlocked(context, Prefs.blockPlayStore(context));
    }

    public static void setSelfUninstallBlocked(Context context, boolean blocked) {
        DevicePolicyManager dpm = dpm(context);
        if (dpm == null || !isDeviceOwner(context)) return;
        dpm.setUninstallBlocked(admin(context), context.getPackageName(), blocked);
    }

    public static void setUnknownSourcesBlocked(Context context, boolean blocked) {
        DevicePolicyManager dpm = dpm(context);
        if (dpm == null || !isDeviceOwner(context)) return;
        if (blocked) {
            dpm.addUserRestriction(admin(context), UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES);
        } else {
            dpm.clearUserRestriction(admin(context), UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES);
        }
        Prefs.setBlockUnknownSources(context, blocked);
    }

    public static void setAppsControlBlocked(Context context, boolean blocked) {
        DevicePolicyManager dpm = dpm(context);
        if (dpm == null || !isDeviceOwner(context)) return;
        if (blocked) {
            dpm.addUserRestriction(admin(context), UserManager.DISALLOW_APPS_CONTROL);
        } else {
            dpm.clearUserRestriction(admin(context), UserManager.DISALLOW_APPS_CONTROL);
        }
        Prefs.setBlockAppsControl(context, blocked);
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
}

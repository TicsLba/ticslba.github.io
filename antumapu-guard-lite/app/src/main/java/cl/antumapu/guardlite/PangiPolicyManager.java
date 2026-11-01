package cl.antumapu.guardlite;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.UserManager;

import java.util.List;

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

    public static void applyCriticalStartup(Context context) {
        if (!isDeviceOwner(context)) return;
        setSelfUninstallBlocked(context, true);
        setPangiAsHome(context);
    }

    public static void applyDefaults(Context context) {
        if (!isDeviceOwner(context)) return;
        applyCriticalStartup(context);
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

    public static boolean setPangiAsHome(Context context) {
        DevicePolicyManager dpm = dpm(context);
        if (dpm == null || !isDeviceOwner(context)) return false;

        rememberOriginalHome(context);

        try {
            IntentFilter filter = new IntentFilter(Intent.ACTION_MAIN);
            filter.addCategory(Intent.CATEGORY_HOME);
            filter.addCategory(Intent.CATEGORY_DEFAULT);

            dpm.addPersistentPreferredActivity(
                    admin(context),
                    filter,
                    new ComponentName(context, HomeActivity.class));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean restoreOriginalHome(Context context) {
        DevicePolicyManager dpm = dpm(context);
        if (dpm == null || !isDeviceOwner(context)) return false;
        try {
            dpm.clearPackagePersistentPreferredActivities(admin(context), context.getPackageName());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isPangiHome(Context context) {
        try {
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.addCategory(Intent.CATEGORY_HOME);
            ResolveInfo r = context.getPackageManager().resolveActivity(
                    home, PackageManager.MATCH_DEFAULT_ONLY);
            return r != null && r.activityInfo != null &&
                    context.getPackageName().equals(r.activityInfo.packageName) &&
                    HomeActivity.class.getName().equals(r.activityInfo.name);
        } catch (Exception e) {
            return false;
        }
    }

    private static void rememberOriginalHome(Context context) {
        String saved = Prefs.getOriginalHome(context);
        if (saved != null && saved.length() > 0) return;

        ComponentName candidate = findOriginalHome(context);
        if (candidate != null) {
            Prefs.setOriginalHome(context, candidate.flattenToString());
        }
    }

    private static ComponentName findOriginalHome(Context context) {
        PackageManager pm = context.getPackageManager();

        try {
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.addCategory(Intent.CATEGORY_HOME);

            ResolveInfo current = pm.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY);
            if (current != null && current.activityInfo != null &&
                    !context.getPackageName().equals(current.activityInfo.packageName)) {
                return new ComponentName(
                        current.activityInfo.packageName,
                        current.activityInfo.name);
            }

            List<ResolveInfo> all = pm.queryIntentActivities(home, 0);
            ComponentName fallback = null;

            for (ResolveInfo info : all) {
                if (info.activityInfo == null) continue;
                String pkg = info.activityInfo.packageName;
                String name = info.activityInfo.name;
                if (context.getPackageName().equals(pkg)) continue;

                String lower = (pkg + " " + name).toLowerCase();
                if (lower.contains("setupwizard") ||
                        lower.contains("provision") ||
                        lower.contains("resolver")) {
                    continue;
                }

                ComponentName c = new ComponentName(pkg, name);
                boolean system = (info.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
                if (system && lower.contains("launcher")) return c;
                if (fallback == null && system) fallback = c;
            }

            return fallback;
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean launchOriginalHome(Context context) {
        ComponentName target = null;

        try {
            String flat = Prefs.getOriginalHome(context);
            if (flat != null && flat.length() > 0) {
                target = ComponentName.unflattenFromString(flat);
            }

            if (target == null || context.getPackageName().equals(target.getPackageName())) {
                target = findOriginalHome(context);
                if (target != null) Prefs.setOriginalHome(context, target.flattenToString());
            }

            if (target == null) return false;

            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setComponent(target);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            context.startActivity(intent);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

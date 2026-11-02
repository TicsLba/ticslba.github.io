package cl.antumapu.guardlite;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;

        Prefs.lock(context);
        PangiPolicyManager.applyDefaults(context);

        if (Prefs.fleetEnabled(context) && Prefs.consoleConfigured(context)) {
            PangiFleetService.start(context);
        }

        // Pangi never replaces HOME. On Android 8.1/9 only, the institutional
        // presentation may appear after boot; Android 10+ keeps the OEM launcher uninterrupted.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                PangiPolicyManager.isDeviceOwner(context)) {
            try {
                Intent splash = new Intent(context, SplashActivity.class);
                splash.putExtra("from_boot", true);
                splash.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                context.startActivity(splash);
            } catch (Exception ignored) {
            }
        }
    }
}

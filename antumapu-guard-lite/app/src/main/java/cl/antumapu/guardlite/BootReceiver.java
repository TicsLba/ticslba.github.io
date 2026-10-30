package cl.antumapu.guardlite;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        Prefs.lock(context);
        PangiPolicyManager.applyDefaults(context);

        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Intent splash = new Intent(context, SplashActivity.class);
            splash.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_CLEAR_TOP |
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            context.startActivity(splash);
        }
    }
}

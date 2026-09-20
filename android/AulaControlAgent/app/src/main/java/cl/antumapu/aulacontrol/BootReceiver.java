package cl.antumapu.aulacontrol;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.UserManager;

/**
 * Earliest supported boot entry point.
 *
 * Aula Móvil intentionally does not register as HOME. The Device Owner is
 * allowed to bring its gate forward from the background; Direct Boot plus
 * retries cover OEM boot-order races without replacing the manufacturer launcher.
 */
public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i) {
        String action = i == null ? "" : i.getAction();

        if (Managed.owner(c)) {
            launchBootGate(c);
            BootGateScheduler.schedule(c);

            UserManager um = (UserManager)c.getSystemService(Context.USER_SERVICE);
            boolean unlocked = Build.VERSION.SDK_INT < 24 || um == null || um.isUserUnlocked();
            if (unlocked && !Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action)) startAgent(c);
            return;
        }

        if (Managed.profileOwner(c)) {
            if (Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action)) return;
            try {
                if (Core.ready(c)) {
                    if (!SessionState.isActive(c)) SessionState.guestSetup(c);
                    Managed.applyGuest(c);
                }
            } catch (Exception ignored) {}
            startAgent(c);
            try {
                c.startActivity(new Intent(c, SplashActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                                Intent.FLAG_ACTIVITY_SINGLE_TOP));
            } catch (Exception ignored) {}
        }
    }

    private void launchBootGate(Context c) {
        try {
            c.startActivity(new Intent(c, BootGateActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_CLEAR_TOP |
                            Intent.FLAG_ACTIVITY_SINGLE_TOP));
        } catch (Exception ignored) {}
    }

    private void startAgent(Context c) {
        try {
            Intent s = new Intent(c, AgentService.class);
            if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(s); else c.startService(s);
        } catch (Exception ignored) {}
    }
}

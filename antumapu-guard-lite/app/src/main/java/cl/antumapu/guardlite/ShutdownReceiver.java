package cl.antumapu.guardlite;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ShutdownReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_SHUTDOWN.equals(intent.getAction()) ||
                !Prefs.fleetEnabled(context) ||
                !Prefs.consoleConfigured(context)) {
            return;
        }

        final PendingResult pending = goAsync();
        new Thread(() -> {
            try {
                FleetClient.shutdown(context);
            } finally {
                pending.finish();
            }
        }, "PangiShutdown").start();
    }
}

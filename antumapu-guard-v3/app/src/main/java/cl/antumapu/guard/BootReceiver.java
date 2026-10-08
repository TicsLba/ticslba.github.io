package cl.antumapu.guard;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        Prefs.lock(context);
        RootPolicy.ensureAccessibility(context);
        RootPolicy.applyLocked(context, false);
    }
}

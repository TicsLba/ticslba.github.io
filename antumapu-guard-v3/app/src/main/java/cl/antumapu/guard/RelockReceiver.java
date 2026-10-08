package cl.antumapu.guard;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class RelockReceiver extends BroadcastReceiver {
    public static void schedule(Context c) {
        AlarmManager am = (AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(c, RelockReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(c, 14782, i, PendingIntent.FLAG_UPDATE_CURRENT);
        long when = System.currentTimeMillis() + Prefs.unlockSeconds(c) * 1000L;
        if (am != null) am.setExact(AlarmManager.RTC_WAKEUP, when, pi);
    }

    public static void cancel(Context c) {
        AlarmManager am = (AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(c, RelockReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(c, 14782, i, PendingIntent.FLAG_UPDATE_CURRENT);
        if (am != null) am.cancel(pi);
    }

    @Override public void onReceive(Context context, Intent intent) {
        Prefs.lock(context);
        RootPolicy.applyLocked(context, true);
    }
}

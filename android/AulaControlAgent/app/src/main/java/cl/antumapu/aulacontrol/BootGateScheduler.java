package cl.antumapu.aulacontrol;

import android.app.ActivityOptions;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;

/**
 * Brings the institutional gate to the foreground after boot without making
 * Tablet Escolar a HOME application. Android 10+ can block a direct
 * startActivity() from BOOT_COMPLETED; an Activity PendingIntent dispatched by
 * AlarmManager is sent by the system and is therefore the supported fallback.
 */
final class BootGateScheduler {
    private static final int REQ_FAST = 30110;
    private static final int REQ_RETRY = 30111;

    private BootGateScheduler() {}

    static void schedule(Context context) {
        Context app = context.getApplicationContext();
        AlarmManager alarms = (AlarmManager) app.getSystemService(Context.ALARM_SERVICE);
        if (alarms == null) return;

        scheduleOne(app, alarms, REQ_FAST, 450L);
        scheduleOne(app, alarms, REQ_RETRY, 2400L);
    }

    static void cancel(Context context) {
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarms == null) return;
        try { alarms.cancel(pending(context, REQ_FAST)); } catch (Exception ignored) {}
        try { alarms.cancel(pending(context, REQ_RETRY)); } catch (Exception ignored) {}
    }

    private static void scheduleOne(Context c, AlarmManager alarms, int requestCode, long delayMs) {
        PendingIntent pi = pending(c, requestCode);
        long when = SystemClock.elapsedRealtime() + delayMs;
        try {
            if (Build.VERSION.SDK_INT >= 31) {
                if (alarms.canScheduleExactAlarms()) {
                    alarms.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, when, pi);
                } else {
                    alarms.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, when, pi);
                }
            } else if (Build.VERSION.SDK_INT >= 23) {
                alarms.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, when, pi);
            } else {
                alarms.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, when, pi);
            }
        } catch (SecurityException exactDenied) {
            try { alarms.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, when, pi); } catch (Exception ignored) {}
        } catch (Exception ignored) {}
    }

    private static PendingIntent pending(Context c, int requestCode) {
        Intent gate = new Intent(c, SplashActivity.class)
                .setAction("cl.antumapu.aulacontrol.BOOT_GATE." + requestCode)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;

        if (Build.VERSION.SDK_INT >= 35) {
            ActivityOptions options = ActivityOptions.makeBasic();
            options.setPendingIntentCreatorBackgroundActivityStartMode(
                    ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED);
            return PendingIntent.getActivity(c, requestCode, gate, flags, options.toBundle());
        }
        return PendingIntent.getActivity(c, requestCode, gate, flags);
    }
}

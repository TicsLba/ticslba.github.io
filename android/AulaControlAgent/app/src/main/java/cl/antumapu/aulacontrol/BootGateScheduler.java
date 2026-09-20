package cl.antumapu.aulacontrol;

import android.app.ActivityOptions;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;

/**
 * OEM boot-order fallback. The Device Owner normally launches BootGateActivity
 * directly; these short retries recover if a manufacturer temporarily replaces
 * the foreground task while finishing its own boot sequence.
 */
final class BootGateScheduler {
    private static final int REQ_FAST=30110, REQ_RETRY=30111, REQ_LAST=30112;
    private BootGateScheduler(){}

    static void schedule(Context context){
        AlarmManager alarms=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        if(alarms==null)return;
        scheduleOne(context,alarms,REQ_FAST,350L);
        scheduleOne(context,alarms,REQ_RETRY,1450L);
        scheduleOne(context,alarms,REQ_LAST,4200L);
    }

    static void cancel(Context context){
        AlarmManager alarms=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        if(alarms==null)return;
        for(int req:new int[]{REQ_FAST,REQ_RETRY,REQ_LAST})try{alarms.cancel(pending(context,req));}catch(Exception ignored){}
    }

    private static void scheduleOne(Context c,AlarmManager alarms,int requestCode,long delayMs){
        PendingIntent pi=pending(c,requestCode);
        long when=SystemClock.elapsedRealtime()+delayMs;
        try{
            if(Build.VERSION.SDK_INT>=31){
                if(alarms.canScheduleExactAlarms())alarms.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,when,pi);
                else alarms.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,when,pi);
            }else if(Build.VERSION.SDK_INT>=23)alarms.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,when,pi);
            else alarms.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,when,pi);
        }catch(SecurityException e){
            try{alarms.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,when,pi);}catch(Exception ignored){}
        }catch(Exception ignored){}
    }

    private static PendingIntent pending(Context c,int requestCode){
        Intent gate=new Intent(c,BootGateActivity.class)
                .setAction("cl.antumapu.aulacontrol.BOOT_GATE."+requestCode)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int flags=PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE;
        if(Build.VERSION.SDK_INT>=35){
            ActivityOptions options=ActivityOptions.makeBasic();
            options.setPendingIntentCreatorBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED);
            return PendingIntent.getActivity(c,requestCode,gate,flags,options.toBundle());
        }
        return PendingIntent.getActivity(c,requestCode,gate,flags);
    }
}

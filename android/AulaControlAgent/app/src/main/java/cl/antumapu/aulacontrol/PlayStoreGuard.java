package cl.antumapu.aulacontrol;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.content.pm.PackageManager;

final class PlayStoreGuard {
    static final String PACKAGE = "com.android.vending";
    static final long ACCESS_WINDOW_MS = 5 * 60 * 1000L;
    private static final String PREF_UNTIL = "play_store_until";
    private static final int RELOCK_REQUEST = 5401;

    private PlayStoreGuard(){}

    static boolean available(Context c){
        try{
            if(Build.VERSION.SDK_INT>=33)c.getPackageManager().getPackageInfo(PACKAGE,PackageManager.PackageInfoFlags.of(PackageManager.MATCH_UNINSTALLED_PACKAGES));
            else c.getPackageManager().getPackageInfo(PACKAGE,PackageManager.MATCH_UNINSTALLED_PACKAGES);
            return true;
        }catch(Exception e){return false;}
    }

    static boolean lock(Context c){
        if(!Managed.managed(c))return false;
        DevicePolicyManager d=Managed.dpm(c);ComponentName a=Managed.admin(c);
        boolean changed=false;
        try{d.setPackagesSuspended(a,new String[]{PACKAGE},true);changed=true;}catch(Exception ignored){}
        try{changed=d.setApplicationHidden(a,PACKAGE,true)||changed;}catch(Exception ignored){}
        Core.sp(c).edit().remove(PREF_UNTIL).apply();
        return changed;
    }

    static boolean unlock(Context c){
        if(!Managed.managed(c))return false;
        DevicePolicyManager d=Managed.dpm(c);ComponentName a=Managed.admin(c);
        boolean changed=false;
        try{changed=d.setApplicationHidden(a,PACKAGE,false)||changed;}catch(Exception ignored){}
        try{d.setPackagesSuspended(a,new String[]{PACKAGE},false);changed=true;}catch(Exception ignored){}
        return changed;
    }

    static boolean unlockForAdmin(Context c){
        if(!Managed.owner(c)||!Core.adminMode(c)||!available(c))return false;
        if(!unlock(c))return false;
        long until=System.currentTimeMillis()+ACCESS_WINDOW_MS;
        Core.sp(c).edit().putLong(PREF_UNTIL,until).apply();
        scheduleRelock(c,ACCESS_WINDOW_MS);
        return true;
    }

    static long remaining(Context c){
        return Math.max(0,Core.sp(c).getLong(PREF_UNTIL,0)-System.currentTimeMillis());
    }

    static void relockIfExpired(Context c){
        long until=Core.sp(c).getLong(PREF_UNTIL,0);
        if(until>0&&System.currentTimeMillis()>=until)lock(c);
    }

    static boolean launch(Context c){
        if(!Managed.owner(c)||!Core.adminMode(c))return false;
        try{
            Intent i=c.getPackageManager().getLaunchIntentForPackage(PACKAGE);
            if(i==null){
                i=new Intent(Intent.ACTION_VIEW,Uri.parse("market://details?id="+PACKAGE)).setPackage(PACKAGE);
            }
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(i);
            return true;
        }catch(Exception e){return false;}
    }

    private static void scheduleRelock(Context c,long delay){
        try{
            Intent i=new Intent(c,PlayRelockReceiver.class);
            PendingIntent p=PendingIntent.getBroadcast(c,RELOCK_REQUEST,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            AlarmManager a=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
            if(a==null)return;
            long when=SystemClock.elapsedRealtime()+delay;
            if(Build.VERSION.SDK_INT>=23)a.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,when,p);
            else a.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,when,p);
        }catch(Exception ignored){}
    }
}

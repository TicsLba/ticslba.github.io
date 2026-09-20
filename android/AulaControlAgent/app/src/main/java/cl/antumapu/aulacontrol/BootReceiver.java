package cl.antumapu.aulacontrol;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.UserManager;

/**
 * Priority boot entry point.
 *
 * Tablet Escolar is never HOME. A Device Owner is exempt from Android's normal
 * background-activity-start restriction, so the institutional BootGuard is
 * launched explicitly at LOCKED_BOOT_COMPLETED and reinforced at later boot
 * stages. A PendingIntent alarm provides an OEM fallback.
 */
public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent i){
        String action=i==null?"":i.getAction();
        UserManager um=(UserManager)c.getSystemService(Context.USER_SERVICE);
        boolean unlocked=um==null||um.isUserUnlocked();

        if(Managed.owner(c)){
            // Do the blocking policy first; do not start the service before the UI.
            Managed.applyBootOwner(c);
            launchOwnerGuard(c);
            BootGateScheduler.schedule(c);

            if(unlocked){
                try{Core.adminMode(c,false);}catch(Exception ignored){}
                try{SessionState.ownerGate(c);}catch(Exception ignored){}
                try{Managed.applyOwner(c);}catch(Exception ignored){}
                startAgent(c);
                cleanupOldSessions(c);
            }
            return;
        }

        if(Managed.profileOwner(c)){
            if(Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action)||!unlocked)return;
            try{
                if(!SessionState.isActive(c))SessionState.guestSetup(c);
                Managed.applyGuest(c);
                c.startActivity(new Intent(c,SplashActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|
                                Intent.FLAG_ACTIVITY_CLEAR_TOP|
                                Intent.FLAG_ACTIVITY_SINGLE_TOP));
            }catch(Exception ignored){}
        }
    }

    private void launchOwnerGuard(Context c){
        try{
            c.startActivity(new Intent(c,BootGuardActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|
                            Intent.FLAG_ACTIVITY_CLEAR_TOP|
                            Intent.FLAG_ACTIVITY_SINGLE_TOP|
                            Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS));
        }catch(Exception ignored){}
    }

    private void startAgent(Context c){
        try{
            Intent s=new Intent(c,AgentService.class);
            if(Build.VERSION.SDK_INT>=26)c.startForegroundService(s);else c.startService(s);
        }catch(Exception ignored){}
    }

    private void cleanupOldSessions(Context c){
        final PendingResult result=goAsync();
        new Thread(()->{
            try{SessionUsers.cleanupSecondaryUsers(c);}catch(Exception ignored){}
            finally{result.finish();}
        },"TabletEscolarBootCleanup").start();
    }
}

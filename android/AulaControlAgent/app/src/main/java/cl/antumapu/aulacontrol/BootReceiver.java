package cl.antumapu.aulacontrol;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.UserManager;

/**
 * Reasserts institutional protection on every boot stage.
 *
 * The persistent owner access guard is the synchronous first line of defence.
 * LOCKED_BOOT_COMPLETED is handled without reading credential-protected prefs.
 */
public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent i){
        String action=i==null?"":i.getAction();
        UserManager um=(UserManager)c.getSystemService(Context.USER_SERVICE);
        boolean unlocked=um==null||um.isUserUnlocked();

        if(Managed.owner(c)){
            // Safe in Direct Boot: no credential-encrypted app data is read.
            Managed.applyBootOwner(c);
            launchOwnerGuard(c);

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

            // A reboot ends a student/teacher session. Never restore it as active.
            try{
                Core.supervisionStarted(c,false);
                SessionState.set(c,SessionState.State.CLOSING);
                Managed.applyGuest(c);
                c.startActivity(new Intent(c,MainActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|
                                Intent.FLAG_ACTIVITY_CLEAR_TOP|
                                Intent.FLAG_ACTIVITY_SINGLE_TOP));
            }catch(Exception ignored){}

            final PendingResult result=goAsync();
            new Thread(()->{
                try{SessionUsers.logoutGuest(c);}catch(Exception ignored){}
                finally{result.finish();}
            },"TabletEscolarBootGuestClose").start();
        }
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

    private void launchOwnerGuard(Context c){
        try{
            c.startActivity(new Intent(c,HomeGateActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|
                            Intent.FLAG_ACTIVITY_CLEAR_TOP|
                            Intent.FLAG_ACTIVITY_SINGLE_TOP));
        }catch(Exception ignored){}
    }
}
